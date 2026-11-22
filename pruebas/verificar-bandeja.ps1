$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot)
. ./scripts/windows-local.ps1
$config = Read-EcommerceEnv '.env.native'
if ($config.MAIL_HOST -and $config.MAIL_HOST -notin @('localhost', '127.0.0.1')) {
    throw 'Esta prueba requiere Mailpit local.'
}
$correo = "bandeja-$([guid]::NewGuid().ToString('N'))@example.test"
$asunto = 'Prueba local de bandeja persistente'
$envio = @{tipo='EMAIL'; email=@{destinatario=$correo; asunto=$asunto; cuerpo='Envio de prueba local'}}
$solicitud = @{
    Uri = "http://127.0.0.1:$($config.NOTIFICACIONES_PORT)/api/ecommerce/notificaciones/encolar"
    Method = 'Post'
    Headers = @{'X-Notification-Key'=$config.NOTIFICACIONES_API_KEY}
    ContentType = 'application/json'
    Body = ($envio | ConvertTo-Json)
}
$aceptado = Invoke-RestMethod @solicitud
if ($aceptado.estado -ne 'PENDIENTE' -or -not $aceptado.id) { throw 'El aviso no fue aceptado.' }
$limite = (Get-Date).AddSeconds(60)
do {
    $buzon = Invoke-RestMethod "http://127.0.0.1:$($config.MAILPIT_UI_PORT)/api/v1/messages" -TimeoutSec 4
    $recibido = $buzon.messages | Where-Object {
        $_.Subject -eq $asunto -and @($_.To | ForEach-Object Address) -contains $correo
    }
    if ($recibido) {
        @{id=$aceptado.id; estadoInicial=$aceptado.estado; recibidoEnMailpit=$true} |
            ConvertTo-Json | Set-Content 'resultados/verificacion-bandeja.json' -Encoding UTF8
        Write-Host "Aviso $($aceptado.id) aceptado y recibido en Mailpit."
        exit 0
    }
    Start-Sleep -Milliseconds 300
} while ((Get-Date) -lt $limite)
throw 'Mailpit no recibio el mensaje antes del limite.'
