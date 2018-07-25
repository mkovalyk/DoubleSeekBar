package com.application.seekbar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.properties.Delegates

/**
 * View which draws background for [DoubleSeekBarLayout]
 *
 * Created on 05.07.18.
 */
class BarWithLimit @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : View(context, attrs, defStyleAttr, defStyleRes) {
    private val path = Path()
    private var clipRect = RectF()
    private val viewHeight: Float
    private val lineStep: Float

    private val backgroundColor: Int
    private val lineColor: Int
    private val selectedColor: Int

    val viewRange = Range.EMPTY
    val pattern: Bitmap

    init {
        val theme = context.theme
        val styleAttrs = theme.obtainStyledAttributes(attrs, R.styleable.BarWithLimit, defStyleAttr, R.style.DefaultBarWithLimitStyle)
        lineStep = styleAttrs.getDimension(R.styleable.BarWithLimit_lineStep, 10f)
        viewHeight = styleAttrs.getDimension(R.styleable.BarWithLimit_barHeight, 10f)
        backgroundColor = styleAttrs.getColor(R.styleable.BarWithLimit_progressBackground, Color.BLACK)
        lineColor = styleAttrs.getColor(R.styleable.BarWithLimit_progressDisabled, Color.MAGENTA)
        selectedColor = styleAttrs.getColor(R.styleable.BarWithLimit_progressSelected, Color.CYAN)

        pattern = BitmapFactory.decodeResource(resources, R.drawable.scale_pattern)

        styleAttrs.recycle()
    }

    /**
     * Ranges of views
     */
    var abstractConstraints: Constraints by Delegates.observable(Constraints.EMPTY) { _, _, newValue ->
        updateTranslatedConstraints(newValue)
    }

    private fun updateTranslatedConstraints(newValue: Constraints) {
        val offset = -newValue.visibleRange.lower
        with(newValue) {
            translatedConstraints = Constraints(allowedRange.shiftImmutable(offset),
                    totalRange.shiftImmutable(offset),
                    selectedRange.shiftImmutable(offset), current + offset, visibleRange.shiftImmutable(offset),
                    minRange, tolerate, multiplier)
            Log.d("BarWithLimit", "TranslatedConstraints: $translatedConstraints" +
                    "\n -----------------------------------------------")
        }
    }

    var translatedConstraints = Constraints.EMPTY

    private val selectedPaint by lazy {
        Paint().apply {
            color = selectedColor
        }
    }

    private val backgroundPaint by lazy {
        Paint().apply {
            shader = BitmapShader(pattern, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }
    }

    private val linePaint = Paint().apply {
        color = lineColor
        isAntiAlias = true
        strokeWidth = lineStep * 0.3f // randomly selected value
        isDither = true
    }

    private fun drawBackground(canvas: Canvas) {
        with(canvas) {
            save()
            clipPath(path)
            drawColor(backgroundColor)

            with(translatedConstraints) {
                val bitmapVerticalOffset = (viewHeight - pattern.height) / 2
                val horizontalOffset = abstractConstraints.visibleRange.lower % pattern.width.toFloat()
                val backgroundRange = allowedRange.apply { clamp(viewRange) }

                save()
                clipRect(backgroundRange.lower.toFloat(), 0f, backgroundRange.upper.toFloat(), viewHeight)
                translate(-horizontalOffset, bitmapVerticalOffset)
                drawRect(viewRange.lower.toFloat(), 0f,
                        viewRange.upper.toFloat() - if (horizontalOffset > 0) -horizontalOffset else horizontalOffset,
                        pattern.height.toFloat(), backgroundPaint)
                restore()

                val startDisabled = Range(totalRange.lower, allowedRange.lower).clamp(viewRange)
                val endDisabled = Range(allowedRange.upper, totalRange.upper).clamp(viewRange)
//                Log.d("Draw", "drawBackground: Start: $startDisabled. End: $endDisabled")
                drawDisabledArea(startDisabled)
                drawDisabledArea(endDisabled)
            }
            restore()
        }
    }

    private fun Canvas.drawDisabledArea(range: Range) {
        if (!range.isEmpty()) {
            clipRect(range.lower.toFloat(), 0f, range.upper.toFloat(), viewHeight)
            var iterator = range.lower - viewHeight
            while (iterator <= range.upper + viewHeight) {
                drawLine(iterator, viewHeight, iterator + viewHeight, 0f, linePaint)
                iterator += lineStep
            }
        }
    }

    private fun drawSelectedArea(canvas: Canvas) {
        with(canvas) {
            save()
            clipPath(path)
            with(translatedConstraints) {
                val left = viewRange.clamp(selectedRange.lower).toFloat()
                val right = viewRange.clamp(selectedRange.upper.toFloat())
                drawRect(left, 0f, right, viewHeight, selectedPaint)
                Log.d("Draw", "drawSelected: Left: $left. Right: $right")
            }
            restore()
        }
    }

    private var prevX = 0f
    private val threshold = 5f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.x
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val delta = (prevX - event.x).toInt()
                // don't spam too often
                if (Math.abs(delta) > threshold) {
                    with(abstractConstraints) {
                        val visibleRangeUpdated = visibleRange.shiftImmutable(delta)
                        val selectedRangeUpdated = selectedRange.shiftImmutable(delta)
                        if (totalRange.contains(visibleRangeUpdated) && allowedRange.contains(selectedRangeUpdated)) {
                            visibleRange.shift(delta)
                            selectedRange.shift(delta)
                            current += delta
//                            translatedConstraints.visibleRange.shift(delta)
//                            translatedConstraints.selectedRange.shift(delta)
                            Log.d("BarWithLimit", "onTouchEvent: delta $delta. Total range: $abstractConstraints." +
                                    "Visible: $visibleRangeUpdated + current: $current")

                            invalidate()
                        }
                    }
                    prevX = event.x
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)

        val evaluatedHeight = viewHeight.toInt() + paddingTop + paddingBottom
        val height =
                when (heightMode) {
                    MeasureSpec.EXACTLY -> heightSize
                    MeasureSpec.AT_MOST -> Math.min(evaluatedHeight, heightSize)
                    MeasureSpec.UNSPECIFIED -> evaluatedHeight
                    else -> {
                        throw  IllegalStateException("HeightMode cannot be found $heightMode")
                    }
                }

        // There is no reason to change width of the bar. Use default
        setMeasuredDimension(widthSize, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // only after view is laid out margins are available
        val marginStart = x.toInt()
        viewRange.set(marginStart + paddingStart, marginStart + measuredWidth - paddingEnd)
        clipRect.set(viewRange.lower.toFloat(), 0f, viewRange.upper.toFloat(), viewHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateTranslatedConstraints(abstractConstraints)
        path.rewind()
        path.addRoundRect(clipRect, viewHeight / 2f, viewHeight / 2f, Path.Direction.CCW)

        //Translate back by margin from start to draw in correct position.
        // It is used that way because to evaluate view range we need to know position related to it's
        // parent which includes margin from left and right.
        canvas.translate(-x, paddingTop.toFloat())

        drawBackground(canvas)
        drawSelectedArea(canvas)
    }
}