@echo off
setlocal enabledelayedexpansion

echo === DestinationSol Setup ===
echo.

:: Check for java on PATH
java -version >nul 2>&1
if errorlevel 1 (
    echo Java not found on PATH.
    echo Attempting to install Java 17 via winget...
    winget install EclipseAdoptium.Temurin.17.JDK --silent
    if errorlevel 1 (
        echo.
        echo Automatic install failed. Please install Java 17 manually:
        echo   https://adoptium.net/
        echo Then re-run this script.
        exit /b 1
    )
    echo.
    echo Java installed. Please open a new terminal and run setup.bat again.
    exit /b 0
)

:: Parse major version from "java -version" output (writes to stderr)
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set RAW=%%v
    goto :parse
)
:parse
set RAW=%RAW:"=%
for /f "tokens=1 delims=." %%m in ("%RAW%") do set MAJOR=%%m
if "%MAJOR%"=="1" (
    for /f "tokens=2 delims=." %%m in ("%RAW%") do set MAJOR=%%m
)

if %MAJOR% LSS 11 (
    echo Java %MAJOR% detected but Java 11 or newer is required.
    echo Please install Java 17 from https://adoptium.net/ and re-run.
    exit /b 1
)
echo Java %MAJOR% detected. OK.
echo.

:: Pre-download all Gradle dependencies and compile
echo Downloading dependencies and compiling (first run may take several minutes)...
call gradlew.bat :desktop:classes
if errorlevel 1 (
    echo.
    echo Build failed. See output above for details.
    exit /b 1
)

echo.
echo Setup complete! Run run.bat to launch the game.
endlocal
