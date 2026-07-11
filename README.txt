Friendai

CURRENT PROTOTYPE:
- Tap Talk to launch Android speech recognition
- First launch opens setup so the caregiver can replace the default PIN and contact info
- Phone and tablet layouts are supported; landscape tablets show conversation and controls side by side
- Captures what Mom says and shows it in a transcript
- Replies through the configured backend when available
- Falls back to a local fake companion engine when backend is not configured
- Speaks replies with Android TextToSpeech
- Device Check screen verifies microphone, speech recognition, voice, caregiver contact, backend URL/token, setup status, and layout
- PIN-protected Caregiver Settings screen saves prompt/profile notes locally
- Caregiver Settings includes starter English/Russian vocabulary notes
- Caregiver Settings can test local rules and the real AI backend before Mom uses it
- Help/contact screen can dial caregiver or emergency services after a tap
- AI backend token can protect the local backend during phone testing
- Recent conversation context is sent to the backend so replies can follow the thread

DEFAULT CAREGIVER PIN:
1234

BACKEND:
1. Open a terminal in backend
2. Set OPENAI_API_KEY in the server environment
3. Set FRIENDAI_BACKEND_TOKEN to a private shared token
4. Run npm start
5. In the Android app, open Caregiver Settings and set AI Backend URL and AI Backend Token
6. Use Test AI Backend in Caregiver Settings to confirm the phone can reach the backend

Backend URLs:
- Emulator: http://10.0.2.2:8787
- Physical phone: http://YOUR_COMPUTER_LAN_IP:8787

HOW TO RUN:
1. Install Android Studio
2. Open this folder
3. Let Gradle sync
4. Connect Android phone
5. Press RUN
6. Complete first setup before handing the phone to Mom

APK build:
.\gradlew.bat :app:assembleDebug

APK path:
app\build\outputs\apk\debug\app-debug.apk

Phone testing checklist:
PHONE_TEST_CHECKLIST.md

---

## Badges

![Android CI](https://github.com/PhotonBounce/lna/actions/workflows/android-ci.yml/badge.svg)

---

## Contributing

See CONTRIBUTING.md for guidelines. All code must pass CI and include KDoc for new Kotlin classes/methods.

## Continuous Integration

All pushes and pull requests to `main` are automatically built, tested, and linted via GitHub Actions. See CI_CD.md for details.

## Release Process

- Update CHANGELOG.md and version in app/build.gradle
- Complete RELEASE_CHECKLIST.md
- Build signed AAB and publish via Gradle Play Publisher

## Accessibility & Onboarding

- All UI elements have content descriptions and color contrast is checked.
- Closed captioning overlays are provided for all speech.
- Onboarding dialog appears on first launch for caregiver setup.

## Play Store Compliance

- See PRIVACY_POLICY.md for privacy details.
- Screenshots and QA steps: PHONE_TEST_CHECKLIST.md
