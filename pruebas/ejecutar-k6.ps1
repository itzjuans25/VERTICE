[CmdletBinding()]
param(
    [ValidateSet('carga', 'estres')][string]$Modo = 'carga',
    [string]$Resultado = '',
    [ValidateRange(4, 16)][int]$VusEstres = 8,
    [string]$PerfilAuth = ''
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
$subastasPort = if ($config.SUBASTAS_PORT) { $config.SUBASTAS_PORT } else { '9094' }
$env:SUBASTAS_URL = "http://127.0.0.1:$subastasPort"
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

function LeerPerfil([hashtable]$Cabeceras) {
    $lecturas = @{}
    foreach ($ruta in 'save','login','renovar','cambiar-clave') {
        foreach ($fase in 'consulta','hash','espera_hash','escritura') {
            $base = "$env:AUTH_URL/actuator/metrics/techstore.auth.fase"
            $medida = Invoke-RestMethod "${base}?tag=ruta:$ruta&tag=fase:$fase" -Headers $Cabeceras -SkipHttpErrorCheck -StatusCodeVariable http
            if ($http -ne 404 -and $http -ne 200) { throw "Actuator métricas respondió HTTP $http" }
            if ($http -eq 200) {
                $series = @{}
                foreach ($s in $medida.measurements) { $series[$s.statistic] = $s.value }
                $lecturas["$ruta/$fase"] = $series
            }
        }
    }
    return $lecturas
}
try {
    & $psql @pgArgs -tAc "UPDATE usuario SET role = 'ADMIN' WHERE id = $adminId" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar la cuenta temporal de k6.' }
    New-Item -ItemType Directory -Path (Split-Path $env:RESULTADO) -Force | Out-Null
    if ($PerfilAuth) {
        $datos = @{email=$env:K6_ADMIN_USER; password=$env:K6_ADMIN_PASSWORD} | ConvertTo-Json
        $sesion = Invoke-RestMethod "$env:AUTH_URL/api/ecommerce/usuario/login" -Method Post -ContentType 'application/json' -Body $datos
        $cabeceras = @{Authorization="Bearer $($sesion.token)"}
        $antes = LeerPerfil $cabeceras
        if (-not $antes.Count) { throw 'Activa AUTH_PROFILE_ENABLED y expón metrics en el perfil local.' }
    }
    & $k6 run --quiet 'pruebas/carga.js'
    $code = $LASTEXITCODE
    if ($PerfilAuth) {
        $despues = LeerPerfil $cabeceras
        $filas = foreach ($clave in ($despues.Keys | Sort-Object)) {
            $n = $despues[$clave].COUNT - $antes[$clave].COUNT
            if ($n -gt 0) {
                $ruta,$fase = $clave -split '/'
                $total = 1000 * ($despues[$clave].TOTAL_TIME - $antes[$clave].TOTAL_TIME)
                [ordered]@{ruta=$ruta;fase=$fase;muestras=$n;mediaMs=[math]::Round($total/$n,2);
                            maximoVentanaMicrometerMs=[math]::Round(1000*$despues[$clave].MAX,2)}
            }
        }
        [ordered]@{resultado=$env:RESULTADO;fases=@($filas)} | ConvertTo-Json -Depth 6 |
            Set-Content -LiteralPath $PerfilAuth -Encoding UTF8
    }
} finally {
    & $psql @pgArgs -tAc "DELETE FROM usuario WHERE id = $adminId" | Out-Null
    Remove-Item Env:PGPASSWORD,Env:K6_ADMIN_PASSWORD,Env:NOTIFICACIONES_API_KEY -ErrorAction SilentlyContinue
}
exit $code
