package com.application.seekbar

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
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
        barWithLimit.viewRange.listener = { range ->
            if (constraints != null) {
                updateCharacteristics(constraints!!)
            }
        }
    }

    private fun rightThumbMoved(newValue: Float) {
        with(viewConstraints) {
            selectedRange.set(selectedRange.lower, newValue.toInt())
            right_label.x = rightThumb.centerX - right_label.width / 2
        }
    }

    private fun leftThumbMoved(newValue: Float) {
        with(viewConstraints) {
            selectedRange.set(newValue.toInt(), selectedRange.upper)
            left_label.x = leftThumb.centerX - left_label.width / 2
        }
    }

    private fun updateCharacteristics(newValue: Constraints) {
        Log.d(TAG, "updateCharacteristics: $newValue")
        viewConstraints = convertToXY(newValue, barWithLimit.viewRange)
                .apply {
                    leftThumb.range = Range(allowedRange.clamp(visibleRange.lower), current)
                    rightThumb.range = Range(current, allowedRange.clamp(visibleRange.upper))
                    leftThumb.centerX = selectedRange.lower.toFloat()
                    rightThumb.centerX = selectedRange.upper.toFloat()

                    Log.d(TAG, "left: ${leftThumb.range}. right: ${rightThumb.range}")
                }
                .also {
                    it.selectedRange.listener = { newRange ->
                        val originConstraints = constraints!!
                        val lower = translatePoint(newRange.lower, barWithLimit.viewRange, originConstraints.visibleRange)
                        val upper = translatePoint(newRange.upper, barWithLimit.viewRange, originConstraints.visibleRange)
                        originConstraints.selectedRange.set(lower, upper)
                        bottom_label.centerX = viewConstraints.selectedRange.center.toFloat()

                        setTime(originConstraints.selectedRange.width, bottom_label)
                        setTime(originConstraints.current - originConstraints.selectedRange.lower, left_label)
                        setTime(originConstraints.selectedRange.upper - originConstraints.current, right_label)

                        barWithLimit.invalidate()
                    }
                }
                .also {
                    barWithLimit.viewConstraints = it
                }
        invalidate()
    }

    private fun setTime(time: Int, into: TextView) {
        val endTime = SpannableStringBuilder()
                .bold { append(time.toString()) }
                .append(" ")
                .append(resources.getString(R.string.time_unit))
        into.text = endTime
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
//        return (translatedRange.lower + translatedRange.width * (x - range.lower) / range.width)
    }

    companion object {
        const val TAG = "DoubleSeekBarLayout"
    }
}