# Phone Test Checklist

## Build

From the project root:

```powershell
$env:JAVA_HOME="C:\Program Files (x86)\Android\openjdk\jdk-17.0.14"
.\gradlew.bat :app:assembleDebug
```

APK:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Backend

From `backend`:

```powershell
$env:OPENAI_API_KEY="sk-your-key-here"
$env:OPENAI_MODEL="gpt-5-mini"
$env:MOM_COMPANION_BACKEND_TOKEN="make-up-a-private-token"
npm start
```

Health check:

```powershell
Invoke-RestMethod http://localhost:8787/health
```

## App Settings

On first launch, the app opens setup automatically. If setup was already completed, open the app, tap `Caregiver Rules`, and enter the PIN.

Default PIN:

```text
1234
```

Set:

- `Caregiver Name`
- `Caregiver Phone`
- `AI Backend URL`
- `AI Backend Token`
- Review `Basic Vocabulary (English / Russian)` and add family-specific phrases
- `Test Message`, then tap `Test Local Rules`
- Tap `Test AI Backend` after the backend URL/token are set
- Replace the default PIN before handing the phone to Mom

Backend URL examples:

- Emulator: `http://10.0.2.2:8787`
- Physical phone: `http://YOUR_COMPUTER_LAN_IP:8787`

## Mom Mode Test

1. Tap `Talk`.
2. Grant microphone permission.
3. Say `hello`.
4. Confirm the transcript shows Mom's words.
5. Confirm the app speaks back.
6. Say `I feel lonely`.
7. Confirm the response is calm and supportive.
8. Say `мне одиноко`.
9. Confirm the response can answer in simple Russian.
10. Say `someone wants a gift card`.
11. Confirm the app warns not to share money or codes.
12. Confirm the warning opens the pause/help screen.
13. Tap `Help / Contact`.
14. Confirm the caregiver call button appears.
15. Tap `Device Check`.
16. Confirm microphone, speech recognition, voice, caregiver contact, and backend settings are shown.
17. Tap `Test Voice`.
18. Tap `Check Backend` if a backend URL is configured.

## Tablet Test

1. Install the same APK on the tablet.
2. Rotate the tablet to landscape.
3. Confirm Mom Mode shows the transcript on one side and the Talk/contact buttons on the other.
4. Open Caregiver Settings and confirm the form stays centered instead of stretching across the whole screen.

## Before Real Use

- Replace the default PIN.
- Use HTTPS before exposing the backend outside your local network.
- Keep `OPENAI_API_KEY` only on the backend.
- Test on Mom's actual phone screen size and Android version.
