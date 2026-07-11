# Gradle Best Practices for Android Projects

- Use the latest stable Gradle and Android Gradle Plugin versions.
- Keep dependencies up to date and remove unused ones.
- Use `compileSdk` and `targetSdk` to the latest supported API.
- Use `implementation` for most dependencies, `api` only if you re-export APIs.
- Store secrets (keystore, service account) outside the repo and reference via properties.
- Use `gradle.properties` for build flags and performance options.
- Separate Play Publisher config into its own file if complex.
- Use `testOptions` to enable animationsDisabled and includeAndroidResources for reliable tests.
- Document custom Gradle tasks in comments.
- Use semantic versioning for `versionName` and increment `versionCode` for each release.

---

This project follows these practices. See `build.gradle` and `publish.gradle.kts` for examples.
