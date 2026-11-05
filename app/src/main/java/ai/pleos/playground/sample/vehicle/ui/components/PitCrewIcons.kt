package ai.pleos.playground.sample.vehicle.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 직접 그린 선 아이콘(두께 2, 24×24 기준). 의존성(material-icons-extended)을 추가하지 않는다.
 * 선 색은 자리일 뿐이고, 실제 색은 Icon(tint = ...)로 정한다. 화면에서는 48dp 이상으로 쓴다.
 */
object PitCrewIcons {

    private val stroke = SolidColor(Color.Black) // tint로 덮어쓰이는 자리 색

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcToRelative(r, r, 0f, true, true, 2 * r, 0f)
        arcToRelative(r, r, 0f, true, true, -2 * r, 0f)
        close()
    }

    private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block
            )
        }.build()

    val Check: ImageVector by lazy {
        icon("Check") {
            moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f)
        }
    }

    /** 정상 */
    val CheckCircle: ImageVector by lazy {
        icon("CheckCircle") {
            circle(12f, 12f, 9.5f)
            moveTo(7.5f, 12.5f); lineTo(10.8f, 15.8f); lineTo(16.5f, 8.8f)
        }
    }

    /** 주의 */
    val Warning: ImageVector by lazy {
        icon("Warning") {
            moveTo(12f, 3.5f); lineTo(22f, 20.5f); lineTo(2f, 20.5f); close()
            moveTo(12f, 10f); lineTo(12f, 14.5f)
            moveTo(12f, 17.4f); lineTo(12f, 17.5f)
        }
    }

    /** 긴급 */
    val ErrorCircle: ImageVector by lazy {
        icon("ErrorCircle") {
            circle(12f, 12f, 9.5f)
            moveTo(12f, 6.8f); lineTo(12f, 13f)
            moveTo(12f, 16.4f); lineTo(12f, 16.5f)
        }
    }

    /** 확인 불가 */
    val HelpCircle: ImageVector by lazy {
        icon("HelpCircle") {
            circle(12f, 12f, 9.5f)
            moveTo(9.2f, 9.6f)
            arcTo(2.9f, 2.9f, 0f, true, true, 12.6f, 12.4f)
            curveTo(12.1f, 12.7f, 12f, 13.1f, 12f, 13.8f)
            moveTo(12f, 16.9f); lineTo(12f, 17f)
        }
    }

    val ChevronLeft: ImageVector by lazy {
        icon("ChevronLeft") {
            moveTo(14.5f, 5f); lineTo(7.5f, 12f); lineTo(14.5f, 19f)
        }
    }

    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight") {
            moveTo(9.5f, 5f); lineTo(16.5f, 12f); lineTo(9.5f, 19f)
        }
    }

    /** 출발 전 점검 */
    val Clipboard: ImageVector by lazy {
        icon("Clipboard") {
            moveTo(9f, 4.5f); lineTo(6.5f, 4.5f)
            arcToRelative(2f, 2f, 0f, false, false, -2f, 2f)
            lineTo(4.5f, 19.5f)
            arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
            lineTo(17.5f, 21.5f)
            arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
            lineTo(19.5f, 6.5f)
            arcToRelative(2f, 2f, 0f, false, false, -2f, -2f)
            lineTo(15f, 4.5f)
            moveTo(9f, 3f); lineTo(15f, 3f); lineTo(15f, 6f); lineTo(9f, 6f); close()
            moveTo(8f, 11f); lineTo(9.2f, 12.2f); lineTo(11f, 10f)
            moveTo(13f, 11.3f); lineTo(16.2f, 11.3f)
            moveTo(8f, 16f); lineTo(9.2f, 17.2f); lineTo(11f, 15f)
            moveTo(13f, 16.3f); lineTo(16.2f, 16.3f)
        }
    }

    /** 설정·정보 */
    val Sliders: ImageVector by lazy {
        icon("Sliders") {
            moveTo(3f, 8f); lineTo(9.5f, 8f)
            circle(12f, 8f, 2.5f)
            moveTo(14.5f, 8f); lineTo(21f, 8f)
            moveTo(3f, 16f); lineTo(7.5f, 16f)
            circle(10f, 16f, 2.5f)
            moveTo(12.5f, 16f); lineTo(21f, 16f)
        }
    }

    /** 소리로 듣기 */
    val SpeakerOn: ImageVector by lazy {
        icon("SpeakerOn") {
            moveTo(3.5f, 9.5f); lineTo(7.5f, 9.5f); lineTo(12f, 5.5f); lineTo(12f, 18.5f)
            lineTo(7.5f, 14.5f); lineTo(3.5f, 14.5f); close()
            moveTo(15.2f, 9f)
            arcToRelative(4f, 4f, 0f, false, true, 0f, 6f)
            moveTo(18f, 6.5f)
            arcToRelative(8f, 8f, 0f, false, true, 0f, 11f)
        }
    }

    /** 소리 꺼짐(비활성) */
    val SpeakerOff: ImageVector by lazy {
        icon("SpeakerOff") {
            moveTo(3.5f, 9.5f); lineTo(7.5f, 9.5f); lineTo(12f, 5.5f); lineTo(12f, 18.5f)
            lineTo(7.5f, 14.5f); lineTo(3.5f, 14.5f); close()
            moveTo(15.5f, 9.5f); lineTo(20.5f, 14.5f)
            moveTo(20.5f, 9.5f); lineTo(15.5f, 14.5f)
        }
    }

    /** 아직 움직이고 있어요 */
    val ArrowsRight: ImageVector by lazy {
        icon("ArrowsRight") {
            moveTo(3f, 9f); lineTo(11f, 9f)
            moveTo(3f, 15f); lineTo(11f, 15f)
            moveTo(3f, 12f); lineTo(19f, 12f)
            moveTo(14f, 6f); lineTo(20f, 12f); lineTo(14f, 18f)
        }
    }

    /** 119 전화 안내 */
    val Phone: ImageVector by lazy {
        icon("Phone") {
            moveTo(6.5f, 3.5f); lineTo(9.5f, 3.5f); lineTo(11f, 7.5f); lineTo(9f, 9f)
            curveTo(10f, 11.2f, 11.8f, 13f, 14f, 14f)
            lineTo(15.5f, 12f); lineTo(19.5f, 13.5f); lineTo(19.5f, 16.5f)
            curveTo(19.5f, 17.6f, 18.6f, 18.5f, 17.5f, 18.5f)
            curveTo(9.5f, 18.5f, 3.5f, 12.5f, 3.5f, 6.5f)
            curveTo(3.5f, 4.6f, 4.9f, 3.5f, 6.5f, 3.5f)
            close()
        }
    }

    /** 사고 장소 */
    val Pin: ImageVector by lazy {
        icon("Pin") {
            moveTo(12f, 21f)
            curveTo(12f, 21f, 5f, 14.8f, 5f, 9.5f)
            arcToRelative(7f, 7f, 0f, false, true, 14f, 0f)
            curveTo(19f, 14.8f, 12f, 21f, 12f, 21f)
            close()
            circle(12f, 9.5f, 2.5f)
        }
    }

    /** 다친 사람 */
    val Person: ImageVector by lazy {
        icon("Person") {
            circle(12f, 8f, 4f)
            moveTo(4.5f, 20.5f)
            curveTo(4.5f, 16.5f, 8f, 14.5f, 12f, 14.5f)
            curveTo(16f, 14.5f, 19.5f, 16.5f, 19.5f, 20.5f)
        }
    }

    /** 긴급 배너 */
    val AlertOctagon: ImageVector by lazy {
        icon("AlertOctagon") {
            moveTo(8.5f, 2.5f); lineTo(15.5f, 2.5f); lineTo(21.5f, 8.5f); lineTo(21.5f, 15.5f)
            lineTo(15.5f, 21.5f); lineTo(8.5f, 21.5f); lineTo(2.5f, 15.5f); lineTo(2.5f, 8.5f)
            close()
            moveTo(12f, 7.5f); lineTo(12f, 13f)
            moveTo(12f, 16.4f); lineTo(12f, 16.5f)
        }
    }

    /** 사진은 앱에 저장되지 않아요 */
    val Camera: ImageVector by lazy {
        icon("Camera") {
            moveTo(3.5f, 8f); lineTo(7f, 8f); lineTo(8.5f, 5.5f); lineTo(15.5f, 5.5f)
            lineTo(17f, 8f); lineTo(20.5f, 8f); lineTo(20.5f, 18.5f); lineTo(3.5f, 18.5f)
            close()
            circle(12f, 13f, 3.5f)
        }
    }

    /** 말로 메모 남기기 */
    val Mic: ImageVector by lazy {
        icon("Mic") {
            moveTo(9f, 6f)
            arcToRelative(3f, 3f, 0f, false, true, 6f, 0f)
            lineTo(15f, 10f)
            arcToRelative(3f, 3f, 0f, false, true, -6f, 0f)
            close()
            moveTo(5.5f, 11f)
            arcToRelative(6.5f, 6.5f, 0f, false, false, 13f, 0f)
            moveTo(12f, 17.5f); lineTo(12f, 21f)
        }
    }

    /** 알아 두세요 */
    val Info: ImageVector by lazy {
        icon("Info") {
            circle(12f, 12f, 9.5f)
            moveTo(12f, 11f); lineTo(12f, 16.5f)
            moveTo(12f, 7.7f); lineTo(12f, 7.8f)
        }
    }

    /** 다시 읽기 */
    val Refresh: ImageVector by lazy {
        icon("Refresh") {
            moveTo(19.5f, 12f)
            arcToRelative(7.5f, 7.5f, 0f, true, true, -2.2f, -5.3f)
            moveTo(17.5f, 3.5f); lineTo(17.5f, 7f); lineTo(14f, 7f)
        }
    }

    /** 문 카드 */
    val DoorCard: ImageVector by lazy {
        icon("DoorCard") {
            moveTo(8f, 3f); lineTo(16f, 3f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
            lineTo(18f, 19f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
            lineTo(8f, 21f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
            lineTo(6f, 5f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
            close()
            moveTo(10f, 12f); lineTo(14f, 12f)
        }
    }

    /** 창문 카드 */
    val WindowCard: ImageVector by lazy {
        icon("WindowCard") {
            moveTo(6f, 4f); lineTo(18f, 4f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
            lineTo(20f, 18f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
            lineTo(6f, 20f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
            lineTo(4f, 6f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
            close()
            moveTo(12f, 4f); lineTo(12f, 20f)
            moveTo(4f, 12f); lineTo(20f, 12f)
        }
    }

    /** 배터리 카드 */
    val Battery: ImageVector by lazy {
        icon("Battery") {
            moveTo(5f, 8f); lineTo(17f, 8f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
            lineTo(19f, 14f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
            lineTo(5f, 16f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
            lineTo(3f, 10f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
            close()
            moveTo(21.5f, 10.5f); lineTo(21.5f, 13.5f)
            moveTo(6.5f, 11f); lineTo(6.5f, 13f)
            moveTo(10f, 11f); lineTo(10f, 13f)
            moveTo(13.5f, 11f); lineTo(13.5f, 13f)
        }
    }

    /** 주행 거리 카드 */
    val Gauge: ImageVector by lazy {
        icon("Gauge") {
            moveTo(4f, 17f)
            arcToRelative(8f, 8f, 0f, false, true, 16f, 0f)
            moveTo(12f, 17f); lineTo(15.5f, 11f)
        }
    }

    /** 방향지시등 카드 */
    val TurnSignal: ImageVector by lazy {
        icon("TurnSignal") {
            moveTo(8f, 6f); lineTo(3f, 12f); lineTo(8f, 18f)
            moveTo(16f, 6f); lineTo(21f, 12f); lineTo(16f, 18f)
        }
    }

    /** 확인 안 함 */
    val MinusCircle: ImageVector by lazy {
        icon("MinusCircle") {
            circle(12f, 12f, 9.5f)
            moveTo(8f, 12f); lineTo(16f, 12f)
        }
    }

    /** 앱 아이콘 자리(상단 바) */
    val Car: ImageVector by lazy {
        icon("Car") {
            moveTo(2.5f, 17.5f); lineTo(2.5f, 13.5f); lineTo(4.8f, 8.5f)
            curveTo(5.2f, 7.6f, 6f, 7f, 7f, 7f); lineTo(17f, 7f)
            curveTo(18f, 7f, 18.8f, 7.6f, 19.2f, 8.5f); lineTo(21.5f, 13.5f)
            lineTo(21.5f, 17.5f); lineTo(18.5f, 17.5f); lineTo(18.5f, 16f)
            lineTo(5.5f, 16f); lineTo(5.5f, 17.5f); close()
            moveTo(6.8f, 12.5f); lineTo(6.9f, 12.5f)
            moveTo(17.1f, 12.5f); lineTo(17.2f, 12.5f)
        }
    }
}
