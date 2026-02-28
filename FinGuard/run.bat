@echo off
REM ============================================================
REM  FinGuard - Fraud Detection System
REM  Build and Run Script for Windows
REM ============================================================

setlocal

set SRC_DIR=src
set OUT_DIR=out
set MAIN_CLASS=FinGuardMain
set RES_DIR=resources

echo.
echo ======================================================
echo  FinGuard - Compiling Java Sources...
echo ======================================================

REM Create output directory if not exists
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

REM Compile all Java source files
javac -d "%OUT_DIR%" -sourcepath "%SRC_DIR%" "%SRC_DIR%\*.java"
if errorlevel 1 (
    echo.
    echo [ERROR] Compilation FAILED. Please fix the errors above.
    pause
    exit /b 1
)

echo [SUCCESS] Compilation complete.
echo.

REM -----------------------------------------------------------
REM Parse mode argument
REM   run.bat               -> simulate 2000 transactions
REM   run.bat --file        -> use resources/transactions.csv
REM   run.bat --simulate N  -> simulate N transactions
REM -----------------------------------------------------------

if "%1"=="--file" (
    echo Running in FILE mode: %RES_DIR%\transactions.csv
    java -cp "%OUT_DIR%" %MAIN_CLASS% --file "%RES_DIR%\transactions.csv"
) else if "%1"=="--simulate" (
    set COUNT=%2
    if "%COUNT%"=="" set COUNT=2000
    echo Running in SIMULATE mode: %COUNT% transactions
    java -cp "%OUT_DIR%" %MAIN_CLASS% --simulate %COUNT%
) else (
    echo Running in default SIMULATE mode: 2000 transactions
    java -cp "%OUT_DIR%" %MAIN_CLASS% --simulate 2000
)

echo.
echo ======================================================
echo  Done. Check fraud_alert.txt for fraud records.
echo ======================================================
pause
