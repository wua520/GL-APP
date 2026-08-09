@echo off
chcp 65001 >nul
echo ========================================
echo 重启Spring Boot服务
echo ========================================
echo.

echo [1/3] 查找占用8080端口的进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080') do (
    set PID=%%a
    goto :found
)
echo 未找到占用8080端口的进程
goto :start

:found
echo 找到进程 PID: %PID%
echo [2/3] 结束进程...
taskkill /PID %PID% /F
timeout /t 2 >nul

:start
echo [3/3] 启动服务...
echo.
call mvnw.cmd spring-boot:run
