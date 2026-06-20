#Requires -Version 5.1
<#
.SYNOPSIS
    Launches DestinationSol via Gradle. Run setup.ps1 first if this is a fresh clone.
#>

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    Write-Host "Java not found. Run setup.ps1 first." -ForegroundColor Red
    exit 1
}

Write-Host "Starting DestinationSol..." -ForegroundColor Cyan
& .\gradlew.bat :desktop:run
exit $LASTEXITCODE
