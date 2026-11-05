# TextToSpeech(TTS) SDK 요약 (공식 문서 기준, 2026-11-04 확인)

출처: https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro 및 하위 페이지
SDK 버전: TextToSpeech SDK v3.3.1
이 파일에 없는 내용은 추측하지 말고 SDK를 추가한 뒤 실제 클래스로 확인한다.

## 개요
- 앱이 텍스트를 음성으로 합성해 차량 오디오로 재생하게 하는 SDK. Pleos Connect Speech Service가 처리한다.
- 합성 모드(`Mode`):
  - `HYBRID`(기본값): 서버 스트리밍 TTS를 우선 쓰고 네트워크가 끊기면 온디바이스 엔진으로 전환. **앱 등록(My Project 인증 정보)이 필요**.
  - `SERVER`: 서버 TTS만. 네트워크와 앱 등록 필요. 실패 시 폴백 없이 오류.
  - `ON_DEVICE`: 차량 온디바이스 엔진. **단독 사용 시 네트워크도 앱 등록도 필요 없음**.
- Pit Crew는 앱 등록과 네트워크 의존을 피하기 위해 **반드시 `Mode.ON_DEVICE`를 명시**한다. 모드를 생략하면 HYBRID가 되어 앱 등록이 필요해진다.

## 프로젝트 설정
- 저장소: `https://nexus-playground.pleos.ai/repository/maven-releases/` (이미 등록됨)
- 의존성: `implementation("ai.pleos.playground:TextToSpeech:3.3.1")`

## 권한
- `pleos.car.permission.TTS_SERVICE`
```xml
<uses-permission android:name="pleos.car.permission.TTS_SERVICE" />
```
- 초기화, 이벤트 리스너 등록, 앱 등록, 합성 요청에 필요하다. 없으면 초기화나 호출이 실패할 수 있고, 오류 콜백에 `PERMISSION_DENIED`가 온다(먼저 매니페스트 확인).

## 소스로 확인한 내용 (3.3.1, 2026-11-05, AAR를 javap로 읽음)
- Nexus에 3.3.1이 있다(소스·javadoc jar는 없음). 의존 라이브러리: core-ktx 1.8.0, coroutines-android 1.6.3, gson 2.10.1, universal-access 0.6.0.
- 권한 `pleos.car.permission.TTS_SERVICE`는 protectionLevel=normal(차량 권한이 아닌 일반 권한). 권한이 없으면 `SecurityException`("Permission … is required")을 동기로 던지는 코드가 있다(문서는 오류 콜백 `PERMISSION_DENIED`라고 함).
- 에뮬레이터 확인(ON_DEVICE): **`initialize()` 뒤에 `addEventListener()`를 불러야 한다**(먼저 부르면 `UninitializedPropertyAccessException`). `initialize` 성공 콜백은 약 5~11ms. `speak()`는 성공 콜백 전에 불러도 동작하지만 앱은 성공 뒤에 부른다. `onReady()`는 발화 요청마다 온다. `isFinal=false`로 나눠 보내도 한 발화로 합쳐져 `onStart`/`onDone`이 한 번씩 오므로 한 번에 읽는다. 말하는 중 `speak()`를 한 번 더 부르면 첫 발화가 `onDone` 없이 끝나지 않는다.
- 소리와 한국어 발음은 출력장치가 없어 확인하지 못했다(이벤트와 `dumpsys audio`의 AudioTrack started, `onUpdatedRms`로만 확인).

## 사용 흐름
1. 인스턴스 생성: `TextToSpeech(applicationContext, Mode.ON_DEVICE)` (패키지 `ai.pleos.playground.tts.TextToSpeech`)
2. `initialize()` — 결과가 필요하면 `OnInitializeListener`(`onSuccess()`, `onFailure(errorType)`) 전달
3. `addEventListener(listener)`
4. 필요하면 `setLocale(Locale.KOREA)` (지원: ko-KR, en-US, fr-CA, es-US, es-MX. 설정하지 않으면 앱의 이전 설정, 없으면 시스템 로케일)
5. 서버를 쓰는 모드에서는 앱 등록 후 `OnServerConnectionListener.onConnected()` 이후에 `speak()`. ON_DEVICE 단독 사용 시 앱 등록은 필요 없으나 `onReady()` 이후 호출해야 하는지는 문서에 명시가 없다 → 에뮬레이터에서 확인.
6. `speak(text)` / `stop()`
7. 종료 시 `removeEventListener(listener)` 후 `release()`. release 후에는 새 인스턴스를 만들어 다시 initialize.

