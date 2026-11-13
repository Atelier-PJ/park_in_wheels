# Storybook Adventure UX Implementation Plan

> **For agentic workers:** Follow this plan directly in the current checkout.

**Goal:** Align the four adventure worlds and their screens with the warm, calm, child friendly visual direction in the PARK ON WHEELS brief.

**Architecture:** Keep the existing Compose navigation, vehicle safety gates, mission engine, and offline assets. Replace the detailed 3D style scene art with a unified 2D storybook set, reshape world cards as illustrated covers, and carry the same visual language through boarding, mission progress, reward, and passport.

**Tech Stack:** Kotlin, Jetpack Compose, packaged PNG/GIF assets.

**Spec:** `/Users/sunny/Documents/PARK ON WHEELS/[차량용 앱 개발 공모전] WONDER WHEELS_김선영.docx`.

## Global Constraints

- Preserve the sequence: parked theme selection, travel chapter, rest stop parked missions, parked destination ending, passport reward.
- Never enable screen play or voice interaction while restricted or in motion.
- Keep assets bundled for offline use and use 2D illustrations.
- Retain the existing Dino Safari GIF inside the boarding pass preview.

## Tasks

- [x] Package four coordinated storybook scenes as versioned assets and map each theme to its new art.
- [x] Redesign home covers to reveal all four worlds with consistent image framing, clear child sized labels, and a compact heading.
- [x] Unify boarding pass, mission progress, success, ending, and passport surfaces with the same warm illustrated language while retaining existing safety and navigation behavior.
- [x] Build demo and Pleos APKs, install the Pleos APK on the running AVD, and inspect the home, Dino boarding, and mission screens.
