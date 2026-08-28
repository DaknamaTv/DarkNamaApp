#!/bin/bash
# Script to verify Gradle wrapper integrity

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

echo "Verifying Gradle wrapper integrity..."

# Check if gradle-wrapper.jar exists
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "ERROR: gradle-wrapper.jar not found!"
    exit 1
fi

# Check if checksum file exists
if [ ! -f "gradle/wrapper/gradle-wrapper.jar.sha256" ]; then
    echo "ERROR: gradle-wrapper.jar.sha256 not found!"
    exit 1
fi

# Verify checksum
cd gradle/wrapper

# Use sha256sum if available, otherwise try shasum (macOS)
if command_exists sha256sum; then
    COMPUTED_CHECKSUM=$(sha256sum gradle-wrapper.jar | cut -d ' ' -f 1)
elif command_exists shasum; then
    COMPUTED_CHECKSUM=$(shasum -a 256 gradle-wrapper.jar | cut -d ' ' -f 1)
else
    echo "ERROR: No checksum tool available (sha256sum or shasum)"
    exit 1
fi

# Extract only the hash (first field) - the file may contain "hash  filename" format from sha256sum
STORED_CHECKSUM=$(awk '{print $1}' gradle-wrapper.jar.sha256 | tr -d ' \t\n')

if [ "$COMPUTED_CHECKSUM" = "$STORED_CHECKSUM" ]; then
    echo "✓ Gradle wrapper checksum verification PASSED"
    echo "Checksum: $STORED_CHECKSUM"
    exit 0
else
    echo "✗ Gradle wrapper checksum verification FAILED"
    echo "Computed: $COMPUTED_CHECKSUM"
    echo "Stored:   $STORED_CHECKSUM"
    exit 1
fi