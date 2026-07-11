# Release Checklist

Before each release, complete the following:

- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update CHANGELOG.md with new features, fixes, and changes
- [ ] Review and update PRIVACY_POLICY.md if needed
- [ ] Run all automated tests (unit, UI, integration)
- [ ] Verify accessibility (content descriptions, color contrast, captions)
- [ ] Capture and update screenshots for Play Store
- [ ] Review README.txt and CONTRIBUTING.md for accuracy
- [ ] Build signed release AAB
- [ ] Publish to Play Store internal track (or alpha/beta/production)
- [ ] Verify Play Console for warnings or errors
- [ ] Run .\release.ps1 to automate build, test, screenshots, metadata, and Play Store publish

---

Automate as many steps as possible for future releases.
