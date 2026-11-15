# Local travel experience verification

Host date 2026-11-14. Published submission Release is unchanged.

## Actual services

`actual-services.txt`: Actual AAOS charge-state read returned false and Pleos NaviHelper 3.0.5 connected on the dedicated API 34 AVD. No actual charging transition or destination arrival has been verified. An observed ROUTE_DRIVE/route start in the current foreground session is required before a destination-arrived callback can open the claim offer.

## Injected tests

- `TravelRewardPolicyTest`: 17 checks for unknown/stale charge, restricted and expired arrivals, duplicate callbacks and persisted deduplication.
- `TravelExperienceVerification`: isolated model and explicit fake context; special episode snapshot/reward, parked-only explicit claim, restoration, opt-out and background reset.
- `TravelUiVerification`: production Compose scenes with a visible test-signal banner; three energy orbs tapped, existing touch finale used, special badge awarded; visit button/dialog/passport verified. Screenshots adjacent. These are not proof of actual telemetry events.
- Existing 48 core assertions continue to pass.

Audio consists of eight original locally synthesized WAV files. Runtime MediaPlayer logs verify `playing=true` for ambience and duck transitions. Physical speaker loudness and real-car audio mixing are not asserted.

Optional features default off except ordinary theme sound, which can be disabled. There are no navigation writes, charge control calls or stored raw place names/coordinates.

Actual production Pleos UI was then exercised for all four themes: `actual-four-theme-audio.txt` confirms MediaPlayer `playing=true` for each departure and ambience. Ordinary story screenshots are adjacent (`actual-*-audio.png`); the emulator was left on the space boarding screen. Audio/motion freshness interruptions retain the existing explicit manual-resume behavior.
