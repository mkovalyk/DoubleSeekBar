package com.application.seekbar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.properties.Delegates

/**
 * Created on 05.07.18.
 */
class BarWithLimit @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : View(context, attrs, defStyleAttr, defStyleRes) {
    private val path = Path()
    private var clipRect: RectF? = null
    private val viewHeight: Float
    private val lineStep: Float

    private val backgroundColor: Int
    private val lineColor: Int
    private val selectedColor: Int

    val viewRange: Range = Range.EMPTY

    private var prevX = 0f

    init {
        val theme = context.theme
        val styleAttrs = theme.obtainStyledAttributes(attrs, R.styleable.BarWithLimit, defStyleAttr, R.style.DefaultBarWithLimitStyle)
        lineStep = styleAttrs.getDimension(R.styleable.BarWithLimit_lineStep, 10f)
        viewHeight = styleAttrs.getDimension(R.styleable.BarWithLimit_barHeight, 10f)
        backgroundColor = styleAttrs.getColor(R.styleable.BarWithLimit_progressBackground, Color.BLACK)
        lineColor = styleAttrs.getColor(R.styleable.BarWithLimit_progressDisabled, Color.MAGENTA)
        selectedColor = styleAttrs.getColor(R.styleable.BarWithLimit_progressSelected, Color.CYAN)

        styleAttrs.recycle()
    }

    /**
     * Ranges of views
     */
    var viewConstraints: Constraints by Delegates.observable(Constraints.EMPTY) { _, _, newValue ->
        clipRect = RectF(newValue.visibleRange.lower.toFloat(), 0f, newValue.visibleRange.upper.toFloat(), viewHeight)
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

//    private fun evaluateCharacteristics(): Constraints {
//        val visibleRange = Range(0, 800)
//        val width = 100
//        val current = (visibleRange.upper + visibleRange.lower) / 2
//
//        return Constraints(allowedRange = Range(0, 1000),
//                totalRange = Range(0 - width / 2, 1000 + width / 2),
//                selectedRange = Range(10, 300),
//                current = current, visibleRange = visibleRange)
//    }

//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        when (event.action) {
//            MotionEvent.ACTION_DOWN -> {
//                prevX = event.x
//                return true
//            }
//            MotionEvent.ACTION_MOVE -> {
//                val delta = (prevX - event.x).toInt()
//                with(viewConstraints) {
//                    // val deltaTranslated = delta * this.visibleRange.width / viewConstraints.visibleRange.width
//                    val visibleRangeUpdated = visibleRange.shiftImmutable(delta)
//                    if (totalRange.contains(visibleRangeUpdated)) {
//                        visibleRange = visibleRangeUpdated
////                      viewRange = Range(paddingStart, measuredWidth - paddingEnd)
////                      viewConstraints = convertToXY(this, viewRange)
//                        invalidate()
//                    }
//                }
//                prevX = event.x
//                return true
//            }
//        }
//        return super.onTouchEvent(event)
//    }

    private fun drawBackground(canvas: Canvas) {
        with(canvas) {
            save()
            clipPath(path)
            drawColor(backgroundColor)

            // TODO optimize for two separate drawing
            if (viewConstraints.totalRange.lower != viewConstraints.allowedRange.lower
                    || viewConstraints.totalRange.upper != viewConstraints.allowedRange.upper) {
                clipRect(viewConstraints.totalRange.lower.toFloat(), 0f, viewConstraints.totalRange.upper.toFloat(),
                        viewHeight)
                clipRect(viewConstraints.allowedRange.lower.toFloat(), 0f, viewConstraints.allowedRange.upper.toFloat(),
                        viewHeight, Region.Op.DIFFERENCE)

                var iterator = viewConstraints.totalRange.lower - viewHeight
                while (iterator <= viewConstraints.totalRange.upper + viewHeight) {
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
            drawRect(viewConstraints.selectedRange.lower.toFloat(), 0f,
                    viewConstraints.selectedRange.upper.toFloat(), viewHeight, selectedPaint)
            restore()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)
        Log.d(TAG, "WidthMode: $widthMode. WidthSize: $widthSize. " +
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
        Log.d(TAG, "Result. Width: $widthSize. Height: $height")

        // There is no reason to change width of the bar
        setMeasuredDimension(widthSize, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // only after view is laid out margins are available
        val marginStart = x.toInt()
        viewRange.set(marginStart + paddingStart, marginStart + measuredWidth - paddingEnd - paddingStart)
        Log.d(TAG, "onLayout: x=$x. Range: $viewRange")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path.rewind()
        path.addRoundRect(clipRect, viewHeight / 2f, viewHeight / 2f, Path.Direction.CCW)

        //Translate back by margin from start to draw in correct position.
        // It is used that way because to evaluate view range we need to know position related to it's
        // parent which includes margin from left and right.
        canvas.translate(-x, paddingTop.toFloat())

        drawBackground(canvas)
        drawSelectedArea(canvas)
    }

    companion object {
        const val TAG = "BarWithLimit"
    }
}