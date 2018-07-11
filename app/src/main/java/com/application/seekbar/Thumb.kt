package com.application.seekbar

import android.content.Context
import android.util.AttributeSet
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
    var range: Range = Range.EMPTY
        set(value) {
            field = value
            if (!field.contains(center)) {
                center = field.clamp(center)
                listener?.invoke(center)
            }
        }
    var listener: ((Float) -> Unit)? = null
    private var prevX = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.rawX
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val delta = (prevX - event.rawX)
                if (range.contains(center - delta)) {
                    center -= delta
                    listener?.invoke(center)
                    prevX = event.rawX
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    var center: Float
        get() = x + width / 2
        set(value) {
            x = value - width / 2
        }
}