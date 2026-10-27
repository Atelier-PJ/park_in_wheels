#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -f .pow-env.sh ]; then source .pow-env.sh; fi
bash scripts/test-core.sh
gradle --no-daemon :core:check :app:lintDemoDebug :app:lintPleosDebug :app:assembleDemoDebug :app:assemblePleosDebug
echo 'PASS: mock and Pleos SDK APKs built. Runtime API verification still requires a Pleos emulator.'
