#!/bin/sh

if [ -z "$1" ]; then
  echo "Usage: sh $0 <version>"
  exit 1
fi

VERSION="$1"

# Update version in build.gradle.kts (coordinates)
sed -i '' "s/\(coordinates(\"io.github.payone-gmbh\", \"pcp-client-android-sdk\", \"\)[0-9]\{1,\}\.[0-9]\{1,\}\.[0-9]\{1,\}\(\")\)/\1$VERSION\2/" ./pcp-client-android-sdk/build.gradle.kts

# Update all version occurrences in README.md
sed -i '' "s/\(io.github.payone-gmbh:pcp-client-android-sdk:\)[0-9]\{1,\}\.[0-9]\{1,\}\.[0-9]\{1,\}/\1$VERSION/g" ./README.md

# Create git tag
git tag -a "v$VERSION" -m "v$VERSION"

echo "Android SDK version updated to $VERSION in pcp-client-android-sdk/build.gradle.kts and README.md"