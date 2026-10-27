#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# This path belongs to the task, never overwrite HOME or CODEX_HOME.
tool_dir="${POW_TOOLS_DIR:-$HOME/.cache/park-on-wheels}"
mkdir -p "$tool_dir"
if ! command -v javac >/dev/null || ! java -version 2>&1 | head -1 | grep -q '"17\.'; then
  if command -v apt-get >/dev/null; then
    if [ "$(id -u)" -eq 0 ]; then
      apt-get update
      apt-get install -y openjdk-17-jdk-headless curl unzip ca-certificates
    else
      sudo apt-get update
      sudo apt-get install -y openjdk-17-jdk-headless curl unzip ca-certificates
    fi
    export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-$(dpkg --print-architecture)"
    export PATH="$JAVA_HOME/bin:$PATH"
  else
    echo 'Install JDK 17 before running this setup.' >&2
    exit 1
  fi
fi
if ! command -v curl >/dev/null || ! command -v unzip >/dev/null; then
  echo 'curl and unzip are required.' >&2
  exit 1
fi
gradle_dir="$tool_dir/gradle-8.11.1"
if [ ! -x "$gradle_dir/bin/gradle" ]; then
  curl -fLsS --retry 3 https://services.gradle.org/distributions/gradle-8.11.1-bin.zip -o "$tool_dir/gradle.zip"
  curl -fLsS --retry 3 https://services.gradle.org/distributions/gradle-8.11.1-bin.zip.sha256 -o "$tool_dir/gradle.sha256"
  (cd "$tool_dir" && printf '%s  gradle.zip\n' "$(cat gradle.sha256)" | sha256sum -c -)
  unzip -q -o "$tool_dir/gradle.zip" -d "$tool_dir"
  rm "$tool_dir/gradle.zip"
fi
export ANDROID_HOME="${ANDROID_HOME:-$tool_dir/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
  mkdir -p "$ANDROID_HOME/cmdline-tools" "$tool_dir/android-tools"
  curl -fLsS --retry 3 https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o "$tool_dir/android-tools.zip"
  unzip -q -o "$tool_dir/android-tools.zip" -d "$tool_dir/android-tools"
  mv "$tool_dir/android-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  rm "$tool_dir/android-tools.zip"
fi
export PATH="$gradle_dir/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
# sdkmanager consumes only the required licenses/packages. Preserve its exit status.
set +o pipefail
yes | sdkmanager --licenses >/dev/null
license_status=${PIPESTATUS[1]}
set -o pipefail
[ "$license_status" -eq 0 ]
sdkmanager 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'
# Persist tool paths for subsequent non-login task shells via an explicit source file.
{
  printf 'export ANDROID_HOME=%q\n' "$ANDROID_HOME"
  printf 'export ANDROID_SDK_ROOT=%q\n' "$ANDROID_HOME"
  if [ -n "${JAVA_HOME:-}" ]; then printf 'export JAVA_HOME=%q\n' "$JAVA_HOME"; fi
  printf 'export PATH=%q:"$PATH"\n' "$gradle_dir/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools${JAVA_HOME:+:$JAVA_HOME/bin}"
} > .pow-env.sh
echo 'Tools installed. Running cloud verification (no emulator).'
bash scripts/verify-cloud.sh
