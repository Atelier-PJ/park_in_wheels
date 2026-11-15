#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out_dir="$(mktemp -d)"
trap 'rm -rf "$out_dir"' EXIT
sources=()
while IFS= read -r path; do sources+=("$path"); done < <(find core/src/main/java core/src/test/java -name '*.java' -print)
javac -encoding UTF-8 -d "$out_dir" "${sources[@]}"
java -cp "$out_dir" com.parkonwheels.core.SafariEngineTest
java -cp "$out_dir" com.parkonwheels.core.TravelRewardPolicyTest

java -cp "$out_dir" com.parkonwheels.core.ThemeRecommendationTest
