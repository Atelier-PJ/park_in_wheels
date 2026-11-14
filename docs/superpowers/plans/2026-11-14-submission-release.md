# Competition submission release implementation plan

**Goal:** Publish an executable signed release APK and source at an immutable submission commit.
**Architecture:** Keep existing Pleos flavor; add environment-based release signing without committing credentials. Publish the oversized APK as a GitHub Release asset and link it from README and apk/README.md.
**Tech stack:** Android Gradle Kotlin DSL, JDK 17, Android build tools, GitHub REST API.
**Spec:** Competition instructions supplied by the user on 2026-11-14, including updated private-repository policy.

## Constraints and review focus
- Filename: WONDER WHEELS_PARK ON WHEELS_release.apk.
- APK must be release, signed, non-debuggable and executable.
- Keep keystore/passwords outside Git; fail explicit release builds if signing is absent.
- Existing debug builds must remain usable without release credentials.
- Keep repository private now; public transition required by November 16 through November 23. No collaborator invitation required.
- Asset larger than GitHub Git file limit goes to Releases; verify uploaded checksum.

## Tasks
- [x] Add env signing and release label in app/build.gradle.kts; build assemblePleosRelease and lintPleosRelease; run core checks.
- [x] Verify apksigner, zipalign, manifest; install exact signed release on Pleos emulator and launch.
- [x] Update README and apk/README.md with install, environment, settings, asset link, SHA256 and release reproduction.
- [ ] Commit/push source, create GitHub release pointing at submission commit, upload and verify APK asset; report full commit and release URL.
