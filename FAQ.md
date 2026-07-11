# FAQ: Friendai

**Q: How do I build and run the app?**
A: Open in Android Studio, let Gradle sync, connect a device/emulator, and press Run. Or use `./gradlew build`.

**Q: How do I run tests?**
A: Use `./gradlew test connectedAndroidTest`. See test results in Android Studio or the terminal.

**Q: How do I automate Play Store publishing?**
A: See PLAY_STORE_AUTOMATION.md. Use `./gradlew publishInternal` for the internal track, or run `./release.ps1` for full automation.

**Q: Where are screenshots for the Play Store?**
A: See the `screenshots/` directory. UI tests generate screenshots automatically.

**Q: How do I update Play Store metadata?**
A: Edit files in the `play/` folder as described in PLAY_STORE_AUTOMATION.md.

**Q: How do I report a bug or security issue?**
A: For bugs, open a GitHub issue. For security, see SECURITY.md and report privately.

**Q: How do I contribute?**
A: See CONTRIBUTOR_GUIDE.md and follow the code style, CI, and code of conduct.

**Q: Is analytics or crash reporting enabled?**
A: No, analytics and crash reporting are opt-in only and currently not enabled by default. See ANALYTICS_PLAN.md.

---
