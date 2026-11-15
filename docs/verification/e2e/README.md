# Journey UI and Pleos integration checks

Run on an emulator with JDK 17 / SDK 35, while the Mac is unlocked.

- `bash scripts/verify-ui.sh`: the real MainActivity launched through the app launcher intent, production Compose navigation and bundled narration for all four themes with explicit simulated vehicle/speech input. Checks interruption recovery, microphone errors and touch fallback, stored passports, a restricted finale, late/wrong/duplicate speech results and manual resume. This is not real speech/vehicle evidence.
- `bash scripts/verify-starlight.sh`: production Compose entrance → parent opt-in → theme → ticket → narrated story → VoiceDirector → PleosSpeechInput → real ON_DEVICE STT → actual vehicle Light write/readback → stamp → passport/collection/detail. Only its microphone source is replaced by generated Korean PCM, sent to the real SDK. It never injects a final recognition result or advances missions directly.

The real integration check requires the official Pleos Virtual Console tab to remain active and the virtual vehicle parked in P. `scripts/check-pleos-console.py` rejects a disconnected simulation. Speech recognition with a live human microphone and physical vehicle light hardware require separate testing.

The test-only E2eRunner installs an emulator-only Application dependency provider. Normal launches cannot select this provider through intents or preferences. Both suites run the actual Activity lifecycle and use uniquely named preferences, never clear the user's passport, and restore app-owned lighting after independent color readback. UI captures use the Compose app window, avoiding AAOS's unrelated active driving-panel accessibility window. JUnit runner failures are checked explicitly even when adb exits 0.

The old direct-model `PleosStarlightJourneyVerification` has been replaced. Its archived reports under `../starlight` are historical component evidence, not evidence that the production UI/voice flow passes.

Set `ANDROID_SERIAL` when more than one emulator is connected. The scripts pass that exact serial to installation, port forwarding and instrumentation.
