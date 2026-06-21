@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"
echo ========================================
echo   Agent Platform - 一键构建打包
echo ========================================

echo.
echo [1/5] 检查前端依赖...
if not exist "node_modules\" (
    echo 安装 npm 依赖...
    call npm install
    if errorlevel 1 (
        echo [ERROR] npm install 失败
        pause
        exit /b 1
    )
) else (
    echo 前端依赖已就绪
)

echo.
echo [2/5] 构建前端产物 (tsc + vite build)...
call npm run build
if errorlevel 1 (
    echo [ERROR] 前端构建失败
    pause
    exit /b 1
)

echo.
echo [3/5] 同步静态资源到 Spring Boot...
set "STATIC_DIR=src\main\resources\static"
if not exist "%STATIC_DIR%" (
    mkdir "%STATIC_DIR%"
)
if exist "%STATIC_DIR%\*" (
    del /q /s "%STATIC_DIR%\*" >nul 2>&1
    for /d %%i in ("%STATIC_DIR%\*") do rmdir /q /s "%%i"
)
xcopy /e /y dist\* "%STATIC_DIR%" >nul
echo dist 已复制到 %STATIC_DIR%

echo.
echo [4/5] Maven 打包 (跳过测试)...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo [ERROR] Maven 打包失败
    pause
    exit /b 1
)

echo.
echo [5/5] 构建完成!
echo ========================================
echo   产物: target\agent-platform-1.0.0-SNAPSHOT.jar
echo ========================================

endlocal
pause
