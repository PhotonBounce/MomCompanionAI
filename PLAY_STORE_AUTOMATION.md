# Play Store Metadata & Screenshot Automation

This project supports Play Store automation using Gradle Play Publisher.

## How to Automate Metadata
- Place your Play Store listing text in the `play/` folder:
  - `play/listing/title.txt`
  - `play/listing/short_description.txt`
  - `play/listing/full_description.txt`
  - `play/listing/en-US/graphics/phone-screenshots/` (for screenshots)
- Gradle Play Publisher will upload these files when you run:
  - `./gradlew publishInternal` (for internal track)
  - Or use `publishAlpha`, `publishBeta`, `publishProduction` for other tracks

## Example Directory Structure
```
play/
  listing/
    title.txt
    short_description.txt
    full_description.txt
    en-US/
      graphics/
        phone-screenshots/
          screenshot1.png
          screenshot2.png
```

## References
- See [Gradle Play Publisher docs](https://github.com/Triple-T/gradle-play-publisher#quickstart)
- See PLAY_STORE_METADATA_TEMPLATE.md for sample text
- See screenshots/README.md for screenshot automation

---

Automate your Play Store listing for faster, error-free releases!
