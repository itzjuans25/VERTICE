[CmdletBinding()]
param(
    [string]$NewmanPath = '',
    [string]$ReporteHtml = 'postman/reporte-local.html',
    [string]$Resumen = 'resultados/postman-local.json'
)

$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot)
. .\scripts\windows-local.ps1
$c = Read-EcommerceEnv '.env.native'
if (-not $NewmanPath) {
    $portableNode = Get-ChildItem "$ecommerceHome/node-*-win-x64/node.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($portableNode) { $env:PATH = "$($portableNode.DirectoryName);$env:PATH" }
    $NewmanPath = Get-EcommerceTool 'newman.cmd' @("$ecommerceHome/postman-tools/node_modules/.bin/newman.cmd") 'Newman y newman-reporter-htmlextra'
}
$psql = Get-EcommerceTool 'psql.exe' @(Get-ChildItem "$env:ProgramFiles/PostgreSQL/*/bin/psql.exe" |
    Select-Object -ExpandProperty FullName) 'PostgreSQL'
$pgArgs = @('-X','-w','-h','127.0.0.1','-p',$c.POSTGRES_PORT,'-U',$c.POSTGRES_USER,'-d',$c.POSTGRES_DB,'-v','ON_ERROR_STOP=1')
$authUrl = "http://localhost:$($c.AUTH_PORT)"
$adminEmail = "postman-admin-$([Guid]::NewGuid().ToString('N'))@example.test"
$adminPassword = New-EcommerceSecret
$datos = @{nombre='Administrador temporal'; email=$adminEmail; password=$adminPassword}
$usuario = Invoke-RestMethod -Uri "$authUrl/api/ecommerce/usuario/save" -Method Post -ContentType 'application/json' -Body ($datos | ConvertTo-Json)
$id = [long]$usuario.id
$env:PGPASSWORD = $c.POSTGRES_PASSWORD
try {
    & $psql @pgArgs -tAc "UPDATE usuario SET role='ADMIN' WHERE id=$id" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar la cuenta temporal.' }
    & $NewmanPath run 'postman/TechStore.postman_collection.json' -e 'postman/Local.postman_environment.json' `
      --env-var "authUrl=$authUrl" --env-var "catalogoUrl=http://localhost:$($c.CATALOGO_PORT)" `
      --env-var "notificacionesUrl=http://localhost:$($c.NOTIFICACIONES_PORT)" --env-var "mailpitUrl=http://localhost:$($c.MAILPIT_UI_PORT)" `
      --env-var "adminEmail=$adminEmail" --env-var "adminPassword=$adminPassword" --env-var "notificationKey=$($c.NOTIFICACIONES_API_KEY)" `
      --delay-request 250 -r cli,htmlextra,json --reporter-cli-no-success-assertions `
      --reporter-htmlextra-skipSensitiveData --reporter-htmlextra-export $ReporteHtml `
      --reporter-json-export "$ecommerceHome/newman-raw.json"
    $codigo = $LASTEXITCODE
    $reporte = Get-Content -LiteralPath "$ecommerceHome/newman-raw.json" -Raw | ConvertFrom-Json
    $salida = [ordered]@{ peticiones=$reporte.run.stats.requests.total; peticionesFallidas=$reporte.run.stats.requests.failed
        aserciones=$reporte.run.stats.assertions.total; asercionesFallidas=$reporte.run.stats.assertions.failed; promedioMs=$reporte.run.timings.responseAverage
        maximoMs=($reporte.run.executions | ForEach-Object { $_.response.responseTime } | Measure-Object -Maximum).Maximum
        fallos=@($reporte.run.failures | ForEach-Object { [ordered]@{ caso=$_.source.name; comprobacion=$_.error.test; mensaje=$_.error.message } })
    }
    $salida | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $Resumen -Encoding UTF8
} finally {
    & $psql @pgArgs -tAc "DELETE FROM usuario WHERE id=$id" | Out-Null
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath "$ecommerceHome/newman-raw.json" -ErrorAction SilentlyContinue
}
exit $codigo
