# Pleos SDK 연결 및 실행 검증

연결 대상: Vehicle SDK **2.3.2**, Android CarUxRestrictions. 확인일: 2026-10-27.

## 구현된 연결

- 공식 Maven 저장소와 `ai.pleos.playground:Vehicle:2.3.2` 의존성은 `pleos` 빌드에 추가했습니다.
- Manifest에 실제 호출의 `@RequiresPermission`에서 확인한 `pleos.car.permission.CAR_DRIVE`를 선언했습니다.
- `PleosVehicleGateway`는 공식 typed API만 사용합니다.
- `Vehicle(context).initialize()` → `getOdometer()` → `checkCurrentDistanceDrivenCapabilities` → `getCurrentDistanceDriven` → `registerDistanceDriven` → `unregisterDistanceDriven` → `release()` 흐름입니다.
- SDK 배포 sources JAR의 계약으로 거리 반환값이 **nullable Float, km**임을 확인했습니다. 임의 환산을 하지 않습니다.
- `Car.createCar` 라이프사이클, UX 현재값·변경 리스너를 별도로 연결했습니다. 초기 미확인 상태·서비스 연결 끊김·5초 초과 거리 만료는 게임을 차단합니다.
- 모든 SDK 콜백은 메인 스레드에서 모델과 화면을 갱신합니다. 앱 중단 이후의 과거 콜백은 세대 번호로 무시합니다.
- 실제 연동 빌드에는 모의 입력 패널이 없습니다. 조명·공조 쓰기 API는 호출하지 않습니다.

## 준비: Pleos 프로젝트

https://pleos.ai/ → 로그인 → Workspace → My Project에서 팀 프로젝트를 생성합니다. SDK 메뉴에서 Vehicle State 제공 상태를 확인하세요. 공식 안내상 Vehicle State와 NaviHelper는 별도 신청 없이 사용 가능합니다. 계정 콘솔의 실제 제공 상태가 다르면 추가 확인이 필요합니다.

Vehicle의 이번 기본 연결에는 client secret을 사용하지 않습니다. 프로젝트 ID는 심사 정보이고 모든 조회에 API 키처럼 전달하지 않습니다. TTS/STT 등 서버 연결 SDK의 client 정보와 구분하세요. CRN은 Emulator 2.1.0 이후 입력 불필요로 안내됩니다.

## 런타임 검증: 로컬 Pleos AVD 필요

1. Android Studio에서 공식 Pleos Connect Emulator를 설치합니다. 설치 세부는 `PLEOS_VERIFICATION.md`의 에뮬레이터 항목을 참고하세요.
2. `bash scripts/verify-cloud.sh` 또는 Gradle로 `:app:assemblePleosDebug`를 빌드합니다.
3. `adb devices`에서 AVD 시리얼을 확인하고 README의 명령으로 APK를 설치·실행합니다.
4. `adb -s <serial> logcat -s ParkOnWheels:I`에서 초기화 반환, capability, `odometer_km`, UX 로그를 구분해 확인합니다.
5. Virtual Console은 `adb -s <serial> forward tcp:8088 tcp:8088` 후 `http://127.0.0.1:8088/`에서 사용합니다.
6. 콘솔에서 누적 거리를 증가시키고 실제 콜백·화면 반영을 대조합니다. 앱의 +1km 모의 버튼으로 대신 검증하지 않습니다.
7. 제한 활성화 시 게임 UI가 숨겨지고, 해제 후에도 보호자가 재개해야 하는지 확인합니다.
8. 홈 이동/복귀, 프로세스 종료/복원, 서비스 끊김, 미지원·권한 오류도 별도로 기록합니다.

초기화 반환·리스너 등록 반환·APK 컴파일은 실제 서비스 데이터 수신 성공과 다릅니다. 실제 데이터 콜백과 제한 적용 영상/로그가 있어야 런타임 PASS로 기록합니다. 이 변경을 작성한 클라우드 작업 환경에서는 Pleos AVD 실행을 아직 하지 않았습니다.

## 기록

APK commit SHA, SDK/Vehicle Service/AVD 버전, 테스트 시각, capability 결과, 콘솔 입력, 실제 값, UX 상태, PASS/FAIL, 증빙 경로를 남깁니다. `scripts/collect-pleos-evidence.sh <serial>`은 로컬 로그만 저장합니다. 저장 결과를 확인하고 필요한 부분만 공유하세요.

## 공식 자료

- https://document.pleos.ai/docs/pleos-only/create-my-project
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object
- https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide
- https://github.com/Pleos-Playground/pleos-sample-vehicle-app

SDK signature/permission/source contract 확인은 저장소의 `Pleos SDK metadata` Actions 실행에서 재현할 수 있습니다. SDK 버전을 바꾸면 계약과 권한을 재확인하세요.

## SpeechToText connection

The Pleos flavor additionally uses SpeechToText SDK 3.3.1 in `ON_DEVICE` mode with `ko-KR` and the `STT_SERVICE` permission. No server key is configured. Actual initialization and generated-PCM recognition succeeded on the local AVD; details and reproduction commands are in `PLEOS_STT.md`. This evidence is distinct from real vehicle or live child-microphone recognition.
