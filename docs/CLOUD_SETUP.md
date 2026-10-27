# Codex Cloud / Codespaces / GitHub Actions

## Roles

| Environment | Purpose | Completion evidence |
|---|---|---|
| GitHub Actions | Compile, domain checks, lint, APK artifacts | Green workflow + downloadable APK |
| GitHub Codespaces | Team members edit using a repeatable dev container | Each member opens the workspace and builds |
| Codex Cloud | Remote coding tasks from a published setup | Environment published, then new task passes verification |
| Local Pleos AVD | Run actual SDK callbacks and UX restriction tests | Logs + observed state changes, separately recorded |

## Create Codex Cloud environment

1. Settings → Codex Cloud → Environments → Create environment.
2. Select `Atelier-PJ/park_in_wheels` and Get started.
3. During setup, use branch `work/pleos-integration` until its PR is merged. `main` initially contains only the initialization README. Selecting the repository alone does not select the development branch.
4. Provide the setup instruction below. Review the detected checkout directory and branch before running scripts.
5. Allow required dependency hosts, run setup, confirm both APKs and domain/lint checks, then Save/Publish.
6. After `Environment published`, open a **new task** from that environment and run `bash scripts/verify-cloud.sh`. This is the reuse test.

Suggested setup instruction:

```text
Set up Atelier-PJ/park_in_wheels for Android development.
Check out work/pleos-integration (use main only after this branch has been merged).
Read AGENTS.md. Use JDK 17, Gradle 8.11.1, Android platform 35 and build-tools 35.0.0.
From the repository root, run bash scripts/setup-cloud.sh.
For subsequent tasks, source .pow-env.sh when present and run bash scripts/verify-cloud.sh.
Do not start a Pleos emulator, publish an app, merge a PR or claim actual SDK execution.
Report the exact checked-out branch, test results and both APK paths. Prepare the environment for review and publishing.
```

## Scripts

- Install script: from the repository root, `bash scripts/setup-cloud.sh`.
- Verification/start instructions: `bash scripts/verify-cloud.sh` (no background service needed).
- The setup writes `.pow-env.sh` with tool paths, excluded from Git. Later shells source it explicitly.
- This is an Android app, so no application web port is configured. Codespaces does not display the Android UI in the browser.
- Initial tool downloads require internet. Licenses and dependency terms apply to the installed Android tools.

## Network destinations

Prefer the package-manager preset plus required hosts. Dependencies can redirect; add a host only after confirming the actual blocked download.

```text
nexus-playground.pleos.ai
dl.google.com
maven.google.com
repo.maven.apache.org
plugins.gradle.org
plugins-artifacts.gradle.org
services.gradle.org
downloads.gradle.org
github.com
release-assets.githubusercontent.com
objects.githubusercontent.com
```

If JDK 17 is not installed, the script uses the Linux package manager; allow the configured OS package mirrors too. No Pleos client secret or OpenAI API key is needed for this read-only Vehicle SDK build. GitHub repository authentication is managed through the connected account.

## Team sharing

Environment sharing is separate from GitHub membership and Pleos project membership. Confirm who can use the published environment; the default should remain private until specific team access is configured. This setup does not invite people or widen repository permissions.

Official OpenAI guide, checked 2026-10-27:
https://learn.chatgpt.com/docs/environments/cloud-environments
