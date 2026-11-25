[CmdletBinding()]
param(
  [Parameter(Mandatory)][string]$Frontend,
  [Parameter(Mandatory)][string]$Auth,
  [Parameter(Mandatory)][string]$Catalogo,
  [Parameter(Mandatory)][string]$Notificaciones,
  [Parameter(Mandatory)][string]$Subastas,
  [Parameter(Mandatory)][long]$ProductoId,
  [Parameter(Mandatory)][long]$LoteId
)
$ErrorActionPreference = 'Stop'
$paso = 'Validar URLs'
function Consultar($url, $body, $headers=@{}) {
    if ($null -eq $body) { return Invoke-RestMethod $url -Method Post -Headers $headers -TimeoutSec 20 }
    return Invoke-RestMethod $url -Method Post -Headers $headers -ContentType 'application/json' -Body ($body | ConvertTo-Json -Compress) -TimeoutSec 20
}
function Esperar($url, $api) {
    $reloj = [Diagnostics.Stopwatch]::StartNew()
    while ($reloj.Elapsed.TotalSeconds -lt 60) {
        $timeout = [Math]::Max(1,[Math]::Min(5,[Math]::Floor(60 - $reloj.Elapsed.TotalSeconds)))
        try {
            if ($api) {
                if ((Invoke-RestMethod $url -TimeoutSec $timeout).status -eq 'UP') { return }
            } elseif ((Invoke-WebRequest $url -UseBasicParsing -TimeoutSec $timeout).Content -match 'TechStore') { return }
        } catch {}
        if ($reloj.Elapsed.TotalSeconds -lt 58) { Start-Sleep -Seconds 2 }
    }
    throw 'Servicio sin respuesta válida'
}
try {
    foreach ($url in $Frontend,$Auth,$Catalogo,$Notificaciones,$Subastas) {
        if ($url -notmatch '^https?://[^\s/]+') { throw 'URL inválida' }
    }
    $Frontend=$Frontend.TrimEnd('/'); $Auth=$Auth.TrimEnd('/')
    $Catalogo=$Catalogo.TrimEnd('/'); $Notificaciones=$Notificaciones.TrimEnd('/'); $Subastas=$Subastas.TrimEnd('/')
    $paso='Frontend'; Esperar "$Frontend/" $false; Write-Host "OK  $paso"
    foreach ($servicio in @(@{n='Auth';u=$Auth},@{n='Catálogo';u=$Catalogo},@{n='Notificaciones';u=$Notificaciones},@{n='Subastas';u=$Subastas})) {
        $paso="$($servicio.n) /health"; Esperar "$($servicio.u)/health" $true; Write-Host "OK  $paso"
    }
    $id=[Guid]::NewGuid().ToString('N')
    $cuenta=@{email="smoke-$id@example.test";password="Smoke-!$id"}
    $authApi="$Auth/api/ecommerce/usuario"; $catApi="$Catalogo/api/ecommerce"
    $paso='Registro'; $u=Consultar "$authApi/save" ($cuenta+@{nombre='Smoke TechStore'})
    if (-not $u.id) { throw 'Registro inválido' }; Write-Host "OK  $paso"
    $paso='Login'; $login=Consultar "$authApi/login" $cuenta
    if (-not $login.token) { throw 'Login inválido' }; Write-Host "OK  $paso"
    $headers=@{Authorization="Bearer $($login.token)"}
    $paso='Catálogo con stock'; $listado=Invoke-RestMethod "$catApi/producto/explorar?tamano=48" -TimeoutSec 20
    if (-not $listado.items.Count) { throw 'Catálogo vacío' }
    $p=$listado.items | Where-Object { $_.stock -gt 0 -and (!$ProductoId -or $_.id -eq $ProductoId) } | Select-Object -First 1
    if ($ProductoId -and -not $p) { $p=Invoke-RestMethod "$catApi/producto/$ProductoId" -TimeoutSec 20 }
    if (-not $p.id -or $p.stock -lt 1) { throw 'Producto agotado' }; Write-Host "OK  $paso"
    $paso='Agregar al carrito'; $c=Consultar "$catApi/carrito/agregar" @{productoId=$p.id;cantidad=1} $headers
    if (-not ($c.items | Where-Object productoId -eq $p.id)) { throw 'Carrito inválido' }; Write-Host "OK  $paso"
    $paso='Confirmar compra'; $compra=Consultar "$catApi/compras/realizar" $null $headers
    if (-not $compra.id -or $compra.estado -ne 'COMPLETADA') { throw 'Pedido inválido' }
    Write-Host "OK  $paso. Pedido sintético $($compra.id)"
    $paso='Puja'; $subApi="$Subastas/api/ecommerce/subastas"
    $lote=Invoke-RestMethod "$subApi/$LoteId" -TimeoutSec 20
    if ($lote.lote.estado -ne 'ACTIVA') { throw 'Lote inactivo' }
    $oferta=Consultar "$subApi/$LoteId/pujas" @{monto=$lote.lote.pujaMinima} $headers
    if (-not $oferta.voyGanando) { throw 'Puja sin confirmar' }
    Write-Host "OK  $paso. Lote $LoteId"
} catch {
    Write-Host "FALLA  $paso ($($_.Exception.GetType().Name))"
    exit 1
}
exit 0

