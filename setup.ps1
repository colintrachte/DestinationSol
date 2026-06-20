#Requires -Version 5.1
<#
.SYNOPSIS
    Checks for Java 11+, installs it if missing, then compiles DestinationSol.
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Write-Host "=== DestinationSol Setup ===" -ForegroundColor Cyan
Write-Host ""

function Get-JavaMajorVersion {
    $javaOutput = & java -version 2>&1 | Select-String 'version'
    if (-not $javaOutput) { return 0 }
    $verString = ($javaOutput -replace '.*"([^"]+)".*', '$1')
    $parts = $verString -split '\.'
    $major = [int]$parts[0]
    if ($major -eq 1) { $major = [int]$parts[1] }
    return $major
}

# Check if java is on PATH
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    Write-Host "Java not found on PATH." -ForegroundColor Yellow
    Write-Host "Attempting to install Java 17 via winget..."
    try {
        winget install EclipseAdoptium.Temurin.17.JDK --silent --accept-package-agreements --accept-source-agreements
        Write-Host ""
        Write-Host "Java installed. Please open a new terminal and run setup.ps1 again." -ForegroundColor Green
    } catch {
        Write-Host ""
        Write-Host "Automatic install failed: $_" -ForegroundColor Red
        Write-Host "Please install Java 17 manually from https://adoptium.net/ then re-run."
    }
    exit 0
}

$major = Get-JavaMajorVersion
if ($major -lt 11) {
    Write-Host "Java $major detected but Java 11 or newer is required." -ForegroundColor Red
    Write-Host "Please install Java 17 from https://adoptium.net/ and re-run."
    exit 1
}
Write-Host "Java $major detected. OK." -ForegroundColor Green
Write-Host ""

Write-Host "Downloading dependencies and compiling (first run may take several minutes)..."
& .\gradlew.bat :desktop:classes
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "Build failed. See output above for details." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "Setup complete! Run run.ps1 to launch the game." -ForegroundColor Green
