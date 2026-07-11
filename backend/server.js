import http from "node:http";

const PORT = Number(process.env.PORT || 8787);
const OPENAI_MODEL = process.env.OPENAI_MODEL || "gpt-4o-mini";
const MAX_OUTPUT_TOKENS = Number(process.env.OPENAI_MAX_OUTPUT_TOKENS || 220);
const BACKEND_TOKEN = process.env.FRIENDAI_BACKEND_TOKEN || "";
const MAX_BODY_BYTES = 64 * 1024;
const MAX_TEXT_CHARS = 12000;
const OPENAI_TIMEOUT_MS = 30_000;   // 30s — enough for a thoughtful reply, not enough to hang a user
const RATE_LIMIT_WINDOW_MS = 60_000; // per-IP: max requests per minute
const RATE_LIMIT_MAX = 30;           // generous for a companion app, stops runaway abuse

/** Simple in-memory rate limiter: { ip → { count, windowStart } } */
const rateLimitMap = new Map();

const server = http.createServer(async (req, res) => {
  const start = Date.now();
  res.on("finish", () => {
    const ms = Date.now() - start;
    const ip = req.socket?.remoteAddress || "-";
    console.log(`${new Date().toISOString()} ${req.method} ${req.url} ${res.statusCode} ${ms}ms ${ip}`);
  });

  try {
    const requestUrl = new URL(req.url || "/", "http://localhost");

    if (req.method === "OPTIONS") {
      sendJson(res, 204, {});
      return;
    }

    if (req.method === "GET" && requestUrl.pathname === "/health") {
      sendJson(res, 200, { ok: true });
      return;
    }

    if (req.method === "POST" && requestUrl.pathname === "/companion/reply") {
      if (!checkRateLimit(req, res)) return;
      await handleCompanionReply(req, res);
      return;
    }

    if (req.method === "GET" && requestUrl.pathname === "/privacy") {
      servePrivacyPage(res);
      return;
    }

    sendJson(res, 404, { error: "Not found" });
  } catch (error) {
    sendJson(res, 500, {
      error: error instanceof Error ? error.message : "Unexpected server error"
    });
  }
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`Mom's AI backend listening on http://0.0.0.0:${PORT}`);
});

async function handleCompanionReply(req, res) {
  if (!isAuthorized(req)) {
    sendJson(res, 401, { error: "Unauthorized" });
    return;
  }

  const hasGemini = isRealKey(process.env.GEMINI_API_KEY);
  const hasOpenAi = isRealKey(process.env.OPENAI_API_KEY);
  if (!hasGemini && !hasOpenAi) {
    sendJson(res, 500, { error: "No AI key configured. Set GEMINI_API_KEY (free at aistudio.google.com/apikey) or OPENAI_API_KEY in backend/.env." });
    return;
  }

  const body = await readJson(req);
  const instructions = typeof body.instructions === "string" ? body.instructions.trim() : "";
  const input = typeof body.input === "string" ? body.input.trim() : "";

  if (!instructions || !input) {
    sendJson(res, 400, { error: "Both instructions and input are required." });
    return;
  }

  if (instructions.length > MAX_TEXT_CHARS || input.length > MAX_TEXT_CHARS) {
    sendJson(res, 400, { error: "Instructions or input are too long." });
    return;
  }

  // Prefer Gemini when configured — its free tier needs no payment method, which is
  // the most accessible option for families running this backend at home.
  if (hasGemini) {
    await requestGeminiReply(res, instructions, input);
  } else {
    await requestOpenAiReply(res, instructions, input);
  }
}

/** Placeholder values from .env templates don't count as configured keys. */
function isRealKey(value) {
  return Boolean(value && value.trim() && !/^PASTE|^sk-your|^change-this/i.test(value.trim()));
}

