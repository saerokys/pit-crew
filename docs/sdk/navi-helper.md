# NaviHelper SDK 요약 (공식 문서 기준, 2026-11-04 확인)

출처: https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro 및 하위 페이지
SDK 버전: NaviHelper SDK v3.0.5 (문서 예시 버전. 프로젝트 기준 버전과 맞는지 확인)
이 파일에 없는 내용(정확한 시그니처, 파라미터 순서)은 추측하지 말고 SDK를 추가한 뒤 실제 클래스로 확인한다.

## 개요
- 서드파티 앱이 경로 안내를 제어하고 내비게이션 정보를 가져오는 SDK. 요청은 Pleos Connect Navigation 앱으로 전달된다.
- 기능: 목적지 설정·확인·취소, 경로 옵션·재탐색, 경유지, 경유지 포함 경로 요청, 경로 탐색·주행 상태 이벤트, 내비게이션 정보 조회, 내비게이션 화면 요청.
- 이벤트는 Navigation 앱의 구현과 상태에 따라 달라질 수 있다. 특정 이벤트가 항상 온다고 가정하지 않는다.

## 프로젝트 설정
- 저장소: `https://nexus-playground.pleos.ai/repository/maven-releases/` (Vehicle SDK용으로 이미 등록됨)
- 의존성: `implementation("ai.pleos.playground:NaviHelper:3.0.5")`

## 권한 (매니페스트)
- `pleos.car.permission.NAVI_ROUTE`: `get*Info()`, `requestTrip()`
- `pleos.car.permission.NAVI_ROUTE_SEARCH`: `requestRoute()`, `requestRouteWithWaypoints()`, `cancelRoute()`, `requestReRoute()`, `addWaypoint()`, `removeWaypoint()`, `changeRouteOption()`
- `pleos.car.permission.NAVI_CUSTOM_MAP`: `requestNavigationScreen()`
- 문서는 매니페스트 선언만 설명한다. 런타임 권한 요청과 권한이 없을 때의 동작은 문서에 없다. API별 정확한 권한은 API Reference의 `@RequiresPermission`을 확인한다.
- Pit Crew는 목적지 설정과 현재 위치 조회에 필요한 `NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`만 선언한다. `NAVI_CUSTOM_MAP`은 쓰지 않는다.

## 초기화와 해제
```kotlin
import ai.pleos.playground.navi.helper.NaviHelper

val naviHelper = NaviHelper(applicationContext)

naviHelper.initialize(
    onConnected = {
        // 첫 연결 성공. 여기서 리스너를 등록하고 API를 호출한다.
    },
    onFailed = { throwable ->
        // 초기 연결 재시도가 모두 실패. 기능을 끄고 "확인 불가"로 표시한다.
    }
)
```
- `onConnected` 안에서만 리스너를 등록하고 API를 호출한다.
- 별도의 지원 여부 확인 API는 문서에 없다. `onFailed`를 실패 경로로 쓴다.
- 종료 시: `removeListener(listener)` 후 `release()` (예: `onDestroy()`). release 후에는 새 객체를 만들어 다시 initialize한다.

## 이벤트 리스너
- 인터페이스: `ai.pleos.playground.navi.helper.listener.NaviHelperEventListener` (필요한 콜백만 override)
- 경로 탐색: `onRouteStarted(routeStartInfo)`, `onRoutingCompleted()`, `onRouteCancelled()`, `onReRouteEnded()`, `onRouteOptionChanged()`
- 목적지·경유지: `onDestinationChanged()`, `onDestinationArrived()`, `onWaypointChanged()`, `onWayPointArrived()`
- 정보 조회: `onBookmarkInfo()`, `onBookMarkChanged()`, `onRecentDestinationInfo()`, `onCurrentLocationInfo()`, `onDestinationInfo(destinationInfo)`, `onWayPointInfo()`, `onRouteStateInfo()`, `onTBTInfo()`, `onChargerOperatorInfo()`, `onTripInfo()`, `onNavigationStatus()`, `onDrivingInfo()`, `onCurrentRoadSpeedLimit()`, `onCameraAlert()`, `onCautionPointInfo()`
- 오류: `onError(errorCode: NaviErrorCode)`
```kotlin
private val naviEventListener = object : NaviHelperEventListener {
    override fun onRouteStarted(routeStartInfo: RouteStartInfo) {}
    override fun onRoutingCompleted() {}
    override fun onError(errorCode: NaviErrorCode) {}
}
naviHelper.addListener(naviEventListener)
naviHelper.removeListener(naviEventListener)   // 등록한 같은 인스턴스
```

