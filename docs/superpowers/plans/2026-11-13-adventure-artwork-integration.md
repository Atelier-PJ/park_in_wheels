# Adventure Artwork Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Wire the four adventure illustrations and the existing Dino Safari GIF into the Android app screens.

**Architecture:** Package generated still images in Android's no-density drawable resources. Keep the existing vector-independent theme API, resolve each theme ID to its image, and use a small Android `ImageView` wrapper to play the Dino GIF on its boarding pass.

**Tech Stack:** Kotlin, Jetpack Compose, Android drawable resources, Android `ImageDecoder` (API 28+).

**Spec:** `/Users/sunny/Documents/PARK ON WHEELS/assets/adventure-worlds/여정 구성안.md`

## Global Constraints

- Preserve all current navigation, mission, and vehicle safety behavior.
- Keep artwork bundled offline and add no third-party image dependency.
- App minimum SDK is 28; Pleos flavor minimum SDK is 29.
- The GIF preview appears only in Dino Safari's boarding pass; no new interaction is introduced while driving.

## Review Focus

- Theme IDs must map to the intended image in cards, adventure panel, and passport.
- Large landscape illustrations must stay legible in both wide and square layouts.
- The animated drawable must stop when its Compose screen leaves composition.
- Missing/unsupported animation must leave the still image fallback visible.

### Task 1: Package theme images

**Files:**
- Create: `app/src/main/res/drawable-nodpi/adventure_dino.png`
- Create: `app/src/main/res/drawable-nodpi/adventure_magic.png`
- Create: `app/src/main/res/drawable-nodpi/adventure_space.png`
- Create: `app/src/main/res/drawable-nodpi/adventure_ocean.png`
- Create: `app/src/main/assets/dino_safari_in_car_demo.gif`

- [ ] Copy the existing Dino Safari hero, the three generated world images, and the original Dino GIF into the listed resource paths.
- [ ] Confirm every resource is present and its dimensions/format are readable.

### Task 2: Connect the themed artwork

**Files:**
- Modify: `app/src/main/java/com/parkonwheels/demo/ThemeArtwork.kt`
- Modify: `app/src/main/java/com/parkonwheels/demo/MainActivity.kt`
- Create: `app/src/main/java/com/parkonwheels/demo/DinoSafariGif.kt`

- [ ] Map `dino`, `magic`, `space`, and `ocean` to their packaged PNGs in `ThemeArtwork`.
- [ ] Keep image cropping intentional for theme cards and preserve full composition on the passport.
- [ ] Show a lifecycle-scoped animated Dino GIF in the Dino Safari boarding pass; retain still artwork for other surfaces.

### Task 3: Verify the app build

**Files:**
- No additional files.

- [ ] Run `./gradlew :app:assembleDemoDebug :app:assemblePleosDebug` from the project root and confirm both APKs build.
- [ ] Review `git diff --check` and the diff to ensure only image wiring and assets changed.
