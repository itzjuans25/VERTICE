[CmdletBinding()]
param([switch]$Docker, [switch]$SinNavegador, [switch]$Recompilar)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
Write-Host "VÉRTICE | Piezas singulares y subastas curadas"
. (Join-Path $PSScriptRoot 'scripts/windows-local.ps1')

if ($Docker) {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Falta Docker Desktop con Compose.' }
    if (-not (Test-Path '.env')) { throw 'Copia .env.example como .env y configura las credenciales.' }
    & docker compose up -d --build --wait
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose no pudo iniciar los servicios. Ejecuta docker compose logs.' }
    $config = Read-EcommerceEnv '.env'
} else {
    New-Item -ItemType Directory -Path $ecommerceHome -Force | Out-Null
    if (-not (Test-Path '.env.native')) {
        $config = Read-EcommerceEnv '.env.example'
        $config.POSTGRES_USER = 'ecommerce_dev'
        $config.POSTGRES_PORT = '55432'
        $config.POSTGRES_PASSWORD = New-EcommerceSecret
        $config.RABBITMQ_PORT = '5673'
        $config.RABBITMQ_USER = 'guest'
        $config.RABBITMQ_PASSWORD = 'guest'
        $config.JWT_SECRET = New-EcommerceSecret
        $config.NOTIFICACIONES_API_KEY = New-EcommerceSecret
        $config.GetEnumerator() | Sort-Object Key | ForEach-Object { "$($_.Key)=$($_.Value)" } |
            Set-Content -LiteralPath '.env.native' -Encoding UTF8
    }
    $config = Read-EcommerceEnv '.env.native'
    if (-not $config.NOTIFICACIONES_API_KEY) {
        Add-Content -LiteralPath '.env.native' -Value "NOTIFICACIONES_API_KEY=$(New-EcommerceSecret)"
    }

    $node = Get-EcommerceTool 'node.exe' @(Get-ChildItem "$ecommerceHome/node-*/node.exe" -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty FullName) 'Node.js 22'
    $env:PATH = (Split-Path $node) + ';' + $env:PATH
    $java = Get-EcommerceTool 'java.exe' @("$env:ProgramFiles/Java/jdk-17/bin/java.exe") 'JDK 17'
    $env:PATH = (Split-Path $java) + ';' + $env:PATH
    $postgres = Get-EcommerceTool 'postgres.exe' @(Get-ChildItem "$env:ProgramFiles/PostgreSQL/*/bin/postgres.exe" -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty FullName) 'PostgreSQL'
    $rabbit = Get-EcommerceTool 'rabbitmq-server.bat' @(Get-ChildItem "$env:ProgramFiles/RabbitMQ Server/*/sbin/rabbitmq-server.bat" -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty FullName) 'RabbitMQ con Erlang OTP'
    $mailpit = Get-EcommerceTool 'mailpit.exe' @("$ecommerceHome/mailpit.exe") 'Mailpit'
    $pgBin = Split-Path $postgres
    if (-not $env:ERLANG_HOME) { $env:ERLANG_HOME = Join-Path $env:ProgramFiles 'Erlang OTP' }
    if (-not (Test-Path (Join-Path $env:ERLANG_HOME 'bin/erl.exe'))) { throw 'Falta Erlang OTP o ERLANG_HOME.' }

    Stop-EcommerceProcesses
    $services = [ordered]@{ auth = 'auth'; catalogo = 'catalogo'; notificaciones = 'notificaciones'; subastas = 'subastas' }
    foreach ($service in $services.Keys) {
        $location = $services[$service]
        $jar = Get-ChildItem "$location/target/$service-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
        $recent = Get-ChildItem "$location/src/main" -Recurse -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($Recompilar -or -not $jar -or $recent.LastWriteTime -gt $jar.LastWriteTime -or
                (Get-Item "$location/pom.xml").LastWriteTime -gt $jar.LastWriteTime) {
            Push-Location $location
            try {
                & .\mvnw.cmd -B -ntp '-DskipTests' package
                if ($LASTEXITCODE -ne 0) { throw "Error compilando $service." }
            } finally { Pop-Location }
        }
    }
    if (-not (Test-Path 'frontend/node_modules/.bin/react-scripts.cmd')) {
        $npm = Get-EcommerceNpm
        Push-Location frontend
        try {
            & $npm ci --no-audit --no-fund
            if ($LASTEXITCODE -ne 0) { throw 'npm ci fallo.' }
        } finally { Pop-Location }
    }

    $processes = [Collections.Generic.List[object]]::new()
    function Start-EcommerceProcess($Name, $File, $Arguments, $Directory, $Control = '') {
        $process = Start-Process -FilePath $File -ArgumentList $Arguments -WorkingDirectory $Directory -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $ecommerceHome "$Name.log") `
            -RedirectStandardError (Join-Path $ecommerceHome "$Name.err.log")
        $processes.Add(@{ Name=$Name; Id=$process.Id; Started=$process.StartTime.ToUniversalTime().Ticks.ToString(); Control=$Control })
        ConvertTo-Json -InputObject @($processes.ToArray()) | Set-Content -LiteralPath $statePath
    }

    try {
        if (-not (Test-Path (Join-Path $pgData 'PG_VERSION'))) {
            $pwFile = Join-Path $ecommerceHome 'init-password.tmp'
            [IO.File]::WriteAllText($pwFile, $config.POSTGRES_PASSWORD)
            try {
                & "$pgBin/initdb.exe" -D $pgData -U $config.POSTGRES_USER -A scram-sha-256 "--pwfile=$pwFile" --encoding=UTF8 --locale=C
                if ($LASTEXITCODE -ne 0) { throw 'No fue posible inicializar PostgreSQL.' }
            } finally { Remove-Item -LiteralPath $pwFile -ErrorAction SilentlyContinue }
        }
        Start-EcommerceProcess postgres $postgres @('-D', ('"' + $pgData + '"'), '-p', $config.POSTGRES_PORT, '-h', '127.0.0.1') $ecommerceHome "$pgBin/pg_ctl.exe"
        $deadline = (Get-Date).AddSeconds(75)
        do {
            & "$pgBin/pg_isready.exe" -q -h 127.0.0.1 -p $config.POSTGRES_PORT -t 2
            if ($LASTEXITCODE -eq 0) { break }
            Start-Sleep -Milliseconds 500
        } while ((Get-Date) -lt $deadline)
        if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL no acepto conexiones a tiempo.' }
        $env:PGPASSWORD = $config.POSTGRES_PASSWORD
        try {
            $sql = 'SELECT datname FROM pg_database'
            $databases = & "$pgBin/psql.exe" -X -w -h 127.0.0.1 -p $config.POSTGRES_PORT -U $config.POSTGRES_USER -d postgres -tAc $sql
            if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL no acepto las credenciales de .env.native.' }
            if ($databases -notcontains $config.POSTGRES_DB) {
                & "$pgBin/createdb.exe" -w -h 127.0.0.1 -p $config.POSTGRES_PORT -U $config.POSTGRES_USER $config.POSTGRES_DB
                if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la base ecommerce.' }
            }
        } finally { Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue }

        $env:RABBITMQ_BASE = Join-Path $ecommerceHome 'rabbitmq'
        $env:RABBITMQ_NODENAME = 'ecommerce_dev'
        $env:RABBITMQ_NODE_PORT = $config.RABBITMQ_PORT
        $env:RABBITMQ_NODE_IP_ADDRESS = '127.0.0.1'
        $env:RABBITMQ_DIST_PORT = [string]([int]$config.RABBITMQ_PORT + 20000)
        $env:RABBITMQ_DEFAULT_USER = $config.RABBITMQ_USER
        $env:RABBITMQ_DEFAULT_PASS = $config.RABBITMQ_PASSWORD
        Start-EcommerceProcess rabbitmq $env:ComSpec @('/d', '/s', '/c', ('""' + $rabbit + '""')) $ecommerceHome (Join-Path (Split-Path $rabbit) 'rabbitmqctl.bat')
        Start-EcommerceProcess mailpit $mailpit @('--listen', "127.0.0.1:$($config.MAILPIT_UI_PORT)", '--smtp', "127.0.0.1:$($config.MAILPIT_SMTP_PORT)", '--database', 'mailpit.db') $ecommerceHome
        Wait-EcommerceTcp $config.RABBITMQ_PORT
        Wait-EcommerceTcp $config.MAILPIT_SMTP_PORT

        $env:SERVER_ADDRESS = '127.0.0.1'
        $env:BROWSER = 'none'
        $env:HOST = 'localhost'
        $powerShell = (Get-Process -Id $PID).Path
        foreach ($service in $services.Keys) {
            Start-EcommerceProcess $service $powerShell @('-NoProfile','-ExecutionPolicy','Bypass','-File','run-local.ps1',$service,'-SkipInfrastructure','-EnvFile','.env.native','-Jar') $PSScriptRoot
        }
        Start-EcommerceProcess frontend $powerShell @('-NoProfile','-ExecutionPolicy','Bypass','-File','run-local.ps1','frontend','-EnvFile','.env.native') $PSScriptRoot
    } catch {
        Stop-EcommerceProcesses
        throw
    }
}

$urls = [ordered]@{
    Tienda = "http://localhost:$($config.FRONTEND_PORT)"
    Mailpit = "http://localhost:$($config.MAILPIT_UI_PORT)"
    Auth = "http://localhost:$($config.AUTH_PORT)"
    Catalogo = "http://localhost:$($config.CATALOGO_PORT)"
    Notificaciones = "http://localhost:$($config.NOTIFICACIONES_PORT)"
    Subastas = "http://localhost:$(if ($config.SUBASTAS_PORT) { $config.SUBASTAS_PORT } else { '9094' })"
}
Wait-EcommerceHttp Tienda $urls.Tienda 240
Wait-EcommerceHttp Mailpit "$($urls.Mailpit)/api/v1/messages"
foreach ($service in 'Auth','Catalogo','Notificaciones','Subastas') {
    Wait-EcommerceHttp $service "$($urls[$service])/actuator/health"
    Wait-EcommerceHttp "Swagger $service" "$($urls[$service])/v3/api-docs"
}
if (-not $SinNavegador) {
    Start-Process $urls.Tienda
    Start-Process $urls.Mailpit
    foreach ($service in 'Auth','Catalogo','Notificaciones','Subastas') {
        Start-Process "$($urls[$service])/swagger-ui.html"
    }
    $pgAdmin = Get-ChildItem "$env:ProgramFiles/PostgreSQL/*/pgAdmin 4/runtime/pgAdmin4.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($pgAdmin) { Start-Process -FilePath $pgAdmin.FullName }
}
