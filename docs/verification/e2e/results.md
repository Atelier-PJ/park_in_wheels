# Local verification — 2026-11-14 (Asia/Seoul)

Base: `8f81d1c` plus the changes committed with this report. Official Pleos 2.1.0 API 34 ARM64 AVD; virtual vehicle parked, Virtual Console DDS connected.

| Check | Result |
|---|---|
| Java domain scenarios | PASS, 48 assertions |
| Console preflight regression | PASS, 3 tests |
| Actual MainActivity / four-theme UI regression | PASS, 2 tests, 203.199 seconds |
| Actual MainActivity / Pleos STT and Light integration | PASS, 1 test, 59.202 seconds |
| demoDebug + pleosDebug + their test APKs | BUILD SUCCESSFUL |
| demoDebug + pleosDebug lint | PASS |

The two UI regressions use explicit simulated vehicle/speech input and isolated preferences. The Pleos integration uses production narration, VoiceDirector, PleosSpeechInput, real ON_DEVICE recognition of generated Korean PCM, actual Light writes and independent RGB readback, one persisted stamp, passport/detail navigation, and verified original-color restoration. See the adjacent runner logs and assertions.

After this successful functional run, screenshot capture was tightened to wait for loaded artwork and assert a nonempty software PNG, with test tags added to the two artwork views. Both app builds and lint passed again. The stricter screenshot rerun was interrupted by emulator disconnection and has not produced a new passing runner result; no incomplete screenshot is offered as evidence. macOS qemu crash reports also show a host Metal graphics abort. This environment instability remains separate from the successful functional run and must be resolved before a continuous demo recording is delivered.

Live child speech, physical vehicle microphones and lights, and moving-vehicle operation were not tested. No submission release APK or final demonstration video has been produced by this commit. Existing release assets predate these fixes.
