@echo off
echo Starting Java Deserialization Payload Platform...
echo.
echo Step 1: Starting Backend (Spring Boot)
echo ==========================================
start "Backend Server" cmd /c "mvn spring-boot:run"
echo.
echo Step 2: Starting Frontend (Vue 3)
echo ==========================================
cd frontend
start "Frontend Server" cmd /c "npm run dev"
cd ..
echo.
echo Servers are starting...
echo Backend will be available at: http://localhost:8080
echo Frontend will be available at: http://localhost:3000
echo.
pause
