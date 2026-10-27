# Pleos API 실행 검증 계획

현재 APK는 **모의 입력 전용 데모**입니다. Pleos SDK 의존성, 권한, 인증, 실제 차량 상태 조회·제어는 구현하지 않았습니다. APK 빌드 성공은 API 지원 여부의 증거가 아닙니다. 실제 차량에 설치하거나 운행 중 사용하지 마세요.

## 실제 어댑터 구현 순서

1. Pleos Workspace에서 앱 프로젝트와 SDK 버전·신청 상태를 확인한다. 비밀키·인증 파일은 Git에 커밋하지 않는다.
2. 공식 샘플의 저장소, SDK dependency 좌표, initialization, permission, callback signature를 확인한다. 추측한 패키지명이나 메서드 시그니처를 코드에 넣지 않는다.
3. `VehicleGateway`를 구현한다. SDK 스레드에서 수신한 콜백은 Android 메인 스레드로 전달한다. 현재 엔진은 단일 스레드 사용이 전제다.
4. 연결 즉시 `UNKNOWN`을 전달한다. UX restriction과 별도 필요 조건을 모두 확인한 경우에만 `AVAILABLE`을 전달한다. 속도 0, 목적지 도착, mock 버튼으로 허용 상태를 만들지 않는다. 연결 해제·권한 오류·상태 미확인·오래된 상태는 `UNKNOWN`으로 전환한다.
5. 앱이 foreground로 복귀할 때 최신 제한 상태를 재조회한다. Activity가 멈출 때 UI를 차단하고 진행을 보존한다. 현재 데모는 복귀 후 개발자가 모의 상태를 다시 지정한다.
6. 거리 지원 여부, 단위, 갱신 시각, 재부팅·초기화 동작을 확인한 뒤 **km 누적 거리**로 정규화해 `onOdometer`로 전달한다. 배경 이벤트가 중단돼도 재진입 때 누적 거리를 재조회해 차이를 계산할 수 있어야 한다.
7. 실제 SDK 연동판에서 모의 입력 패널을 제거하고 데모와 application ID·배포 경로를 분리한다. 게임 관련 manifest/카테고리/UX 정책은 공식 가이드로 검증한다.

## 검증 매트릭스

| 항목 | 초기 코드 | 실제 환경 통과 기준 |
|---|---|---|
| SDK 연결·권한 | 미구현 | 승인된 SDK 연결/해제 로그 기록 |
| 이용 제한 | 모의 Gate | 출발·신호대기·제한 상태 재실행에서 게임 노출·입력 없음 |
| 누적 거리 | 모의 거리 | 실제 값과 단위 일치, null/미지원/역행 대응 |
| 저장·재개 | 로컬 단일 세션 | 프로세스 종료 후 복원, 자동 재개 없음 |
| 중복 부화 | 엔진에서 차단 | 동일 이벤트/연속 클릭/재실행으로 중복 지급 없음 |
| NaviHelper 도착 | 미구현 | 실제 콜백 확인, 도착만으로 이용 허용하지 않음 |
| ETA·위치 | 미구현 | 응답 필드·단위·갱신·권한 확인 |
| 조명·풍량 | 미구현 | 제공/권한/차종 지원 확인 후 변경·조회·복원 |
| 음성 | 미구현 | 제공 상태·오디오 포커스·이용 제한 적용 확인 |

## 기록 형식

SDK/에뮬레이터 버전, 차종(해당 시), 앱 commit SHA, 테스트 시각, capability 응답, 입력 값, 앱 로그, 실제 결과, PASS/FAIL, 증빙 영상 링크를 남긴다. mock 테스트 결과와 실제 SDK 결과는 별도 열로 관리한다.

## 에뮬레이터 준비

공식 설치 안내 기준: minSdk 28+, Gradle 8+, Java/Kotlin, Pleos Connect Emulator 2.1.0/API 34. 이 앱은 compileSdk 35와 minSdk 28을 사용하므로 API 34 에뮬레이터에서 설치 가능한 구성을 목표로 한다. 실제 실행 검증은 별도다.

Android Studio → SDK Manager → SDK Update Sites에서 공식 이미지 소스를 추가한다.

- Mac: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
- Windows/Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`

Device Manager에서 Automotive → Pleos Connect v2.1.0, API 34, RAM 4 GB, Cold boot를 설정한다.

Virtual Console은 지원 에뮬레이터에서 `adb forward tcp:8088 tcp:8088` 후 `http://127.0.0.1:8088/`에 접근한다. 콘솔 직접 조작과 앱 API 호출 성공은 구분한다.

공식 문서 확인일: 2026-10-27.

- https://appdevchallenge.auto-hmg.io/guide/development
- https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator
