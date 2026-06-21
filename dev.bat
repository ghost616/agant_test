@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ========================================
echo   Agent Platform - Dev Mode
echo ========================================

echo.
echo [1/5] Checking frontend dependencies...
if not exist "node_modules\" (
    echo Installing npm dependencies...
    call npm install
    if errorlevel 1 (
        echo [ERROR] npm install failed
        pause
        exit /b 1
    )
) else (
    echo Frontend dependencies ready
)

echo.
echo [2/5] Ensuring data directory exists...
if not exist "data\" mkdir data

echo [3/5] Starting backend in new window...
echo cd /d "%~dp0" ^&^& chcp 65001 ^>nul ^&^& mvn spring-boot:run > "%TEMP%\agent_platform_backend.bat"
start "AgentPlatformBackend" cmd /k "%TEMP%\agent_platform_backend.bat"

echo.
echo [4/5] Waiting for backend on port 8080...
set /a WAIT_COUNT=0
:wait_backend
timeout /t 3 /nobreak >nul
set /a WAIT_COUNT+=1
netstat -ano 2>nul | findstr "LISTENING" | findstr ":8080" >nul
if not errorlevel 1 (
    echo Backend ready (http://localhost:8080)
    goto start_frontend
)
if %WAIT_COUNT% lss 40 (
    echo Waiting for backend... (%WAIT_COUNT%/40)
    goto wait_backend
)
echo [WARN] Backend not ready after 2 minutes, starting frontend anyway...

:start_frontend
echo.
echo [5/5] Starting frontend dev server (port 3000, /api -^> localhost:8080)...
call npm run dev

echo.
echo ========================================
echo   Frontend stopped. Shutting down backend...
echo ========================================
taskkill /fi "WINDOWTITLE eq AgentPlatformBackend*" /f >nul 2>&1
del "%TEMP%\agent_platform_backend.bat" >nul 2>&1
echo Dev mode stopped.

endlocal
pause
