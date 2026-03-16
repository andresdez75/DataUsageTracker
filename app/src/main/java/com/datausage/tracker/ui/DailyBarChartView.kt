package com.datausage.tracker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Simple horizontal bar chart for daily breakdown.
 * Black background, green (#B5FFB5) bars, white labels.
 */
class DailyBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class BarData(val label: String, val value: Float, val valueLabel: String)

    private var bars: List<BarData> = emptyList()
    private var maxValue: Float = 0f

    private val bgPaint = Paint().apply {
        color = Color.BLACK
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB5FFB5.toInt() // light green
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 26f
        textAlign = Paint.Align.RIGHT
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB5FFB5.toInt()
        textSize = 22f
        textAlign = Paint.Align.LEFT
    }
    private val gridPaint = Paint().apply {
        color = 0xFF333333.toInt()
        strokeWidth = 1f
    }

    fun setData(data: List<BarData>) {
        bars = data
        maxValue = data.maxOfOrNull { it.value } ?: 0f
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val rowHeight = 56
        val padding = 32
        val height = (bars.size * rowHeight + padding).coerceAtLeast(100)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Black background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        if (bars.isEmpty() || maxValue == 0f) return

        val leftMargin = 100f
        val rightMargin = 16f
        val barHeight = 24f
        val rowHeight = 56f
        val topMargin = 16f
        val maxBarWidth = width - leftMargin - rightMargin

        for ((index, bar) in bars.withIndex()) {
            val y = topMargin + index * rowHeight

            // Subtle grid line
            canvas.drawLine(leftMargin, y + rowHeight - 4, width - rightMargin, y + rowHeight - 4, gridPaint)

            // Date label on the left
            canvas.drawText(bar.label, leftMargin - 12, y + barHeight / 2 + 9, labelPaint)

            // Bar with rounded corners
            val barWidth = if (maxValue > 0) (bar.value / maxValue) * (maxBarWidth - 120f) else 0f
            if (barWidth > 0) {
                val rect = RectF(leftMargin, y + 4, leftMargin + barWidth, y + 4 + barHeight)
                canvas.drawRoundRect(rect, 4f, 4f, barPaint)
            }

            // Value label to the right of bar
            val valueX = leftMargin + barWidth + 8
            canvas.drawText(bar.valueLabel, valueX, y + barHeight / 2 + 8, valuePaint)
        }
    }
}
