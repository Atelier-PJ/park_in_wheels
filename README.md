# PARK ON WHEELS

주차 중 어린이·보호자가 함께 사용하는 DINO SAFARI Android 앱입니다.

두 빌드가 있습니다. **실제 차량 운행 중 사용하거나 실차에 배포하지 마세요.**

| 빌드 | 데이터 | 패키지 | 모의 입력 패널 |
|---|---|---|---|
| `demoDebug` | 명시적인 모의 거리·제한 | `com.parkonwheels.demo` | 있음 |
| `pleosDebug` | Vehicle SDK 2.3.2 + CarUxRestrictions | `com.parkonwheels.pleos` | 없음 |

SDK 연동 코드와 실제 런타임 검증은 구분합니다. APK 빌드 통과만으로 에뮬레이터 API 호출 성공을 주장하지 않습니다. 실제 호출 증빙은 로컬 Pleos AVD에서 수집해야 합니다.

## 현재 구현

- Kotlin/Compose UI, Java 도메인 엔진, demo minSdk 28 / pleos minSdk 29 / compileSdk 35.
- 탐험 시작 → 제한 시 중단 → 거리 누적 → 수동 재개 → 5km 부화 → 카드 저장.
- SDK의 capability → 현재 누적 거리 조회 → 변경 리스너 → 해제 흐름.
- Car 서비스 연결·현재 제한 조회·변경 리스너·제한 해제 후 수동 재개.
- 메인 스레드의 화면 갱신, 5초 초과 거리 데이터 만료, null/NaN/역행 거리 차단.
- 앱 중단 시 입력 차단·구독 정리, 복귀 시 상태 재확인, 이전 세대 콜백 무시.
- 조명·공조 제어·내비·음성·AI 개인화는 이번 범위에 포함하지 않습니다.

## 개발 환경 설치와 검증

빈 Linux Codex Cloud 환경:

```bash
bash scripts/setup-cloud.sh
```

도구가 준비된 환경(Codespaces·Actions·로컬):

```bash
bash scripts/verify-cloud.sh
```

JDK 17만 있는 환경에서 도메인 검사:

```bash
bash scripts/test-core.sh
```

JDK 17·Gradle 8.11.1·Android platform 35·build-tools 35.0.0을 사용합니다. 별도 키 없이 공식 Pleos Maven에서 의존성을 내려받습니다. 로컬 Android Studio용 wrapper는 설치된 Gradle로 `gradle wrapper --gradle-version 8.11.1 --distribution-type bin`을 실행해 생성할 수 있습니다.

APK 경로:

- `app/build/outputs/apk/demo/debug/app-demo-debug.apk`
- `app/build/outputs/apk/pleos/debug/app-pleos-debug.apk`

## 실제 SDK 테스트

1. Pleos Playground에서 팀 프로젝트를 만들고 Vehicle State 제공 상태를 확인합니다.
2. Android Studio에 Pleos Connect 전용 AVD를 설치합니다. 기본 Android 이미지에는 Vehicle Service가 없습니다.
3. 실행 중인 AVD의 시리얼을 `adb devices`로 확인합니다.
4. `pleosDebug` APK를 설치하고 실행합니다.

```bash
adb -s <serial> install -r app/build/outputs/apk/pleos/debug/app-pleos-debug.apk
adb -s <serial> shell am start -n com.parkonwheels.pleos/com.parkonwheels.demo.MainActivity
adb -s <serial> logcat -s ParkOnWheels:I
```

`Vehicle.initialize returned`는 연결 완료 표시가 아닙니다. capability 결과와 `odometer_km=...` 로그, UX 상태를 함께 확인합니다. Virtual Console에서 값을 변경하고 실제 앱 값 갱신·중단·복원까지 확인하세요. 권한·미지원·타임아웃은 게임을 차단합니다.

검증 후 로그 수집:

```bash
bash scripts/collect-pleos-evidence.sh <serial>
```

이 스크립트는 로그를 로컬에 저장하고 자동 업로드하지 않습니다.

## 클라우드 협업

- **GitHub Actions**: PR마다 두 빌드의 lint·컴파일·도메인 검사. 실행의 Artifacts에서 APK ZIP을 받습니다.
- **GitHub Codespaces**: 해당 브랜치 → Code → Codespaces로 개발 컨테이너를 생성합니다. 브라우저에서 코드 편집·빌드 가능하며 Android UI는 로컬 AVD로 확인합니다.
- **Codex Cloud**: 별도로 환경을 생성·설치·게시해야 합니다. `.devcontainer` 파일만으로 생성되지 않습니다. [클라우드 설정 안내](docs/CLOUD_SETUP.md)를 따르세요.
- 팀원 접근은 GitHub 조직·Pleos 프로젝트·Codex Cloud 환경에서 각각 관리합니다.

`work/pleos-integration` PR이 병합되기 전에는 이 개발 브랜치를 선택합니다. `main`은 초기 README만 있는 상태일 수 있습니다.

## 문서와 구조

| 경로 | 내용 |
|---|---|
| `core/` | 여정 엔진, 차량 상태 준비·만료 판단, 도메인 시나리오 |
| `app/src/main/` | 공통 화면·모델·저장 |
| `app/src/demo/` | 모의 데이터 어댑터 |
| `app/src/pleos/` | 실제 SDK 어댑터·최소 권한 |
| [Pleos 연결](docs/PLEOS_CONNECTION.md) | 실제 연결과 증빙 절차 |
| [클라우드 환경](docs/CLOUD_SETUP.md) | Codex Cloud·네트워크·설치·재사용 |
| [테스트](docs/TESTING.md) | 수동 시나리오와 검증 범위 |

현재는 단일 여정·카드 수를 동기 로컬 저장하는 초기판입니다. 공룡 에셋·음성·여러 사용자·여정 이력·고급 오류 복구 UX는 후속 범위입니다.
