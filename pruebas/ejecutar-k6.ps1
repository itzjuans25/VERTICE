[CmdletBinding()]
param(
    [ValidateSet('carga', 'estres')][string]$Modo = 'carga',
    [string]$Resultado = '',
    [ValidateRange(4, 16)][int]$VusEstres = 8
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
Set-Location $root
. (Join-Path $root 'scripts/windows-local.ps1')
$config = Read-EcommerceEnv '.env.native'
$k6 = Get-EcommerceTool 'k6.exe' @(Get-ChildItem "$ecommerceHome/k6" -Recurse -Filter 'k6.exe' -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty FullName) 'Grafana k6'
$psql = Get-EcommerceTool 'psql.exe' @(Get-ChildItem "$env:ProgramFiles/PostgreSQL/*/bin/psql.exe" -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty FullName) 'PostgreSQL'
$env:AUTH_URL = "http://127.0.0.1:$($config.AUTH_PORT)"
$env:CATALOGO_URL = "http://127.0.0.1:$($config.CATALOGO_PORT)"
$env:NOTIFICACIONES_URL = "http://127.0.0.1:$($config.NOTIFICACIONES_PORT)"
$env:NOTIFICACIONES_API_KEY = $config.NOTIFICACIONES_API_KEY
$env:MODO = $Modo
$env:K6_VUS_ESTRES = [string]$VusEstres
$env:K6_PREFIJO = 'k6-' + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$env:RESULTADO = if ($Resultado) { $Resultado } else { "resultados/$Modo.json" }
$env:K6_ADMIN_USER = "k6-admin-$([guid]::NewGuid().ToString('N'))@example.test"
$env:K6_ADMIN_PASSWORD = New-EcommerceSecret
$body = @{ nombre = 'Administrador temporal k6'; email = $env:K6_ADMIN_USER; password = $env:K6_ADMIN_PASSWORD }
$usuario = Invoke-RestMethod -Uri "$env:AUTH_URL/api/ecommerce/usuario/save" -Method Post -ContentType 'application/json' -Body ($body | ConvertTo-Json)
$adminId = [long]$usuario.id
$env:PGPASSWORD = $config.POSTGRES_PASSWORD
$pgArgs = @('-X', '-w', '-h', '127.0.0.1', '-p', $config.POSTGRES_PORT, '-U', $config.POSTGRES_USER, '-d', $config.POSTGRES_DB, '-v', 'ON_ERROR_STOP=1')
try {
    & $psql @pgArgs -tAc "UPDATE usuario SET role = 'ADMIN' WHERE id = $adminId" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar la cuenta temporal de k6.' }
    New-Item -ItemType Directory -Path (Split-Path $env:RESULTADO) -Force | Out-Null
    & $k6 run --quiet 'pruebas/carga.js'
    $code = $LASTEXITCODE
} finally {
    & $psql @pgArgs -tAc "DELETE FROM usuario WHERE id = $adminId" | Out-Null
    Remove-Item Env:PGPASSWORD,Env:K6_ADMIN_PASSWORD,Env:NOTIFICACIONES_API_KEY -ErrorAction SilentlyContinue
}
exit $code
