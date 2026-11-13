# Collection artwork

Generated with the built-in image_gen tool, 2026-11-14. Four original 1536×1024 PNG sheets live in app/src/main/assets/collections/. Each contains six 512×512 cells in reading order. The UI displays individual cells at their original resolution with ContentScale.Fit, preserving the whole subject. Example-only collection pages do not award or modify actual records.

Prompt set: children ages 4–9, cozy picture-book illustrations, hand-painted gouache/watercolor/colored-pencil texture, warm ivory paper, sage/peach/lavender/dusty blue/honey gold palette. Exact equal 3-column × 2-row grid, six separate centered subjects, no typography or interface, no flat vector/3D look, each subject fully inside its cell.

- dino.png: stegosaurus, triceratops, brachiosaurus, tyrannosaurus, parasaurolophus, ankylosaurus; distinctive plates, horns, neck, bipedal posture, crest and armor/club tail; friendly rounded faces and small botanical ground accents.
- magic.png: wizard star, crescent moon, rainbow, flower, sun, castle gate key; unique crafted gold-edged badges with ribbons, subtle sparkles and fairy-tale details.
- space.png: Earth with oceans/continents/clouds, ringed Saturn, cratered Mars, banded Jupiter with storm spot and no rings, cloudy Venus, blue Neptune; tiny golden stars.
- ocean.png: clownfish, yellow butterflyfish, blue tang, pufferfish, seahorse, pink anthias; characteristic shapes and markings, rosy cheeks, soft coral and bubbles.

These are stylized collection illustrations, not scientific diagrams.

## Detail narration

All 24 entries have a large-image detail and a short Korean explanation in CollectionContent.kt. Tapping an entry initiates playback; replay restarts it, close releases it. Existing VoiceDirector enforces foreground parked state and audio focus. Local Yuna WAV fallback (22,050 Hz mono) is bundled for devices without Korean offline TTS, matching the existing fallback pipeline. This does not replace the pending JiYoung production voice.

Content references: [Natural History Museum: Parasaurolophus](https://www.nhm.ac.uk/discover/dino-directory/parasaurolophus.html), [NHM: ankylosaurs](https://www.nhm.ac.uk/discover/ankylosaurs-the-dinosaurs-with-built-in-armour.html), [NASA planetary fact sheets](https://nssdc.gsfc.nasa.gov/planetary/planetfact.html), [Monterey Bay Aquarium coral community](https://www.montereybayaquarium.org/visit/exhibits/ocean-in-motion/coral-community). Magic badges are fictional story content.

Runtime verification 2026-11-14: both demoDebug and pleosDebug build successfully. All 24 content strings resolve to valid local mono WAVs. On Pleos AVD, opened stegosaurus, wizard-star badge, Saturn and seahorse detail pages, captured screenshots, and verified app MediaPlayer with active unpaused renderer during each narration; closing each detail removed its media client. Replay verified on stegosaurus. These are runtime playback-state checks, not a subjective evaluation of the voice sound quality. An intervening host emulator SIGSEGV was recovered by restarting the AVD; latest APK installed and four-theme checks repeated successfully.

## Magic collection refinement

Active magic atlas: app/src/main/assets/collections/magic-v2.png (built-in image_gen). Original ornate magic.png retained. Prompt: six quiet handmade picture-book sticker illustrations in an equal 3×2 grid; smiling star in lavender wizard hat, sleepy crescent moon and tiny star, three-band pastel rainbow, pink five-petal flower, smiling sun, heart-bow golden key. Gouache and colored-pencil grain on warm ivory paper, generous margins; no ornate frames, filigree, ribbons, jewels, banners, glow or text. Collection UI uses a light notebook binding, a single fine border and row divider rather than nested rounded cards; larger labels and tap-to-hear hint.

Lighting audit: SafariModel.refresh applies a fixed per-theme ambient RGB only when ambientEnabled and canPlay. StoryJourneyScreen recognized finale invokes model.hatch; it does not trigger a separate spell lighting effect. AmbientSceneController modifies RGB only with permission and supported READ_WRITE capability; power and brightness are not controlled. No lighting behavior was changed in this visual refinement.