```kotlin
textToSpeech.initialize(object : OnInitializeListener {
    override fun onSuccess() { /* TTS API 사용 가능 */ }
    override fun onFailure(errorType: ErrorType) { /* 초기화 실패 원인 처리 */ }
})
```

## 이벤트 리스너 (`EventListener`)
- `onReady()` 합성 요청을 받을 준비 완료
- `onStart()` 재생 시작
- `onUpdatedRms(rms: Double)` 재생 중 RMS 값
- `onDone()` 재생 정상 종료
- `onStop()` 재생 중단
- `onError(errorType: ErrorType)` 오류
```kotlin
private val eventListener = object : EventListener {
    override fun onReady() = Unit
    override fun onStart() = Unit
    override fun onUpdatedRms(rms: Double) = Unit
    override fun onDone() = Unit
    override fun onStop() = Unit
    override fun onError(errorType: ErrorType) { /* 오류 처리 */ }
}
textToSpeech.addEventListener(eventListener)
```

## 말하기와 멈추기
- `speak(text)`: 한 번에 완결된 요청. 여러 조각으로 나눌 때는 마지막을 제외하고 `isFinal = false`를 넘긴다(생략하면 기본값 true로 요청이 끝난다).
  ```kotlin
  textToSpeech.speak("안전한 곳에 차를 세웠는지 확인하세요.")
  textToSpeech.speak("비상등을 켜세요. ", isFinal = false)
  textToSpeech.speak("모두 도로 밖으로 대피하세요.", isFinal = true)
  ```
- `stop()`: 진행 중인 합성·재생을 중단하고 `onStop()`이 호출된다. 별도 연결 조건은 문서에 없다.

## 오류 (`ErrorType`)
| 값 | 의미 | 조치 |
|---|---|---|
| `AUDIO_ERROR` | 오디오 출력·재생 실패 | 오디오 설정과 출력 장치 확인 후 재시도 |
| `INVALID_REQUEST_ERROR` | 요청 값이나 상태가 잘못됨 | 텍스트와 호출 순서 확인 |
| `MIC_ERROR` | 오디오 입력 장치 오류 | 런타임 환경의 오디오 상태 확인 |
| `NETWORK_ERROR` | 네트워크 통신 실패 | 연결 확인 후 재시도 |
| `NOT_SUPPORTED_API` | 현재 모드·환경에서 미지원 | `Mode`와 플랫폼 지원 확인 |
| `ON_DEVICE_ENGINE_ERROR` | 온디바이스 엔진 실패 | 엔진 상태 확인 (HYBRID·SERVER는 앱 등록이 필요해 쓰지 않음) |
| `PERMISSION_DENIED` | TTS 서비스 접근 권한 없음 | 매니페스트의 `TTS_SERVICE`와 런타임 권한 상태 확인 |
| `SERVER_INTERNAL_ERROR` | Speech Service 내부 오류 | 나중에 재시도 |
| `TIMEOUT` | 시간 초과 | 네트워크·서버 확인 후 재시도 |
| `UNAUTHENTICATED` | 앱 인증 실패 | My Project/UA 인증 정보 확인 |
| `UNAVAILABLE_LANGUAGE` | 요청한 언어 사용 불가 | 지원되는 `Locale` 선택 |
| `UNKNOWN` | 분류되지 않은 오류 | 호출 시점과 환경 정보 수집 |

## 에뮬레이터
- 에뮬레이터에서 개발·검증한다. 마이크 접근과 cold boot 설정 문서가 있다. 온디바이스 엔진이 에뮬레이터에서 동작하는지, 한국어(ko-KR)를 온디바이스로 합성하는지는 미확인 → 첫 호출로 확인한다.

## Pit Crew 사용 규칙
- 사용자가 SC-205에서 음성을 허용하고 [소리로 듣기]를 누를 때만 `speak()`를 호출한다. 자동 재생하지 않는다.
- `Mode.ON_DEVICE`, 로케일 `Locale.KOREA`를 쓴다. 앱 등록이 필요한 모드는 쓰지 않는다.
- 초기화 실패나 오류 시 [소리로 듣기] 버튼을 숨기거나 비활성화하고, 화면 텍스트 안내는 그대로 둔다.
- 화면을 떠나거나 사고 모드를 종료할 때 `stop()`을 호출한다.
