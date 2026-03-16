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
 * Title with icon at top left, aggregated total at top right.
 * Optional two-line subtitle for session limitation note.
 */
class DailyBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class BarData(val label: String, val value: Float, val valueLabel: String)

    private var bars: List<BarData> = emptyList()
    private var maxValue: Float = 0f
    private var title: String = ""
    private var subtitleLine1: String = ""
    private var subtitleLine2: String = ""
    private var totalLabel: String = ""
    private var iconDrawable: Drawable? = null

    private val bgPaint = Paint().apply { color = Color.BLACK }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB5FFB5.toInt()
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF999999.toInt()
        textSize = 24f
        textAlign = Paint.Align.LEFT
    }
    private val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB5FFB5.toInt()
        textSize = 30f
        textAlign = Paint.Align.RIGHT
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

    fun setSubtitle(line1: String, line2: String = "") {
        subtitleLine1 = line1
        subtitleLine2 = line2
        requestLayout()
        invalidate()
    }

    fun setTotalLabel(text: String) {
        totalLabel = text
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

    private fun getTitleAreaHeight(): Int {
        // Top padding + title + gap
        var h = 16 + 40 + 16
        if (subtitleLine1.isNotEmpty()) {
            h += 16 // gap between title and subtitle
            h += 30 // line 1
            if (subtitleLine2.isNotEmpty()) h += 28 // line 2
            h += 14 // gap after subtitle
        }
        return h
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val titleArea = getTitleAreaHeight()
        val rowHeight = 48
        val bottomPad = 16
        val height = (titleArea + bars.size * rowHeight + bottomPad).coerceAtLeast(120)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val bgRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(bgRect, 16f, 16f, bgPaint)

        if (bars.isEmpty()) return

        val leftMargin = 110f
        val rightMargin = 20f
        val rowHeight = 48f
        val barHeight = 22f
        val titleAreaBottom = getTitleAreaHeight().toFloat()

        // Top padding
        val topPad = 16f

        // Icon + title
        val iconSize = 40
        val iconLeft = 20f
        val titleX = iconLeft + iconSize + 12f
        val titleY = topPad + 34f

        iconDrawable?.let { icon ->
            val iconTop = (topPad + 2).toInt()
            icon.setBounds(iconLeft.toInt(), iconTop, (iconLeft + iconSize).toInt(), iconTop + iconSize)
            icon.draw(canvas)
        }

        if (title.isNotEmpty()) {
            canvas.drawText(title, titleX, titleY, titlePaint)
        }

        // Total aggregated at top right
        if (totalLabel.isNotEmpty()) {
            canvas.drawText(totalLabel, width - 20f, titleY, totalPaint)
        }

        // Subtitle (two lines, with spacing from title)
        var subtitleBottom = titleY
        if (subtitleLine1.isNotEmpty()) {
            subtitleBottom = titleY + 40f
            canvas.drawText(subtitleLine1, iconLeft + 4, subtitleBottom, subtitlePaint)
            if (subtitleLine2.isNotEmpty()) {
                subtitleBottom += 28f
                canvas.drawText(subtitleLine2, iconLeft + 4, subtitleBottom, subtitlePaint)
            }
        }

        // Separator line
        canvas.drawLine(16f, titleAreaBottom - 4, width - 16f, titleAreaBottom - 4, gridPaint)

        if (maxValue == 0f) return

        val maxBarWidth = width - leftMargin - rightMargin - 100f

        for ((index, bar) in bars.withIndex()) {
            val y = titleAreaBottom + 4f + index * rowHeight

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
