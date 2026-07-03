@echo off
setlocal enabledelayedexpansion

:: Always run from the folder this script lives in (the project root), no matter
:: how it was launched (double-click in Explorer, another drive, etc.).
cd /d "%~dp0"

:: Runs the test suite for every module under modules\ in one Gradle invocation.
:: Usage:  double-click, or
::         test.bat            (all modules)
::         test.bat engine     (also run engine tests)

if not exist "gradlew.bat" (
    echo ERROR: gradlew.bat was not found in:
    echo     %CD%
    echo This script must sit in the DestinationSol project root, next to gradlew.bat.
    goto :end
)

:: Java sanity check - Explorer launches inherit the system PATH.
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java was not found on your PATH.
    echo Install Java 17+ ^(or run setup.bat once^), then reopen Explorer and retry.
    goto :end
)

set "TASKS="
:: Optionally include engine tests when the first argument is "engine".
if /i "%~1"=="engine" set "TASKS=:engine:test"

:: Discover every module folder that has a build.gradle.
for /d %%D in (modules\*) do (
    if exist "%%D\build.gradle" (
        set "TASKS=!TASKS! :modules:%%~nxD:test"
    )
)

if "!TASKS!"=="" (
    echo No modules with a build.gradle were found under:
    echo     %CD%\modules
    goto :end
)

echo Project root : %CD%
echo Test tasks   :!TASKS!
echo.
call gradlew.bat !TASKS! --continue
set "EXIT_CODE=!errorlevel!"

echo.
if "!EXIT_CODE!"=="0" (
    echo ============================================
    echo  ALL TESTS PASSED
    echo ============================================
) else (
    echo ============================================
    echo  SOME TESTS FAILED ^(exit code !EXIT_CODE!^)
    echo ============================================
    echo Open the HTML report under each module:
    echo     modules\^<name^>\build\reports\tests\test\index.html
)

:end
echo.
echo Press any key to close this window . . .
pause >nul
endlocal
