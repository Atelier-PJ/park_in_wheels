# PARK ON WHEELS

어린이 동승 가족을 위한 **주차 중 DINO SAFARI 탐험 앱** 초기 개발판입니다.

> **DEMO: 실제 Pleos SDK 미연결.** 앱의 차량 입력 버튼은 모두 모의 데이터입니다. 실제 차량·운행 중 사용하지 마세요. 공모전 제출 가능한 실제 연동판이나 안전성 검증 완료 제품이 아닙니다.

## 구현 범위

- Kotlin + Jetpack Compose Android 앱, minSdk 28, compileSdk 35 / targetSdk 34.
- 탐험 시작 → 이용 제한에 따른 중단 → 거리 누적 → 수동 재개 → 5km 부화 → 카드 저장.
- 단일 여정과 카드 수 로컬 저장, 재실행 시 자동 재개 차단, 중복 부화 방지.
- 거리 미지원·역행·이용 상태 미확인 처리. 모의 데이터는 별도 개발자 패널에 표시.
- Java 도메인 모듈과 Android UI/모의 어댑터 분리.
- Codespaces 개발 컨테이너, PR별 테스트·lint·debug APK 빌드.

현재 화면은 초기 기능 확인용입니다. 공룡 일러스트·부화 애니메이션·음성·AI 개인화·내비·실제 조명/공조 제어는 포함하지 않았습니다.

## 동료와 클라우드에서 시작

1. 저장소 소유자가 GitHub 저장소 **Settings → Collaborators**에서 동료에게 접근 권한을 부여합니다. 초대 대상은 팀에서 결정하며 이 PR은 초대를 보내지 않습니다.
2. GitHub에서 **`feat/android-starter` 브랜치**로 전환합니다. PR 병합 후에는 `main`을 사용합니다.
3. **Code → Codespaces → Create codespace**를 선택합니다. 컨테이너에 JDK 17·Gradle 8.11.1·Android SDK가 설치됩니다. 최초 생성에는 다운로드 시간이 필요합니다.
4. 터미널에서 실행합니다.

```bash
bash scripts/test-core.sh
gradle --no-daemon :core:check :app:lintDebug :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

Codespaces는 각자가 동일한 설정으로 생성하는 개발 환경입니다. Android 앱 화면은 브라우저 미리보기로 제공하지 않으며, Pleos 에뮬레이터도 컨테이너에 설치하지 않습니다. Codespaces 생성 가능 여부·이용 한도·비용은 GitHub 계정 정책을 따릅니다. 이 변경은 유료 환경을 생성하거나 결제 설정을 바꾸지 않습니다.

## GitHub Actions로 APK 받기

PR의 **Checks → Android demo checks**에서 결과를 확인합니다. 성공한 실행의 **Artifacts**에서 `park-on-wheels-demo-<commit>` ZIP을 다운로드하고 압축을 해제합니다. Actions가 제한된 저장소에서는 소유자가 Actions 사용 정책을 확인해야 합니다.

APK는 개발용 debug 서명이며 릴리스 배포용이 아닙니다. 개인 키나 클라우드 비밀키 없이 빌드합니다.

## Android Studio / Pleos 에뮬레이터에서 실행

JDK 17, Gradle 8.11.1, Android SDK Platform 35, Build Tools 35.0.0을 준비합니다. `ANDROID_HOME`을 SDK 경로로 지정하거나 `local.properties`에 `sdk.dir`을 설정합니다. 이 파일은 Git에서 제외됩니다.

초기 저장소에는 Gradle Wrapper 바이너리가 없습니다. Codespaces에서는 `gradle` 명령을 그대로 사용합니다. 로컬에서 표준 wrapper를 만들려면 설치된 Gradle 8.11.1로 실행하세요.

```bash
gradle wrapper --gradle-version 8.11.1 --distribution-type bin
```

이후 Android Studio에서 프로젝트를 열거나 `./gradlew :app:assembleDebug` (Windows: `gradlew.bat :app:assembleDebug`)를 실행합니다. 생성한 wrapper 파일은 팀에서 버전 관리할 수 있습니다.

Pleos 에뮬레이터 설치 후:

```bash
adb devices
# 기기가 여러 개면 adb -s <serial> install -r ... 사용
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.parkonwheels.demo/.MainActivity
```

에뮬레이터에서 설치·실행하더라도 현재 APK는 실제 SDK 값을 읽지 않습니다. SDK 연동판에서는 이 데모 패키지를 재사용하지 말고 모의 입력을 제거해야 합니다.

## 테스트 흐름

`주차·이용 가능 가정` → 새 탐험 → `주행·이용 제한 가정` → 거리 +1km 다섯 번 → `주차·이용 가능 가정` → 이어하기 → 부화.

제한 해제 후 자동 재개되지 않아야 하며, 카드는 한 번만 지급돼야 합니다. 홈 이동·재실행 후 이용 상태를 다시 확인하도록 초기 차단됩니다.

## 구조

| 경로 | 책임 |
|---|---|
| `core/` | 여정 상태·거리·부화·중복 방지·어댑터 인터페이스 |
| `app/` | Compose UI·모의 입력·로컬 저장 |
| `.devcontainer/` | Codespaces 도구 구성 |
| `.github/workflows/android.yml` | 도메인 검사·lint·APK 빌드 |
| `docs/TESTING.md` | 시나리오와 검증 범위 |
| `docs/PLEOS_VERIFICATION.md` | 실제 SDK 연동·검증 계획 |

상태 처리는 메인 스레드 기준입니다. 현재는 짧은 세션을 동기 저장하여 단순화했으며, 후속판에서 저장소를 Room 등으로 교체할 때도 UI 중단을 저장 완료까지 지연하지 않아야 합니다. 카드 종류·여러 사용자·여정 이력·스토리지 오류 복구 UX는 후속 범위입니다.

## 검증 상태

- 실제 실행 결과는 해당 PR의 Actions Checks를 기준으로 확인하세요.
- Pleos SDK API 호출·에뮬레이터 실행·실차 검증: **미수행**.
- 계정별 SDK 제공 상태·권한·지원 차종: **추가 확인 필요**.

공식 개발 환경 안내: https://appdevchallenge.auto-hmg.io/guide/development
