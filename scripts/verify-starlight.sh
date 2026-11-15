#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -f .pow-env.sh ]]; then source .pow-env.sh; fi
adb_command="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
adb_target=(-e)
if [[ -n "${ANDROID_SERIAL:-}" ]]; then adb_target=(-s "$ANDROID_SERIAL"); fi
"$adb_command" "${adb_target[@]}" forward tcp:8088 tcp:8088
python3 scripts/check-pleos-console.py
./gradlew :app:assemblePleosDebug :app:assemblePleosDebugAndroidTest \
  -PtestRunner=com.parkonwheels.demo.E2eRunner --no-daemon
"$adb_command" "${adb_target[@]}" install -r app/build/outputs/apk/pleos/debug/app-pleos-debug.apk
"$adb_command" "${adb_target[@]}" install -r app/build/outputs/apk/androidTest/pleos/debug/app-pleos-debug-androidTest.apk
log_path="$(mktemp -t pow-starlight.XXXXXX)"
trap 'rm -f "$log_path"' EXIT
"$adb_command" "${adb_target[@]}" shell am instrument --user current -w \
  -e class com.parkonwheels.demo.PleosJourneyUiTest \
  com.parkonwheels.pleos.test/com.parkonwheels.demo.E2eRunner | tee "$log_path"
# adb can return 0 even when AndroidJUnitRunner reports failure.
if ! rg -q 'OK \(1 test\)' "$log_path" || \
   rg -q 'FAILURES|INSTRUMENTATION_FAILED|Process crashed' "$log_path"; then
  echo "FAIL: production UI / voice / real ambient E2E did not fully pass" >&2
  exit 1
fi
