# Privacy Policy — Friendai

**Last updated: June 11, 2026**

Friendai ("the app", "we") is designed to be a safe, private AI companion for seniors, people living with dementia, and their families. This policy explains what data the app uses, how it is handled, and your rights.

---

## 1. What data the app collects

### Voice / Speech
- Friendai is a hands-free companion: **by default the app listens while it is open**, for a caregiver-adjustable window (1–12 hours, default 12), so users who cannot press buttons can simply speak. Microphone access starts only after you grant the runtime permission.
- A **visible ongoing notification is always shown while listening is active**, and the status bar shows the standard Android microphone indicator. The caregiver can shorten the window or switch to push-to-talk-only mode at any time in Settings, and listening stops automatically when the timer ends.
- Captured audio is sent to Android's on-device speech recognition service to convert speech to text. Friendai does **not** receive the raw audio; it only receives the transcribed text.

### Conversation text
- Text from each conversation is used only to generate the current AI reply. It is **not stored on our servers**, **not logged**, and **not shared**.
- A short rolling history of recent turns (up to 3 exchanges) is kept in memory within the app session to give the AI conversational context. This memory is discarded when the app closes.

### Caregiver settings
- The caregiver's PIN, emergency contact name/number, custom rules, and backend URL are stored **on your device only** in private SharedPreferences. This data never leaves the device unless you explicitly configure a remote AI backend.
- ADB backup is disabled. This data cannot be extracted via cloud backup or Android device transfer.

### AI backend requests
- If the caregiver has configured an AI backend URL (optional), the conversation text is sent to that URL to generate replies. The backend is **caregiver-provided and -controlled** — it is not Friendai's infrastructure.
- By default (no backend configured), all AI processing happens on-device via the local rule-based companion.

### Billing
- VIP subscriptions are handled entirely through **Google Play Billing**. Friendai does not receive or store your payment information. Google's privacy policy applies to billing transactions.

### Analytics and advertising
- Friendai collects **no analytics**. We use **no third-party advertising SDKs**. We do **not sell any data**.

---

## 2. Permissions used

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Microphone for hands-free speech recognition (default listening mode, or push-to-talk fallback) |
| `INTERNET` | Optional AI backend calls (only if caregiver configures a backend URL) |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE` | Visible foreground service for timed listening mode |
| `POST_NOTIFICATIONS` | Ongoing notification shown while timed listening is active (Android 13+) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevents Android from suspending the timed-listening service mid-session (health/safety app exemption per Play Store policy) |
| `WAKE_LOCK` | Keeps CPU running during timed-listening window so the service isn't suspended |

No location, contacts, storage, camera, or other sensitive permissions are requested.

---

## 3. Data sharing

We do not share data with any third parties except:
- **Android Speech Recognition** — receives audio for on-device transcription (Google's privacy policy applies)
- **Google Play** — billing only
- **Google Gemini API** — if the caregiver configures a Gemini API key in Settings, conversation text is sent to Google's Gemini API to generate AI replies (Google's privacy policy applies; data is not retained beyond the request)
- **Your caregiver-configured AI backend** — if and only if the caregiver sets a custom backend URL in Settings

---

## 4. Data retention

- No conversations are stored after the app session ends.
- Caregiver settings persist on-device until the app is uninstalled or the caregiver resets them.

---

## 5. Children's privacy

Friendai is not directed at children under 13. It is designed for adults — specifically seniors and their adult caregivers.

---

## 6. Security

- All caregiver settings are protected by a PIN (minimum 4 digits).
- There is a 30-second lockout after 5 incorrect PIN attempts.
- ADB backup is disabled so settings cannot be extracted from the device without the PIN.
- Backend communication uses HTTPS (or HTTP if the caregiver explicitly sets an HTTP URL for a local private server).

---

## 7. Changes to this policy

We will update this page when the policy changes. The "Last updated" date at the top reflects the current version. Continued use of the app after changes constitutes acceptance.

---

## 8. Contact

For privacy questions, please contact the person who set up this app (your caregiver), or the app developer via the Google Play Store listing.

---

*Friendai is built with love for families who want their loved ones to feel heard, safe, and never alone.*
