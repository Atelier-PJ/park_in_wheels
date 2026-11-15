# 2026-11-15 기본 추천 검증

- core: 48 domain + 17 travel + 27 destination assertions PASS.
- Pleos release APK / demo debug / demo instrumentation build PASS; Pleos release lint PASS.
- PleosConnect API 34 emulator, user 10: injected model instrumentation PASS (four themes, opt-in/out, stale and unknown input, manual source, free theme choice, late callbacks, background cleanup, no raw-name persistence).
- Real NaviHelper destination callback not verified in this run.
- Production release install on this AVD rejected: INSTALL_FAILED_UPDATE_INCOMPATIBLE (existing package signed with another key). Existing app/data preserved; no uninstall performed. Do not treat the existing production app on that AVD as this build.
- Locally built signed preview APK copied to submission materials APK directory; published competition Release untouched.

## Follow-up actual UI verification

- Installed certificate Android Debug f9925698… differs from submission certificate d22f45ac…. Built latest pleosDebug with the matching debug certificate and successfully updated with `-r`; existing records preserved, submission signing unchanged.
- Real PleosConnect API34/user10, real MainActivity and Vehicle SDK, no model/vehicle injection.
- DestinationUiVerification completed guardian gate, recommendation opt-in, four manual text-field inputs, Apply and home marker checks: all four PASS. This verifies manual input, not a real NaviHelper destination callback.
- Original captures: actual-ui/destination-{dino,magic,space,ocean}-{input,home}.png. Montage and app crops are derived from these screenshots.

## Revised flow: theme → transformation → destination → story

- Both PleosDebug and DemoDebug builds PASS; PleosDebug lint PASS; signed PleosRelease build + lint PASS; core 92 assertions PASS.
- DestinationFlowVerification: all four actual Pleos UI flows PASS, no injected vehicle/model. Locations selected: 서울숲/경복궁/국립과천과학관/해운대해수욕장. Existing story rendered and exit/cancel returned to home in each case.
- Captures and exact runner output: new-flow/. Both initial and final layout revisions were run; captures are from the final wider text column/larger typography build.
- Curated nationwide travel ideas, not nearby/live search and not navigation changes. Selected destination does not rewrite the existing story episode.
