@echo off
echo ===== Building Deserialization Test Target =====
echo.

echo [1/3] Building Spring Boot JAR (JDK 8)...
cd /d "%~dp0"
call mvn clean package -DskipTests -q

echo.
echo [2/3] Copying JDK 7 runtime dependencies...
if not exist "target\jdk7-lib" mkdir target\jdk7-lib
call mvn dependency:copy-dependencies -DoutputDirectory=target/jdk7-lib -DincludeGroupIds=commons-collections,org.javassist -q 2>nul

echo.
echo [3/3] Starting Docker Compose...
docker compose up -d --build

echo.
echo ===== Build Complete =====
echo Dashboard:      http://localhost:8080
echo JDK8 HTTP:      http://localhost:8080/CC2 ~ /ROME
echo JDK8 TCP raw:   localhost:9090  (Socket test mode)
echo JDK7 HTTP:      http://localhost:8081/CC1 /CC3 /JDK7
pause
