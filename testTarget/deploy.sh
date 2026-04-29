#!/bin/bash

set -e

echo "===== Deploying Deserialization Test Target ====="
echo "(Docker-only mode — no Maven/JDK required on host)"
echo ""

docker compose up -d --build

echo ""
echo "===== Running ====="
echo "Dashboard:      http://localhost:8080"
echo "JDK8 HTTP:      http://localhost:8080/CC2 ~ /ROME"
echo "JDK8 TCP raw:   localhost:9090  (Socket test mode)"
echo "JDK7 HTTP:      http://localhost:8081/CC1 /CC3 /JDK7"
