# Collection detail implementation plan

Goal: Tap each of 24 collectibles to see a large illustration and hear a short child-friendly description.
Architecture: Authored Kotlin collection content plus a detail overlay in PassportScreen, using existing VoiceDirector and packaged offline narration. No STT or vehicle SDK changes.
Tech stack: Kotlin Compose, existing bitmap atlas, offline TTS/recorded WAV fallback.

- [x] Add 24 matching names and two-sentence descriptions in CollectionContent.kt.
- [x] Open a large detail on cell tap, show text, replay and close. Preserve selected collection on dismiss. Stop playback on dismiss, disposal, background, safety restriction and audio focus loss; do not auto resume on return.
- [x] Bundle local narration for all descriptions and extend narration_index.json. Preserve prior entries and voice precedence.
- [x] Build both APKs, check all 24 asset mappings, and exercise detail/replay/close across four themes on Pleos emulator. Record actual playback evidence separately from compilation.
