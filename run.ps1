#Requires -Version 5.1
<#
.SYNOPSIS
    Launches DestinationSol from source via Gradle.
    Run setup.ps1 first if this is a fresh clone.
#>

# Ensure we are in the project root
if (-not (Test-Path "gradlew.bat")) {
    Write-Host "ERROR: gradlew.bat not found." -ForegroundColor Red
    Write-Host "Make sure you are running this script from the DestinationSol project root."
    exit 1
}

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    Write-Host "Java not found. Run setup.ps1 first." -ForegroundColor Red
    exit 1
}

$logFile = Join-Path (Get-Location) "destinationsol.log"
Write-Host "Starting DestinationSol..." -ForegroundColor Cyan
Write-Host "Log file: $logFile"
Write-Host ""

& .\gradlew.bat :desktop:run
$exitCode = $LASTEXITCODE

if ($exitCode -ne 0) {
    Write-Host ""
    Write-Host "The game exited with an error (code $exitCode)." -ForegroundColor Red
    Write-Host "Check destinationsol.log in this directory for the full crash log."
}

exit $exitCode
