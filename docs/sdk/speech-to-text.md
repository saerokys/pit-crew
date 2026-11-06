# SpeechToText(STT) SDK 요약 (공식 문서 기준, 2026-11-04 확인)

출처: https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro 및 하위 페이지
SDK 버전: SpeechToText SDK v3.3.1
이 파일에 없는 내용은 추측하지 말고 SDK를 추가한 뒤 실제 클래스로 확인한다.

## 개요
- 차량 마이크로 들은 음성을 텍스트로 바꿔 돌려주는 SDK. 인식된 텍스트만 돌려주고 동작(명령)은 실행하지 않는다("음성 명령 기능을 제공하지 않음").
- 인식 모드(`Mode`):
  - `HYBRID`(기본값): 서버 스트리밍 우선, 네트워크 실패 시 온디바이스 전환. 앱 등록(My Project) 필요.
  - `SERVER`: 네트워크와 앱 등록 필요. 폴백 없음.
  - `ON_DEVICE`: 차량 안에서 동작. 이 모드만 쓰면 앱 등록이 필요 없다.
- Pit Crew는 `Mode.ON_DEVICE`를 명시해서 쓴다. 모드를 생략하면 HYBRID가 되어 앱 등록이 필요해진다.
- 온디바이스 모드가 한국어를 지원하는지는 문서에서 확인하지 못했다. 에뮬레이터에서 `setLocale(Locale.KOREA)`는 서비스가 ko-KR로 받아들였고 `UNAVAILABLE_LANGUAGE`는 오지 않았다. **실제 인식은 마이크가 없어 미확인**(2026-11-06).

## 프로젝트 설정
- 저장소: `https://nexus-playground.pleos.ai/repository/maven-releases/` (이미 등록됨)
- 의존성: `implementation("ai.pleos.playground:SpeechToText:3.3.1")`

## 권한
- `pleos.car.permission.STT_SERVICE`
```xml
<uses-permission android:name="pleos.car.permission.STT_SERVICE" />
```
- 초기화, 결과 리스너 등록, 앱 등록, 인식 요청, 오디오 전송에 필요하다. 없으면 초기화나 호출이 실패할 수 있고 `PERMISSION_DENIED`가 온다.
- 문서는 Android `RECORD_AUDIO` 권한을 언급하지 않는다. 확인 결과(2026-11-06, 에뮬레이터): `STT_SERVICE`는 protectionLevel=normal(설치 때 자동 허용), SDK 안에 `RECORD_AUDIO` 문자열이 없고, 서비스가 호출한 앱의 `RECORD_AUDIO` 앱옵스를 확인해 허용했다. 앱은 `RECORD_AUDIO`를 선언하지 않는다(실제 차량은 미확인).

## 사용 흐름
1. 인스턴스 생성: `SpeechToText(context, Mode.ON_DEVICE)` (패키지 `ai.pleos.playground.stt.SpeechToText`)
2. `initialize()` — 결과가 필요하면 `OnInitializeListener`(`onSuccess()`, `onFailure(errorType)`) 전달
   ```kotlin
   import ai.pleos.playground.stt.constant.ErrorType
   import ai.pleos.playground.stt.listener.OnInitializeListener

   speechToText.initialize(object : OnInitializeListener {
       override fun onSuccess() { /* API 사용 가능 */ }
       override fun onFailure(errorType: ErrorType) { /* 실패 원인 처리 */ }
   })
   ```
3. `addListener(resultListener)` — `request()` 전에 등록
4. 필요하면 `setLocale()`로 언어 설정 (지원 언어는 문서 확인 필요)
5. `request()` — 차량 마이크를 켜고 인식 시작. 초기화와 리스너 등록이 끝난 뒤에만 호출. 서버 모드는 앱 등록 필요
6. `stop()` — 인식을 멈추고 마이크 해제. 끝나면 `onEndedRecognition()` 호출
7. 종료 시 `removeListener(resultListener)` 후 `release()`. release 후에는 새 인스턴스를 만들어 다시 initialize.

## 결과 리스너 (`ResultListener`)
| 콜백 | 시점 |
|---|---|
| `onReady()` | 인식 요청을 받을 수 있음 |
| `onStartedRecognition()` | 인식 시작 |
| `onUpdated(stt: String, completed: Boolean)` | 인식 텍스트 변경. `completed`가 true면 최종 결과 |
| `onUpdatedEpdData(on, off)` | 음성 시작·끝 위치(바이트 오프셋) |
| `onUpdatedRms(rms)` | 입력 신호 크기(dB 아님) |
| `onEndedRecognition()` | 인식 종료 |
| `onError(errorType)` | 오류 |
```kotlin
private val resultListener = object : ResultListener {
    override fun onReady() {}
    override fun onStartedRecognition() {}
    override fun onUpdated(stt: String, completed: Boolean) {
        if (completed) handleFinal(stt)   // 최종: 확인 화면에 보여 주고 사용자가 저장 여부 결정
        else showPartial(stt)             // 중간: 실시간 표시용, 바뀔 수 있음
    }
    override fun onUpdatedEpdData(on: Long, off: Long) {}
    override fun onUpdatedRms(rms: Double) {}
    override fun onEndedRecognition() {}
    override fun onError(errorType: ErrorType) { /* 오류 처리 */ }
}
```

## 오류
- 오류 종류는 `ErrorType`으로 전달된다. 전체 표는 문서의 Error code 페이지(`SpeechToText/error-code`)에 있으며 아직 읽지 않았다. TTS의 `ErrorType`과 `PERMISSION_DENIED`, `MIC_ERROR`, `NETWORK_ERROR` 같은 값이 겹칠 가능성이 있지만 확인 전에는 같다고 가정하지 않는다.

## 에뮬레이터
- 확인한 콜백 의미(에뮬레이터): `onReady`는 서비스가 마이크를 열고 말을 받을 준비가 된 시점(`request()` 뒤 약 0.3~1.3초, 앱 설치 직후 첫 호출은 약 4.7초)이고, `onStartedRecognition`은 말소리가 감지되었을 때 온다(조용하면 오지 않음). 말소리가 없으면 `onEndedRecognition`·`onError` 없이 계속 기다리므로 앱이 `stop()`을 불러야 한다. 해제 순서 `stop()` → `removeListener()` → `release()`는 서비스 쪽에서도 `close`와 녹음기 해제로 보인다.
- 에뮬레이터에서 개발·검증하며, 마이크 접근 설정(configure-pleos-connect-emulator의 microphone access)이 필요하다. 에뮬레이터에서 실제로 인식이 되는지는 미확인.

## Pit Crew 사용 규칙 (SC-230)
- 사용자가 SC-205에서 음성을 허용하고 [말하기]를 눌렀을 때만 `request()`를 호출한다. 자동으로 마이크를 켜지 않는다.
- 최종 결과(`completed = true`)만 화면에 보여 주고 [저장] [다시 말하기]를 사용자가 고른다. 저장하지 않은 텍스트는 보관하지 않는다.
- 텍스트는 기록 패키지에만 들어가며, 보험사 전달은 SC-290에서 사용자가 항목별로 선택했을 때만 한다.
- 오류나 초기화 실패 시 음성 카드를 비활성화하고 "음성 사용이 꺼져 있거나 사용할 수 없어요"를 표시한다. 사고 모드는 그대로 진행된다.
- 사고 모드 종료, 화면 이탈 시 `stop()`과 리스너 해제를 한다.
