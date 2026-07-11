# Google Play Store Launch Checklist — Friendai

## ✅ Done (automated by Claude Code)

### App Build
- [x] `assembleRelease` builds cleanly (0 errors, 0 warnings)
- [x] `lintRelease` passes: 0 errors, 0 warnings
- [x] targetSdk 35, compileSdk 35 (AGP 8.3.2 + Gradle 8.5)
- [x] minSdk 24 (Android 7.0 — covers 99%+ of active devices)
- [x] versionCode 9, versionName "1.8"
- [x] ProGuard/R8 minification + resource shrinking for release
- [x] Network security config (replaces blanket `usesCleartextTraffic`)
- [x] Proguard rules for Play Billing, Kotlin, OpenAI client

### Store Listing
- [x] `play/listings/en-US/title.txt` — "Friendai – Senior AI Companion"
- [x] `play/listings/en-US/short-description.txt`
- [x] `play/listings/en-US/full-description.txt`
- [x] `play/listings/ru-RU/title.txt` — "Friendai – ИИ-компаньон для пожилых"
- [x] `play/listings/ru-RU/short-description.txt`
- [x] `play/listings/ru-RU/full-description.txt`
- [x] `play/listings/en-US/phoneScreenshots/` — 4 screenshots (1204×2408, 2:1 compliant)
- [x] `play/release-notes/en-US/default.txt`
- [x] `play/release-notes/ru-RU/default.txt`

### Core Features
- [x] Push-to-talk voice conversation
- [x] Timed listening mode (0–12 hours, caregiver-configurable)
- [x] Bilingual English/Russian
- [x] PIN-protected caregiver settings
- [x] Emergency escalation (medical / scam / distress)
- [x] VIP subscription via Google Play Billing (persistent across restarts)
- [x] Free tier: 20 AI replies/day
- [x] Battery optimization exemption request
- [x] Foreground service with visible notification
- [x] Privacy policy (`PRIVACY_POLICY.md`)

### Code Quality
- [x] Dementia-care tuned system prompt in PromptBuilder
- [x] Rate limiting + 30s timeout on backend
- [x] VIP billing lifecycle (endConnection, no offline revocation)
- [x] PIN brute-force protection (5 attempts, 30s lockout)
- [x] AI reply counter persisted to SharedPreferences (not in-memory)
- [x] TTS speech rate setting (Slow/Normal/Fast) — Caregiver Settings
- [x] Auto-greeting when TTS ready (push-to-talk mode)
- [x] Keep screen on during timed listening (`FLAG_KEEP_SCREEN_ON`)
- [x] `PARTIAL_WAKE_LOCK` in `TimedListeningService` (bounded; CPU stays awake when screen off)
- [x] Privacy Policy button in Caregiver Settings → opens `https://friendai.app/privacy`
- [x] `PRIVACY_POLICY.md` — comprehensive, covers all Google Play required sections
- [x] `backend/server.js` serves `GET /privacy` as full HTML privacy policy page
- [x] Play Store full description rewritten (en-US + ru-RU) with better ASO keywords
- [x] Short description updated for better search conversion
- [x] Conversation text scrollable in MainActivity
- [x] `caregiverSettingsButton` touch target 44dp → 48dp (accessibility minimum)
- [x] `BootReceiver` — auto-restarts timed listening after phone reboot (`BOOT_COMPLETED` + `QUICKBOOT_POWERON`)
- [x] `RECEIVE_BOOT_COMPLETED` permission — documented as health/safety in manifest
- [x] Show on lock screen + turn screen on when timed listening active (`setShowWhenLocked` / `FLAG_SHOW_WHEN_LOCKED`)
- [x] Auto-bilingual recognition — `TimedListeningService` auto-detects Russian vs English from Cyrillic/Latin characters, switches recognition locale and TTS language mid-session
- [x] Feature graphic generator — `play/feature-graphic/generate-feature-graphic.html` (open in browser, download 1024×500 PNG)
- [x] ProGuard: `BootReceiver` covered by `-keep public class * extends android.content.BroadcastReceiver`
- [x] Haptic feedback on Talk button (60ms, `VIBRATE` permission)
- [x] Back press → `moveTaskToBack` (app stays alive, timed listening uninterrupted)
- [x] Bilingual push-to-talk: `currentInputLocale` auto-switches on Cyrillic/Latin detection
- [x] Volume nudge: raises media volume to ≥55% before TTS speaks
- [x] Onboarding language preference applied to both `MainActivity` and `TimedListeningService`
- [x] Russian idle check-ins and end-of-window warning in `TimedListeningService`
- [x] versionCode 9, versionName "1.8"
- [x] Direct Gemini AI in app — `GeminiAiClient` calls Gemini 2.0 Flash from Android, no server needed
- [x] `geminiApiKey` field in Caregiver Settings — caregiver pastes free key from aistudio.google.com/apikey
- [x] AI priority: Gemini key → backend URL → offline rule engine
- [x] Proactive conversation: 45s opener after greeting, idle check-in every 3 min
- [x] Offline engine: date/time answers, varied responses, topic steering

---

## ⏳ Manual Steps Required (cannot be automated)

### 1. Signing Config — ✅ DONE (automated)
- Upload keystore generated at `keystore/friendai-upload.jks` (RSA 2048, valid ~27 years, alias `friendai`)
- Credentials in `keystore.properties` at the repo root (random 24-char password)
- `app/build.gradle` auto-signs release builds when `keystore.properties` exists
- Signed AAB verified: `app/build/outputs/bundle/release/app-release.aab` (~2.7 MB, `jarsigner -verify` passed)
- Both files are git-ignored — they will NOT be committed

