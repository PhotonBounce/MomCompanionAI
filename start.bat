@echo off
REM ============================================================
REM  Friendai — project launcher
REM
REM  Default (no flags):  start backend only
REM  --build              build fresh debug APK, then install
REM  --install            install existing APK on device/emulator
REM  --no-backend         skip Node.js server
REM  --device <serial>    target a specific ADB device
REM
REM  Examples:
REM    start.bat                         -- backend only
REM    start.bat --build                 -- build + install + backend
REM    start.bat --install               -- install existing APK + backend
REM    start.bat --build --no-backend    -- build + install, no server
REM    start.bat --install --device emulator-5554
REM ============================================================
setlocal enabledelayedexpansion

set "BUILD=0"
set "INSTALL=0"
set "BACKEND=1"
set "TARGET_DEVICE="

:parse_args
if /I "%1"=="--build"       ( set "BUILD=1"   & set "INSTALL=1" & shift & goto :parse_args )
if /I "%1"=="--install"     ( set "INSTALL=1" & shift & goto :parse_args )
if /I "%1"=="--no-backend"  ( set "BACKEND=0" & shift & goto :parse_args )
if /I "%1"=="--device"      ( set "TARGET_DEVICE=%2" & shift & shift & goto :parse_args )

REM --- paths -----------------------------------------------------------------
set "ROOT=%~dp0"
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
set "APK=%ROOT%app\build\outputs\apk\debug\app-debug.apk"
set "PACKAGE=com.friendai"
set "MAIN=%PACKAGE%/.MainActivity"

REM prefer D:\jdk17, fall back to Adoptium install
set "JAVA_HOME=D:\jdk17"
if not exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo.
echo  ============================================================
echo   Friendai launcher
echo  ============================================================
echo.

REM ============================================================
REM  ANDROID BUILD
REM ============================================================
if "%BUILD%"=="1" (
    echo [1/2] Building debug APK...
    pushd "%ROOT%"
    call gradlew.bat assembleDebug
    if errorlevel 1 (
        echo.
        echo  [ERROR] Build failed — scroll up to see the error.
        pause & exit /b 1
    )
    popd
    if not exist "%APK%" (
        echo  [ERROR] APK missing after build — something went wrong.
        pause & exit /b 1
    )
    echo  [OK] APK: %APK%
    echo.
)

REM ============================================================
REM  ADB INSTALL
REM ============================================================
if "%INSTALL%"=="1" (
    if not exist "%ADB%" (
        echo  [WARN] adb.exe not found at %ADB% — skipping install.
        goto :backend
    )
    if not exist "%APK%" (
        echo  [WARN] APK not found — run with --build first.
        echo         Expected: %APK%
        goto :backend
    )

    REM auto-pick first connected device/emulator
    if not defined TARGET_DEVICE (
        for /f "skip=1 tokens=1" %%D in ('"%ADB%" devices 2^>nul') do (
            if not "%%D"=="" if not defined TARGET_DEVICE set "TARGET_DEVICE=%%D"
        )
    )

    if not defined TARGET_DEVICE (
        echo  [WARN] No ADB device or emulator connected — skipping install.
        echo         Connect a device or start an emulator first.
        goto :backend
    )

    echo [2/2] Installing on !TARGET_DEVICE!...
    "%ADB%" -s "!TARGET_DEVICE!" install -r "%APK%"
    if errorlevel 1 (
        echo  [ERROR] Install failed. Try: adb uninstall %PACKAGE%  then rerun.
        goto :backend
    )
    echo  [OK] Installed. Launching %MAIN%...
    "%ADB%" -s "!TARGET_DEVICE!" shell am start -n "%MAIN%"
    echo.
)

:backend
REM ============================================================
REM  NODE.JS BACKEND
REM ============================================================
if "%BACKEND%"=="0" (
    echo  [SKIP] Backend not started (--no-backend).
    echo.
    pause
    goto :eof
)

cd /d "%ROOT%backend"

where node >nul 2>nul
if errorlevel 1 (
    echo  [ERROR] Node.js not found.
    echo          Download from https://nodejs.org (v20+), then rerun.
    pause & exit /b 1
)

if exist ".env" (
    echo  [ENV] Loading backend\.env
    for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do (
        if not "%%b"=="" set "%%a=%%b"
    )
) else (
    echo  [WARN] backend\.env not found — AI replies disabled.
    echo         Copy backend\.env.example to backend\.env and add OPENAI_API_KEY.
)

if not defined PORT set "PORT=8787"

echo.
echo  Backend   : http://localhost:!PORT!
echo  Health    : http://localhost:!PORT!/health
echo  Privacy   : http://localhost:!PORT!/privacy
echo  AI reply  : POST http://localhost:!PORT!/companion/reply
echo.
echo  Quick cmds:
echo    Build + install:   start.bat --build
echo    Install only:      start.bat --install
echo    Build, no server:  start.bat --build --no-backend
echo.
echo  Press Ctrl+C to stop.
echo.
node server.js
pause
