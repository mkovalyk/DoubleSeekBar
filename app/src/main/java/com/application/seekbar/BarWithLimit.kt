package com.application.seekbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
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
    private val pattern: Bitmap

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

    var currentListener: ((value: Int) -> Unit)? = null

    var translatedConstraints = Constraints.EMPTY

    private fun updateTranslatedConstraints(newValue: Constraints) {
        val offset = -newValue.visibleRange.lower
        with(newValue) {
            translatedConstraints.let {
                it.selectedRange.set(selectedRange.shiftImmutable(offset)).shift(viewRange.lower)
                it.allowedRange.set(allowedRange.shiftImmutable(offset)).shift(viewRange.lower)
                it.totalRange.set(totalRange.shiftImmutable(offset)).shift(viewRange.lower)
                it.visibleRange.set(visibleRange.shiftImmutable(offset)).shift(viewRange.lower)
                it.current = current + offset + viewRange.lower
                it.minRange = minRange
            }
        }
    }


    private val selectedPaint by lazy {
        Paint().apply {
            color = selectedColor
        }
    }

    private val backgroundPaint by lazy {
        Paint().apply {
            shader = BitmapShader(pattern, Shader.TileMode.REPEAT, Shader.TileMode.MIRROR)
        }
    }

    private val linePaint = Paint().apply {
        color = lineColor
        isAntiAlias = true
        strokeWidth = lineStep * 0.3f // randomly selected value
        isDither = true
    }

    fun updateSelectedRange(range: Range) {
        translatedConstraints.selectedRange.set(range)
        abstractConstraints.selectedRange.set(range.shiftImmutable(abstractConstraints.visibleRange.lower)
                .shift(-viewRange.lower))
    }

    private fun drawBackground(canvas: Canvas) {
        with(canvas) {
            save()
            clipPath(path)
            drawColor(backgroundColor)

            with(translatedConstraints) {
                val startDisabled = Range(totalRange.lower, allowedRange.lower).clamp(viewRange)
                val endDisabled = Range(allowedRange.upper, totalRange.upper).clamp(viewRange)
                drawDisabledArea(startDisabled)
                drawDisabledArea(endDisabled)
            }
            restore()
        }
    }

    private fun drawTimeDividers(constraints: Constraints, canvas: Canvas) {
        val bitmapVerticalOffset = (viewHeight - pattern.height) / 2
        val horizontalOffset = (abstractConstraints.visibleRange.lower) % pattern.width - viewRange.lower
        val backgroundRange = constraints.allowedRange.clampImmutable(constraints.visibleRange).shift(-viewRange.lower)

        with(canvas) {
            save()
            clipPath(path)
            clipRect(constraints.allowedRange.lower.toFloat(), 0f, constraints.allowedRange.upper.toFloat(), viewHeight)
            translate(-horizontalOffset.toFloat(), bitmapVerticalOffset)
            val rect = RectF(backgroundRange.lower.toFloat() - pattern.width, 0f, backgroundRange.upper.toFloat() + pattern.width, pattern.height.toFloat())
            drawRect(rect, backgroundPaint)
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
            }
            restore()
        }
    }

    private var prevX = 0f
    private val threshold = 3f

    @SuppressLint("ClickableViewAccessibility")
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
                            currentListener?.invoke(current)

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
        clipRect.set(translatedConstraints.visibleRange.lower.toFloat(), 0f,
                translatedConstraints.visibleRange.upper.toFloat(), viewHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateTranslatedConstraints(abstractConstraints)
        path.rewind()
        path.addRoundRect(clipRect, viewHeight / 2f, viewHeight / 2f, Path.Direction.CCW)

        canvas.save()
        //Translate back by margin from start to draw in correct position.
        // It is used that way because to evaluate view range we need to know position related to it's
        // parent which includes margin from left and right.
        canvas.translate(-x, paddingTop.toFloat())

        drawBackground(canvas)
        drawSelectedArea(canvas)

        drawTimeDividers(translatedConstraints, canvas)
        canvas.restore()
    }
}