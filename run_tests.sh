#!/usr/bin/env bash
# Test Execution Script for Distributed Banking System
set -e

echo "=========================================================="
echo "Running Distributed Banking System Automated Test Suite"
echo "=========================================================="

mvn test

echo "All tests passed successfully!"
