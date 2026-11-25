$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot)
. .\scripts\windows-local.ps1
$c=Read-EcommerceEnv '.env.native'
$pg=(Get-ChildItem "$env:ProgramFiles/PostgreSQL/*/bin/psql.exe" | Select-Object -First 1).FullName
$esquema='vertice_it_'+[DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$env:PGPASSWORD=$c.POSTGRES_PASSWORD
try {
    & $pg -w -X -h 127.0.0.1 -p $c.POSTGRES_PORT -U $c.POSTGRES_USER -d $c.POSTGRES_DB -v ON_ERROR_STOP=1 -c "CREATE SCHEMA $esquema" | Out-Null
    if($LASTEXITCODE -ne 0) { throw 'No se pudo aislar la prueba PostgreSQL.' }
} finally { Remove-Item Env:PGPASSWORD }
$env:SUBASTAS_IT_JDBC="jdbc:postgresql://localhost:$($c.POSTGRES_PORT)/$($c.POSTGRES_DB)?currentSchema=$esquema"
$env:SUBASTAS_IT_USUARIO=$c.POSTGRES_USER
$env:SUBASTAS_IT_CLAVE=$c.POSTGRES_PASSWORD
$salida=[ordered]@{}
try {
    foreach($m in 'auth','catalogo','notificaciones','subastas'){
        Push-Location $m
        try {
            $datos=[ordered]@{}
            foreach($modo in 'unitarias','conjunta'){
                $opciones=@()
                if($modo -eq 'unitarias'){
                    $unitarias=Get-ChildItem src/test/java -Recurse -Filter '*.java' | Where-Object {
                        $_.BaseName -notmatch 'ArquitecturaTest|IntegrationTest|ApplicationTests|FlujoSubastasTest' -and
                        -not ((Get-Content $_.FullName -Raw) -match '@SpringBootTest|@WebMvcTest|@DataJpaTest|@JdbcTest|@SpringJUnitConfig')
                    } | Select-Object -ExpandProperty BaseName
                    $opciones+=("-Dtest="+($unitarias -join ','))
                }
                $exec=Join-Path (Get-Location) "target/vertice-$modo.exec"
                $reporte="$ecommerceHome/vertice-cobertura-$m-$modo.log"
                & .\mvnw.cmd -B -ntp "-Djacoco.destFile=$exec" "-Djacoco.dataFile=$exec" '-Djacoco.append=false' @opciones test jacoco:report@cobertura *> $reporte
                $codigo=$LASTEXITCODE
                if($codigo -eq 0) {
                    [xml]$xml=Get-Content target/site/jacoco/jacoco.xml
                    $l=$xml.report.counter | Where-Object type -eq 'LINE'
                    $total=[int]$l.covered+[int]$l.missed
                    $suite=(Select-String $reporte -Pattern '^\[INFO\] Tests run: ([0-9]+), Failures:' | Select-Object -Last 1)
                    $pruebas=if($suite.Line -match 'Tests run: ([0-9]+)') {[int]$Matches[1]} else {$null}
                    $datos[$modo]=[ordered]@{pruebas=$pruebas;lineas=[int]$l.covered;total=$total;porcentaje=[math]::Round(100*[int]$l.covered/$total,2)}
                } else {
                    $datos[$modo]=[ordered]@{error=$codigo}
                    (Get-Content $reporte -Tail 6)
                }
                "$m $modo $codigo"
            }
            $salida[$m]=$datos
        } finally { Pop-Location }
        $salida | ConvertTo-Json -Depth 5 | Set-Content resultados/vertice-cobertura.json -Encoding utf8
    }
} finally { Remove-Item Env:SUBASTAS_IT_CLAVE -ErrorAction SilentlyContinue }