package com.application.seekbar

import android.content.Context
import android.util.AttributeSet
import android.util.Log
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
    var range: Range<Float>  = Range(100f, 800f)
        set(value) {
            field = value
            if (!field.contains(x)) {
                this.x = field.clamp(x)
                listener?.invoke(this.x)
            }
        }
    var listener: ((Float) -> Unit)? = null
    var prevX = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d("Thumb", "OnTouchEvent:$event")
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.rawX
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val delta = (prevX - event.rawX)
                if (range.contains(this.x - delta)) {
                    this.x -= delta
                    listener?.invoke(this.x)
                    prevX = event.rawX
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}