# Pleos AVD ambient RGB: inactive Virtual Console cause and fix

Verified 2026-11-14 on local PleosConnect AVD, Vehicle SDK 2.3.2.

## Cause

The app's typed Light request reached Vehicle Service and VHAL correctly. VHAL logged RGB (255,229,161), yet repeated reads stayed white. SDK LightImpl.setAmbientLightBrightnessColor invokes its completion with the caller's submitted value after the asynchronous write completes; that callback alone is not an independent physical-state observation.

This AVD's Virtual Console only runs its vehicle DDS simulation while a browser client session is alive. Its served page calls /browser/session every second and the state endpoint reports a 3000 ms client timeout. Without an open browser, browser.active=false, body.emulator_active=false, and all ambient DDS readiness flags were false. Commands were accepted by the HAL but no active simulated vehicle responded with updated actual-color state.

Controlled reproduction: production app and SDK unchanged; simply open the forwarded Virtual Console. browser.active/body.emulator_active and ambient DDS readiness become true. Repeating the exact starlight test now gives desired=0xffffe5a1 and readback=0x00ffe5a1 (same RGB; alpha ignored per the existing comparison). The live console's custom color field and cabin-light preview also changed to #ffe5a1. The test restores the original cyan after inspection.

## Fix / operation

1. Start PleosConnect AVD. In a terminal, use adb -e forward tcp:8088 tcp:8088.
2. Open http://127.0.0.1:8088/ in a normal browser and **leave the tab open throughout vehicle-control testing**. The connected local console tab has been left open in Codex.
3. Keep the virtual gear in P and wait for real SDK P/UX/odometer readiness. Do not bypass restrictions.
4. Run python3 scripts/check-pleos-console.py. This read-only preflight rejects an inactive browser or unavailable actual ambient-color DDS publisher; it does not publish synthetic vehicle states.
5. With the project's JDK 17 / Android SDK environment loaded, run bash scripts/verify-starlight.sh. It checks console readiness, builds/installs, runs instrumentation against --user current, and rejects missing success markers even when adb itself returns exit code 0.

No app HTTP connection, external bridge, fake RGB update or private vehicle API was introduced. The app still uses typed Light APIs and all parked/permission/capability checks. Root cause was emulator setup, so production lighting code did not need modification.

## Verification

- scripts/test-pleos-console.py: 3 regression checks passed (no client, missing actual publisher, healthy session).
- scripts/verify-starlight.sh: exit 0.
- Real ON_DEVICE STT recognized generated Korean test PCM of 별빛을 밝혀라.
- Real vehicle gateway requested #FFE5A1, set acknowledgement and actual getter readback matched RGB on the first read.
- One stamp persisted in isolated test preferences. Production Compose completion window was attached/visible and captured; manual screenshot review confirmed the stamp. Automotive active-window accessibility can expose the driving panel, so that is not used as a false failure condition.
- Existing user passport records were not modified; original ambient color restored after the run.
- This is emulator verification using test PCM, not a live child's microphone or real vehicle hardware test.

Evidence: verification/starlight/fixed-runtime.txt, fixed-light-readback.txt and superpowers/previews/starlight-fixed-before.png, starlight-fixed-stamp.png, starlight-fixed-success.mp4.

Official guide: https://document.pleos.ai/docs/pleos-only/app-test-guide/virtual-console-guide
