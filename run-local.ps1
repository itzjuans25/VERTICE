[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('auth', 'catalogo', 'notificaciones', 'subastas', 'frontend')]
    [string]$Service,
    [string]$EnvFile = '.env',
    [switch]$SkipInfrastructure,
    [switch]$Jar
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (-not (Test-Path -LiteralPath $EnvFile -PathType Leaf)) {
    throw "No existe $EnvFile. Copia .env.example y configura las credenciales locales."
}

# Carga la configuracion local en el proceso actual.
Get-Content -LiteralPath $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
        $parts = $line -split '=', 2
        Set-Item -Path ("env:" + $parts[0].Trim()) -Value $parts[1].Trim()
    }
}

$defaults = @{
    POSTGRES_PORT = '5432'
    POSTGRES_DB = 'ecommerce'
    POSTGRES_USER = 'postgres'
    POSTGRES_PASSWORD = 'postgres'
    RABBITMQ_PORT = '5672'
    RABBITMQ_USER = 'ecommerce'
    RABBITMQ_PASSWORD = 'ecommerce'
    AUTH_PORT = '9090'
    CATALOGO_PORT = '9092'
    NOTIFICACIONES_PORT = '9093'
    SUBASTAS_PORT = '9094'
    FRONTEND_PORT = '3000'
    MAILPIT_SMTP_PORT = '1025'
}
foreach ($name in $defaults.Keys) {
    if (-not [Environment]::GetEnvironmentVariable($name)) {
        Set-Item -Path "env:$name" -Value $defaults[$name]
    }
}

if ($Service -eq 'frontend') {
    $env:PORT = $env:FRONTEND_PORT
    if (-not $env:REACT_APP_AUTH_URL) {
        $env:REACT_APP_AUTH_URL = "http://localhost:$($env:AUTH_PORT)"
    }
    if (-not $env:REACT_APP_CATALOGO_URL) {
        $env:REACT_APP_CATALOGO_URL = "http://localhost:$($env:CATALOGO_PORT)"
    }
    if (-not $env:REACT_APP_SUBASTAS_URL) {
        $env:REACT_APP_SUBASTAS_URL = "http://localhost:$($env:SUBASTAS_PORT)"
    }
    if (-not $env:REACT_APP_NOTIFICACIONES_URL) {
        $env:REACT_APP_NOTIFICACIONES_URL = "http://localhost:$($env:NOTIFICACIONES_PORT)"
    }
    Push-Location frontend
    try {
        & npm.cmd start
        exit $LASTEXITCODE
    } finally {
        Pop-Location
    }
}

if (-not $SkipInfrastructure) {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker no esta disponible. Si PostgreSQL, RabbitMQ y Mailpit ya estan activos, usa -SkipInfrastructure.'
    }
    Write-Host 'Iniciando PostgreSQL, RabbitMQ y Mailpit...' -ForegroundColor Cyan
    & docker compose --env-file $EnvFile up -d --wait postgres rabbitmq mailpit
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

# Traduce las variables del compose a las que espera cada servicio.
$env:DB_URL = "jdbc:postgresql://localhost:$($env:POSTGRES_PORT)/$($env:POSTGRES_DB)"
$env:DB_USERNAME = $env:POSTGRES_USER
$env:DB_PASSWORD = $env:POSTGRES_PASSWORD
$env:RABBITMQ_HOST = 'localhost'
$env:RABBITMQ_USERNAME = $env:RABBITMQ_USER
$env:AUTH_SERVICE_URL = "http://localhost:$($env:AUTH_PORT)"
if (-not $env:CORS_ALLOWED_ORIGINS) {
    $env:CORS_ALLOWED_ORIGINS = "http://localhost:$($env:FRONTEND_PORT)"
}
if (-not $env:MAIL_HOST) { $env:MAIL_HOST = 'localhost' }
if (-not $env:MAIL_PORT) { $env:MAIL_PORT = $env:MAILPIT_SMTP_PORT }

$services = @{
    auth = @{ Path = 'auth'; Port = $env:AUTH_PORT }
    catalogo = @{ Path = 'catalogo'; Port = $env:CATALOGO_PORT }
    notificaciones = @{ Path = 'notificaciones'; Port = $env:NOTIFICACIONES_PORT }
    subastas = @{ Path = 'subastas'; Port = $env:SUBASTAS_PORT }
}

$env:SERVER_PORT = $services[$Service].Port
Push-Location $services[$Service].Path
try {
    if ($Jar) {
        $artifact = Get-ChildItem -Path target -Filter "$Service-*.jar" | Select-Object -First 1
        if (-not $artifact) { throw "No se encontro el JAR de $Service." }
        $jvmArgs = if ($Service -eq 'auth') {
            @('-Xms96m', '-Xmx448m', '-XX:ActiveProcessorCount=4')
        } else {
            @('-Xms32m', '-Xmx256m', '-XX:ActiveProcessorCount=2')
        }
        & java.exe @jvmArgs -jar $artifact.FullName
    } else {
        Write-Host "Ejecutando $Service con Maven..." -ForegroundColor Green
        & .\mvnw.cmd spring-boot:run
    }
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