## 목적지
| 동작 | API | 비고 |
|---|---|---|
| 목적지 설정 | `requestRoute(routeInfo: RouteInfo)` | 다시 설정하면 `requestRoute()`를 다시 호출 |
| 목적지 확인 | `getDestinationInfo()` | 반환값 없음. 결과는 `onDestinationInfo`로 받음 |
| 목적지 취소 | `cancelRoute()` | 결과는 이벤트로 확인 |

`RouteInfo`(Kotlin 예제의 이름 있는 인자): `latitude`, `longitude`, `displayLatitude`(null 가능), `displayLongitude`(null 가능), `poiName`, `poiId`, `poiSubId`, `address`
```kotlin
naviHelper.requestRoute(
    RouteInfo(
        latitude = 37.48544722,
        longitude = 127.03636666,
        displayLongitude = null,
        displayLatitude = null,
        poiName = "42dot",
        poiId = "36khp37hh8gqcm5nqhe5",
        poiSubId = "0",
        address = "서울시 강남구 남부순환로 2621"
    )
)
```
- `requestRoute()` 호출 성공은 경로 탐색 완료를 뜻하지 않는다. 완료는 `onRoutingCompleted()` 등 이벤트로 확인한다.
- 문서에 장소 검색(POI 검색) API는 없다. 좌표 없이 이름이나 주소만으로 목적지를 설정할 수 있는지도 문서에 없다(예제는 항상 좌표와 POI 정보를 넣는다). Java 예제는 인자 순서와 `RouteOption.RECOMMENDED`가 Kotlin과 달라 API Reference로 확인해야 한다.

## 오류 (`NaviHelperEventListener.onError(errorCode)`)
| 코드 | 의미 | 처리 |
|---|---|---|
| `CAUSE_LOW_BATTERY` | 배터리 낮음 | 사용자에게 알림 |
| `CAUSE_SEARCH_ROUTE_OVER_DISTANCE` | 최대 탐색 거리 초과 | 경로가 너무 멀다고 알림 |
| `CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE` | 목적지가 너무 가까움 | 알림 |
| `CAUSE_SEARCH_ROUTE` | 경로 탐색 서버 오류 | 재시도 또는 알림 |
- 오류 시 확인: SDK 연결 후 호출했는가, 필요한 권한을 선언했는가, 좌표·POI ID가 유효한가, Navigation 앱이 현재 상태에서 요청을 처리할 수 있는가.

## 에뮬레이터
- Pleos Connect Emulator에서 개발·검증한다. Navigation 앱이 에뮬레이터에서 요청을 처리하는지, `onFailed`가 나는지는 미확인 → 첫 호출로 확인한다.

## Pit Crew 사용 규칙 (SC-280)
- 사용자가 SC-280에서 목적지 유형을 고른 경우에만 `requestRoute()`를 호출한다. 앱이 임의로 길안내를 시작하지 않는다.
- 문서에 장소 검색 API가 없으므로 "가까운 정비소·병원"은 NaviHelper만으로는 만들 수 없다. 좌표가 필요하면 외부 API 결과(예: 키가 필요 없는 지도 검색)를 좌표로 넘기는 방식을 후보로 두되, 구현 전에 사용자와 합의한다. "안전한 곳으로 이동"도 마찬가지다.
- 현재 위치는 FusedLocation 대신 `getCurrentLocationInfo` 계열(`onCurrentLocationInfo`)도 후보다. 정확한 호출명은 API Reference로 확인한다.
- 연결 실패·오류 시 SC-280 버튼을 비활성화하고 이유를 표시한다.
