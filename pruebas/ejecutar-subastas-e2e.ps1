$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot)
. .\scripts\windows-local.ps1
$c = Read-EcommerceEnv '.env.native'
$node = Get-EcommerceTool 'node.exe' @(Get-ChildItem "$ecommerceHome/node-*/node.exe" | Select-Object -ExpandProperty FullName) 'Node.js'
$env:TECHSTORE_E2E_PSQL = Get-EcommerceTool 'psql.exe' @(Get-ChildItem "$env:ProgramFiles/PostgreSQL/*/bin/psql.exe" | Select-Object -ExpandProperty FullName) 'PostgreSQL'
$env:TECHSTORE_E2E_DB_PORT = $c.POSTGRES_PORT
$env:TECHSTORE_E2E_DB_USERNAME = $c.POSTGRES_USER
$env:TECHSTORE_E2E_DB = $c.POSTGRES_DB
$env:PGPASSWORD = $c.POSTGRES_PASSWORD
try {
    & $node 'pruebas/subastas-e2e.cjs'
    exit $LASTEXITCODE
} finally { Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue }