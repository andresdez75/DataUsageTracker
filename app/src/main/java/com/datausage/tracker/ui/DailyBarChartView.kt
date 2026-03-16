package com.datausage.tracker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

/**
 * Horizontal bar chart for daily breakdown.
 * Black background, green (#B5FFB5) horizontal bars.
 * Title with icon at top, date labels on left, values on right.
 */
class DailyBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class BarData(val label: String, val value: Float, val valueLabel: String)

    private var bars: List<BarData> = emptyList()
    private var maxValue: Float = 0f
    private var title: String = ""
    private var iconDrawable: Drawable? = null

    private val bgPaint = Paint().apply { color = Color.BLACK }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB5FFB5.toInt()
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
    }
    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFAAAAAA.toInt()
        textSize = 24f
        textAlign = Paint.Align.RIGHT
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.LEFT
    }
    private val gridPaint = Paint().apply {
        color = 0xFF2A2A2A.toInt()
        strokeWidth = 1f
    }

    fun setTitle(text: String) {
        title = text
        invalidate()
    }

    fun setIcon(resId: Int) {
        iconDrawable = ContextCompat.getDrawable(context, resId)?.mutate()
        iconDrawable?.setTint(Color.WHITE)
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
        val titleArea = 60
        val rowHeight = 48
        val bottomPad = 16
        val height = (titleArea + bars.size * rowHeight + bottomPad).coerceAtLeast(120)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Black rounded background
        val bgRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(bgRect, 16f, 16f, bgPaint)

        if (bars.isEmpty()) return

        val leftMargin = 110f
        val rightMargin = 20f
        val rowHeight = 48f
        val barHeight = 22f
        val titleAreaBottom = 55f

        // Icon + title
        val iconSize = 36
        val iconLeft = 20f
        val titleX = iconLeft + iconSize + 10f

        iconDrawable?.let { icon ->
            val iconTop = ((titleAreaBottom - iconSize) / 2).toInt()
            icon.setBounds(iconLeft.toInt(), iconTop, (iconLeft + iconSize).toInt(), iconTop + iconSize)
            icon.draw(canvas)
        }

        if (title.isNotEmpty()) {
            canvas.drawText(title, titleX, 36f, titlePaint)
        }

        // Separator line
        canvas.drawLine(16f, titleAreaBottom, width - 16f, titleAreaBottom, gridPaint)

        if (maxValue == 0f) return

        val maxBarWidth = width - leftMargin - rightMargin - 100f

        for ((index, bar) in bars.withIndex()) {
            val y = titleAreaBottom + 8f + index * rowHeight

            // Grid line
            canvas.drawLine(leftMargin, y + rowHeight - 6, width - rightMargin, y + rowHeight - 6, gridPaint)

            // Date label
            canvas.drawText(bar.label, leftMargin - 14f, y + barHeight / 2 + 8, datePaint)

            // Bar
            val barWidth = (bar.value / maxValue) * maxBarWidth
            if (barWidth > 0) {
                val rect = RectF(leftMargin, y + 4, leftMargin + barWidth, y + 4 + barHeight)
                canvas.drawRoundRect(rect, 4f, 4f, barPaint)
            }

            // Value to the right
            canvas.drawText(bar.valueLabel, leftMargin + barWidth + 10, y + barHeight / 2 + 7, valuePaint)
        }
    }
}
