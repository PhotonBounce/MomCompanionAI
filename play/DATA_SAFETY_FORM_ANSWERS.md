# Google Play Data Safety Form — Copy-Paste Answer Sheet

Fill this in Play Console → App content → Data safety. Answers below match the app's
actual behavior as of v1.8 (no analytics, no ads, local-only storage, optional
free Gemini AI key or caregiver-configured AI backend).

---

## Section 1 — Overview questions

**Does your app collect or share any of the required user data types?**
→ **Yes** (voice audio is processed for speech recognition; messages are sent to the
caregiver-configured AI backend when one is set)

**Is all of the user data collected by your app encrypted in transit?**
→ **Yes** (HTTPS; the network security config blocks cleartext except where explicitly allowed)

**Do you provide a way for users to request that their data is deleted?**
→ **Yes** (all data is on-device; uninstalling or clearing app data deletes everything.
State this in the form's free-text: "All data is stored locally on the device. Clearing
app storage or uninstalling deletes all data. Nothing is retained on servers.")

---

## Section 2 — Data types

### Audio files (under "Audio")
- Collected? **Yes**
- Shared? **No** (audio goes to the device's on-device/Google speech recognizer; only the
  transcribed text continues through the app)
- Processed ephemerally? **Yes**
- Required or optional? **Required** (core voice functionality)
- Purpose: **App functionality**

### Other in-app messages (under "Messages")
- Collected? **Yes** (conversation text is sent to an AI service when one is configured)
- Shared? **Yes** — select **App functionality** as the purpose. Two scenarios:
  1. **Gemini API key set** (caregiver-optional): text is sent to Google's Gemini API
     (`generativelanguage.googleapis.com`) for AI reply generation. Google's privacy
     policy applies. Data is not stored by Google beyond the request.
  2. **Custom backend URL set** (caregiver-optional): text is sent to the caregiver's
     own server. That server is under the caregiver's control, not the developer's.
  3. **Neither set**: all processing is on-device only; no data leaves the phone.
- Processed ephemerally? **Yes** (not stored after generating the reply; rolling 3-turn
  memory in app RAM only, discarded when app closes)
- Required or optional? **Optional** (app works fully offline without any key)
- Purpose: **App functionality**

> For the "Is this data shared with third parties?" question: mark **Yes → Google** if
> you want to be conservative. Technically the Gemini API is Google's own service,
> but Play's form treats any external API as third-party sharing. Mark purpose:
> App functionality. This is the honest, reviewer-friendly answer.

### Name (under "Personal info")
- Collected? **No** (caregiver contact name is stored on-device only and never leaves
  the device — Play's definition of "collected" requires off-device transmission, so
  answer **No**)

### Phone number
- Collected? **No** (same on-device-only reasoning)

### All other categories (Location, Financial, Health, Photos, Web browsing, Identifiers,
Device IDs, Crash logs, Diagnostics, etc.)
- Collected? **No**

> Note on billing: VIP subscriptions are processed entirely by Google Play Billing.
> Google's own collection through Play doesn't need to be declared by your app.

---

## Section 3 — Security practices summary (shown on your store listing)

- Data is encrypted in transit ✓
- You can request that data be deleted ✓
- No data shared with third parties ✓
- Data isn't sold ✓

---

## Content rating questionnaire (IARC) — quick answers

- Category: **Utility / Productivity / Communication or "Other"** (companion app)
- Violence / Sexuality / Profanity / Drugs / Gambling: **None**
- User-generated content visible to others? **No**
- Users can communicate with each other? **No** (AI chat only)
- Shares user location? **No**
- Purchases? **Yes — digital goods (VIP subscription)**
- Expected rating: **Everyone / PEGI 3**

## App access (for review)

Provide review notes: "No login required. To test the always-listening mode: open the
app → tap the gear icon (top-right) → first-run setup opens directly (or enter PIN 1234
if you set one) → set 'Always-listening window' to 1 hour → Save. Emergency escalation
can be tested by saying 'I have chest pain'."

## Ads declaration
- Contains ads? **No**
