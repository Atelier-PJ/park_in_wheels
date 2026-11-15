# Vehicle sound charging and arrival implementation plan

**Goal:** Add theme sound, an opt-in actual-charging space episode and a parked-only destination visit stamp.
**Architecture:** A shared read-only travel context interface and Java policy isolate optional services from the existing safety engine. Theme audio shares VoiceDirector audio focus; optional telemetry never grants gameplay permission.
**Tech stack:** Kotlin Compose, offline PCM/WAV, AAOS CarPropertyManager EV_CHARGE_STATE, Pleos NaviHelper 3.0.5.
**Spec:** User requested sound → charging episode → arrival stamp, in that order, 2026-11-14.

## Constraints and review focus
- Preserve existing local vehicle-animation changes and published submission Release.
- Audio stops on background, audio-focus loss, unsafe state and microphone; narration ducks ambience.
- Actual charging enum is required; charge switch or cable connection is not evidence of charging.
- Parent opt-in and Android energy read permission; no energy control permission or writes.
- Navigation is read-only; never replace destination or request a route. Parent opt-in. No coordinates/address stored or logged by app.
- Arrival alone does not permit playback/rewards; explicit parked claim, expiry, duplicate prevention and durable storage.
- Unsupported/denied/stale optional data falls back to normal story, with no false special reward.
- Snapshot charging episode at explicit start so telemetry changes cannot replace an active story.

## Tasks
- [x] Generate four short departure sounds and four looping ambience clips, add parent audio setting and focus-coordinated playback.
- [x] Add Java freshness/arrival policy and tests for unknown, stale, noncharging, restricted, expired, duplicate and restart cases.
- [x] Add AAOS charge-state reader and opt-in charging space episode with normal fallback.
- [x] Add NaviHelper arrival adapter and explicit parked visit stamp UI/storage in passport.
- [x] Build both variants/lint/core checks; test actual emulator services separately from injected UI/policy verification; record limitations precisely.
