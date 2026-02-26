#!/bin/sh

# PCP Android SDK Test Runner Script
# Runs unit tests for the pcp-client-android-sdk module

echo "Running Android SDK tests..."
echo ""

# Run tests
./gradlew :pcp-client-android-sdk:test

# Check if tests passed
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ All tests passed successfully!"
    echo ""
    echo "📊 View detailed test report:"
    echo "   open pcp-client-android-sdk/build/reports/tests/testDebugUnitTest/index.html"
    echo ""
    exit 0
else
    echo ""
    echo "❌ Tests failed. Check the report for details:"
    echo "   open pcp-client-android-sdk/build/reports/tests/testDebugUnitTest/index.html"
    echo ""
    exit 1
fi
