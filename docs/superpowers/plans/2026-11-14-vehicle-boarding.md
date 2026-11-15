# Vehicle transformation boarding implementation plan

**Goal:** Theme selection opens a four-second vehicle transformation, then a themed departure button starts the existing story.
**Architecture:** Replace the existing ticket invitation with a shared lifecycle-aware boarding screen. Bundle four offline GIFs and four stills; leave system Driving view unchanged.
**Tech stack:** Kotlin Compose, Android AnimatedImageDrawable, offline GIF.
**Spec:** User approved selection → transformation → departure → existing story on 2026-11-14.

## Constraints and review focus
- Keep current Jua typeface, cream button style and theme palette.
- Show full vehicle composition without zoom cropping; keep text clear of vehicle.
- No automatic story/microphone start; require explicit departure tap.
- Play once, stop in background, respect reduced motion using the final still.
- Reuse existing safety gate and resume behavior; leave published competition release unchanged.

## Tasks
- [x] Produce before/after artwork, four 4-second GIFs and final stills.
- [x] Replace ticket content with a shared transformation screen, back navigation and theme departure copy.
- [x] Build/lint release with private signing environment and run domain checks.
- [x] Install on Pleos AVD, check all four transformations and departure to story; capture emulator proof.
