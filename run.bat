@echo off
setlocal

:: Ensure we are in the project root
if not exist "gradlew.bat" (
    echo ERROR: gradlew.bat not found.
    echo Make sure you are running this script from the DestinationSol project root.
    exit /b 1
)

:: Quick Java sanity check
java -version >nul 2>&1
if errorlevel 1 (
    echo Java not found. Run setup.bat first.
    exit /b 1
)

echo Starting DestinationSol...
echo Log file: %CD%\destinationsol.log
echo.
call gradlew.bat :desktop:run
set EXIT_CODE=%errorlevel%

if %EXIT_CODE% neq 0 (
    echo.
    echo The game exited with an error (code %EXIT_CODE%^).
    echo Check destinationsol.log in this directory for the full crash log.
)

endlocal
pause
exit /b %EXIT_CODE%
