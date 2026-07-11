# Friendai — Upload Day Runbook

Everything automated is done. This is the exact sequence for launch day, in order.
Estimated time: 60–90 minutes for a first-time Play Console setup.

## 0. Before you start (5 min)
- [ ] **Back up the keystore**: copy `keystore/friendai-upload.jks` AND `keystore.properties`
      to a password manager / encrypted drive / USB stick. Do this first.
- [ ] Have a Google account ready; Play Console developer registration costs a one-time $25.

## 1. Build the upload artifact (2 min)
```powershell
cd C:\Users\fucktrumpandrednecks\Desktop\MYSHIT\MomCompanionAI
$env:JAVA_HOME = "D:\jdk17"
.\gradlew.bat :app:bundleRelease
```
Artifact: `app\build\outputs\bundle\release\app-release.aab` (signed automatically, ~2.7 MB).

## 2. Play Console — create the app (10 min)
1. https://play.google.com/console → Create app
2. Name: **Friendai – Senior AI Companion** · Default language: **English (US)**
3. App (not game) · **Free** · accept declarations
4. When asked about app signing: **use Play App Signing** (default — Google holds the
   signing key; your generated keystore is the upload key).

## 3. Store listing (15 min) — all text is ready, just paste
| Field | Source file (already within Play's length limits) |
|---|---|
| Title (30) | `play/listings/en-US/title.txt` |
| Short description (80) | `play/listings/en-US/short-description.txt` |
| Full description (4000) | `play/listings/en-US/full-description.txt` |
| Phone screenshots | `play/listings/en-US/phoneScreenshots/` — 13 files, 1204×2408 (2:1 compliant) |
| App icon 512×512 | `play/store-icon/play_store_icon_512.png` (full-bleed green, Play applies corner rounding) |
| Feature graphic 1024×500 | open `play/feature-graphic/generate-feature-graphic.html` in a browser → Download PNG |
| Russian listing | add language → Russian → paste from `play/listings/ru-RU/` |

## 4. App content forms (15 min) — answers are pre-written
Open `play/DATA_SAFETY_FORM_ANSWERS.md` and copy answers into:
- [ ] **Data safety** (collects audio: yes/ephemeral; messages: optional; everything else: no)
- [ ] **Content rating (IARC)** → expect Everyone / PEGI 3
- [ ] **Ads**: No ads
- [ ] **Target audience**: 18+ (designed for adults/seniors — do NOT select children)
- [ ] **Privacy policy URL**: the page from `friendai-microsite.zip` once uploaded to your
      hostupon.com site (e.g. `https://yourdomain.com/privacy.html`).
      Then update `PRIVACY_POLICY_URL` in `RulesActivity.kt` to match and rebuild.
- [ ] **App access**: provide the review notes from the answer sheet (no login; gear icon
      → set listening window; PIN if asked: whatever you set, default flow has none)
- [ ] **Permissions declaration**: `FOREGROUND_SERVICE_MICROPHONE` + `RECEIVE_BOOT_COMPLETED`
      → justification: health/safety app for dementia patients who cannot press buttons;
      caregiver-enabled listening window with visible ongoing notification.

## 5. Billing products (10 min)
Play Console → Monetize → Subscriptions → create:
- [ ] `vip_monthly` — monthly VIP
- [ ] `vip_yearly` — yearly VIP
(IDs must match exactly — the app queries these two strings.)

## 6. Internal testing release (10 min)
1. Release → Testing → Internal testing → Create release
2. Upload `app-release.aab`
3. Release notes: paste `play/release-notes/en-US/default.txt` (and ru-RU)
4. Add your own Gmail as a tester → Save → Start rollout to Internal testing
5. Install via the opt-in link on a real phone

## 7. Smoke test on the phone (15 min)
- [ ] Fresh install → grant mic → setup screen opens with listening pre-set to 12h → Save auto-returns
- [ ] App speaks "Hello! I'm here and listening" — then just TALK (no button): reply comes back out loud
- [ ] Speak Russian → reply spoken in Russian (auto-switch, both directions)
- [ ] Ask "what day is it?" → correct spoken date (works offline)
- [ ] Listening banner + ongoing notification + green mic indicator all visible
- [ ] Lock the phone → app still visible over lock screen
- [ ] Reboot phone → listening resumes by itself
- [ ] Say "I have chest pain" → emergency screen appears and speaks
- [ ] Gear → PIN 'whatever caregiver set' → Settings opens
- [ ] Paste free Gemini key in "🤖 Free AI Key (Gemini)" → Save → talk: AI gives a smart, natural reply
- [ ] "Off (push-to-talk only)" makes Talk button appear
- [ ] VIP screen → purchase flow opens (license testers aren't charged)

## 8. Production
When the smoke test passes: Release → Production → Create release → same AAB →
roll out. First review typically takes 1–7 days for a new developer account.

---

### If review rejects `FOREGROUND_SERVICE_MICROPHONE`
Reply with: the service only starts when a caregiver explicitly enables a bounded
(1–12 h) listening window, shows a persistent notification the entire time, stops
automatically, and exists because the target users (dementia patients) cannot
reliably operate a push-to-talk button. Point to the demo video if asked.

### Versioning for future updates
Bump `versionCode` (integer, must increase) and `versionName` in `app/build.gradle`,
update `play/release-notes/*/default.txt` (≤500 chars), rebuild the AAB, upload.
