package com.datausage.tracker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Vertical bar chart for daily breakdown.
 * Black background, green (#B5FFB5) vertical bars.
 * Title (filter name) at top in white, value above each bar, date below.
 */
class DailyBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class BarData(val label: String, val value: Float, val valueLabel: String)

    private var bars: List<BarData> = emptyList()
    private var maxValue: Float = 0f
    private var title: String = ""

    private val bgPaint = Paint().apply {
        color = Color.BLACK
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB5FFB5.toInt()
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }
    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFAAAAAA.toInt()
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }

    fun setTitle(text: String) {
        title = text
        invalidate()
    }

    fun setData(data: List<BarData>) {
        bars = data
        maxValue = data.maxOfOrNull { it.value } ?: 0f
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = 280
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Black background with rounded corners
        val bgRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        bgPaint.color = Color.BLACK
        canvas.drawRoundRect(bgRect, 16f, 16f, bgPaint)

        if (bars.isEmpty()) return

        val topPadding = 50f
        val bottomPadding = 40f
        val sidePadding = 24f
        val chartTop = topPadding + 30f
        val chartBottom = height - bottomPadding
        val maxBarHeight = chartBottom - chartTop

        // Title
        if (title.isNotEmpty()) {
            canvas.drawText(title, width / 2f, 36f, titlePaint)
        }

        if (maxValue == 0f) return

        val barCount = bars.size
        val totalWidth = width - 2 * sidePadding
        val barSlotWidth = totalWidth / barCount
        val barWidth = (barSlotWidth * 0.6f).coerceAtMost(40f)

        for ((index, bar) in bars.withIndex()) {
            val centerX = sidePadding + barSlotWidth * index + barSlotWidth / 2

            // Bar height proportional to value
            val barHeight = (bar.value / maxValue) * maxBarHeight
            val barTop = chartBottom - barHeight
            val barLeft = centerX - barWidth / 2
            val barRight = centerX + barWidth / 2

            if (barHeight > 0) {
                val rect = RectF(barLeft, barTop, barRight, chartBottom)
                canvas.drawRoundRect(rect, 4f, 4f, barPaint)
            }

            // Value above the bar
            canvas.drawText(bar.valueLabel, centerX, barTop - 8, valuePaint)

            // Date below the bar
            canvas.drawText(bar.label, centerX, height - 12f, datePaint)
        }
    }
}
