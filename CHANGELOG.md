# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.3] - 2026-06-14
### Added
- **Verbal emulator test harness** (`CompanionEngineEmulatorTest`) — runs the offline engine on the JVM (no device) so replies/escalations can be inspected and regression-tested. 18 tests.
- Handlers: activity suggestions ("what should I do today"), positive-mood celebration (negation-guarded), short voice acknowledgements ("yes/no/ok/hmm"), dementia validation-therapy (deceased-relative confusion, time displacement, theft-paranoia, "am I in trouble"), lost-item natural voice phrasings.

### Fixed — engine quality (found via the emulator)
- **Safety net no longer hijacks normal speech.** It matched bare substrings and ran before the conversational handlers, so "television" (→vision), "Spain" (→pain), and Russian "плохой"/"больше" (→плохо/боль) were forced into a canned "call your doctor" line. Now word-boundary matching, and conversation runs before soft medical fallback.
- **SAFETY: suicidal ideation now escalates.** "I want to die" / "не хочу жить" previously got a non-escalating reply (no caregiver alert) because a "you sound tired" handler ran before the safety check. Safety now runs first; added active+passive SI detection (EN+RU) with a compassionate crisis reply that escalates EMERGENCY.
- **SAFETY: cardiac + voice-phrased scams escalate** — "chest feels tight and my arm hurts", "caller asking for my bank details".
- **Escalation calibrated against false alarms** — "I want to diet" (≠ die), birthday "gift card", paying by "credit card", a doctor/medicine mention, and ordinary sadness no longer over-escalate. Scam detection now requires pressure context.
- Question-aware fallback: unhandled questions get an honest reply instead of a story-prompt ("and what did you think when that happened?").

## [1.7.1] - 2026-06-11
### Changed
- **Store listings rewritten for the hands-free product** (en-US + ru-RU): lead with "No buttons. Just talk." — the old text described a Talk-button-first app, which no longer matches the UI (Play review checks listing accuracy).
- **Screenshots replaced** — old 13 showed the previous Talk-button UI/branding; now 4 fresh captures from a real device (hands-free main screen, caregiver settings, Device Check, PIN screen), padded to 1204×2408 (2:1 compliant). Old set kept in `play/screenshots-original-backup/`.
- **Offline engine upgraded** — answers "what day/time is it" with the real date/time (orientation help, bilingual), responds properly to "how are you"/"thank you"/"good night"/"who are you", varied non-repeating fallbacks, and steers conversation toward caregiver topics.
- Fixed "replys" typo in Device Check VIP status line.

## [1.7] - 2026-06-11
### Changed — hands-free is now THE product (user directive: "no press to talk")
- **Listening on by default** — `timedListeningHours` defaults to 12 (was 0). The app listens from first launch; Mom never needs to press anything. Caregiver can still set "Off (push-to-talk only)" in Settings as a fallback mode.
- **Talk button removed from hands-free mode** — hidden entirely while listening is active; only appears in the caregiver's push-to-talk fallback mode.
- **Service greets on start** — "Hello! I'm here and listening. Just talk to me whenever you like." (Russian when in Russian mode), so Mom knows she can just speak.
- **Banner reworded** — "🎙 I'm listening — just talk to me".
- Conversation card constrained above the banner (no overlap in either mode).

### Fixed (found via on-device QA, Samsung/Android 15)
- **ANR: infinite POST_NOTIFICATIONS request loop** — `onRequestPermissionsResult` re-ran `applyTimedListeningPreference()`, which re-requested the permission forever ("Can request only one set of permissions at a time" ~30×/s, main-thread livelock). Now guarded to request once per launch.
- **Crash: SecurityException starting microphone FGS before RECORD_AUDIO granted** (Android 14+ kills the app). `TimedListeningService.start()` is now a no-op without the permission, and `onStartCommand` double-checks and stops cleanly.

### Verified on device
- Fresh install → mic permission → first-run setup (spinner pre-set to 12 hours) → save → notification + battery-optimization prompts → main screen with NO Talk button, listening banner, green mic indicator active in status bar. Zero crashes, zero ANRs.

- versionCode 8, versionName "1.7"

