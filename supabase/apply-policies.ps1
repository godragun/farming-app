param(
    [string]$DatabaseUrl = $env:SUPABASE_DB_URL,
    [string]$SqlFile     = (Join-Path $PSScriptRoot "policies.sql")
)

if ([string]::IsNullOrWhiteSpace($DatabaseUrl)) {
    Write-Host "SUPABASE_DB_URL is not set." -ForegroundColor Yellow
    Write-Host "Get it from Supabase dashboard: Project Settings -> Database -> Connection string (URI)." -ForegroundColor Yellow
    Write-Host "Then set it in this shell BEFORE running this script, e.g.:" -ForegroundColor Yellow
    Write-Host '    $env:SUPABASE_DB_URL = "postgresql://postgres.[ref]:[PASSWORD]@aws-0-....pooler.supabase.com:6543/postgres"' -ForegroundColor Cyan
    Write-Host "Then re-run:  ./apply-policies.ps1" -ForegroundColor Cyan
    exit 1
}

if (-not (Test-Path $SqlFile)) {
    Write-Host "SQL file not found: $SqlFile" -ForegroundColor Red
    exit 1
}

$psql = Get-Command psql -ErrorAction SilentlyContinue
if (-not $psql) {
    Write-Host "psql is not installed or not in PATH." -ForegroundColor Yellow
    Write-Host "Install options:" -ForegroundColor Yellow
    Write-Host "  winget install -e --id PostgreSQL.PostgreSQL.17" -ForegroundColor Cyan
    Write-Host "  (or) choco install postgresql" -ForegroundColor Cyan
    exit 1
}

Write-Host "Applying $SqlFile ..." -ForegroundColor Green
& psql $DatabaseUrl -v ON_ERROR_STOP=1 -f $SqlFile
if ($LASTEXITCODE -ne 0) {
    Write-Host "psql exited with code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}
Write-Host "Done. Policies applied." -ForegroundColor Green
