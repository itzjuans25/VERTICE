$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot)
. .\scripts\windows-local.ps1
$c = Read-EcommerceEnv '.env.native'
$node = Get-EcommerceTool 'node.exe' @(Get-ChildItem "$ecommerceHome/node-*/node.exe" | Select-Object -ExpandProperty FullName) 'Node.js'
$pg = Get-EcommerceTool 'psql.exe' @(Get-ChildItem "$env:ProgramFiles/PostgreSQL/*/bin/psql.exe" | Select-Object -ExpandProperty FullName) 'PostgreSQL'
$auth = "http://localhost:$($c.AUTH_PORT)/api/ecommerce/usuario"
$producto = "http://localhost:$($c.CATALOGO_PORT)/api/ecommerce/producto"
$clave = New-EcommerceSecret
$email = "admin-compra-e2e-$([guid]::NewGuid().ToString('N'))@example.test"
$sesion = @{ email=$email; password=$clave }
$usuario = Invoke-RestMethod "$auth/save" -Method Post -ContentType 'application/json' -Body (($sesion + @{nombre='QA compra'}) | ConvertTo-Json)
$usuarioId = [long]$usuario.id
$pgArgs = @('-X','-w','-h','127.0.0.1','-p',$c.POSTGRES_PORT,'-U',$c.POSTGRES_USER,'-d',$c.POSTGRES_DB,'-v','ON_ERROR_STOP=1')
$admin = $null
$lote = $null
$env:PGPASSWORD = $c.POSTGRES_PASSWORD
try {
    & $pg @pgArgs -c "UPDATE usuario SET role='ADMIN' WHERE id=$usuarioId" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar la cuenta temporal.' }
    $login = Invoke-RestMethod "$auth/login" -Method Post -ContentType 'application/json' -Body ($sesion | ConvertTo-Json)
    $admin = @{ Authorization="Bearer $($login.token)" }
    $listado = Invoke-RestMethod "$producto/explorar?condicion=USADO&tamano=48"
    $base = $listado.items | Where-Object imagenSecundariaUrl | Select-Object -First 1
    if (-not $base) { throw 'Falta un ejemplo usado con dos imágenes.' }
    $campos = 'descripcion','precio','imagenUrl','categoria','condicion','marca','garantiaDias','grado','modelo','descripcionLarga','incluye','compatibilidad','notaEstado','imagenSecundariaUrl','especificaciones'
    $datos = @{nombre="Equipo de prueba $([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"; stock=1}
    foreach ($campo in $campos) { $datos[$campo]=$base.$campo }
    $lote = Invoke-RestMethod $producto -Method Post -ContentType 'application/json' -Headers $admin -Body ($datos | ConvertTo-Json -Depth 8)
    $env:TECHSTORE_E2E_PRODUCTO = [string]$lote.id
    $env:TECHSTORE_E2E_RESULTADO = 'resultados/e2e-compra-subastas.json'
    $env:TECHSTORE_E2E_CAPTURAS = 'entrega-subastas-'
    & $node 'pruebas/techstore-e2e.cjs'
    exit $LASTEXITCODE
} finally {
    if ($lote -and $admin) {
        Invoke-RestMethod "$producto/$($lote.id)" -Method Delete -Headers $admin | Out-Null
    }
    & $pg @pgArgs -c "DELETE FROM usuario WHERE id=$usuarioId" | Out-Null
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
