# 실제 Pleos SDK 연결 순서

확인일: 2026-10-27. 현재 PR은 모의 입력 전용이며, 아래 설정과 어댑터는 아직 앱에 적용하지 않았습니다.

## 1. 팀 프로젝트 준비

https://pleos.ai/ → 로그인 → Workspace → My Project → 프로젝트 생성. 프로젝트 이름은 `PARK ON WHEELS`로 정하고, 팀원은 프로젝트 팀 관리에서 추가합니다. SDK 메뉴에서 제공 상태를 확인합니다.

현재 공식 안내: Vehicle State·NaviHelper는 신청 없이 사용 가능. Vehicle Control·FusedLocation·TTS·STT는 준비 중. 프로젝트 콘솔 상태가 다르면 해당 조건을 추가 확인합니다. 프로젝트 ID는 심사 관련 정보이며 모든 차량 조회에 API 키처럼 전달하는 값이 아닙니다. Client ID/Secret 안내는 서버 연결을 요구하는 SDK에 해당합니다. CRN은 에뮬레이터 2.1.0 이후 입력 불필요로 안내됩니다.

근거: https://document.pleos.ai/docs/pleos-only/create-my-project

## 2. 공식 샘플로 서비스 연결 확인

로컬 Android Studio에 Pleos 전용 에뮬레이터를 설치하고 실행합니다. 일반 Android 에뮬레이터에는 Pleos Vehicle Service가 없어 SDK 기능을 실행할 수 없습니다. 설치 순서는 `PLEOS_VERIFICATION.md`를 참고합니다.

https://github.com/Pleos-Playground/pleos-sample-vehicle-app 의 공식 샘플을 먼저 실행하여 초기화, 기능 지원 확인, 실제 값 조회를 구분해 확인합니다. SDK 초기화 성공만으로 개별 API 연결 성공이라고 판단하지 않습니다.

## 3. 우리 프로젝트에 의존성 추가

`settings.gradle.kts`의 기존 `dependencyResolutionManagement.repositories`에 추가할 설정:

```kotlin
maven {
    url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/")
}
```

`app/build.gradle.kts`의 기존 dependencies에 추가할 설정:

```kotlin
implementation("ai.pleos.playground:Vehicle:2.3.2")
```

공식 연동 가이드에서 확인한 버전입니다. SDK와 실행 중 Vehicle Service의 호환성은 별도 확인합니다. 단순 의존성 다운로드에는 공식 기본 예제에 별도 키 설정이 없습니다.

근거: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup

## 4. 실제 어댑터 구현

- 실제 사용하는 API의 `@RequiresPermission`을 확인하여 Manifest에 최소 권한을 선언합니다. 도메인 요약표만 보고 개별 메서드의 권한을 확정하지 않습니다.
- 공식 `Vehicle(context)` 생성 → `initialize()` → 필요한 도메인 획득 → capability 확인 → 조회·구독 → 리소스 해제 흐름을 구현합니다.
- `PleosVehicleGateway`(신규)가 `VehicleGateway`를 구현하게 합니다. 누적 거리를 확인된 단위에서 km로 변환하고 메인 스레드에서 `onOdometer`로 전달합니다.
- 상태 콜백 수신 후 Compose 상태도 갱신하도록 `SafariModel`을 리팩터링합니다. 현재 데모는 버튼 처리 후 직접 refresh하는 구조이므로 SDK 어댑터만 교체해서는 화면이 자동 갱신되지 않습니다.
- 주행 제한은 별도 `CarUxRestrictionsManager` 조회·리스너로 연결합니다. 초기값/연결 끊김은 UNKNOWN, 제한 해제 후에는 보호자 선택으로 재개합니다. 속도 0이나 내비 도착만으로 허용하지 않습니다.
- 실제 SDK 전용 빌드 구성과 application ID를 분리하고 모의 입력 UI를 제거합니다.

공식 문서:

- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object
- https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide

## 5. 완료 판정

지원 여부 확인 → 현재값 조회 → Virtual Console 값 변경 → 앱의 실제 SDK 콜백·화면 반영까지 확인합니다. 콘솔만 바꾸는 것은 앱 연동 성공이 아닙니다. 연결 해제·앱 복귀·누락 값·권한 오류도 검증합니다.

Codespaces/Actions는 코드 수정·빌드·도메인 테스트용입니다. 현재 컨테이너에는 Pleos 에뮬레이터·차량 서비스를 구성하지 않았습니다. 실제 호출 검증은 로컬 Pleos 전용 에뮬레이터에서 진행합니다.
