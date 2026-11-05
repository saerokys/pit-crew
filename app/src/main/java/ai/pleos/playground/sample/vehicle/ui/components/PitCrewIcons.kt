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
