package com.datausage.tracker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Simple bar chart view for daily breakdown dialog.
 * Light blue bars on white background with date labels and value labels.
 */
class DailyBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class BarData(val label: String, val value: Float, val valueLabel: String)

    private var bars: List<BarData> = emptyList()
    private var maxValue: Float = 0f

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFADD8E6.toInt() // light blue
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    fun setData(data: List<BarData>) {
        bars = data
        maxValue = data.maxOfOrNull { it.value } ?: 0f
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (bars.size * 100 + 40).coerceAtLeast(200)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bars.isEmpty() || maxValue == 0f) return

        val leftMargin = 120f
        val rightMargin = 40f
        val barHeight = 36f
        val rowHeight = 80f
        val topMargin = 20f
        val maxBarWidth = width - leftMargin - rightMargin

        for ((index, bar) in bars.withIndex()) {
            val y = topMargin + index * rowHeight

            // Date label on the left
            canvas.drawText(bar.label, leftMargin / 2, y + barHeight / 2 + 10, labelPaint)

            // Bar
            val barWidth = if (maxValue > 0) (bar.value / maxValue) * maxBarWidth else 0f
            val rect = RectF(leftMargin, y, leftMargin + barWidth, y + barHeight)
            canvas.drawRoundRect(rect, 6f, 6f, barPaint)

            // Value label to the right of bar
            val valueX = leftMargin + barWidth + 10
            if (valueX + 100 < width) {
                valuePaint.textAlign = Paint.Align.LEFT
                canvas.drawText(bar.valueLabel, valueX, y + barHeight / 2 + 8, valuePaint)
            } else {
                valuePaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(bar.valueLabel, leftMargin + barWidth - 10, y + barHeight / 2 + 8, valuePaint)
            }
        }
    }
}
