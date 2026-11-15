# Destination recommendations implementation plan

**Goal:** Offer one optional theme suggestion from the current destination; never auto-select or change navigation.
**Architecture:** Pure Java curated-name/category classifier; ephemeral destination snapshots through the existing read-only NaviHelper gateway; consent-controlled model and shared home card badges. No server or external search in this phase (user preference).
**Constraints:** Preserve P/UX gate, no raw destination persistence/logs, no raw voice network path, no API keys in APK. Unknown/ambiguous/stale destinations produce no recommendation. Existing submission Release stays unchanged.

- [x] Test curated destinations, generic keywords, conflicting names, unknowns and precedence.
- [x] Implement classifier without a server.
- [x] Read destination snapshots in existing NaviHelper connection; reset on changes/background/disconnect and expire missing replies.
- [x] Add parent opt-in, no forced theme selection and home card badge.
- [ ] Verify core, both builds/lint, native model and all four home recommendations on emulator with explicit test input. Probe actual NaviHelper read separately.

Verification outcome and remaining real-navigation/UI checks: `docs/verification/destination-recommendations/results.md`.
