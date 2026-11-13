# PARK ON WHEELS

4~9세 아이와 보호자가 **주차 중 함께 사용하는** 네 가지 테마의 가족 탐험 앱입니다. Kotlin/Compose, Java 여정 엔진, Pleos Vehicle SDK 2.3.2를 사용합니다. 운전자 화면을 덮거나 주행 제한을 우회하지 않습니다.

## 구현된 경험

- 다이노 사파리 / 매직 캐슬 / 스페이스 포트 / 오션 서브마린의 독자 제작 벡터 일러스트.
- 테마별 두 가지 이야기, 세 개의 선택 미션, 엔딩과 보상 스탬프. 다음 방문에는 다른 이야기를 번갈아 제공합니다.
- 역할 선택, 탑승권, 에피소드 번호, 가족 여행 패스포트와 날짜별 기록.
- **주차 중 짧은 탐험:** 세 미션 후 엔딩. 실제 거리를 생성하거나 증가시키지 않습니다.
- **이동 기록 탐험:** 실제 누적 거리 5km와 세 미션 후, 다시 주차한 상태에서 엔딩. 내비 목적지 도착을 의미하지 않습니다.
- 세션·테마·역할·미션·설정·스탬프를 하나의 로컬 저장으로 반영. 재시작 시 PAUSED, 재개는 수동 확인.
- 보호자 확인 영역, 기록 삭제 확인, 차분한 엔딩 장식 설정. 결제와 서버 계정은 없습니다.
- 넓은 창에서는 장면과 미션을 나란히 배치하고 좁은 창에서는 세로 배치합니다.

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

내비 도착/ETA, 실제 충전 연계, 주행 중 음성 게임, 실차 검증, 클라우드 동기화는 아직 구현·검증하지 않았습니다. 우주정거장 도킹 미션은 **상상 미션**이며 실제 차량 충전을 제어하지 않습니다.

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
