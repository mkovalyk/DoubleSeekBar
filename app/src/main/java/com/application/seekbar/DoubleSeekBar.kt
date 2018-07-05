package com.application.seekbar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Range
import android.view.View

/**
 * Created on 05.07.18.
 */
class DoubleSeekBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : View(context, attrs, defStyleAttr, defStyleRes) {
    val path = Path()
    val anotherPath = Path()
    val viewHeight = 32f
    val rect = RectF(0f, 0f, 1000f, viewHeight)
    val anotherRect = RectF(0f, 0f, 900f, viewHeight)
    val lineStep: Float
    /**
     * Usually from [0 to max]
     */
    lateinit var allowedRange: Range<Int>
    /**
     * Range bigger than allowed by some threshold. In this case it can be [0 - width/2 to max + width/2 ]
     */
    lateinit var totalRange: Range<Int>
    lateinit var selectedRange: Range<Int>
    val totalWidth = 30

    init {
        val styleAttrs = context.obtainStyledAttributes(attrs, R.styleable.DoubleSeekBar, defStyleAttr, R.style.DoubleSeekBarStyle)
        lineStep = styleAttrs.getDimension(R.styleable.DoubleSeekBar_lineStep, 10f)
        styleAttrs.recycle()
    }

    val linePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        strokeWidth = lineStep * 0.3f
        isDither = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path.rewind()
        path.addRoundRect(rect, viewHeight / 2f, viewHeight / 2f, Path.Direction.CCW)

        anotherPath.rewind()
        anotherPath.addRect(anotherRect, Path.Direction.CCW)

        with(canvas) {
            drawColor(Color.GRAY)

            save()
            clipPath(path)
            drawColor(Color.BLUE)

            clipPath(anotherPath)
            for (index in -5..100)
                drawLine(index * lineStep, viewHeight, index * lineStep + viewHeight, 0f, linePaint)
            restore()
        }
    }
}