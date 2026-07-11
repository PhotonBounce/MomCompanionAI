# Release Automation Script for MomCompanionAI

# This script automates the release process: build, test, screenshots, metadata, and Play Store publish.
# Usage: .\release.ps1

# 1. Clean and build the app
./gradlew clean build

# 2. Run all unit and instrumented tests
./gradlew test connectedAndroidTest

# 3. Generate screenshots (ensure emulator/device is connected)
# (UI tests already generate screenshots in the screenshots/ directory)

# 4. Copy/update Play Store metadata and screenshots
# (Assumes play/ folder is set up as per PLAY_STORE_AUTOMATION.md)

# 5. Publish to Play Store internal track
./gradlew publishInternal

Write-Host "Release automation complete. Check Play Console for results."