⚠️ **BACK UP `keystore/friendai-upload.jks` and `keystore.properties` somewhere safe NOW**
(password manager, encrypted drive). If lost before first upload, regenerate; if lost after,
use Play App Signing's upload-key reset. Enroll in **Play App Signing** during first upload
(default for new apps) so Google holds the app signing key.

### 2. Google Play Service Account (for automated publishing)
To use `./gradlew publishReleaseBundle`:
1. Go to Google Play Console → Setup → API access
2. Link to a Google Cloud project
3. Create a service account with "Release manager" permissions
4. Download the JSON key → save as `app/google-play-service-account.json`
5. **DO NOT commit this file** (it's a secret credential)

### 3. Content Rating Questionnaire (in Play Console)
Answer the IARC questionnaire:
- App category: Tools / Health & Fitness
- Violence: None
- Sexuality: None
- Language: None (mild only if any)
- **Expected rating**: Everyone / PEGI 3

### 4. Data Safety Declaration (in Play Console)
Fill in the data safety form. Based on this app:

| Data type | Collected? | Shared? | Notes |
|-----------|-----------|---------|-------|
| Voice data | Yes — processed, not stored | No | Real-time speech recognition only |
| Name/contact | Optional — stays on device | No | Caregiver contact stored locally |
| App interactions | No | No | |
| Device/Other IDs | No | No | |

**Key disclosures:**
- Data encrypted in transit (HTTPS to AI backend)
- Data not sold
- Users can delete all data by clearing app data

### 5. App Review / Play Policies to Address
- [x] Sensitive permissions declared: `RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE`
- [x] `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — disclose in store listing (justified for health app)
- [ ] **Contact Play Store support** if the app is rejected for `FOREGROUND_SERVICE_MICROPHONE` —
      explain the medical/care use case (dementia patients who cannot press buttons)

### 6. Google Play Billing Products
Create these subscription products in Play Console:
- Product ID: `vip_monthly` — Monthly VIP subscription
- Product ID: `vip_yearly` — Annual VIP subscription

### 7. AI Setup (pick one — no server needed for option A)

**Option A — Free Gemini key (recommended, no server):**
1. Get a free key at aistudio.google.com/apikey (no credit card)
2. Open app → gear → Settings → paste into "🤖 Free AI Key (Gemini)" → Save
3. Done — app calls Gemini 2.0 Flash directly from the phone

**Option B — Self-hosted backend:**
Deploy `backend/server.js` to a server. Set env vars:
- `GEMINI_API_KEY` — free key from aistudio.google.com/apikey (preferred)
- OR `OPENAI_API_KEY` — requires paid OpenAI account
- `FRIENDAI_BACKEND_TOKEN` — random secret (≥32 chars)
- `PORT` — default 8787

Then enter the backend URL + token in Caregiver Settings (AI Backend URL/Token fields).

### 8. Screenshots Review — ✅ FIXED (automated)
13 screenshots in `play/listings/en-US/phoneScreenshots/`:
- Were 1080×2408 (ratio 2.23:1) — **would have been REJECTED** (Play requires max
  dimension ≤ 2× min dimension)
- Now padded to **1204×2408 (exactly 2:1)** with dark side bars matching the app theme
- Originals backed up in `play/screenshots-original-backup/`
- Remaining manual check: eyeball that they show current UI (haptics/banner changes
  don't alter visuals much, so existing shots are still representative)

### 8b. Data Safety + Content Rating — ✅ ANSWER SHEET READY
Copy-paste answers prepared in `play/DATA_SAFETY_FORM_ANSWERS.md` (data types, security
practices, IARC questionnaire, app-access review notes, ads declaration).

### 8c. Listing text length limits — ✅ FIXED (automated)
Three fields exceeded Play's hard limits and **would have been rejected**:
- en-US short description: was 94 chars → now 76 (limit 80)
- ru-RU title: was 35 chars → now 25 (limit 30)
- ru-RU short description: was 88 chars → now 75 (limit 80)
- Release notes (both locales): were ~650 chars → now ≤457 (limit 500)
- en-US title: 30/30 exactly — OK as is
- Full descriptions: 3389 and 2916 chars (limit 4000) — OK

### 8e. App icon — ✅ FIXED (automated)
- Launcher icon said **"Mom's AI"** in orange — off-brand (app is "Friendai", green theme
  everywhere else) and there was **no 512×512 store icon at all** (required field).
- Regenerated: green-gradient "Friendai" icon at all 5 mipmap sizes (circle, in-app) +
  full-bleed `play/store-icon/play_store_icon_512.png` for the Console (Play rounds corners).
- AAB rebuilt with new icons and signature re-verified.

### 8d. Upload-day runbook — ✅ `UPLOAD_DAY_RUNBOOK.md`
Single ordered checklist for launch day: keystore backup, AAB build, Console setup,
listing paste-map, content forms, billing products, internal testing, on-phone smoke
test (incl. reboot + lock-screen checks), production rollout, and a prepared response
if review questions the microphone foreground service.

### 9. Internal Testing Track
Before going to production:
1. Build and sign: `./gradlew bundleRelease`
2. Upload to Play Console → Internal Testing
3. Test on a real device with a dementia-appropriate user
4. Test the VIP purchase flow
5. Test timed listening mode for 1+ hours
6. Test emergency escalation

---

## Notes on Play Store Policies (potential friction points)

**`FOREGROUND_SERVICE_MICROPHONE`**
Google requires a permission declaration. We have it. The foreground service is only started when the caregiver explicitly enables timed listening. The visible notification is shown. This should pass review.

**`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`**
Google's policy: permitted for healthcare apps where continuous operation is essential. Friendai is exactly that — timed listening for dementia patients who cannot press buttons. Disclose in store description (already done).

**`usesCleartextTraffic` replacement**
We replaced the blanket flag with `network_security_config.xml`. This is more explicit and reviewable by Google's policy team.
