# Contributor Guide

Welcome to Friendai! This guide will help you get started as a contributor.

## Prerequisites
- Android Studio (latest stable)
- JDK 17 (Microsoft OpenJDK recommended)
- Android SDK and emulator/device
- Node.js (for backend, optional)
- Git

## Setup
1. Clone the repo
2. Open in Android Studio
3. Let Gradle sync and download dependencies
4. Set up your local.properties and key.properties (see README)

## Build & Run
- To build: `./gradlew build`
- To run on device/emulator: Use Android Studio Run button
- To run tests: `./gradlew test connectedAndroidTest`

## Play Store Automation
- See PLAY_STORE_AUTOMATION.md for metadata and screenshot automation
- To publish: `./gradlew publishInternal` (internal track)

## Release Automation
- Run `./release.ps1` to automate build, test, screenshots, and publish

## Contributing
- Follow the code style and KDoc conventions
- All code must pass CI and tests
- See CODE_OF_CONDUCT.md and SECURITY.md

## Need Help?
- See FAQ below or open an issue

---
