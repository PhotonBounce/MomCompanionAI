# Friendai Backend

This is the small proxy the Android app should call for AI replies. Keep the OpenAI API key here, not inside the APK.

## Run

PowerShell:

```powershell
$env:OPENAI_API_KEY="sk-your-key-here"
$env:OPENAI_MODEL="gpt-4o-mini"
$env:FRIENDAI_BACKEND_TOKEN="change-this-shared-token"
npm start
```

The server listens on port `8787` by default.

Health check:

```powershell
Invoke-RestMethod http://localhost:8787/health
```

Test reply:

```powershell
Invoke-RestMethod http://localhost:8787/companion/reply `
  -Method Post `
  -Headers @{ Authorization = "Bearer change-this-shared-token" } `
  -ContentType "application/json" `
  -Body '{"instructions":"Be warm and brief.","input":"Hello"}'
```

## Android URL

Use this in the app's `Caregiver Settings > AI Backend URL` field. The app accepts either the base URL or the full reply path.

- Android emulator: `http://10.0.2.2:8787`
- Physical phone: `http://YOUR_COMPUTER_LAN_IP:8787`

If `FRIENDAI_BACKEND_TOKEN` is set on the backend, put the same value in `Caregiver Settings > AI Backend Token`.

For physical phone testing, the computer and phone must be on the same network and Windows Firewall must allow inbound traffic to Node.js on port `8787`.
