#Requires -Version 5.1
<#
.SYNOPSIS
    Runs the test suite for every module under modules/ (auto-discovered), in a
    single Gradle invocation. Use -IncludeEngine to also run the engine tests.

.PARAMETER IncludeEngine
    Also run :engine:test (slower; pulls in headless libGDX).

.PARAMETER Rerun
    Force tests to re-run even if Gradle thinks they are up to date.

.EXAMPLE
    .\test.ps1
    .\test.ps1 -IncludeEngine
    .\test.ps1 -Rerun
#>
param(
    [switch]$IncludeEngine,
    [switch]$Rerun
)

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

# Discover every module: a subdirectory of modules/ that has a build.gradle.
$modulesRoot = Join-Path (Get-Location) "modules"
$moduleDirs = Get-ChildItem -Path $modulesRoot -Directory |
    Where-Object { Test-Path (Join-Path $_.FullName "build.gradle") } |
    Sort-Object Name

if (-not $moduleDirs) {
    Write-Host "No modules with a build.gradle found under modules/." -ForegroundColor Red
    exit 1
}

# Build the task list.
$tasks = @()
if ($IncludeEngine) {
    $tasks += ":engine:test"
}
foreach ($dir in $moduleDirs) {
    $tasks += ":modules:$($dir.Name):test"
}

# Gradle flags: --continue so one failing module does not hide the others.
$gradleArgs = @()
$gradleArgs += $tasks
$gradleArgs += "--continue"
if ($Rerun) {
    $gradleArgs += "--rerun-tasks"
}

Write-Host "Testing modules: $((($moduleDirs).Name) -join ', ')" -ForegroundColor Cyan
if ($IncludeEngine) {
    Write-Host "Including engine tests." -ForegroundColor Cyan
}
Write-Host "gradlew $($gradleArgs -join ' ')"
Write-Host ""

& .\gradlew.bat @gradleArgs
$exitCode = $LASTEXITCODE

Write-Host ""
if ($exitCode -eq 0) {
    Write-Host "All tests passed." -ForegroundColor Green
} else {
    Write-Host "Some tests failed (exit code $exitCode)." -ForegroundColor Red
}

# Point at the HTML reports for anything that ran.
Write-Host ""
Write-Host "HTML reports:"
foreach ($dir in $moduleDirs) {
    $report = Join-Path $dir.FullName "build\reports\tests\test\index.html"
    if (Test-Path $report) {
        Write-Host "  $report"
    }
}
if ($IncludeEngine) {
    $engineReport = Join-Path (Get-Location) "engine\build\reports\tests\test\index.html"
    if (Test-Path $engineReport) {
        Write-Host "  $engineReport"
    }
}

exit $exitCode
