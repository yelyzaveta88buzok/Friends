package pl.edu.friends.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import pl.edu.friends.util.RoomTheme
import kotlin.math.abs
import kotlin.math.roundToInt

data class DayWarmthPoint(
    val dayLabel: String,
    val myScore: Float,
    val friendScore: Float
)

class WarmthWaveChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var points: List<DayWarmthPoint> = emptyList()
    private var animatedMyScores: FloatArray = FloatArray(0)
    private var selectedIndex: Int = 0
    private var scoreAnimator: ValueAnimator? = null

    private var primaryColor = "#4F46E5".toColorInt()
    private var primarySoftColor = "#818CF8".toColorInt()
    private var secondaryColor = "#94A3B8".toColorInt()
    private var areaTopColor = "#554F46E5".toColorInt()
    private var areaBottomColor = "#044F46E5".toColorInt()
    private var secondaryAreaTopColor = "#2494A3B8".toColorInt()
    private var secondaryAreaBottomColor = "#0094A3B8".toColorInt()
    private var labelColor = "#6B7280".toColorInt()

    var compactMode: Boolean = false
    var onDaySelected: ((Int, DayWarmthPoint) -> Unit)? = null

    private val bgRect = RectF()
    private val highlightRect = RectF()
    private val dayPillRect = RectF()

    private val myLinePath = Path()
    private val myAreaPath = Path()
    private val friendLinePath = Path()
    private val friendAreaPath = Path()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#F8FAFC".toColorInt()
        style = Paint.Style.FILL
    }

    private val bgStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#E2E8F0".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#E2E8F0".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        pathEffect = DashPathEffect(floatArrayOf(dp(5f), dp(5f)), 0f)
    }

    private val highlightPillarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#164F46E5".toColorInt()
        style = Paint.Style.FILL
    }

    private val yEmojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = sp(13f)
    }

    private val friendAreaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val friendLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#94A3B8".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(2.2f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = DashPathEffect(floatArrayOf(dp(6f), dp(4f)), 0f)
    }

    private val friendDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#94A3B8".toColorInt()
        style = Paint.Style.FILL
    }

    private val myAreaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val myLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#4F46E5".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(3.2f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#334F46E5".toColorInt()
        style = Paint.Style.FILL
    }

    private val nodeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FFFFFF".toColorInt()
        style = Paint.Style.FILL
    }

    private val nodeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val nodeEmojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val dayPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#4F46E5".toColorInt()
        style = Paint.Style.FILL
    }

    private val dayLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = sp(11f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    fun setTheme(theme: RoomTheme) {
        primaryColor = theme.primaryColor
        primarySoftColor = ColorUtils.setAlphaComponent(theme.primaryColor, 185)
        secondaryColor = theme.secondaryColor
        areaTopColor = ColorUtils.setAlphaComponent(theme.primaryColor, 85)
        areaBottomColor = ColorUtils.setAlphaComponent(theme.primaryColor, 5)
        secondaryAreaTopColor = ColorUtils.setAlphaComponent(theme.secondaryColor, 36)
        secondaryAreaBottomColor = ColorUtils.setAlphaComponent(theme.secondaryColor, 0)
        labelColor = "#6B7280".toColorInt()

        bgPaint.color = theme.waveBgColor
        bgStrokePaint.color = theme.waveGridColor
        gridPaint.color = theme.waveGridColor
        highlightPillarPaint.color = ColorUtils.setAlphaComponent(theme.primaryColor, 20)
        haloPaint.color = ColorUtils.setAlphaComponent(theme.primaryColor, 48)
        myLinePaint.color = theme.primaryColor
        friendLinePaint.color = theme.secondaryColor
        friendDotPaint.color = theme.secondaryColor
        dayPillPaint.color = theme.primaryColor
        invalidate()
    }

    fun setData(newPoints: List<DayWarmthPoint>, animate: Boolean = true) {
        val hadSameSize = (points.size == newPoints.size && points.isNotEmpty())
        val startScores = if (hadSameSize && animatedMyScores.size == newPoints.size) {
            animatedMyScores.copyOf()
        } else {
            FloatArray(newPoints.size) { i ->
                newPoints.getOrNull(i)?.myScore ?: 3f
            }
        }

        points = newPoints
        if (newPoints.isEmpty()) {
            animatedMyScores = FloatArray(0)
            selectedIndex = 0
            invalidate()
            return
        }

        selectedIndex = if (!hadSameSize) {
            newPoints.lastIndex
        } else {
            selectedIndex.coerceIn(0, newPoints.lastIndex)
        }

        scoreAnimator?.cancel()
        if (animate && hadSameSize) {
            val targetScores = FloatArray(newPoints.size) { i -> newPoints[i].myScore }
            animatedMyScores = startScores.copyOf()
            scoreAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 400L
                interpolator = DecelerateInterpolator()
                addUpdateListener { anim ->
                    val frac = anim.animatedValue as Float
                    for (i in targetScores.indices) {
                        animatedMyScores[i] = startScores[i] + (targetScores[i] - startScores[i]) * frac
                    }
                    invalidate()
                }
                start()
            }
        } else {
            animatedMyScores = FloatArray(newPoints.size) { i -> newPoints[i].myScore }
            invalidate()
        }

        onDaySelected?.invoke(selectedIndex, newPoints[selectedIndex])
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val w = width.toFloat()
        val h = height.toFloat()

        if (compactMode) {
            if (points.isEmpty()) return
            val plotLeft = dp(4f)
            val plotRight = w - dp(6f)
            val plotTop = dp(6f)
            val plotBottom = h - dp(4f)
            if (plotRight <= plotLeft || plotBottom <= plotTop) return

            val count = points.size
            val myX = FloatArray(count) { i ->
                if (count == 1) (plotLeft + plotRight) / 2f
                else plotLeft + (i.toFloat() / (count - 1).toFloat()) * (plotRight - plotLeft)
            }
            val myY = FloatArray(count) { i ->
                val s = animatedMyScores.getOrNull(i) ?: points[i].myScore
                scoreToY(s, plotTop, plotBottom)
            }
            buildCubicWavePaths(myX, myY, plotBottom, myLinePath, myAreaPath)

            myAreaPaint.shader = LinearGradient(
                0f, plotTop, 0f, plotBottom,
                areaTopColor,
                areaBottomColor,
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(myAreaPath, myAreaPaint)

            val prevWidth = myLinePaint.strokeWidth
            myLinePaint.strokeWidth = dp(2.4f)
            canvas.drawPath(myLinePath, myLinePaint)
            myLinePaint.strokeWidth = prevWidth

            val lastX = myX.last()
            val lastY = myY.last()
            canvas.drawCircle(lastX, lastY, dp(4f), nodeFillPaint)
            nodeBorderPaint.color = primaryColor
            nodeBorderPaint.strokeWidth = dp(2f)
            canvas.drawCircle(lastX, lastY, dp(4f), nodeBorderPaint)
            return
        }

        bgRect.set(dp(1f), dp(1f), w - dp(1f), h - dp(1f))
        canvas.drawRoundRect(bgRect, dp(18f), dp(18f), bgPaint)
        canvas.drawRoundRect(bgRect, dp(18f), dp(18f), bgStrokePaint)

        val plotLeft = dp(38f)
        val plotRight = w - dp(18f)
        val plotTop = dp(24f)
        val plotBottom = h - dp(36f)
        if (plotRight <= plotLeft || plotBottom <= plotTop) return

        for (score in 1..5) {
            val y = scoreToY(score.toFloat(), plotTop, plotBottom)
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint)
            drawCenteredText(canvas, emojiForScore(score), dp(18f), y, yEmojiPaint)
        }

        if (points.isEmpty()) return

        val count = points.size
        fun indexToX(i: Int): Float {
            if (count == 1) return (plotLeft + plotRight) / 2f
            return plotLeft + (i.toFloat() / (count - 1).toFloat()) * (plotRight - plotLeft)
        }

        val selIdx = selectedIndex.coerceIn(0, count - 1)
        val selX = indexToX(selIdx)
        highlightRect.set(
            selX - dp(17f),
            plotTop - dp(10f),
            selX + dp(17f),
            plotBottom + dp(28f)
        )
        canvas.drawRoundRect(highlightRect, dp(14f), dp(14f), highlightPillarPaint)

        val friendX = FloatArray(count) { i -> indexToX(i) }
        val friendY = FloatArray(count) { i -> scoreToY(points[i].friendScore, plotTop, plotBottom) }
        buildCubicWavePaths(friendX, friendY, plotBottom, friendLinePath, friendAreaPath)

        friendAreaPaint.shader = LinearGradient(
            0f, plotTop, 0f, plotBottom,
            secondaryAreaTopColor,
            secondaryAreaBottomColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(friendAreaPath, friendAreaPaint)
        canvas.drawPath(friendLinePath, friendLinePaint)
        for (i in 0 until count) {
            canvas.drawCircle(friendX[i], friendY[i], dp(3.5f), friendDotPaint)
        }

        val myX = FloatArray(count) { i -> indexToX(i) }
        val myY = FloatArray(count) { i ->
            val s = animatedMyScores.getOrNull(i) ?: points[i].myScore
            scoreToY(s, plotTop, plotBottom)
        }
        buildCubicWavePaths(myX, myY, plotBottom, myLinePath, myAreaPath)

        myAreaPaint.shader = LinearGradient(
            0f, plotTop, 0f, plotBottom,
            areaTopColor,
            areaBottomColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(myAreaPath, myAreaPaint)
        canvas.drawPath(myLinePath, myLinePaint)

        for (i in 0 until count) {
            val isSel = (i == selIdx)
            val cx = myX[i]
            val cy = myY[i]
            val scoreInt = points[i].myScore.roundToInt().coerceIn(1, 5)
            val emoji = emojiForScore(scoreInt)

            if (isSel) {
                canvas.drawCircle(cx, cy, dp(16f), haloPaint)
                canvas.drawCircle(cx, cy, dp(12.5f), nodeFillPaint)
                nodeBorderPaint.color = primaryColor
                nodeBorderPaint.strokeWidth = dp(2.5f)
                canvas.drawCircle(cx, cy, dp(12.5f), nodeBorderPaint)
                nodeEmojiPaint.textSize = sp(12f)
            } else {
                canvas.drawCircle(cx, cy, dp(9.5f), nodeFillPaint)
                nodeBorderPaint.color = primarySoftColor
                nodeBorderPaint.strokeWidth = dp(1.8f)
                canvas.drawCircle(cx, cy, dp(9.5f), nodeBorderPaint)
                nodeEmojiPaint.textSize = sp(9.5f)
            }
            drawCenteredText(canvas, emoji, cx, cy, nodeEmojiPaint)
        }

        val labelY = h - dp(14f)
        for (i in 0 until count) {
            val cx = myX[i]
            val isSel = (i == selIdx)
            if (isSel) {
                dayPillRect.set(
                    cx - dp(19f),
                    labelY - dp(10f),
                    cx + dp(19f),
                    labelY + dp(10f)
                )
                canvas.drawRoundRect(dayPillRect, dp(10f), dp(10f), dayPillPaint)
                dayLabelPaint.color = "#FFFFFF".toColorInt()
            } else {
                dayLabelPaint.color = labelColor
            }
            drawCenteredText(canvas, points[i].dayLabel, cx, labelY, dayLabelPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (compactMode || points.isEmpty()) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val plotLeft = dp(38f)
                val plotRight = width.toFloat() - dp(18f)
                val count = points.size
                var bestIdx = 0
                var bestDist = Float.MAX_VALUE
                for (i in 0 until count) {
                    val x = if (count == 1) {
                        (plotLeft + plotRight) / 2f
                    } else {
                        plotLeft + (i.toFloat() / (count - 1).toFloat()) * (plotRight - plotLeft)
                    }
                    val d = abs(event.x - x)
                    if (d < bestDist) {
                        bestDist = d
                        bestIdx = i
                    }
                }
                if (bestIdx != selectedIndex) {
                    selectedIndex = bestIdx
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    invalidate()
                }
                onDaySelected?.invoke(selectedIndex, points[selectedIndex])
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun buildCubicWavePaths(
        xs: FloatArray,
        ys: FloatArray,
        baselineY: Float,
        linePath: Path,
        areaPath: Path
    ) {
        linePath.reset()
        areaPath.reset()
        if (xs.isEmpty()) return

        linePath.moveTo(xs[0], ys[0])
        areaPath.moveTo(xs[0], baselineY)
        areaPath.lineTo(xs[0], ys[0])

        for (i in 0 until xs.size - 1) {
            val midX = (xs[i] + xs[i + 1]) / 2f
            linePath.cubicTo(midX, ys[i], midX, ys[i + 1], xs[i + 1], ys[i + 1])
            areaPath.cubicTo(midX, ys[i], midX, ys[i + 1], xs[i + 1], ys[i + 1])
        }

        areaPath.lineTo(xs.last(), baselineY)
        areaPath.close()
    }

    private fun scoreToY(score: Float, plotTop: Float, plotBottom: Float): Float {
        val clamped = score.coerceIn(1f, 5f)
        val fraction = (clamped - 1f) / 4f
        return plotBottom - fraction * (plotBottom - plotTop)
    }

    private fun emojiForScore(score: Int): String = when (score) {
        5 -> "😍"
        4 -> "😊"
        3 -> "😐"
        2 -> "😔"
        else -> "😢"
    }

    private fun drawCenteredText(canvas: Canvas, text: String, cx: Float, cy: Float, paint: Paint) {
        val fm = paint.fontMetrics
        val baseline = cy - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, cx, baseline, paint)
    }

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
}
