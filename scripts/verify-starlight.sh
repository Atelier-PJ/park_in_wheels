#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -f .pow-env.sh ]]; then source .pow-env.sh; fi
adb_command="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
"$adb_command" -e forward tcp:8088 tcp:8088
python3 scripts/check-pleos-console.py
./gradlew :app:assemblePleosDebug :app:assemblePleosDebugAndroidTest \
  -PpleosTestRunner=com.parkonwheels.demo.PleosStarlightJourneyVerification --no-daemon
"$adb_command" -e install -r app/build/outputs/apk/pleos/debug/app-pleos-debug.apk
"$adb_command" -e install -r app/build/outputs/apk/androidTest/pleos/debug/app-pleos-debug-androidTest.apk
log_path="$(mktemp -t pow-starlight.XXXXXX)"
trap 'rm -f "$log_path"' EXIT
"$adb_command" -e shell am instrument --user current -w \
  com.parkonwheels.pleos.test/com.parkonwheels.demo.PleosStarlightJourneyVerification | tee "$log_path"
# adb can return 0 even when Instrumentation.finish reports failure.
if ! rg -q 'PASS real Pleos ambient RGB write AND readback confirmed' "$log_path" || \
   ! rg -q 'PASS production Compose stamp ending captured' "$log_path" || \
   rg -q 'FAIL |LIGHT_NOT_CONFIRMED|INSTRUMENTATION_FAILED' "$log_path"; then
  echo "FAIL: starlight runtime verification did not fully pass" >&2
  exit 1
fi
