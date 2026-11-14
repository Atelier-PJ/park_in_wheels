#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -f .pow-env.sh ]]; then source .pow-env.sh; fi
adb_command="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
adb_target=(-e)
if [[ -n "${ANDROID_SERIAL:-}" ]]; then adb_target=(-s "$ANDROID_SERIAL"); fi
./gradlew :app:assembleDemoDebug :app:assembleDemoDebugAndroidTest \
  -PtestRunner=com.parkonwheels.demo.E2eRunner
"$adb_command" "${adb_target[@]}" install -r app/build/outputs/apk/demo/debug/app-demo-debug.apk
"$adb_command" "${adb_target[@]}" install -r app/build/outputs/apk/androidTest/demo/debug/app-demo-debug-androidTest.apk
log_path="$(mktemp -t pow-ui.XXXXXX)"
trap 'rm -f "$log_path"' EXIT
"$adb_command" "${adb_target[@]}" shell am instrument --user current -w \
  -e class com.parkonwheels.demo.JourneyUiTest \
  com.parkonwheels.demo.test/com.parkonwheels.demo.E2eRunner | tee "$log_path"
if ! rg -q 'OK \(2 tests\)' "$log_path" || \
   rg -q 'FAILURES|INSTRUMENTATION_FAILED|Process crashed' "$log_path"; then
  echo 'FAIL: UI regression checks did not fully pass' >&2
  exit 1
fi
