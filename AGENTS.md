# PARK ON WHEELS development

Android Kotlin/Compose + Java core. Read `README.md`, `docs/PLEOS_CONNECTION.md` and `docs/CLOUD_SETUP.md` before changing build or SDK code.

## Build and verification

- JDK 17, Gradle 8.11.1, Android SDK platform 35/build-tools 35.0.0.
- Install an empty Linux cloud environment: `bash scripts/setup-cloud.sh`.
- Verify an already prepared environment: `bash scripts/verify-cloud.sh`.
- Pure domain checks: `bash scripts/test-core.sh`.
- If `.pow-env.sh` exists, source it before Gradle/adb commands. Never commit that file or credentials.
- `demoDebug` is explicit mock input; `pleosDebug` uses real Vehicle SDK 2.3.2 + android.car, no mock panel.
- APK outputs: `app/build/outputs/apk/demo/debug/` and `app/build/outputs/apk/pleos/debug/`.

## SDK constraints

- Keep SDK calls out of the Java domain engine.
- Unknown restrictions, lifecycle reconnect, expired reads and invalid distances must block gameplay.
- Follow CarUxRestrictions; never infer permission from speed 0 or destination arrival.
- The SDK odometer contract is nullable Float kilometres. Check capability before get/register.
- UI state must be updated on the main thread and late callbacks ignored after background/disconnect.
- Ambient RGB writes are user-requested: use only typed Light API, granted CAR_LIGHTS + CONTROL_CAR_LIGHTS, READ_WRITE capability, supported areas and fresh parked/UX state. Never write brightness, power, headlamps or HVAC. Never send new light commands while restricted or non-P.
- Voice uses offline Korean TTS and on-device STT only, explicit foreground initiation and microphone permission. Stop on background, restrictions and audio focus loss. No network fallback, raw voice logging or driving-game bypass.
- No VIN, location, hidden APIs or secret credentials are required.
- A successful build only verifies dependencies and compilation. Record emulator callback/UX results separately and never claim a mock or compile check as actual SDK execution evidence.
- Do not install on or test with a moving real vehicle.

## Cloud environment

GitHub Actions builds APKs. GitHub Codespaces is a developer workspace. Codex Cloud is an independent published task environment. `.devcontainer` is not proof that a Codex Cloud environment was created or published. The standard cloud setup does not start a Pleos emulator. Optional manual GitHub Actions runtime testing is documented in docs/EMULATOR_TEST.md; local Android Studio/Pleos AVD remains available. Keep full workflow status separate from per-app runtime evidence.
