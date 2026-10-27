#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out_dir="$(mktemp -d)"
trap 'rm -rf "$out_dir"' EXIT
mapfile -t sources < <(find core/src/main/java core/src/test/java -name '*.java' -print)
javac -encoding UTF-8 -d "$out_dir" "${sources[@]}"
java -cp "$out_dir" com.parkonwheels.core.SafariEngineTest
