# Continuous Integration (CI) with GitHub Actions

This project uses GitHub Actions to automatically build, lint, and test the app on every push and pull request to `main`.

- Workflow file: `.github/workflows/android-ci.yml`
- Runs on Ubuntu with JDK 17
- Caches Gradle dependencies for faster builds
- Runs unit and (if possible) instrumented tests
- Runs lint for code quality

## How it works
1. Checks out the code
2. Sets up JDK 17
3. Caches Gradle dependencies
4. Builds the app
5. Runs all tests
6. Runs lint

See the workflow file for details. All contributors should ensure the CI passes before merging changes.
