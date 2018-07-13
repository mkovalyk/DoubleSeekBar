package com.application.seekbar

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import kotlinx.android.synthetic.main.layout_double_seek_bar.view.*
import kotlin.math.max
import kotlin.math.min

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

    private var viewConstraints: Constraints? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_double_seek_bar, this)
        left_thumb.listener = { newValue, byUser ->
            if (byUser) {
                leftThumbMoved(newValue)
            }
        }

        right_thumb.listener = { newValue, byUser ->
            if (byUser) {
                rightThumbMoved(newValue)
            }
        }
        bar_with_limit.viewRange.listener = { _ ->
            if (constraints != null) {
                updateCharacteristics(constraints!!)
            }
        }
    }

    private fun rightThumbMoved(newValue: Float) {
        viewConstraints?.apply {
            val intValue = newValue.toInt()
            selectedRange.set(selectedRange.lower, intValue)
            val value = min(current, intValue - minRange)
            left_thumb.range.set(left_thumb.range.lower, value)
        }
    }

    private fun leftThumbMoved(newValue: Float) {
        viewConstraints?.apply {
            val intValue = newValue.toInt()
            selectedRange.set(intValue, selectedRange.upper)
            val value = max(current, intValue + minRange)
            right_thumb.range.set(value, right_thumb.range.upper)
        }
    }

    private fun translateIfTextLabelsOverlapsWithIndicator() {
        val distanceToLeft = tag_icon.x - left_label.width - left_label.x
        val distanceToRight = right_label.x - tag_icon.x - tag_icon.width
        tag_icon.translationY = minOf(0f, distanceToLeft, distanceToRight)
        Log.d(TAG, "translateIfTextLabelsOverlapsWithIndicator: ${tag_icon.translationY}")
    }

    private fun updateCharacteristics(newValue: Constraints) {
        Log.d(TAG, "updateCharacteristics: $newValue")
        viewConstraints = convertToXY(newValue, bar_with_limit.viewRange)
                .apply {
                    left_thumb.range = Range(allowedRange.clamp(visibleRange.lower), current)
                    right_thumb.range = Range(current, allowedRange.clamp(visibleRange.upper))

                    left_thumb.centerX = selectedRange.lower.toFloat()
                    right_thumb.centerX = selectedRange.upper.toFloat()

                    constraints?.updateTextLabels()
                    Log.d(TAG, "left: ${left_thumb.range}. right: ${right_thumb.range}")
                }
                .also {
                    it.selectedRange.listener = ::updateSelectedRange
                    bar_with_limit.viewConstraints = it
                }
    }

    private fun updateSelectedRange(newRange: Range) {
        with(constraints!!) {
            // convert to origin coordinate
            val lower = translatePoint(newRange.lower, bar_with_limit.viewRange, visibleRange)
            val upper = translatePoint(newRange.upper, bar_with_limit.viewRange, visibleRange)
            selectedRange.set(lower, upper)

            updateTextLabels()

            bar_with_limit.invalidate()
        }
    }

    private fun Constraints.updateTextLabels() {
        setTime(selectedRange.width, bottom_label)
        setTime(current - selectedRange.lower, left_label)
        setTime(selectedRange.upper - current, right_label)

        viewConstraints?.apply {
            bottom_label.centerX = selectedRange.center.toFloat()
        }

        right_label.centerX = right_thumb.centerX
        left_label.centerX = left_thumb.centerX
        translateIfTextLabelsOverlapsWithIndicator()
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