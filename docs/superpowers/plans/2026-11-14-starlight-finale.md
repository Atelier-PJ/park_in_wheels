# Starlight finale implementation plan

Goal: Successful final phrase recognition requests warm starlight RGB on supported, opted-in parked vehicles and awards exactly one passport stamp.
Architecture: Voice callback invokes a dedicated model completion action. Model gates once, requests existing typed gateway RGB, then commits normal reward. Ambient refresh preserves finale color rather than immediately reverting it. Callback status is scoped to lifecycle/session changes. Lighting failure never blocks a reward; touch completion never triggers the voice-only effect.

- [x] Add dedicated recognized-finale action and transient starlight scene to SafariModel; keep parent preference, fresh gate and hardware safeguards.
- [x] Wire only successful STT callback, retain normal hatch for touch. Keep stamp animation unchanged.
- [x] Use isolated model preferences and fake gateway instrumentation to verify opted-in request-before-stamp, no duplicates, opt-out, restricted/missing distance, unsupported/failure and stale callback behavior. These tests do not claim real hardware operation.
- [x] Build both APKs and run core checks. Run model instrumentation on emulator, then real Pleos PCM callback integration if feasible. Distinguish synthetic PCM from live-child mic and real vehicle light evidence.
