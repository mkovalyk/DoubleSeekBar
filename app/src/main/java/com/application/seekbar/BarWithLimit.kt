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
    private val pattern: Bitmap

    val viewRange = Range.EMPTY

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

    private val selectedPaint by lazy {
        Paint().apply {
            color = selectedColor
        }
    }

    private val backgroundTimeMarkPaint by lazy {
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

    private var prevX = 0f
    private var index = INVALID_ID

    /**
     * It is absolute values of constraints.. Doesn't change after visible range is changed.
     */
    var absoluteConstraints: Constraints by Delegates.observable(Constraints.EMPTY) { _, _, newValue ->
        updateTranslatedConstraints(newValue)
    }

    /**
     * Constraints that is using for drawing. It is translated to visible range on every change of
     * the visible area.
     */
    var relativeConstraints = Constraints.EMPTY

    var currentListener: ((value: Int) -> Unit)? = null

    fun updateSelectedRange(range: Range) {
        relativeConstraints.selectedRange.set(range)
        with(absoluteConstraints) {
            selectedRange.set(range.shiftImmutable(visibleRange.lower - viewRange.lower))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerId = event.getPointerId(0)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (index == INVALID_ID) {
                    prevX = event.x
                    index = pointerId
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (index == pointerId) {
                    val delta = (prevX - event.x).toInt()
                    with(absoluteConstraints) {
                        val visibleRangeUpdated = visibleRange.shiftImmutable(delta)
                        val selectedRangeUpdated = selectedRange.shiftImmutable(delta)
                        // make sure new values fits our needs
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

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (pointerId == index) {
                    index = INVALID_ID
                }
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
        clipRect.set(relativeConstraints.visibleRange.lower.toFloat(), 0f,
                relativeConstraints.visibleRange.upper.toFloat(), viewHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateTranslatedConstraints(absoluteConstraints)
        path.rewind()
        path.addRoundRect(clipRect, viewHeight / 2f, viewHeight / 2f, Path.Direction.CCW)

        canvas.save()
        //Translate back by margin from start to draw in correct position.
        // It is used that way because to evaluate view range we need to know position related to it's
        // parent which includes margin from left and right.
        canvas.translate(-x, paddingTop.toFloat())

        drawBackground(canvas)
        drawSelectedArea(canvas)

        drawTimeDividers(relativeConstraints, canvas)
        canvas.restore()
    }

    private fun updateTranslatedConstraints(newValue: Constraints) {
        // to translate it to currently visible area - just shift it by start offset of the visible area
        // and add view start padding.
        val offset = -newValue.visibleRange.lower + viewRange.lower
        with(newValue) {
            relativeConstraints.let {
                it.selectedRange.set(selectedRange.shiftImmutable(offset))
                it.allowedRange.set(allowedRange.shiftImmutable(offset))
                it.totalRange.set(totalRange.shiftImmutable(offset))
                it.visibleRange.set(visibleRange.shiftImmutable(offset))
                it.current = current + offset
                it.minRange = minRange
            }
        }
    }

    private fun drawBackground(canvas: Canvas) {
        with(canvas) {
            save()
            clipPath(path)
            drawColor(backgroundColor)

            with(relativeConstraints) {
                val startDisabled = Range(totalRange.lower, allowedRange.lower).clamp(viewRange)
                val endDisabled = Range(allowedRange.upper, totalRange.upper).clamp(viewRange)
                drawDisabledArea(canvas, startDisabled, true)
                drawDisabledArea(canvas, endDisabled, false)
            }
            restore()
        }
    }

    private fun drawDisabledArea(canvas: Canvas, range: Range, startFromEnd: Boolean) {
        if (!range.isEmpty()) {
            canvas.clipRect(range.lower.toFloat(), 0f, range.upper.toFloat(), viewHeight)
            if (startFromEnd) {
                // draw from end so for movements it should changes it's location as well
                var iterator = range.upper + viewHeight
                while (iterator >= range.lower - viewHeight) {
                    canvas.drawLine(iterator, viewHeight, iterator + viewHeight, 0f, linePaint)
                    iterator -= lineStep
                }
            } else {
                var iterator = range.lower - viewHeight
                while (iterator <= range.upper + viewHeight) {
                    canvas.drawLine(iterator, viewHeight, iterator + viewHeight, 0f, linePaint)
                    iterator += lineStep
                }
            }
        }
    }

    private fun drawSelectedArea(canvas: Canvas) {
        with(canvas) {
            save()
            clipPath(path)
            with(relativeConstraints) {
                val left = viewRange.clamp(selectedRange.lower).toFloat()
                val right = viewRange.clamp(selectedRange.upper.toFloat())
                drawRect(left, 0f, right, viewHeight, selectedPaint)
            }
            restore()
        }
    }

    private fun drawTimeDividers(constraints: Constraints, canvas: Canvas) {
        val bitmapVerticalOffset = (viewHeight - pattern.height) / 2
        val horizontalOffset = (absoluteConstraints.visibleRange.lower) % pattern.width - viewRange.lower
        val backgroundRange = constraints.allowedRange.clampImmutable(constraints.visibleRange).shift(-viewRange.lower)

        with(canvas) {
            save()
            clipPath(path)
            clipRect(constraints.allowedRange.lower.toFloat(), 0f, constraints.allowedRange.upper.toFloat(), viewHeight)
            translate(-horizontalOffset.toFloat(), bitmapVerticalOffset)
            val rect = RectF(backgroundRange.lower.toFloat() - pattern.width, 0f, backgroundRange.upper.toFloat() + pattern.width, pattern.height.toFloat())
            drawRect(rect, backgroundTimeMarkPaint)
            restore()
        }
    }

    companion object {
        const val INVALID_ID = -1
    }
}