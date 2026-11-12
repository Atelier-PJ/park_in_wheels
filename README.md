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

주차 중 선택지 읽기와 음성 선택을 연결했습니다. 한국어 **오프라인 TTS 음성**과 Android 31 이상 **기기 내 SpeechRecognizer**가 있는 기기에서 사용할 수 있습니다. 엔진 미지원·권한 거부·인식 실패 시 터치로 동일한 미션을 수행합니다. 네트워크 인식기로 자동 대체하지 않습니다.

- “첫 번째”, “두 번째”, 선택지의 이름, “다음”, “다시”, “그만”, “깨어나라” 등의 제한된 명령.
- 안내 후 최대 12초만 듣습니다. 원본 음성·인식 문장은 저장하거나 로그에 남기지 않습니다.
- 화면 이탈·백그라운드·UX 제한·P 해제·오디오 포커스 상실 시 취소. 통화 중에는 시작하지 않습니다.
- 주행 중 게임·음성 상호작용은 비활성화되어 있습니다. DO나 오디오 서비스로 우회하지 않습니다.
- 이 연결은 Android 플랫폼 TTS/STT입니다. 별도의 Pleos TTS/STT 서버 SDK 통합을 완료했다고 주장하지 않습니다.

## 앰비언트 조명

보호자 설정에서 켜면 **주차 중에만** 테마별 고정 RGB 색을 적용합니다. 실제 Vehicle Light API, `CAR_LIGHTS` + `CONTROL_CAR_LIGHTS` 권한, `READ_WRITE` capability와 지원 좌석 영역을 확인하고 기존 색을 읽은 다음 적용합니다. 지원·권한·콜백이 불명확하면 변경하지 않습니다.

밝기·점멸·전조등·실내등 스위치·주행모드 자동 색 설정은 변경하지 않습니다. 주행 중에는 새 명령을 보내지 않습니다. 켜 둔 고정 색은 차량에 남을 수 있습니다. 보호자가 주차 중 기능을 끄면 원래 색을 복원하며, 앱 적용 뒤 차량에서 직접 바꾼 색은 덮어쓰지 않습니다. 강제 종료나 서비스 단절 뒤 원래 색 복원은 보장할 수 없으므로 차량 기본 조명 UI에서도 변경할 수 있습니다.

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
