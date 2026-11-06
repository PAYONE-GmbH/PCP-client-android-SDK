#!/bin/sh

# PCP Android SDK Test Runner Script
# Runs SDK and demo unit tests (requires JDK 21 for API 36+ Robolectric).

echo "Running Android SDK tests..."
echo ""

# Run tests
./gradlew testDebugUnitTest

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
