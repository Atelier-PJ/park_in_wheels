# Latest submission release verification

Verified 2026-11-15. APK code source: `c270631df8cc186a3006941d763d2325ce9d4f3f`; subsequent commit changes documentation and evidence only.

- `bash scripts/test-core.sh`: PASS, 48 domain + 17 travel policy + 27 destination assertions (92 total).
- `:app:assemblePleosRelease :app:lintPleosRelease`: BUILD SUCCESSFUL, JDK 17 / Android SDK 35.
- APK: `WONDERWHEELS_PARKONWHEELS_release.apk`, 244698831 bytes.
- SHA256: `67216e890f60d928ae5581aae67ed36e60ee72da6b0f474e1cb8fb34ed803be6`.
- `apksigner verify --verbose --print-certs`: PASS, v2, RSA 3072; same release certificate SHA256 `d22f45ac131fb4de02d311ceb641e5389f6152f36b18dfb0664632f19c160e8b`.
- `zipalign -c 4`: PASS; manifest has no debuggable flag; package `com.parkonwheels.pleos`, version `0.2.0-family` (2).
- Exact signed release installed successfully on dedicated Pleos API 34 AVD `ParkOnWheelsReleaseCheck`, emulator-5556 / user 10. Existing developer AVD and passport retained.
- `am start -W`: Status ok, COLD. Observed entrance → four-theme selector → Magic Castle transformation → ready → destination selection (Lotte World, Everland, Gyeongju World) → selected destination → full-screen narrated story.
- Evidence: [destination selection](destinations.png), [story](story.png).

This is an exact-release install/startup/destination/story smoke check. It does not repeat real STT/RGB/stamp or child-microphone/real-vehicle validation. Previous SDK integration and mock UI regression results remain separately documented under `docs/verification/merged-2026-11-15/` and `docs/verification/e2e/`.