## [1.6.2] - 2026-06-11
### Fixed
- **Buttons hidden on real device (critical)** — on a physical Android 15 phone the conversation CardView (elevation 12dp) z-ordered above the Talk and Call Caregiver buttons (elevation 8–10dp), hiding them almost entirely. Card is now constrained above the Talk button (`layout_constraintBottom_toTopOf="@id/talkButton"`, elevation 4dp) so no overlap is possible. Verified fixed on-device.
- **Header behind status bar** — added `fitsSystemWindows="true"` to the root layout for Android 15 edge-to-edge.
- **Secrets in git** — `my-upload-key.jks` + `key.properties` (dead placeholder scaffold) were tracked in the local repo; untracked via `git rm --cached`, `.gitignore` now blocks `*.jks` and `key.properties`. Never pushed to the GitHub remote (verified against origin/main).

### Verified on physical device (Samsung, Android 15)
- Fresh install → First Setup opens → Save → auto-returns to main screen
- Onboarding dialogs complete correctly
- Talk button → mic permission → Google speech recognizer with custom prompt
- New green Friendai branding renders correctly

## [1.6.1] - 2026-06-10
### Added
- **Release signing automated** — upload keystore generated (`keystore/friendai-upload.jks`, RSA 2048, alias `friendai`), credentials in git-ignored `keystore.properties`, `app/build.gradle` auto-signs release builds when the file exists. Signed AAB verified with `jarsigner -verify` (~2.7 MB).
- `.gitignore` now excludes `keystore.properties`, `keystore/`, and `app/google-play-service-account.json`.

## [1.6] - 2026-06-09
### Fixed
- **`promptTopics` never sent to AI backend** — `PromptBuilder` was including `rules`, `profileNotes`, and `vocabularyNotes` but silently dropping `promptTopics`. Caregiver-set topics (e.g. "Moscow", "cats", "flowers") now reach the AI and are listed as "Conversation topics Mom enjoys" in the system prompt.
- **Closed caption shown after TTS finishes instead of during** — AI reply caption is now shown immediately when the reply arrives, before `speakReply()`, with an 8-second window so Mom can read along while hearing the response.

### Added
- **Stronger bilingual instruction** in system prompt: AI is now told "ENTIRE reply must be in Russian" (or English) — reduces language mixing.
- **Bilingual conversation labels** — conversation area and closed captions now show "Вы:" / "Ответ:" in Russian mode instead of "You:" / "AI:".
- **`PRIVACY_POLICY_URL` constant** in `RulesActivity.Companion` — single place to update when the microsite goes live.
- **TroubleshootingActivity** test voice is now bilingual (Russian test phrase if Russian was selected in onboarding); lock-screen status line added.
- **`readTimeout` 20s → 30s** in `ProxyAiClient` — some AI backends are slow on cold start.

### Changed
- versionCode 7, versionName "1.6"

## [1.5] - 2026-06-09
### Added
- **Critical Russian emergency keywords** in `CompanionEngine`: "помогите" (help!), "мне плохо" (I feel unwell), "вызовите скорую" (call ambulance), "я упал" (masculine fell — was only "упала"), "нужна помощь", "нужен помощник".
- **Bilingual music reply** — `CompanionEngine` now responds in Russian ("Я пока не могу включить музыку…") when the message is Russian. Also detects "песня" (song) in addition to "музыка".
- **First-run auto-navigate** — `RulesActivity` after saving first-run setup shows "Setup complete!" then automatically calls `finish()` after 1.2s. Caregiver no longer needs to hunt for the "Done" button.
- **Play Store full-description** (en-US + ru-RU) rewritten for v1.5: all 1.3/1.4 features documented, accessibility section added, ASO keywords reinforced.

### Changed
- versionCode 6, versionName "1.5"

