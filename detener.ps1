[CmdletBinding()]
param([switch]$Docker)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
Write-Host "Remata | Compra directa y subastas"
. (Join-Path $PSScriptRoot 'scripts/windows-local.ps1')
if ($Docker) {
    & docker compose down
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose no pudo detener los servicios.' }
} else {
    Stop-EcommerceProcesses
}
Write-Host 'Proyecto detenido. Se conservan los datos.'
