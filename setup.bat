@echo off
setlocal enabledelayedexpansion

echo === DestinationSol Setup ===
echo.

:: Ensure gradlew.bat is present (must run from project root)
if not exist "gradlew.bat" (
    echo ERROR: gradlew.bat not found.
    echo Make sure you are running this script from the DestinationSol project root.
    exit /b 1
)

:: Check for Java on PATH
java -version >nul 2>&1
if errorlevel 1 (
    echo Java not found on PATH.
    echo Attempting to install Java 25 via winget...
    winget install EclipseAdoptium.Temurin.25.JDK --silent --accept-package-agreements --accept-source-agreements
    if errorlevel 1 (
        echo.
        echo Automatic install failed. Please install Java 25 manually:
        echo   https://adoptium.net/
        echo Then open a new terminal and run setup.bat again.
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

if %MAJOR% LSS 17 (
    echo Java %MAJOR% detected but Java 17 or newer is required ^(Gradle 9 will not run on anything older^).
    echo Please install Java 25 from https://adoptium.net/ and re-run.
    exit /b 1
)
if %MAJOR% LSS 25 (
    :: Gradle's own daemon-JVM auto-provisioning (gradle-daemon-jvm.properties) can't
    :: download a JDK on its own - it needs one installed locally, so we install it here
    :: rather than letting the build fail with a "no defined toolchain download url" error.
    echo Java %MAJOR% detected. The project needs a Java 25 toolchain to compile.
    echo Attempting to install Java 25 via winget...
    winget install EclipseAdoptium.Temurin.25.JDK --silent --accept-package-agreements --accept-source-agreements
    if errorlevel 1 (
        echo.
        echo Automatic install failed. Please install Java 25 manually:
        echo   https://adoptium.net/
        echo Then open a new terminal and run setup.bat again.
        exit /b 1
    )
    echo.
    echo Java 25 installed. Please open a new terminal and run setup.bat again.
    exit /b 0
) else (
    echo Java %MAJOR% detected. OK.
)
echo.

:: Download all Gradle dependencies and compile
echo Downloading dependencies and compiling (first run may take several minutes)...
echo.
call gradlew.bat :desktop:classes
if errorlevel 1 (
    echo.
    echo Build failed. See the output above for details.
    echo Common causes:
    echo   - No internet connection (Gradle needs to download dependencies)
    echo   - Corporate proxy blocking Maven Central
    exit /b 1
)

echo.
echo Setup complete!
echo Run run.bat (Command Prompt) or .\run.ps1 (PowerShell) to launch the game.
endlocal
