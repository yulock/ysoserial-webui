#!/bin/bash

set -e

echo "===== Building Deserialization Test Target ====="

echo ""
echo "[1/3] Building Spring Boot JAR (JDK 8 target)..."
cd "$(dirname "$0")"
mvn clean package -DskipTests -q

echo ""
echo "[2/3] Copying JDK 7 runtime dependencies..."
mkdir -p target/jdk7-lib
mvn dependency:copy-dependencies -DoutputDirectory=target/jdk7-lib \
    -DincludeGroupIds=commons-collections,org.javassist \
    -q 2>/dev/null || true

echo ""
echo "[3/3] Starting Docker Compose..."
docker compose up -d --build

echo ""
echo "===== Build Complete ====="
echo "Dashboard:      http://localhost:8080"
echo "JDK8 HTTP:      http://localhost:8080/CC2 ~ /ROME"
echo "JDK8 TCP raw:   localhost:9090  (for Socket test mode)"
echo "JDK7 HTTP:      http://localhost:8081/CC1 /CC3 /JDK7"
