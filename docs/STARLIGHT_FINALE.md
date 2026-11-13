# Voice-triggered starlight finale

Latest verification: **resolved on local AVD** after restoring the Virtual Console browser session. Actual RGB readback #FFE5A1 and stamp passed. See [cause, fix and reproduction](PLEOS_AMBIENT_EMULATOR.md). Older failed runs below are retained as history.

Magic Castle's authored final phrase is 별빛을 밝혀라. VoiceDirector's existing normalized final-result match invokes SafariModel.completeRecognizedFinale, which rechecks canFinish (fresh available vehicle gate, valid distance, active session and all missions complete). If the parent ambient preference is on, the model requests warm starlight RGB #FFE5A1 through the existing AppVehicleGateway before committing the normal completion/stamp. It retains that scene for the completed session so refresh does not overwrite it with ordinary purple. Duplicate final callbacks cannot award or request again.

The stamp does not depend on an asynchronous lighting acknowledgement: unsupported lights, errors and a thrown gateway exception still allow the normal reward. A successful stamp is not evidence that vehicle lights changed. Opt-out and touch completion do not request the voice light effect. Background/restriction/scene changes invalidate late status callbacks. The Pleos controller still enforces foreground, fresh P/UX state, permissions, READ_WRITE capability and supported areas. Only RGB changes, never power or brightness.

Enable through 보호자 → 테마 앰비언트 색상. Existing preference is respected; the feature does not silently turn the setting on.

Verification, 2026-11-14:

- demoDebug + pleosDebug builds and lint passed; 48 domain assertions passed.
- Isolated Android model instrumentation (fake gateway/preferences): request precedes reward; one request/stamp despite duplicates; opt-out; unsupported response; exception; restricted gate; missing distance; paused session; touch fallback; other theme; stale callback after background passed.
- Real Pleos ON_DEVICE Korean SDK recognized test PCM of the phrase and the result exercised model starlight request plus one stamp through a fake light gateway. See verification/starlight/pcm-runtime.txt.
- This does not verify a live child's microphone utterance or a physical vehicle's ambient light hardware.


## Real light gateway run (current vehicle user), 2026-11-14

PleosStarlightJourneyVerification was run with `adb shell am instrument --user current` on the Pleos AVD. Synthetic Korean PCM was recognized by the real ON_DEVICE STT SDK, then the production model requested RGB #FFE5A1 through the real Pleos vehicle gateway and persisted one stamp in isolated test preferences. Manual visual review of starlight-spell-prompt.png and starlight-stamp-confirmed.png confirms the production story/stamp screen. Original user passport records were not modified.

**Full end-to-end result: NOT PASSING.** The light write callback acknowledged ARGB 0xffffe5a1, but four subsequent reads still returned 0x00ffffff. No visible ambient color change is claimed. The cause remains unconfirmed. The virtual console also reported inactive DDS bridge state; no console writes or fake color updates were used to manufacture a successful result.

The harness active-window accessibility assertion could not find the stamp in this automotive multi-window layout, although its captured screenshot visibly shows the stamp. This is a test-window limitation, recorded separately from the light readback failure. Earlier instrumentation omitted `--user current` and ran off the visible vehicle user; only the current-user recording/screenshots are presentation evidence.

Artifacts: real-emulator-runtime.txt and real-emulator-light.txt under docs/verification/starlight; raw current-user screen recording and before/after PNGs under docs/superpowers/previews. The local PCM test is not a live child microphone test. No production gameplay or light adapter code changed during this verification.
