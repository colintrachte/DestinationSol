@echo off
setlocal

:: Quick Java sanity check
java -version >nul 2>&1
if errorlevel 1 (
    echo Java not found. Run setup.bat first.
    exit /b 1
)

echo Starting DestinationSol...
call gradlew.bat :desktop:run
endlocal