## [1.4] - 2026-06-09
### Added
- **Haptic feedback on Talk button** — 60ms vibration on tap confirms the button registered; vital for users with tremors or vision impairment. `VIBRATE` permission added.
- **Back press protection** — back button moves app to background instead of exiting; timed-listening service stays alive. Caregivers can still close from the recents screen.
- **Bilingual push-to-talk** — `MainActivity` now tracks `currentInputLocale` with the same Cyrillic/Latin heuristic as `TimedListeningService`. After each recognised utterance, recognition locale and TTS reply language auto-switch. Welcome greeting is in Russian if Russian was selected during onboarding.
- **Volume nudge before TTS** — if media volume is below 55% of max, raises it before speaking AI reply. Elderly users often have phones on near-silent and miss replies entirely.
- **Onboarding language preference actually applied** — selecting Russian during first-run setup now seeds `currentInputLocale = Locale("ru","RU")` in both `MainActivity` and `TimedListeningService`.
- **Russian idle check-ins and end-of-window warnings** — `TimedListeningService` speaks Russian prompts when `currentRecognitionLocale` is `ru-RU`.

### Changed
- versionCode 5, versionName "1.4"

## [1.3] - 2026-06-09
### Added
- **Boot auto-restart** — `BootReceiver` restarts `TimedListeningService` after device reboot when timed-listening was enabled. Registered for `BOOT_COMPLETED` and `QUICKBOOT_POWERON`. Uses `goAsync()` safely.
- **`RECEIVE_BOOT_COMPLETED` permission** — added to manifest with health/safety justification comment.
- **Show on lock screen** — `MainActivity.applyTimedListeningPreference()` now sets `setShowWhenLocked(true)` + `setTurnScreenOn(true)` (API 27+) or `FLAG_SHOW_WHEN_LOCKED | FLAG_TURN_SCREEN_ON` (older) when timed-listening is active. Cleared on stop.
- **Auto-bilingual recognition** — `TimedListeningService` tracks `currentRecognitionLocale`. After each recognised utterance, it counts Cyrillic vs Latin characters and auto-switches to `ru-RU` or `en-US`. TTS locale is mirrored so AI speaks back in the same language Mom used.
- **Play Store feature graphic generator** — `play/feature-graphic/generate-feature-graphic.html` (open in browser → Download PNG → 1024×500).

### Changed
- versionCode 4, versionName "1.3"
- Release notes for en-US and ru-RU updated for 1.3.

## [1.2] - 2026-06-09
### Added
- **TTS speech rate setting** — caregivers can now choose Slow / Normal / Fast in Settings. Slow mode (0.7×) gives dementia patients extra time to process AI replies. Applied to both push-to-talk and timed-listening service.
- **Auto-greeting on startup** — app speaks "Hello! I'm here. Tap the Talk button when you're ready." when TTS initialises, so Mom knows the app is ready without touching the screen (push-to-talk mode only; timed-listening mode already handles this via the service).
- **Keep screen on during timed listening** — `FLAG_KEEP_SCREEN_ON` is set on the main activity window when timed-listening is active, so Mom can read the AI's reply without having to unlock the phone.
- **PARTIAL_WAKE_LOCK in `TimedListeningService`** — CPU wake lock acquired for the duration of the listening window (bounded; auto-releases on window end or crash) so the service stays alive when the screen turns off.
- **`WAKE_LOCK` permission** in AndroidManifest — required for the partial wake lock.
- **Privacy Policy button** in Caregiver Settings — opens `https://friendai.app/privacy` in the browser. Required for Play Store submission.

### Changed
- Wake lock release added to `stopSelfCleanly` and `onDestroy` in `TimedListeningService` (no leak on early stop).
- versionCode 3, versionName "1.2"
- Release notes for en-US and ru-RU updated for 1.2.

## Features added in 1.2 loop pass 2
### Added
- **End-of-window spoken warning** — `TimedListeningService` speaks "I will stop listening in about five minutes..." 5 min before the caregiver's window expires. Mom is never blindsided by silence.
- **Idle check-in prompt** — After 15 minutes of silence, the service speaks a gentle "Are you there? How are you feeling today?" Rotates through 4 prompts. Resets on every user message. Essential for dementia patients who won't initiate.
- **`EmergencyActivity` speaks aloud** — Emergency/scam/medical escalation screen now auto-reads its title and message via TTS the moment it appears. Mom hears what happened without reading the screen.

### Fixed
- Duplicate `val settings` compile error in `EmergencyActivity` (introduced and immediately fixed this pass).

