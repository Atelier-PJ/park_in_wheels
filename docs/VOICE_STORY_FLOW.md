# Voice led parked adventure

The journey screen now plays narrated story beats without asking children to choose answers. An explicit foreground start begins playback. Each beat waits for Korean narration and a minimum visual hold before advancing. The final authored phrase is recognized on device, then the existing engine awards the reward and the passport card animates its stamp. Recognition is exact after removing punctuation and spaces; low confidence input does not finish the journey.

Four finale phrases: 깨어나라 작은 친구 / 별빛을 밝혀라 / 새로운 행성 안녕 / 반짝 보물아 나와라.

Korean offline TTS is used when available. Bundled WAV narration provides an offline fallback when the device has no Korean TTS. The recording index maps authored text to local files; narration was synthesized locally with macOS Yuna. Microphone permission and on-device Korean recognition are required for speech completion. An explicitly labeled touch alternative is available when recognition cannot run. No recognition result is fabricated.

The ocean second chapter cleanup beat shows paper on the sand before progression, then plays the collection GIF once during feedback and holds the paper inside the basket. Reduced motion uses static before and after frames.

Playback stops on leaving the screen, foreground loss, safety restrictions, or audio focus loss. Resuming requires an explicit start. The vehicle engine still enforces parked status and any selected distance requirement.

## Runtime evidence

Both demo and Pleos debug APKs built successfully. On the local Pleos AVD, recorded Korean narration created started speech audio players, story beats advanced without choosing answers, and the final spell prompt appeared. This AVD has no on-device recognition engine, so the ending and persisted stamp were exercised using the labeled touch alternative. Real microphone recognition remains unverified on a supported device.

Screenshots: `docs/superpowers/previews/voice-story/`.

## HD scene rendering

The sixteen main story scenes now use independently generated 1586 × 992 originals. Runtime files are native-size lossless animated WebP assets in `app/src/main/assets/hd`, with no camera zoom or cropping. The full composition is fitted to the viewport; 16:10 artwork closely matches the local Pleos app pane. Reduced motion stops the same HD drawable rather than substituting an unrelated hero image. The asset manifest records dimensions and frame counts; encoding validation checked that each first frame exactly matches its RGB source. Original PNGs are retained in the parent workspace at `assets/adventure-worlds/hd-scenes`.

Demo and Pleos debug builds passed after this change. The local Pleos AVD played the new full-scene Dino artwork and Korean narration; screenshot: `docs/superpowers/previews/park-hd-full-scene.png`.

## Pleos STT update

The earlier Android-provider limitation above is historical. The Pleos flavor now uses the dedicated SpeechToText 3.3.1 SDK in Korean ON_DEVICE mode. The local AVD returned a real successful recognition result for a generated Korean PCM test phrase. See `PLEOS_STT.md` for evidence, permission behavior, microphone-request verification, and the remaining live-speech limitations.
