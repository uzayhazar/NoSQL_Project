#!/bin/bash

# Script to compile and run tests
# Requires: Maven and Redis

set -e

echo "=========================================="
echo "  Task 1.4 Test Runner"
echo "=========================================="
echo ""

# Check Redis
echo "Checking Redis..."
if command -v redis-cli &> /dev/null; then
    if redis-cli ping &> /dev/null; then
        echo "✓ Redis is running"
    else
        echo "✗ Redis is not running. Please start it with: redis-server"
        exit 1
    fi
else
    echo "⚠ redis-cli not found. Assuming Redis is running on localhost:6379"
fi

# Check Maven
echo "Checking Maven..."
if command -v mvn &> /dev/null; then
    MVN_CMD="mvn"
elif [ -f "./mvnw" ]; then
    MVN_CMD="./mvnw"
    chmod +x ./mvnw
else
    echo "✗ Maven not found. Please install Maven or use IntelliJ to run tests."
    echo ""
    echo "To run in IntelliJ:"
    echo "  1. Open the project in IntelliJ"
    echo "  2. Right-click on Test.java"
    echo "  3. Select 'Run Test.main()'"
    exit 1
fi

echo "✓ Maven found: $MVN_CMD"
echo ""

# Compile
echo "Compiling project..."
$MVN_CMD clean compile -q
if [ $? -eq 0 ]; then
    echo "✓ Compilation successful"
else
    echo "✗ Compilation failed"
    exit 1
fi

echo ""
echo "=========================================="
echo "  Running Tests"
echo "=========================================="
echo ""

# Run tests
$MVN_CMD exec:java -Dexec.mainClass="provided_classes.Test" -q

echo ""
echo "=========================================="
echo "  Tests Complete"
echo "=========================================="

