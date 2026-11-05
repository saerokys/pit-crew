package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import ai.pleos.playground.vehicle.constant.area.DoorValue
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** 문 영역 하나를 그림에서 어떻게 보일지. UNKNOWN은 지원하지 않거나 읽지 못해 닫혔다고 말할 수 없는 영역이다. */
enum class DoorMark { CLOSED, OPEN, UNKNOWN }

/**
 * 차량 윗면 그림(Compose로 직접 그림, 이미지 파일 없음). 위쪽이 차 앞이다.
 * 열린 영역은 주의색으로 벌어진 문/덮개를 그리고, 읽지 못한 영역은 회색 점선으로 그려 닫혔다고 보이지 않게 한다.
 * [marks]에 없는 영역은 UNKNOWN으로 본다.
 */
@Composable
fun CarTopView(marks: Map<DoorValue, DoorMark>, modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    val openColor = colors.statusWarn
    val unknownColor = colors.textDisabled
    Canvas(modifier = modifier) {
        fun mark(area: DoorValue): DoorMark = marks[area] ?: DoorMark.UNKNOWN

        val ch = size.height * 0.74f
        val cw = (ch * 0.38f).coerceAtMost(size.width * 0.6f)
        val left = (size.width - cw) / 2f
        val top = (size.height - ch) / 2f - size.height * 0.03f
        val right = left + cw
        val bottom = top + ch
        val line = 3.dp.toPx()

        // 바퀴 4개
        val wheelW = cw * 0.09f
        val wheelH = ch * 0.16f
        listOf(0.17f, 0.68f).forEach { fraction ->
            listOf(left - wheelW * 0.55f, right - wheelW * 0.45f).forEach { x ->
                drawRoundRect(
                    color = colors.textSecondary,
                    topLeft = Offset(x, top + ch * fraction),
                    size = Size(wheelW, wheelH),
                    cornerRadius = CornerRadius(wheelW / 2f)
                )
            }
        }
        // 차체
        drawRoundRect(
            color = colors.surfaceSelected,
            topLeft = Offset(left, top),
            size = Size(cw, ch),
            cornerRadius = CornerRadius(cw * 0.45f)
        )
        drawRoundRect(
            color = colors.textSecondary,
            topLeft = Offset(left, top),
            size = Size(cw, ch),
            cornerRadius = CornerRadius(cw * 0.45f),
            style = Stroke(width = line)
        )
        // 앞유리, 뒷유리
        val glass = Path().apply {
            moveTo(left + cw * 0.18f, top + ch * 0.13f)
            lineTo(right - cw * 0.18f, top + ch * 0.13f)
            lineTo(right - cw * 0.10f, top + ch * 0.25f)
            lineTo(left + cw * 0.10f, top + ch * 0.25f)
            close()
        }
        drawPath(glass, colors.background)
        val rearGlass = Path().apply {
            moveTo(left + cw * 0.10f, top + ch * 0.80f)
            lineTo(right - cw * 0.10f, top + ch * 0.80f)
            lineTo(right - cw * 0.18f, top + ch * 0.88f)
            lineTo(left + cw * 0.18f, top + ch * 0.88f)
            close()
        }
        drawPath(rearGlass, colors.background)
        // 지붕선
        drawRoundRect(
            color = colors.textSecondary.copy(alpha = 0.6f),
            topLeft = Offset(left + cw * 0.14f, top + ch * 0.28f),
            size = Size(cw * 0.72f, ch * 0.49f),
            cornerRadius = CornerRadius(cw * 0.12f),
            style = Stroke(width = line * 0.6f)
        )

        // 옆 문 6개(Row 1~3, 왼쪽/오른쪽)
        val rows = listOf(
            DoorValue.ROW_1_LEFT to DoorValue.ROW_1_RIGHT,
            DoorValue.ROW_2_LEFT to DoorValue.ROW_2_RIGHT,
            DoorValue.ROW_3_LEFT to DoorValue.ROW_3_RIGHT
        )
        rows.forEachIndexed { index, (leftArea, rightArea) ->
            val y0 = top + ch * (0.29f + 0.19f * index)
            val y1 = y0 + ch * 0.17f
            drawDoor(mark(leftArea), x = left, y0 = y0, y1 = y1, outward = -1f, ch = ch, open = openColor, unknown = unknownColor)
            drawDoor(mark(rightArea), x = right, y0 = y0, y1 = y1, outward = 1f, ch = ch, open = openColor, unknown = unknownColor)
        }
        // 앞쪽 덮개(후드)와 뒤쪽
        drawEnd(mark(DoorValue.HOOD), y = top - 12.dp.toPx(), x0 = left + cw * 0.18f, x1 = right - cw * 0.18f, open = openColor, unknown = unknownColor)
        drawEnd(mark(DoorValue.REAR), y = bottom + 12.dp.toPx(), x0 = left + cw * 0.18f, x1 = right - cw * 0.18f, open = openColor, unknown = unknownColor)
    }
}

private fun DrawScope.drawDoor(
    mark: DoorMark, x: Float, y0: Float, y1: Float, outward: Float, ch: Float,
    open: Color, unknown: Color
) {
    when (mark) {
        DoorMark.CLOSED -> Unit
        DoorMark.OPEN -> {
            drawLine(open, Offset(x, y0), Offset(x, y1), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
            drawLine(
                open,
                Offset(x, y0),
                Offset(x + outward * ch * 0.16f, y0 + ch * 0.10f),
                strokeWidth = 8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        DoorMark.UNKNOWN -> drawLine(
            unknown, Offset(x, y0), Offset(x, y1),
            strokeWidth = 4.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))
        )
    }
}

private fun DrawScope.drawEnd(mark: DoorMark, y: Float, x0: Float, x1: Float, open: Color, unknown: Color) {
    when (mark) {
        DoorMark.CLOSED -> Unit
        DoorMark.OPEN -> drawLine(open, Offset(x0, y), Offset(x1, y), strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
        DoorMark.UNKNOWN -> drawLine(
            unknown, Offset(x0, y), Offset(x1, y),
            strokeWidth = 4.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))
        )
    }
}
