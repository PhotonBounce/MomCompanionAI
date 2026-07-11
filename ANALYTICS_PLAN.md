# Analytics Integration Plan (Opt-In, Privacy-Respecting)

## Goal
Add optional, privacy-respecting analytics to understand app usage and improve quality, while maintaining user trust and Play Store compliance.

## Principles
- Analytics is opt-in only (default OFF)
- No personal or sensitive data collected
- No third-party ad SDKs
- Users can enable/disable analytics in Caregiver Settings
- All analytics events are documented in PRIVACY_POLICY.md

## Recommended Solution
- Use Firebase Analytics (with minimal, custom events)
- Or use open-source alternatives (e.g., self-hosted Matomo, Plausible)
- Track only essential events: app launch, onboarding complete, feature usage (no content)

## Implementation Steps
1. Add analytics dependency (Firebase or open-source)
2. Add toggle in Caregiver Settings
3. Wrap all analytics calls with opt-in check
4. Document events in PRIVACY_POLICY.md
5. Add tests to verify analytics is disabled by default

---

## Next Steps
- Confirm analytics provider (Firebase or open-source)
- Implement opt-in toggle and event wrappers
- Update privacy policy and release notes
