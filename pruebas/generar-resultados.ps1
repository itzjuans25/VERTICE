param([string]$Sufijo = '')

$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot)
$cult = [Globalization.CultureInfo]::InvariantCulture
function Numero([double]$valor) { $valor.ToString('0.00', $cult) }

$modulos = [ordered]@{ Auth='auth'; 'Catálogo'='catalogo'; Notificaciones='notificaciones' }
$cobertura = foreach ($m in $modulos.GetEnumerator()) {
    [xml]$reporte = Get-Content -LiteralPath "$($m.Value)/target/site/jacoco/jacoco.xml" -Raw
    $lineas = $reporte.report.counter | Where-Object type -EQ 'LINE'
    $cubiertas = [int]$lineas.covered
    $total = $cubiertas + [int]$lineas.missed
    $pruebas = Get-ChildItem "$($m.Value)/target/surefire-reports" -Filter 'TEST-*.xml' | ForEach-Object {
        [xml]$suite = Get-Content -LiteralPath $_.FullName -Raw
        [int]$suite.testsuite.tests
    } | Measure-Object -Sum
    [pscustomobject]@{
        service=$m.Key; covered=$cubiertas; total=$total
        percentage=[math]::Round(100*$cubiertas/$total, 2); tests=[int]$pruebas.Sum
    }
}

ConvertTo-Json -InputObject @($cobertura) | Set-Content -LiteralPath 'resultados/cobertura.json' -Encoding UTF8
$inicial = Get-Content -LiteralPath 'resultados/cobertura-inicial.json' -Raw | ConvertFrom-Json
$carga = Get-Content -LiteralPath "resultados/carga$Sufijo.json" -Raw | ConvertFrom-Json
$estres = Get-Content -LiteralPath "resultados/estres$Sufijo.json" -Raw | ConvertFrom-Json
$perfiles = @($carga, $estres)
if (Test-Path "resultados/estres-6vu$Sufijo.json") {
    $perfiles += Get-Content "resultados/estres-6vu$Sufijo.json" -Raw | ConvertFrom-Json
}
$filasCobertura = for ($i = 0; $i -lt $cobertura.Count; $i++) {
    $fila = $cobertura[$i]
    "$($fila.service) & $(Numero $inicial[$i].percentage)\% & $(Numero $fila.percentage)\% & $($fila.covered)/$($fila.total) \\"
}
$filasTiempo = foreach ($perfil in $perfiles) {
    "$($perfil.modo) ($($perfil.vusMaximos)) & $(Numero $perfil.avg) & $(Numero $perfil.'p(95)') & $(Numero $perfil.'p(99)') & $(Numero $perfil.max) \\"
}
$filasVolumen = foreach ($perfil in $perfiles) {
    $rutas = @($perfil.endpoints | Where-Object peticiones -GT 0).Count
    "$($perfil.modo) ($($perfil.vusMaximos)) & $($perfil.peticiones) & $rutas & $($perfil.errores) & $(Numero $perfil.rps) \\"
}
$rutasFuera = @($perfiles | ForEach-Object endpoints | Where-Object max -GE 500).Count
$rutasSinMuestras = @($perfiles | ForEach-Object endpoints | Where-Object peticiones -EQ 0).Count
$erroresMedidos = ($perfiles | Measure-Object errores -Sum).Sum
if (($rutasFuera -eq 0) -and ($rutasSinMuestras -eq 0) -and ($erroresMedidos -eq 0)) {
    $observacion = 'Todas las rutas medidas quedaron por debajo de 500 ms, sin errores HTTP en los perfiles finales.'
} else {
    $observacion = if ($rutasFuera -eq 1) {
        'Una combinación de ruta y perfil alcanzó o superó 500 ms.'
    } else {
        "Se observaron $rutasFuera combinaciones de ruta y perfil con máximos de 500 ms o superiores."
    }
    if ($rutasSinMuestras -gt 0) {
        $observacion += " En $rutasSinMuestras combinaciones faltaron muestras."
    }
    if ($estres.endpoints | Where-Object { $_.nombre -eq 'auth_cambiar' -and $_.max -ge 500 }) {
        $observacion += ' El máximo con ocho usuarios correspondió al cambio de contraseña.'
    }
    if ($erroresMedidos -gt 0) {
        $observacion += " Se registraron $erroresMedidos respuestas distintas de las esperadas en los perfiles publicados."
    }
}
$observacion += ' La primera corrida detectó que vaciar un carrito inexistente devolvía 404. La operación quedó idempotente y se añadió una prueba de regresión. Se reutilizaron canales RabbitMQ con confirmación por mensaje. El historial pasó a cargar las líneas de compra en la misma consulta y el cambio de clave aplica BCrypt antes de una escritura condicional.'
if ($Sufijo -eq '-bandeja') {
    $observacion += ' La API de notificaciones acepta el aviso en PostgreSQL y un proceso separado confirma la publicación con RabbitMQ.'
}
if (Test-Path 'resultados/estres-durante-arranque.json') {
    $arranque = Get-Content 'resultados/estres-durante-arranque.json' -Raw | ConvertFrom-Json
    $observacion += " En la versión anterior, una corrida coincidente con el arranque de React alcanzó $(Numero $arranque.max) ms."
}
$tex = @(
    '% Generado por pruebas/generar-resultados.ps1 a partir de JaCoCo y k6.'
    '\newcommand{\FilasCobertura}{'
) + $filasCobertura + @('}', '\newcommand{\FilasLatencia}{') + $filasTiempo + @(
    '}', '\newcommand{\FilasVolumen}{'
) + $filasVolumen + @('}', "\newcommand{\ResultadoMedido}{$observacion}")
[IO.File]::WriteAllLines((Join-Path (Get-Location) 'docs/articulo/metricas.tex'), $tex)
