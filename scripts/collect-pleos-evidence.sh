#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -f .pow-env.sh ]; then source .pow-env.sh; fi
if [ "$#" -ne 1 ]; then echo 'Usage: bash scripts/collect-pleos-evidence.sh <adb-serial>' >&2; exit 1; fi
serial="$1"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
output=".local-check/pleos-$stamp"
mkdir -p "$output"
adb -s "$serial" get-state
adb -s "$serial" shell getprop ro.build.version.release > "$output/android-version.txt"
adb -s "$serial" shell dumpsys package com.parkonwheels.pleos > "$output/app-package.txt"
adb -s "$serial" shell dumpsys car_service --services CarUxRestrictionsManagerService > "$output/ux-state.txt"
adb -s "$serial" logcat -d -s ParkOnWheels:I > "$output/app-log.txt"
echo "Evidence saved: $output (review before sharing; no automatic upload)"
