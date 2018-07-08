package com.application.seekbar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.Range
import android.view.MotionEvent
import android.view.View

/**
 * Created on 05.07.18.
 */
class BarWithLimit @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : View(context, attrs, defStyleAttr, defStyleRes) {
    private val path = Path()
    private val clipRect: RectF by lazy {
        RectF(xyCharacteristics.visibleRange.lower.toFloat(), 0f,
                xyCharacteristics.visibleRange.upper.toFloat(), viewHeight)
    }
    private val viewHeight: Float
    private val lineStep: Float

    private val backgroundColor: Int
    private val lineColor: Int
    private val selectedColor: Int


    private val abstractCharacteristics by lazy { evaluateCharacteristics() }
    private lateinit var viewRange: Range<Int>

    /**
     * Same as [abstractCharacteristics] but converted to xy coordinate. Is used for better performance
     * and prevent recalculating it each time
     */
    private lateinit var xyCharacteristics: Characteristics

    private var prevX = 0f

    init {
        val styleAttrs = context.obtainStyledAttributes(attrs, R.styleable.BarWithLimit, defStyleAttr, R.style.BarWithLimitStyle)
        lineStep = styleAttrs.getDimension(R.styleable.BarWithLimit_lineStep, 10f)
        viewHeight = styleAttrs.getDimension(R.styleable.BarWithLimit_barHeight, 10f)
        backgroundColor = styleAttrs.getColor(R.styleable.BarWithLimit_progressBackground, Color.BLACK)
        lineColor = styleAttrs.getColor(R.styleable.BarWithLimit_progressDisabled, Color.MAGENTA)
        selectedColor = styleAttrs.getColor(R.styleable.BarWithLimit_progressSelected, Color.CYAN)

        styleAttrs.recycle()
    }

    private val selectedPaint by lazy {
        Paint().apply {
            color = selectedColor
        }
    }

    val linePaint = Paint().apply {
        color = lineColor
        isAntiAlias = true
        strokeWidth = lineStep * 0.3f
        isDither = true
    }

    private fun evaluateCharacteristics(): Characteristics {
        val visibleRange = Range(0, 800)
        val width = 100
        val current = (visibleRange.upper + visibleRange.lower) / 2

        return Characteristics(allowedRange = Range(0, 1000),
                totalRange = Range(0 - width / 2, 1000 + width / 2),
                selectedRange = Range(10, 300),
                current = current, visibleRange = visibleRange)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.x
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val delta = prevX - event.x
                with(abstractCharacteristics) {
                    val deltaTranslated = delta * abstractCharacteristics.visibleRange.width() / xyCharacteristics.visibleRange.width()
                    val visibleRangeUpdated = visibleRange.shift(deltaTranslated.toInt())
                    if (totalRange.contains(visibleRangeUpdated)) {
                        selectedRange = selectedRange.shift(deltaTranslated.toInt())
                        visibleRange = visibleRangeUpdated
                        viewRange = Range(paddingStart, measuredWidth - paddingEnd)
                        xyCharacteristics = convertToXY(this, viewRange)
                        invalidate()
                    }
                }
                prevX = event.x
                return true
            }
        }
        return super.onTouchEvent(event)
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
        return translatedRange.clamp((translatedRange.lower + translatedRange.width() * (x - range.lower) / range.width()))
    }

    private fun drawBackground(canvas: Canvas) {
        with(canvas) {
            save()
            clipPath(path)
            drawColor(backgroundColor)

            // TODO optimize for two separate drawing
            if (xyCharacteristics.totalRange.lower != xyCharacteristics.allowedRange.lower
                    || xyCharacteristics.totalRange.upper != xyCharacteristics.allowedRange.upper) {
                clipRect(xyCharacteristics.totalRange.lower.toFloat(), 0f, xyCharacteristics.totalRange.upper.toFloat(),
                        viewHeight)
                clipRect(xyCharacteristics.allowedRange.lower.toFloat(), 0f, xyCharacteristics.allowedRange.upper.toFloat(),
                        viewHeight, Region.Op.DIFFERENCE)

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
        with(canvas) {
            save()
            clipPath(path)
            drawRect(xyCharacteristics.selectedRange.lower.toFloat(), 0f,
                    xyCharacteristics.selectedRange.upper.toFloat(), viewHeight, selectedPaint)
            restore()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path.rewind()
        path.addRoundRect(clipRect, viewHeight / 2f, viewHeight / 2f, Path.Direction.CCW)

        canvas.translate(0f, paddingTop.toFloat())

        drawBackground(canvas)
        drawSelectedArea(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)
        Log.d("BarWithLimit", "WidthMode: $widthMode. WidthSize: $widthSize. " +
                "HeightMode: $heightMode. HeightSize: $heightSize")

        val evaluatedHeight = viewHeight.toInt() + paddingTop + paddingBottom
        val height =
                when (heightMode) {
                    MeasureSpec.EXACTLY -> heightSize
                    MeasureSpec.AT_MOST -> Math.min(evaluatedHeight, heightSize)
                    MeasureSpec.UNSPECIFIED -> evaluatedHeight
                    else -> {
                        throw  IllegalStateException("WidthMode cannot be found $heightMode")
                    }
                }
        Log.d("BarWithLimit", "Result. Width: $widthSize. Height: $height")

        // There is no reason to change width of the bar
        setMeasuredDimension(widthSize, height)

        viewRange = Range(paddingStart, measuredWidth - paddingEnd)
        xyCharacteristics = convertToXY(abstractCharacteristics, viewRange)

        Log.d("BarWithLimit", "Abstract: $abstractCharacteristics")
        Log.d("BarWithLimit", "Real: $xyCharacteristics")
    }
}