async function requestGeminiReply(res, instructions, input) {
  const model = process.env.GEMINI_MODEL || "gemini-2.0-flash";
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), OPENAI_TIMEOUT_MS);

  let response;
  try {
    response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`,
      {
        method: "POST",
        signal: controller.signal,
        headers: {
          "x-goog-api-key": process.env.GEMINI_API_KEY,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          system_instruction: { parts: [{ text: instructions }] },
          contents: [{ role: "user", parts: [{ text: input }] }],
          generationConfig: { maxOutputTokens: MAX_OUTPUT_TOKENS, temperature: 0.8 }
        })
      }
    );
  } catch (fetchErr) {
    if (fetchErr.name === "AbortError") {
      sendJson(res, 504, { error: "AI backend timed out. Please try again." });
    } else {
      sendJson(res, 502, { error: "Could not reach Gemini." });
    }
    return;
  } finally {
    clearTimeout(timeoutId);
  }

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    sendJson(res, response.status, { error: data.error?.message || "Gemini request failed." });
    return;
  }

  const reply = (data.candidates?.[0]?.content?.parts || [])
    .map((p) => p.text || "")
    .join("")
    .trim();
  if (!reply) {
    sendJson(res, 502, { error: "Gemini response did not include text." });
    return;
  }

  sendJson(res, 200, { reply });
}

async function requestOpenAiReply(res, instructions, input) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), OPENAI_TIMEOUT_MS);

  let openAiResponse;
  try {
    openAiResponse = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      signal: controller.signal,
      headers: {
        "Authorization": `Bearer ${process.env.OPENAI_API_KEY}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: OPENAI_MODEL,
        instructions,
        input,
        max_output_tokens: MAX_OUTPUT_TOKENS,
        store: false
      })
    });
  } catch (fetchErr) {
    if (fetchErr.name === "AbortError") {
      sendJson(res, 504, { error: "AI backend timed out. Please try again." });
    } else {
      sendJson(res, 502, { error: "Could not reach AI service." });
    }
    return;
  } finally {
    clearTimeout(timeoutId);
  }

  const data = await openAiResponse.json().catch(() => ({}));

  if (!openAiResponse.ok) {
    sendJson(res, openAiResponse.status, {
      error: data.error?.message || "OpenAI request failed."
    });
    return;
  }

  const reply = extractOutputText(data);
  if (!reply) {
    sendJson(res, 502, { error: "OpenAI response did not include output text." });
    return;
  }

  sendJson(res, 200, { reply });
}

function checkRateLimit(req, res) {
  const ip = req.socket.remoteAddress || "unknown";
  const now = Date.now();
  let entry = rateLimitMap.get(ip);
  if (!entry || now - entry.windowStart > RATE_LIMIT_WINDOW_MS) {
    entry = { count: 0, windowStart: now };
  }
  entry.count++;
  rateLimitMap.set(ip, entry);
  if (entry.count > RATE_LIMIT_MAX) {
    sendJson(res, 429, { error: "Too many requests. Please wait a moment." });
    return false;
  }
  return true;
}

// Prune stale rate-limit entries every 5 minutes so the map doesn't grow indefinitely.
setInterval(() => {
  const cutoff = Date.now() - RATE_LIMIT_WINDOW_MS * 2;
  for (const [ip, entry] of rateLimitMap) {
    if (entry.windowStart < cutoff) rateLimitMap.delete(ip);
  }
}, 5 * 60_000);

function isAuthorized(req) {
  if (!BACKEND_TOKEN) {
    return true;
  }

  const authorization = req.headers.authorization || "";
  const bearerToken = authorization.startsWith("Bearer ")
    ? authorization.slice("Bearer ".length).trim()
    : "";
  const headerToken = req.headers["x-mom-companion-token"] || "";

  return bearerToken === BACKEND_TOKEN || headerToken === BACKEND_TOKEN;
}

function extractOutputText(data) {
  if (typeof data.output_text === "string" && data.output_text.trim()) {
    return data.output_text.trim();
  }

  const parts = [];
  for (const item of data.output || []) {
    for (const content of item.content || []) {
      if (content.type === "output_text" && typeof content.text === "string") {
        parts.push(content.text);
      }
    }
  }

  return parts.join("\n").trim();
}

function readJson(req) {
  return new Promise((resolve, reject) => {
    let raw = "";

    req.on("data", (chunk) => {
      raw += chunk;
      if (Buffer.byteLength(raw) > MAX_BODY_BYTES) {
        reject(new Error("Request body is too large."));
        req.destroy();
      }
    });

    req.on("end", () => {
      try {
        resolve(raw ? JSON.parse(raw) : {});
      } catch {
        reject(new Error("Request body must be valid JSON."));
      }
    });

    req.on("error", reject);
  });
}

function sendJson(res, statusCode, payload) {
  res.writeHead(statusCode, {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
    "Access-Control-Allow-Headers": "Authorization,Content-Type,X-Mom-Companion-Token",
    "Content-Type": "application/json"
  });

  if (statusCode === 204) {
    res.end();
    return;
  }

  res.end(JSON.stringify(payload));
}

