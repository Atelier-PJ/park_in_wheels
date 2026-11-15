# Vehicle boarding runtime verification

Host date 2026-11-14. Dedicated Pleos API 34 arm64 AVD `ParkOnWheelsReleaseCheck`, emulator-5556. Updated signed `pleosRelease` installed over the previous matching release signature; published GitHub Release was not changed.

- Release build and lint: BUILD SUCCESSFUL, domain checks: PASS 48 assertions.
- Four offline GIFs: 732×480, duration 4200ms, no loop extension.
- Actual app UI for dino, magic, space, ocean: theme selection → transforming vehicle → correct departure button → existing full-screen story. All four passed using UI taps; screenshots, screen recordings and results.json are adjacent.
- Actual Android GIF completion callbacks opened the departure buttons; no timed automatic story start.
- Full vehicle composition uses FIT_CENTER. System Driving view remained unchanged.
- No new claim of voice recognition, ambient writes, stamp or real-car verification is made by this visual-flow test.
- Parent-gated reduced-motion setting enabled through actual UI: final still and immediate departure confirmed; setting restored to animated mode afterward.
- Emulator left on Magic Castle ready screen with `마법차 타고 출발` visible.
