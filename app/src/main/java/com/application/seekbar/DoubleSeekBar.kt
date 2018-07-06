package com.application.seekbar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.Range
import android.view.View

/**
 * Created on 05.07.18.
 */
class DoubleSeekBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : View(context, attrs, defStyleAttr, defStyleRes) {
    private val path = Path()
    private val viewHeight = 32f
    private val rect: RectF
    private val lineStep: Float
    private val leftThumb: Thumb? = null
    private val rightThumb: Thumb? = null

    private val backgroundColor: Int = Color.parseColor("#99606060")
    private val lineColor: Int = Color.parseColor("#ffd38b")
    private val selectedColor: Int = Color.parseColor("#38a0b4")
    private val selectedPaint = Paint().apply {
        color = selectedColor
    }

    private val abstractCharacteristics: Characteristics
    /**
     * Same as [abstractCharacteristics] but converted to xy coordinate. Is used for better performance
     * and prevent recalculating it each time
     */
    private val xyCharacteristics: Characteristics

    private val viewRange: Range<Int>

    init {
        val styleAttrs = context.obtainStyledAttributes(attrs, R.styleable.DoubleSeekBar, defStyleAttr, R.style.DoubleSeekBarStyle)
        lineStep = styleAttrs.getDimension(R.styleable.DoubleSeekBar_lineStep, 10f)

        styleAttrs.recycle()
        val width = 60
        val current = 15
        abstractCharacteristics = Characteristics(allowedRange = Range(0, 1000),
                totalRange = Range(0 - width / 2, 1000 + width / 2),
                selectedRange = Range(10, 30),
                current = current, visibleRange = Range(-30, 50))
        viewRange = Range(0, 1080)
        Log.d("DoubleSeekBar", "Abstract: $abstractCharacteristics")
        xyCharacteristics = convertToXY(abstractCharacteristics, viewRange)

        rect = RectF(xyCharacteristics.visibleRange.lower.toFloat(), 0f, xyCharacteristics.visibleRange.upper.toFloat(), viewHeight)
        Log.d("DoubleSeekBar", "Real: $xyCharacteristics")
    }

    val linePaint = Paint().apply {
        color = lineColor
        isAntiAlias = true
        strokeWidth = lineStep * 0.3f
        isDither = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
    private fun convertToXY(characteristics: Characteristics, viewRange: Range<Int>): Characteristics {
        with(characteristics) {
            val allowedRange = Range<Int>(translatePoint(allowedRange.lower, visibleRange, viewRange),
                    translatePoint(allowedRange.upper, visibleRange, viewRange))

            val totalRange = Range<Int>(translatePoint(totalRange.lower, visibleRange, viewRange),
                    translatePoint(totalRange.upper, visibleRange, viewRange))

            val selectedRange = Range<Int>(translatePoint(selectedRange.lower, visibleRange, viewRange),
                    translatePoint(selectedRange.upper, visibleRange, viewRange))

            val current = translatePoint(current, visibleRange, viewRange)

            val range = Range<Int>(translatePoint(visibleRange.lower, visibleRange, viewRange),
                    translatePoint(visibleRange.upper, visibleRange, viewRange))
            return Characteristics(allowedRange, totalRange, selectedRange, current, range)
        }
    }

    private fun translatePoint(x: Int, range: Range<Int>, translatedRange: Range<Int>): Int {
        return translatedRange.clamp((translatedRange.width() * (x - range.lower) / range.width()))
    }

    private fun drawBackground(canvas: Canvas) {
        with(canvas) {
            save()
            clipPath(path)
            drawColor(backgroundColor)

            // TODO optimize for two separate drawing
            if (xyCharacteristics.totalRange.lower != xyCharacteristics.allowedRange.lower
                    || xyCharacteristics.totalRange.upper != xyCharacteristics.allowedRange.upper) {
                clipRect(xyCharacteristics.totalRange.lower.toFloat(), 0f, xyCharacteristics.totalRange.upper.toFloat(), viewHeight)
                clipRect(xyCharacteristics.allowedRange.lower.toFloat(), 0f, xyCharacteristics.allowedRange.upper.toFloat(), viewHeight, Region.Op.DIFFERENCE)
                var iterator = xyCharacteristics.totalRange.lower - viewHeight
                while (iterator <= xyCharacteristics.totalRange.upper + viewHeight) {
                    drawLine(iterator, viewHeight, iterator + viewHeight, 0f, linePaint)
                    iterator += lineStep
                }
            }
            restore()
        }
    }

    private fun drawSelectedArea(canvas: Canvas) {
        canvas.drawRect(xyCharacteristics.selectedRange.lower.toFloat(), 0f, xyCharacteristics.selectedRange.upper.toFloat(), viewHeight, selectedPaint)
    }

    private fun drawThumbs() {

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path.rewind()
        path.addRoundRect(rect, viewHeight / 2f, viewHeight / 2f, Path.Direction.CCW)

        drawBackground(canvas)
        drawSelectedArea(canvas)
        drawThumbs()
    }
}