function servePrivacyPage(res) {
  const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Privacy Policy — Friendai</title>
  <style>
    body { font-family: system-ui, sans-serif; max-width: 720px; margin: 40px auto; padding: 0 20px; line-height: 1.7; color: #333; }
    h1 { color: #1565C1; } h2 { color: #2E7D32; border-bottom: 1px solid #eee; padding-bottom: 4px; }
    table { border-collapse: collapse; width: 100%; margin: 12px 0; }
    th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; } th { background: #f5f5f5; }
    em { color: #555; }
  </style>
</head>
<body>
<h1>Privacy Policy — Friendai</h1>
<p><em>Last updated: June 11, 2026</em></p>
<p>Friendai is a compassionate AI companion for seniors and people living with dementia. This policy explains what data the app uses and how it is handled.</p>

<h2>1. Voice &amp; Speech</h2>
<p>Friendai is a hands-free companion: <strong>by default the app listens while it is open</strong> for a caregiver-adjustable window (1&ndash;12 hours, default 12), so users who cannot press buttons can simply speak. Microphone access begins only after the Android microphone permission is granted. A <strong>visible ongoing notification</strong> and the standard Android mic indicator are always shown while listening is active; the caregiver can shorten the window or switch to push-to-talk-only mode in Settings. Audio is sent to Android's on-device speech recognition — Friendai receives only the transcribed text, never the raw audio.</p>

<h2>2. Conversations</h2>
<p>Conversation text is used only to generate the current reply. It is <strong>not stored on our servers</strong>, not logged, and not shared. A short rolling memory of recent turns stays in-app memory only — discarded when the app closes.</p>

<h2>3. Caregiver Settings</h2>
<p>PIN, emergency contact, and custom rules are stored <strong>on your device only</strong>. ADB backup is disabled. This data never leaves the device unless the caregiver configures a remote AI backend.</p>

<h2>4. AI Backend (Optional)</h2>
<p>If the caregiver configures a backend URL, conversation text is sent there to generate replies. The backend is caregiver-provided and caregiver-controlled. By default all processing is on-device.</p>

<h2>5. Permissions</h2>
<table>
  <tr><th>Permission</th><th>Why</th></tr>
  <tr><td>RECORD_AUDIO</td><td>Microphone for speech recognition</td></tr>
  <tr><td>INTERNET</td><td>Optional AI backend calls</td></tr>
  <tr><td>FOREGROUND_SERVICE</td><td>Visible foreground service for timed listening</td></tr>
  <tr><td>POST_NOTIFICATIONS</td><td>Ongoing "listening" notification (Android 13+)</td></tr>
  <tr><td>REQUEST_IGNORE_BATTERY_OPTIMIZATIONS</td><td>Keeps timed-listening service running (health/safety exemption)</td></tr>
  <tr><td>WAKE_LOCK</td><td>Keeps CPU awake during timed-listening window</td></tr>
</table>
<p>No location, contacts, storage, camera, or other sensitive permissions are used.</p>

<h2>6. Data Sharing</h2>
<p>We share no data with any third parties except: Android Speech Recognition (Google), Google Play Billing, Google Gemini API (if caregiver configures a Gemini key — not retained beyond the reply), and your caregiver-configured custom AI backend (if set).</p>

<h2>7. Analytics &amp; Advertising</h2>
<p>Friendai collects <strong>no analytics</strong> and uses <strong>no advertising SDKs</strong>. We do <strong>not sell any data</strong>.</p>

<h2>8. Billing</h2>
<p>VIP subscriptions are handled entirely through Google Play Billing. Friendai does not receive or store payment information.</p>

<h2>9. Security</h2>
<ul>
  <li>Caregiver settings are PIN-protected (min. 4 digits)</li>
  <li>30-second lockout after 5 incorrect PIN attempts</li>
  <li>ADB backup disabled</li>
</ul>

<h2>10. Children</h2>
<p>Friendai is not directed at children under 13. It is designed for adults — seniors and their adult caregivers.</p>

<h2>11. Contact</h2>
<p>For privacy questions, contact the app developer via the Google Play Store listing.</p>

<p><em>Friendai is built with love for families who want their loved ones to feel heard, safe, and never alone.</em></p>
</body>
</html>`;
  res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
  res.end(html);
}
