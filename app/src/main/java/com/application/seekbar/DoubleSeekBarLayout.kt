package com.application.seekbar

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import kotlinx.android.synthetic.main.layout_double_seek_bar.view.*

/**
 * Created on 06.07.18.
 */
class DoubleSeekBarLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        ConstraintLayout(context, attrs, defStyleAttr) {
    var constraints: Constraints? = null
        set(value) {
            field = value
            updateCharacteristics(value!!)
        }

    var viewConstraints: Constraints = Constraints.EMPTY
        private set

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_double_seek_bar, this)
        leftThumb.listener = { newValue ->
            leftThumbMoved(newValue)
        }

        rightThumb.listener = { newValue ->
            rightThumbMoved(newValue)
        }
    }

    private fun rightThumbMoved(newValue: Float) {
        with(viewConstraints) {

            // todo move text label as well

//            val maxLeft = min(current, newValue.toInt() - minRange)
//            leftThumb.range = Range(allowedRange.clamp(visibleRange.lower), allowedRange.clamp(maxLeft))
            Log.d(TAG, "RightThumbChanged: Value: $newValue ")
//            Log.d(TAG, "RightThumbChanged: Left - ${leftThumb.range} ")
            selectedRange.set(selectedRange.lower, newValue.toInt())
        }
    }

    private fun leftThumbMoved(newValue: Float) {
        with(viewConstraints) {

            // todo move text label as well

//            val minRight = max(current, newValue.toInt() + minRange)
//            rightThumb.range = Range(minRight, allowedRange.clamp(visibleRange.upper))
            Log.d(TAG, "LeftThumbChanged: value : $newValue ")
//            Log.d(TAG, "LeftThumbChanged: Right - ${rightThumb.range} ")
            selectedRange.set(newValue.toInt(), selectedRange.upper)
        }
    }

    private fun updateCharacteristics(newValue: Constraints) {
        Log.d(TAG, "updateCharacteristics: $newValue")
        viewConstraints = convertToXY(newValue, barWithLimit.viewRange)
                .apply {
                    leftThumb.range = Range(allowedRange.clamp(visibleRange.lower), current)
                    rightThumb.range = Range(current, allowedRange.clamp(visibleRange.upper))
                    Log.d(TAG, "left: ${leftThumb.range}. right: ${rightThumb.range}")
                }
                .also {
                    it.selectedRange.listener = { newRange ->
                        val lower = translatePoint(newRange.lower, barWithLimit.viewRange, it.visibleRange)
                        val upper = translatePoint(newRange.upper, barWithLimit.viewRange, it.visibleRange)
                        constraints!!.selectedRange.set(lower, upper)
                        barWithLimit.invalidate()
                    }
                }
                .also {
                    barWithLimit.viewConstraints = it
                }
        invalidate()
    }

    private fun convertToXY(constraints: Constraints, viewRange: Range): Constraints {
        with(constraints) {
            Log.d(TAG, "convertToXY: Range:$viewRange. Constraints: $constraints")
            val allowedRange = Range(translatePoint(allowedRange.lower, visibleRange, viewRange),
                    translatePoint(allowedRange.upper, visibleRange, viewRange))

            val totalRange = Range(translatePoint(totalRange.lower, visibleRange, viewRange),
                    translatePoint(totalRange.upper, visibleRange, viewRange))

            val selectedRange = Range(translatePoint(selectedRange.lower, visibleRange, viewRange),
                    translatePoint(selectedRange.upper, visibleRange, viewRange))

            val current = translatePoint(current, visibleRange, viewRange)

            val range = Range(translatePoint(visibleRange.lower, visibleRange, viewRange),
                    translatePoint(visibleRange.upper, visibleRange, viewRange))

            val minDuration = minRange * viewRange.width / visibleRange.width
            return Constraints(allowedRange, totalRange, selectedRange, current, range, minDuration, true)
        }
    }

    private fun translatePoint(x: Int, range: Range, translatedRange: Range): Int {
        return translatedRange.clamp((translatedRange.lower + translatedRange.width * (x - range.lower) / range.width))
    }

    companion object {
        const val TAG = "DoubleSeekBarLayout"
    }
}