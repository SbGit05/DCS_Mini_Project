#!/usr/bin/env bash
# Application Startup Script for Distributed Banking System
set -e

PORT="${PORT:-8085}"

echo "=========================================================="
echo "Starting Distributed Banking System"
echo "Web Portal Port: $PORT"
echo "RMI Registry Port: 1099"
echo "=========================================================="

mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=$PORT"
