# Destination Stop Implementation Plan

**Goal:** Theme → vehicle transformation → themed destination ideas → start story.
**Architecture:** Keep existing safety/model/story contracts. Add one Compose route with a small offline, nationwide destination catalogue for the selected theme; no location or network call, no navigation mutation. Resume bypasses destination selection. Home returns to its original four cards without recommendation badges.
**Tech Stack:** Kotlin, Compose, existing ThemeArtwork/Jua/Action components.

- [x] Add DestinationSelectionScreen.kt: three named destinations per theme with region + child-friendly hook. Full illustration background, readable left panel, selected destination and story CTA; allow story without destination.
- [x] MainActivity.kt: insert destination route after boarding for new episodes, keep resume direct, preserve safety blocking/fullscreen layout and back to home.
- [x] Remove home recommendation badge parameter and leave prior parent opt-in implementation available as a separate optional feature.
- [x] Build/lint and core checks. Actual Pleos UI test: all four theme choices → transformation → destination choice → story with existing end/voice lifecycle; capture screens. Do not inject vehicle data.

Visual direction: existing cream #FFFCF5, navy #24394A, ticket #FFF6DF/#957347 and per-theme green/purple/blue/teal. Existing Jua typography. One full scene; text sits on soft cream fade on left, companion visible on right. Destination rows express a real choice, no new decorative badges. Illustrations/font/button are reused so the new stop belongs to the existing park journey.
