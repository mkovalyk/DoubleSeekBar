package com.application.seekbar

import android.content.Context
import android.util.AttributeSet
import android.util.Range
import android.view.MotionEvent
import android.widget.ImageView

/**
 * Class for displaying
 *
 * Created on 05.07.18.
 */
class Thumb @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {
    private val range: Range<Int>
    var listener: ((Int) -> Unit)? = null

    init {
        range = Range(100, 200)
    }

    var prevX = 0f
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (range.contains(event.x.toInt())) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    prevX = event.x
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val delta = prevX - event.x
                    listener?.invoke(delta.toInt())
                    prevX = event.x
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}