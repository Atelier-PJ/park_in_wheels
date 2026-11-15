# PARK ON WHEELS

4~9세 아이와 보호자가 **주차 중 함께 사용하는** 네 가지 테마의 가족 탐험 앱입니다. Kotlin/Compose, Java 여정 엔진, Pleos Vehicle SDK 2.3.2를 사용합니다. 운전자 화면을 덮거나 주행 제한을 우회하지 않습니다.

## 공모전 제출 APK

**[제출용 release APK 다운로드 페이지](https://github.com/Atelier-PJ/park_in_wheels/releases/tag/competition-2026-v1)**

- 파일명: `WONDERWHEELS_PARKONWHEELS_release.apk`
- 위치: 위 GitHub Release의 **Assets**. APK가 Git 저장소의 단일 파일 용량 한도를 초과하여 Releases로 제공합니다. `apk/README.md`에도 동일한 링크를 제공합니다.
- 빌드: `pleosRelease`, 패키지 `com.parkonwheels.pleos`, 버전 `0.2.0-family` (2). 전용 release 키로 서명하고 디버그 기능을 비활성화합니다.
- 최종 소스 커밋과 APK SHA256은 Release 본문 및 `apk/README.md`에서 확인할 수 있습니다. Actions의 일시 보관 debug 산출물과 구분해 주세요.

### 설치와 실행

Pleos Connect API 34 에뮬레이터 또는 지원 Pleos 기기에서 실행합니다. 일반 휴대폰이나 Google 기본 에뮬레이터에는 차량 서비스가 없어 체험이 제한될 수 있습니다.

1. Release의 Assets에서 위 APK를 내려받습니다.
2. Android SDK의 `adb`가 PATH에 있는 터미널에서 아래 명령을 실행합니다. 파일 경로는 실제 다운로드 위치로 바꿉니다.

```bash
adb devices
adb -e install -r "/다운로드/경로/WONDERWHEELS_PARKONWHEELS_release.apk"
adb -e shell am start --user current -n com.parkonwheels.pleos/com.parkonwheels.demo.MainActivity
```

기존 debug 앱은 서명키가 달라 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`이 발생할 수 있습니다. 기존 기록이 필요하면 보존한 별도 AVD에서 시험하세요. 기존 앱을 삭제해도 괜찮은 경우에만 `adb -e uninstall com.parkonwheels.pleos` 후 설치합니다. 삭제하면 앱 기록도 삭제됩니다. 실제 기기가 여러 개 연결되면 `-e` 대신 `-s <serial>`을 사용합니다.

### 테스트 환경과 필요한 설정

- 로컬 검증 환경: macOS Apple Silicon, Pleos Connect AVD / Android API 34, Vehicle SDK 2.3.2, SpeechToText SDK 3.3.1.
- 기어 **P**, UX 제한 해제, 유효한 차량 거리 조회가 필요합니다. 상태 미확인·주행·백그라운드에서는 재생과 음성 상호작용을 중단합니다.
- 마이크는 보호자가 시작하고 `RECORD_AUDIO` 권한을 허용한 뒤 사용합니다. Pleos `STT_SERVICE`와 한국어 온디바이스 인식을 사용하며 서버 키는 필요하지 않습니다.
- 한국어 오프라인 TTS를 사용하고, 사용할 수 없으면 앱에 포함된 한국어 녹음을 재생합니다.
- 조명은 보호자 설정에서 켭니다. 차량의 `CAR_LIGHTS` / `CONTROL_CAR_LIGHTS` 권한과 지원 capability가 필요합니다.
- 에뮬레이터 조명 시험에서는 아래 포트 전달 후 **[Pleos Virtual Console](http://127.0.0.1:8088/)** 브라우저 탭을 열어 둡니다. 탭이 닫히면 DDS 시뮬레이션이 비활성화되어 색이 바뀌지 않을 수 있습니다.

```bash
adb -e forward tcp:8088 tcp:8088
```

체험 순서: **입장하기 → 매직 캐슬 → 이야기 듣기 → 마지막 장면에서 “별빛을 밝혀라!” → 금빛 조명과 스탬프 → 내 패스포트**. 조명 미지원·실패가 스탬프 지급을 막지는 않습니다. 음성이 어려우면 화면에 표시되는 터치 대안으로 엔딩을 열 수 있습니다.

### 제출 저장소 공개 일정

주최 측의 수정 안내에 따라 접수 마감인 **2026년 11월 15일 23:59까지는 비공개 제출이 가능**하며 심사 계정 초대는 필요하지 않습니다. 접수 메일에 저장소 URL과 최종 커밋 해시를 기재합니다. **11월 16일까지 Public으로 전환하고 11월 23일 1차 결과 발표일까지 공개 상태를 유지**해야 합니다. 현재 공개 전환 완료를 의미하지 않으며, 실제 공개 여부는 GitHub 저장소 설정에서 확인해야 합니다.

## 구현된 경험

- 다이노 사파리 / 매직 캐슬 / 스페이스 포트 / 오션 서브마린의 그림책 스타일 일러스트와 애니메이션.
- 테마 선택 다음에 우리 차가 탐험차·마법차·우주선·잠수함으로 변하는 4.2초 GIF가 한 번 재생됩니다. 테마별 출발 버튼으로 이야기를 시작하며 차분한 화면 설정에서는 정지 그림을 표시합니다. 이 변경은 로컬 개발 버전으로, 기존 제출 Release에는 아직 포함하지 않았습니다.
- 놀이공원 입구에서 테마를 고르고 전체 화면 이야기·한국어 나레이션·마지막 음성 주문·엔딩과 스탬프로 이어지는 체험.
- 매직 캐슬의 “별빛을 밝혀라!” 인식 성공 시 지원 차량 앰비언트 조명에 금빛 RGB를 요청합니다.
- 여행 패스포트의 스탬프 기록과 네 테마 수집함. 공룡·마법 배지·행성·물고기 총 24종은 도감 예시이며, 그림 확대와 짧은 설명 나레이션을 제공합니다. 개별 수집품 잠금 해제는 향후 계획입니다.
- 세션·미션 단계·설정·스탬프를 로컬 저장하고, 재시작 시 수동으로 재개합니다. 음성의 초 단위 재생 위치 복원은 제공하지 않습니다.
- 보호자 설정과 기록 삭제 확인. 결제와 서버 계정은 없습니다. 선택적 충전·내비 연동은 아래 로컬 개발 기능을 참고하세요.

## 음성

주차 중 그림과 한국어 안내를 따라가는 이야기 재생과 마지막 주문 인식을 제공합니다. Pleos 빌드는 **SpeechToText SDK 3.3.1 / ON_DEVICE / ko-KR**를 사용하며, 데모 빌드는 Android의 기기 내 SpeechRecognizer를 사용합니다. 서버 인식으로 자동 전환하지 않으며 Pleos ON_DEVICE 연결에는 서버 인증키를 사용하지 않습니다.

- Pleos 권한: `pleos.car.permission.STT_SERVICE`. 마이크 사용은 보호자가 시작하고 `RECORD_AUDIO`를 허용한 뒤 진행합니다.
- 한국어 오프라인 TTS가 있으면 사용하고, 없으면 앱에 담긴 한국어 녹음을 재생합니다.
- 마지막 주문을 말하면 앱이 최종 인식 결과를 비교해 엔딩과 스탬프 저장을 실행합니다. 부분 결과는 보상을 열지 않습니다.
- 최대 12초 듣기, 취소·주차 해제·화면 이탈·오디오 포커스 상실 시 중단. 과거 세션의 늦은 결과는 무시합니다.
- 원본 음성과 인식 문장을 저장하거나 로그에 남기지 않습니다. 인식 실패 시 다시 말하기와 명시적 터치 대안을 제공합니다.
- 로컬 Pleos AVD에서 SDK 초기화, 한국어 설정과 생성된 PCM 테스트 음원의 실제 온디바이스 인식을 확인했습니다. 어린이 발화나 실제 차량 마이크 검증과는 구분합니다.

전용 검증 APK 빌드 및 실행:

```bash
./gradlew :app:assemblePleosDebugAndroidTest -PpleosTestRunner=com.parkonwheels.demo.PleosSpeechVerification
adb install -r app/build/outputs/apk/androidTest/pleos/debug/app-pleos-debug-androidTest.apk
adb shell am instrument -w com.parkonwheels.pleos.test/com.parkonwheels.demo.PleosSpeechVerification
```

이 검사는 에뮬레이터에서만 실행되며, 합성 테스트 PCM을 공식 `sendAudio` API에 전달합니다. 실제 음성 결과를 주입하거나 마이크 성공으로 보고하지 않습니다.

## 앰비언트 조명

보호자 설정에서 켜면 **주차 중에만** 테마별 고정 RGB 색을 적용합니다. 실제 Vehicle Light API, `CAR_LIGHTS` + `CONTROL_CAR_LIGHTS` 권한, `READ_WRITE` capability와 지원 좌석 영역을 확인하고 기존 색을 읽은 다음 적용합니다. 지원·권한·콜백이 불명확하면 변경하지 않습니다.

밝기·점멸·전조등·실내등 스위치·주행모드 자동 색 설정은 변경하지 않습니다. 주행 중에는 새 명령을 보내지 않습니다. 켜 둔 고정 색은 차량에 남을 수 있습니다. 보호자가 주차 중 기능을 끄면 원래 색을 복원하며, 앱 적용 뒤 차량에서 직접 바꾼 색은 덮어쓰지 않습니다. 강제 종료나 서비스 단절 뒤 원래 색 복원은 보장할 수 없으므로 차량 기본 조명 UI에서도 변경할 수 있습니다.

Pleos AVD 조명 검증은 **Virtual Console 브라우저 탭을 열어 둔 상태**에서 진행해야 합니다. 탭 연결이 없으면 차량 DDS 시뮬레이션이 비활성화되어 쓰기 성공 응답 뒤에도 실제 색이 갱신되지 않을 수 있습니다. `python3 scripts/check-pleos-console.py`로 준비 상태를 확인하고 `bash scripts/verify-starlight.sh`로 실제 인식·조명 재조회·스탬프 검증을 실행합니다. [원인과 해결 기록](docs/PLEOS_AMBIENT_EMULATOR.md).

## 안전과 미연동 범위

Pleos 빌드는 최신 UX 제한, 실제 기어 P, 최신·유효 누적 거리값을 함께 확인합니다. 속도 0이나 도착 이벤트만으로 주차를 추정하지 않습니다. SDK 콜백은 메인 스레드에서 갱신하며 과거 세대·늦은 기어 콜백은 무시합니다.

로컬 개발 버전에는 선택적 충전 상태 읽기와 내비 도착 이벤트 연결이 추가되었습니다. ETA 기반 콘텐츠, 주행 중 음성 게임, 실차 검증, 클라우드 동기화는 제공하지 않습니다. 기본 우주정거장은 상상 미션이며, 충전 상태가 확인된 특별 에피소드도 실제 차량 충전을 제어하지 않습니다.

## 빌드

JDK 17, Gradle 8.11.1, Android platform 35 / build-tools 35.0.0이 필요합니다. `local.properties`의 SDK 경로는 자신의 환경에 맞게 설정합니다.

```bash
bash scripts/test-core.sh
./gradlew :app:assembleDemoDebug :app:assemblePleosDebug :app:lintDemoDebug :app:lintPleosDebug
```

| 빌드 | 패키지 | 데이터와 제어 |
|---|---|---|
| demoDebug | com.parkonwheels.demo | 보호자 개발 영역의 명시적 모의 입력. 조명은 미리보기만 제공 |
| pleosDebug | com.parkonwheels.pleos | 실제 Vehicle SDK, CarUxRestrictions, 기어, 거리, 지원 시 조명 |

APK: `app/build/outputs/apk/{demo,pleos}/debug/`.

## 검증

- Java 도메인 시나리오: `scripts/test-core.sh` (주차/만료/보상/명령 파싱).
- 데모 네이티브 모델 통합 검사: 네 테마, 저장 복원, 보상 중복 방지, 다음 에피소드, 주행 제한.
- Pleos capability 검사는 에뮬레이터에서만 허용하며 실제 P/UX/거리 수신과 음성·조명 지원 상태를 기록합니다. 실제 음성 인식 성공 또는 조명 변경 성공과 구분합니다.

```bash
./gradlew :app:assembleDemoDebugAndroidTest :app:assemblePleosDebugAndroidTest
adb install -r app/build/outputs/apk/demo/debug/app-demo-debug.apk
adb install -r app/build/outputs/apk/androidTest/demo/debug/app-demo-debug-androidTest.apk
adb shell am instrument -w com.parkonwheels.demo.test/com.parkonwheels.demo.AdventureVerification
```

Pleos 검사는 해당 SDK APK와 `app-pleos-debug-androidTest.apk` 설치 후 `com.parkonwheels.pleos.test/com.parkonwheels.demo.PleosCapabilityVerification`을 실행합니다. 이 검사는 지원될 경우 에뮬레이터 앰비언트 색을 잠시 적용하고 복원을 시도합니다. 실차에서는 실행하지 않습니다.

공식 정책: https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-driver-distraction-elements

## 서명된 release 재빌드

JDK 17과 위 Android SDK 환경에서 다음 환경변수를 설정합니다. 서명키와 비밀번호는 별도로 보관하며 저장소나 명령 출력에 노출하지 않습니다.

- `POW_RELEASE_STORE_FILE`: release keystore 절대 경로
- `POW_RELEASE_STORE_PASSWORD`: keystore 비밀번호
- `POW_RELEASE_KEY_ALIAS`: 키 alias
- `POW_RELEASE_KEY_PASSWORD`: 키 비밀번호

```bash
./gradlew :app:assemblePleosRelease :app:lintPleosRelease
```

산출물: `app/build/outputs/apk/pleos/release/app-pleos-release.apk`. 서명 설정이 없으면 명시적 release 작업은 실패합니다. debug 빌드는 서명 환경변수 없이 계속 사용할 수 있습니다. 심사용 설치에는 직접 재빌드한 파일보다 위 Release의 서명 완료 APK를 사용해 주세요.

## 로컬 개발 중인 모험 소리와 여행 연동

기존 제출 Release에는 아직 포함하지 않은 변경입니다.

- **모험 소리:** 네 테마의 출발 효과음과 루프 배경음을 앱에 포함합니다. 보호자 설정에서 끌 수 있으며, 나레이션 중 음량을 낮추고 마이크·안전 제한·화면 이탈·오디오 포커스 상실 시 중단합니다.
- **충전소 특별 모험:** 보호자 opt-in과 `android.car.permission.CAR_ENERGY` 읽기 권한이 필요합니다. AAOS `EV_CHARGE_STATE`의 실제 CHARGING과 최신 주차 조건을 확인해 스페이스 포트 시작 시 특별 에피소드를 선택합니다. 충전 스위치나 케이블 연결만으로 충전 중이라고 추정하지 않습니다. 에너지 조각 3개를 보내고 결말을 열면 우주정비 배지를 저장합니다. 충전이 끝날 때까지 기다릴 필요가 없으며, 상태를 모르면 일반 이야기로 진행합니다.
- **여행지 방문 스탬프:** 보호자가 켜면 NaviHelper 3.0.5의 경로 진행과 목적지 도착 이벤트를 관찰합니다. 앱이 전면에 있는 동안 경로 진행을 확인한 뒤 도착 알림을 받으면, 10분 안에 실제 주차 조건을 확인하고 버튼을 눌러 받습니다. 도착만으로 게임을 허용하거나 자동 보상하지 않습니다. 같은 목적지는 하루 한 번이며 재시작 후 중복 지급을 막습니다. 앱이 닫혀 있을 때의 도착 알림은 수집하지 않습니다.
- **정보 처리:** 좌표를 요청하지 않고 목적지 이름·주소를 저장하지 않습니다. SDK 도착 알림의 이름·주소를 기기 내에서 하루별 salted digest로 변환한 식별자와 테마·날짜만 저장합니다. 주소·이름 원문은 앱 로그에 남기지 않습니다. 보호자 기록 삭제로 방문 기록도 지워집니다.

검증: Java 48개 기존 검사 + 여행 정책 17개; 테스트 신호를 사용한 에너지 조각→배지·방문 버튼→패스포트 UI 검사 통과. 실제 Pleos AVD에서 충전 상태 `false` 조회와 NaviHelper 연결을 확인했습니다. 실제 충전 전환 및 실제 목적지 도착 양성 이벤트는 아직 검증하지 않았습니다. 증빙은 `docs/verification/travel-experiences/`에 구분해 기록합니다.

### 여행지 기반 기본 추천 (로컬 개발본)

테마를 고르고 차량이 변신하면 ‘오늘은 어디로 가볼까?’에서 테마에 맞는 여행지 3곳을 보여 줍니다. 여행지를 고르거나 나중에 고르기로 건너뛴 뒤 이야기를 시작합니다. 서버 없이 동작합니다. [지원 범위와 사용법](docs/DESTINATION_RECOMMENDATIONS.md)을 참고하세요. 기존 공모전 Release 파일에는 이 로컬 변경이 자동 반영되지 않습니다.