## [1.1] - 2026-06-09
### Added
- **Timed listening mode** — caregivers can now set a listening window of 1–12 hours so the app converses with Mom without her pressing any button. Essential for dementia patients who cannot reliably operate a touch interface. Configured in Caregiver Settings; the app shows an ongoing notification while active and stops automatically when the timer ends.
- `TimedListeningService` foreground service: routes all speech through the real AI pipeline (OpenAI proxy with local fallback), escalates to EmergencyActivity on urgent/scam/medical triggers, and avoids the mic-hears-itself feedback loop by pausing recognition while TTS is speaking.
- Mic-contention prevention: pressing the on-screen Talk button tells the timed-listening service to yield the microphone for ~20s to avoid both recognizers fighting.
- Proguard / R8 rules (`proguard-rules.pro`) for release builds — keeps all app entry points, Kotlin metadata, and Play Billing classes.
- `buildTypes` block in `app/build.gradle` with `minifyEnabled true` and `shrinkResources true` for release.
- `POST_NOTIFICATIONS` permission request for the timed-listening notification (Android 13+).
- Updated privacy policy and in-app notice to disclose always-listening window, visible notification, and auto-stop behavior.

### Changed
- `versionCode` bumped to 2, `versionName` to "1.1"
- Replaced deprecated `android.preference.PreferenceManager` with direct `getSharedPreferences` in CompanionEngine
- Fixed `isRunningInTest()` detection logic (always-true null check corrected)
- Fixed deprecated-override warnings in `UtteranceProgressListener` subclasses
- AGP upgraded 8.2.0 → 8.3.2, Gradle 8.2 → 8.5, Kotlin 1.9.10 → 1.9.25 — unlocks `targetSdk 35`
- `compileSdk`/`targetSdk` upgraded 34 → 35 (Play Store compliance)
- `core-ktx` upgraded 1.13.1 → 1.15.0
- Replaced `android:usesCleartextTraffic="true"` with explicit `network_security_config.xml`
- `android:allowBackup="false"` — prevents ADB backup of PIN and backend token
- `MonetizationManager` rewritten as context-based class persisting VIP/daily count to SharedPreferences (was in-memory singleton; VIP was lost on every app restart)
- Play Store listing and release notes added for en-US and ru-RU locales
- PromptBuilder system prompt significantly improved for dementia-care context
- Backend default model updated to `gpt-4o-mini`
- Backend rate limiting (30 req/min per IP) and 30s request timeout added

### Fixed
- VIP status silently revoked when billing query returned no results (offline users)
- Missing `billingClient.endConnection()` in VipActivity onDestroy (connection leak)
- Billing callback toasts called on background thread (crash on some devices)
- `EqualizerView` memory leak: `postDelayed` animation loop now stopped in `onDetachedFromWindow`
- `EqualizerView` crash when `height=0` before layout (`Random.nextInt(0,0)` → exception)
- Speech recognition error handling in TimedListeningService — different delays per error type; service stops cleanly on INSUFFICIENT_PERMISSIONS
- 0 lint errors, 0 lint warnings in both debug and release builds (was 5 errors, 142 warnings)
- Custom notification icon for timed-listening foreground service
- `listening_mode_hours` converted to proper `<plurals>` resource (Russian plurals: 1 час / 2 часа / 5 часов)
- Layout constraint conflict: footer and callCaregiverButton both constrained to parent bottom
- Ambient chime when AI responds (ToneGenerator) — removes the only TODO in the codebase

## [Unreleased]
### Added
- Modernized UI with parallax and stylish header
- Closed captioning overlay for all speech
- Onboarding dialog for first-time users
- Accessibility improvements (content descriptions, color contrast)
- Automated test for activity launch and onboarding
- CONTRIBUTING.md and privacy policy
- Push-to-talk Talk button wired with runtime mic permission request
- Real AI backend routing via AiClientFactory (OpenAI proxy + local fallback)
- EmergencyActivity escalation for urgent/scam/medical replies
- First-run caregiver setup flow on fresh install
- TroubleshootingActivity, VipActivity, PinActivity now reachable from caregiver menu

### Changed
- Refactored MainActivity for maintainability and documentation
- Simplified CompanionEngine from 150-level nested if/else to keyword-list check

### Fixed
- Gradle Play Publisher configuration
- Build issues with JAVA_HOME and signing
- Removed always-listening background service and its risky always-on mic permissions

---

## [1.0.0] - YYYY-MM-DD
- Initial release
