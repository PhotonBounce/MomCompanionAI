# Mom's AI Development Plan

## Current State

The app is now a working native Android/Kotlin prototype with a backend proxy scaffold.

- `MainActivity` has Mom Mode with push-to-talk, transcript display, AI reply routing, local fallback replies, and TextToSpeech.
- First launch opens caregiver setup so the default PIN, contact, backend URL, and token are not forgotten.
- `RulesActivity` stores caregiver rules, Mom profile notes, starter English/Russian vocabulary, PIN, caregiver contact, backend URL, and backend token locally.
- Caregiver Settings includes local rule testing and end-to-end AI backend testing.
- `PinActivity` protects caregiver settings.
- `EmergencyActivity` gives a tap-to-call escalation screen.
- `TroubleshootingActivity` provides a Device Check screen for microphone, speech recognition, voice, backend, caregiver contact, setup, and layout status.
- The Android app calls a backend proxy when configured and falls back to local replies when not configured.
- AI replies get deterministic post-reply escalation checks so backend responses can still open the right help screen.
- The backend proxy calls OpenAI from the server side so the API key is not shipped in the APK.
- The backend can require a shared bearer token for local phone testing.
- Recent conversation turns are included in backend prompts for short-term context.
- A Gradle wrapper is available for repeatable debug APK builds.
- A debug APK build has been verified locally.

## Product Goal

Build a simple Android companion app for Mom that can:

- Listen to her through a clear push-to-talk interface.
- Answer out loud in a warm, calm voice.
- Follow caregiver-defined rules that can be edited by you.
- Keep the experience simple enough for daily use.
- Escalate or avoid risky situations instead of pretending to be a doctor, emergency service, therapist, lawyer, or financial advisor.

## Recommended MVP

Start with a push-to-talk companion instead of always-on listening.

This keeps the first version simpler, less invasive, and easier to test on an Android phone. Always-listening behavior can come later as a foreground mode with a visible notification if it is still wanted.

### Mom Mode

- Large Talk button.
- Big status text: listening, thinking, speaking, error.
- Voice response through Android TextToSpeech.
- Optional transcript view with large readable text.
- Minimal controls so she cannot get lost in settings.

### Caregiver/Admin Mode

- PIN-protected settings screen.
- Editable companion rules prompt.
- Editable profile notes: language preference, tone, topics to avoid, family contact names, emergency contact behavior.
- Editable starter vocabulary for important English/Russian phrases.
- Test prompt box so you can try how the rules affect replies.
- Save rules locally first; sync/cloud can come later.

### AI Behavior

Each AI turn should be built from separate layers:

1. Fixed app safety instructions.
2. Your caregiver rules.
3. Mom profile/preferences.
4. Short conversation summary or recent transcript.
5. Mom's latest spoken message.

Caregiver rules should be sent as higher-priority instructions, not mixed into Mom's message. Mom should not be able to override them by saying things like "ignore your instructions."

### Safety Rules

The app should have built-in rules for:

- Emergency phrases: chest pain, cannot breathe, falling, self-harm, immediate danger.
- Scam resistance: gift cards, passwords, banking codes, suspicious callers.
- Medical boundaries: supportive language plus escalation, not diagnosis.
- Sensitive topics you define.
- Contact escalation: show or call a chosen contact when configured.

The MVP should not automatically call emergency services until that behavior is explicitly designed and tested.

## Architecture

### Android App

- Kotlin native Android.
- `MainActivity` for Mom Mode.
- Admin/settings screen for caregiver rules.
- `SpeechRecognizer` or Android speech intent for speech-to-text.
- `TextToSpeech` for voice output.
- Local storage for rules and preferences.
- Clear runtime permission handling for microphone.

### AI Layer

- Add an `AiClient` interface so development can start with a fake local response.
- Add a real network-backed implementation later.
- Do not ship an AI provider API key inside the Android APK.
- Use a small backend/proxy for model calls if using a paid cloud AI provider.

### Storage

First version:

- Local Android preferences for caregiver rules.
- Local conversation transcript can be off by default.

Later:

- Encrypted storage for sensitive profile data.
- Optional caregiver summaries.
- Optional cloud backup/sync.

## Development Milestones

### Milestone 1: Stabilize the Android App

- Add Gradle wrapper or document exact Android Studio build requirements.
- Add runtime microphone permission flow.
- Handle speech recognition result in `MainActivity`.
- Add TextToSpeech lifecycle cleanup.
- Replace the current bare layout with a large, readable Mom Mode screen.
- Add basic error handling when speech recognition is unavailable.

### Milestone 2: Local Companion Loop

- Create a local `CompanionEngine`.
- Add a fake `AiClient` that responds without network access.
- Wire the flow: tap Talk, recognize speech, generate reply, speak reply.
- Show status and transcript on screen.
- Add language preference support.

### Milestone 3: Caregiver Rules

- Add admin/settings screen.
- Store caregiver rules locally.
- Build a prompt assembler that combines safety instructions, caregiver rules, profile notes, and latest user input.
- Add a test area for rules.

### Milestone 4: Real AI Integration

- Add backend/proxy service for AI calls.
- Add app networking with timeout, retry, and offline/error messages.
- Keep the AI provider key off the phone.
- Add configurable endpoint in a developer-only settings section.

### Milestone 5: Safety and Escalation

- Add deterministic phrase checks before and/or after AI calls.
- Add contact escalation screen.
- Add "call caregiver" and "show emergency guidance" flows.
- Add scam warning behavior.
- Add safe fallback replies when the AI call fails.

### Milestone 6: Phone Testing and Packaging

- Build installable APK.
- Test on Mom's actual Android version and screen size.
- Test microphone permission, TTS voice, network failures, and sleep/wake behavior.
- Create simple update/install notes.

## Next Implementation Slice

The next coding pass should focus on phone-readiness hardening:

1. Improve Mom Mode visual accessibility: larger contrast, clearer button spacing, and an obvious "call caregiver" action.
2. Add better first-run setup guidance for choosing language, voice, and family-specific phrases.
3. Add optional conversation summary export for caregiver review, kept local unless explicitly shared.
4. Package and test the debug APK on the actual Android phone and tablet.
