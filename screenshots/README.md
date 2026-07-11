# Friendai Screenshot Plan

This file documents the screenshots to be captured for each possible interaction in the app. For each interaction, a UI test will be generated to capture the screen and save it to the screenshots/ directory.

## Interactions to Capture

1. Onboarding Dialog (first launch)
2. Main Screen (default, after onboarding)
3. Fun Button (joke/fact shown)
4. Talk Button (speech input prompt)
5. Conversation Transcript (after several turns)
6. Help/Contact Button (escalation dialog)
7. Export Conversation (admin mode)
8. Device Check Button (troubleshooting screen)
9. Settings/Admin UI (if present)
10. Offline Mode (status shown)

Each test will launch the app, navigate to the interaction, and capture a screenshot. Screenshots will be saved as PNG files in the screenshots/ folder.

---

To automate this, Espresso UI tests will be generated in `app/src/androidTest/java/com/moms/ai/`.
