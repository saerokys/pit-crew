package ai.pleos.playground.sample.vehicle.navi

/** SC-280 "어디로 갈까요?" 카드 3개. 제목과 부제는 시안 문구. */
enum class DestinationCategory(val title: String, val subtitle: String) {
    SAFE("안전한 곳으로 이동", "차를 세울 수 있는 곳"),
    GARAGE("정비소", "차를 점검하고 고칠 곳"),
    HOSPITAL("병원", "다친 곳을 확인할 곳")
}

/**
 * 시연용 예시 후보 한 곳. 사용자 위치와 무관한 상수라 거리는 보여 주지 않는다.
 * 이름은 가상("예시 병원 1" 등)이고 좌표는 공공 장소(공원)의 것이다. 실제 병원·정비소가 아니며 추천이 아니다.
 */
data class DemoPlace(val id: String, val name: String, val latitude: Double, val longitude: Double)

/**
 * 후보 장소 목록(결정: 앱에 넣은 시연용 후보 목록, 실제 장소 검색은 2차 PT 전에 다시 검토할 후속 과제).
 * 좌표는 OpenStreetMap에서 공원(leisure=park)으로 확인한 공공 장소이고, 에뮬레이터 가상 위치(서울 강남 일대) 근처다.
 * 개인 주택이나 사업장 근처가 아닌 공공 장소만 쓴다. 사용자 위치 좌표는 어디에도 쓰지 않는다.
 * 확인한 장소(좌표 제외): 도곡근린공원, 한티근린공원, 대치근린공원, 포이공원, 양재천 근린공원, 양재시민의숲, 도산공원, 청담근린공원, 학동공원.
 */
object DemoPlaces {
    private val safe = listOf(
        DemoPlace("demo-safe-1", "예시 지점 1", latitude = 37.5242213, longitude = 127.0352168), // 도산공원
        DemoPlace("demo-safe-2", "예시 지점 2", latitude = 37.5212572, longitude = 127.0523901), // 청담근린공원
        DemoPlace("demo-safe-3", "예시 지점 3", latitude = 37.5155791, longitude = 127.0252579) // 학동공원
    )
    private val garage = listOf(
        DemoPlace("demo-garage-1", "예시 정비소 1", latitude = 37.4727520, longitude = 127.0519046), // 포이공원
        DemoPlace("demo-garage-2", "예시 정비소 2", latitude = 37.4767867, longitude = 127.0416036), // 양재천 근린공원
        DemoPlace("demo-garage-3", "예시 정비소 3", latitude = 37.4702765, longitude = 127.0358788) // 양재시민의숲
    )
    private val hospital = listOf(
        DemoPlace("demo-hospital-1", "예시 병원 1", latitude = 37.4894534, longitude = 127.0437322), // 도곡근린공원
        DemoPlace("demo-hospital-2", "예시 병원 2", latitude = 37.4954023, longitude = 127.0621185), // 한티근린공원
        DemoPlace("demo-hospital-3", "예시 병원 3", latitude = 37.4952798, longitude = 127.0698658) // 대치근린공원
    )

    fun forCategory(category: DestinationCategory): List<DemoPlace> = when (category) {
        DestinationCategory.SAFE -> safe
        DestinationCategory.GARAGE -> garage
        DestinationCategory.HOSPITAL -> hospital
    }

    fun find(category: DestinationCategory, id: String?): DemoPlace? =
        forCategory(category).firstOrNull { it.id == id }
}
