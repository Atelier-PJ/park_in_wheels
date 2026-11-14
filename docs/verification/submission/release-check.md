# Submission release verification

Verified on 2026-11-14 (Mac host date). Pleos AVD guest clock may differ.

- JDK 17.0.21; Android SDK 35 / build-tools 35.0.0.
- `bash scripts/test-core.sh`: PASS, 48 domain scenario assertions.
- `:app:assemblePleosRelease :app:lintPleosRelease :app:assembleDemoDebug`: BUILD SUCCESSFUL.
- Debug build without release environment: BUILD SUCCESSFUL.
- Explicit release build without signing environment: refused with required-variable message.
- `apksigner verify --verbose --print-certs`: verifies, v2 signature, RSA 3072, WONDER WHEELS certificate.
- Certificate SHA256: `d22f45ac131fb4de02d311ceb641e5389f6152f36b18dfb0664632f19c160e8b`.
- `zipalign -c 4`: exit 0.
- Manifest: package `com.parkonwheels.pleos`, version 2 / `0.2.0-family`, label `PARK ON WHEELS`, no debuggable flag.
- Exact submission APK: `WONDER WHEELS_PARK ON WHEELS_release.apk`, 229314866 bytes.
- SHA256: `389ebf78ac3fee1ac16777b912ec8393e7c67370b1ee9c0de5a0398c890269e1`.
- Fresh dedicated AVD `ParkOnWheelsReleaseCheck`, Pleos API 34 arm64-v8a: `adb install` Success, `am start -W` Status ok / COLD.
- Observed entrance, four-theme selector, Magic Castle invitation, microphone permission and full-screen story subtitles. Screenshot: `release-story.png`.
- Existing developer AVD and its app data were retained. The release signing key/passwords are stored outside Git.

This is an exact-release installation/startup/story smoke check, not a new end-to-end release voice/light test. The earlier actual SDK generated-PCM spell, ambient RGB write/readback and stamp evidence is documented separately in `docs/PLEOS_AMBIENT_EMULATOR.md`. Neither check establishes real-vehicle or child-microphone validation.
