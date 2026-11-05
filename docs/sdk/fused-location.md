# FusedLocation SDK 요약 (공식 문서 기준, 2026-11-04 확인)

출처: https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro 및 하위 페이지
SDK 버전: FusedLocation SDK v1.1.2
이 파일에 없는 내용(메서드 시그니처, 예외 종류)은 추측하지 말고 SDK 의존성을 추가한 뒤 실제 클래스(IDE 자동완성/소스 보기)로 확인한다.

## 프로젝트 설정
- 저장소(settings.gradle.kts, dependencyResolutionManagement > repositories):
  `maven("https://nexus-playground.pleos.ai/repository/maven-releases/")` (이미 Vehicle SDK용으로 등록되어 있음)
- 의존성(app/build.gradle.kts 또는 libs.versions.toml): `implementation("ai.pleos.playground:FusedLocation:1.1.2")`

## 권한
- 매니페스트에 `pleos.car.permission.FUSED_LOCATION` 선언 필요.
- API별 필요 권한은 API Reference의 각 메서드 페이지에 있다(문서 표가 비어 있음 → 확인 필요).
- Android 위치 권한(ACCESS_COARSE/FINE/BACKGROUND_LOCATION)의 동작은 이 SDK 문서에서 보장하지 않는다. Google Fused Location 문서를 Pleos 동작의 근거로 쓰지 않는다.
- 위치 접근이 막힌 상태의 사용자 안내는 Declaring Permission 페이지의 "Location Access Blocked State" 절과 `showLocationAccessBlockedPopup()`을 따른다(상세 미확인).

## 사용 흐름 (FusedLocation이 진입점)
1. `FusedLocation` 객체 생성(앱 범위에서 인스턴스 하나만 관리)
2. `isSupported()`로 지원 여부 확인. 지원하지 않으면 위치 관련 UI와 기능을 숨기거나 끈다.
3. `initialize()`
4. `getLastKnownLocation()`(마지막으로 알려진 위치를 콜백으로 1회 반환, 새로 계산하지 않음) 또는 `registerFusedLocationCallback(listener)`
5. `unregisterFusedLocationCallback(listener)` (등록할 때와 같은 listener 인스턴스)
6. `release()` — initialize()를 호출했다면 반드시 호출. release 전에 리스너를 먼저 해제. release 후에는 같은 인스턴스를 쓰지 않고, 다시 쓰려면 새 인스턴스 → isSupported → initialize.

## 리스너 (문서 예제)
```kotlin
val listener = object : FusedLocationListener {
    override fun onChanged(location: LocationData?) {
        location ?: return   // null은 유효한 위치가 아니다
        // 위치 사용
    }
    override fun onError(exception: Exception) {
        // 로그를 남기고 사용자에게 알리거나 기능을 줄인다
    }
}
fusedLocation.registerFusedLocationCallback(listener)
// 종료 시
fusedLocation.unregisterFusedLocationCallback(listener)
```

## 데이터
- `LocationData`: 위도, 경도, 고도, 방향(heading), confidence, timestamp
- 위치를 얻을 수 없으면 콜백에 null이 올 수 있다.

## 오류 처리
- SDK 오류는 `FusedLocationListener.onError(Exception)`으로 전달된다.
- 오류 종류·코드는 문서에 없다. 재시도 여부는 예외 종류와 실행 환경에 따라 판단한다.
- 위치가 없으면 Pit Crew는 "확인 불가"로 표시하고 앱은 계속 진행한다.

## 에뮬레이터
- Pleos Connect Emulator에서 개발·검증한다.
- Virtual Console 문서에는 위치/GPS를 시뮬레이션하는 항목이 없다(Motion은 기어, 브레이크, 조향각, 주행 거리). 에뮬레이터에서 가상 위치를 줄 수 있는지는 미확인 → 첫 호출로 직접 확인한다.

## Pit Crew 사용 규칙
- 사용자가 SC-205에서 위치를 허용한 경우에만 호출한다.
- 사용 목적: 사고 위치 기록(SC-200, SC-250), 주소 변환(Nominatim)에 좌표 전달.
- `addProximityAlert`, `removeProximityAlert`는 사용하지 않는다.
- 호출은 `isSupported()` → `initialize()` → `getLastKnownLocation()` 순으로, 사고 모드 시작 시 1회만 한다. 연속 업데이트는 쓰지 않는다.
