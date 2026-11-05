package ai.pleos.playground.sample.vehicle.vehicle

import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.WindowValue

/**
 * 영역의 한국어 이름(SC-250, SC-260 공통). 운전석·조수석 같은 표현은 쓰지 않는다.
 * 목록 순서가 화면에 나열되는 순서이다.
 */
object AreaNames {
    val door: Map<DoorValue, String> = linkedMapOf(
        DoorValue.ROW_1_LEFT to "앞 왼쪽 문",
        DoorValue.ROW_1_RIGHT to "앞 오른쪽 문",
        DoorValue.ROW_2_LEFT to "뒤 왼쪽 문",
        DoorValue.ROW_2_RIGHT to "뒤 오른쪽 문",
        DoorValue.ROW_3_LEFT to "3열 왼쪽 문",
        DoorValue.ROW_3_RIGHT to "3열 오른쪽 문",
        DoorValue.HOOD to "앞쪽 덮개",
        DoorValue.REAR to "뒤쪽"
    )

    val window: Map<WindowValue, String> = linkedMapOf(
        WindowValue.ROW_1_LEFT to "앞 왼쪽 창문",
        WindowValue.ROW_1_RIGHT to "앞 오른쪽 창문",
        WindowValue.ROW_2_LEFT to "뒤 왼쪽 창문",
        WindowValue.ROW_2_RIGHT to "뒤 오른쪽 창문",
        WindowValue.FRONT_WINDSHIELD to "앞유리",
        WindowValue.REAR_WINDSHIELD to "뒷유리",
        WindowValue.ROOF_TOP_1 to "지붕 창 1",
        WindowValue.ROOF_TOP_2 to "지붕 창 2"
    )
}
