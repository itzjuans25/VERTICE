$ecommerceHome = Join-Path $env:LOCALAPPDATA 'EcommerceDev'
$statePath = Join-Path $ecommerceHome 'procesos.json'
$pgData = Join-Path $ecommerceHome 'postgres-data'

function Read-EcommerceEnv([string]$Path) {
    $config = @{}
    Get-Content -LiteralPath $Path | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z_0-9]*)\s*=\s*(.*?)\s*$') {
            $config[$Matches[1]] = $Matches[2]
        }
    }
    return $config
}

function New-EcommerceSecret {
    $bytes = New-Object byte[] 36
    $random = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $random.GetBytes($bytes) } finally { $random.Dispose() }
    return [Convert]::ToBase64String($bytes)
}

function Get-EcommerceTool([string]$Name, [string[]]$Candidates, [string]$Package) {
    $onPath = Get-Command $Name -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($onPath) { return $onPath.Source }
    foreach ($candidate in $Candidates) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) { return $candidate }
    }
    throw "Falta $Package ($Name). Consulta Instrucciones de arranque.md."
}

function Wait-EcommerceTcp([int]$Port) {
    $until = (Get-Date).AddSeconds(75)
    do {
        $client = New-Object Net.Sockets.TcpClient
        try {
            if ($client.ConnectAsync('127.0.0.1', $Port).Wait(350) -and $client.Connected) { return }
        } catch {
            Start-Sleep -Milliseconds 300
        } finally {
            $client.Dispose()
        }
    } while ((Get-Date) -lt $until)
    throw "El puerto $Port no abrio. Revisa los logs en $ecommerceHome."
}

function Wait-EcommerceHttp([string]$Name, [string]$Url, [int]$Seconds = 180) {
    $until = (Get-Date).AddSeconds($Seconds)
    do {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 4
            if ($response.StatusCode -eq 200) {
                Write-Host "$Name OK  $Url"
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    } while ((Get-Date) -lt $until)
    throw "$Name no respondio en $Url. Revisa los logs en $ecommerceHome."
}

function Stop-EcommerceProcesses {
    if (-not (Test-Path -LiteralPath $statePath)) { return }
    $processes = @(Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json)
    [array]::Reverse($processes)
    foreach ($item in $processes) {
        $process = Get-Process -Id $item.Id -ErrorAction SilentlyContinue
        if (-not $process -or $process.StartTime.ToUniversalTime().Ticks.ToString() -ne $item.Started) {
            continue
        }
        if ($item.Name -eq 'postgres' -and (Test-Path -LiteralPath $item.Control)) {
            & $item.Control -D $pgData -w stop -m fast
            if ($LASTEXITCODE -eq 0) { continue }
        }
        if ($item.Name -eq 'rabbitmq' -and (Test-Path -LiteralPath $item.Control)) {
            & $item.Control -n ecommerce_dev stop
            if ($LASTEXITCODE -eq 0) { continue }
        }
        & taskkill.exe /PID $item.Id /T /F | Out-Null
    }
    Remove-Item -LiteralPath $statePath
}
