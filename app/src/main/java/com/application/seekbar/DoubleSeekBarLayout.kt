package com.application.seekbar

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import kotlinx.android.synthetic.main.layout_double_seek_bar.view.*
import kotlin.math.max
import kotlin.math.min

/**
 * View which draws whole component for selecting range
 *
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
            // update maximal value for left thumb
            val value = min(current, intValue - minRange)
            val lower = left_thumb.range.lower
            left_thumb.range.set(lower, max(lower, value))
        }
    }

    private fun leftThumbMoved(newValue: Float) {
        viewConstraints?.apply {
            val intValue = newValue.toInt()
            selectedRange.set(intValue, selectedRange.upper)
            // update minimal value for right thumb
            val value = max(current, intValue + minRange)
            val upper = right_thumb.range.upper
            right_thumb.range.set(min(upper, value), upper)
        }
    }

    private fun translateIfTextLabelsOverlapsWithIndicator() {
        val distanceToLeft = tag_icon.x - left_label.width - left_label.x
        val distanceToRight = right_label.x - tag_icon.x - tag_icon.width
        tag_icon.translationY = minOf(0f, distanceToLeft, distanceToRight)
    }

    private fun updateCharacteristics(newValue: Constraints) {
        viewConstraints = convertToXY(newValue, bar_with_limit.viewRange)
                .apply {
                    left_thumb.range = Range(allowedRange.clamp(visibleRange.lower), current)
                    right_thumb.range = Range(current, allowedRange.clamp(visibleRange.upper))

                    left_thumb.centerX = selectedRange.lower.toFloat()
                    right_thumb.centerX = selectedRange.upper.toFloat()

                    constraints?.updateTextLabels()
                    tag_icon.centerX = current.toFloat()
                    vertical_line.centerX = current.toFloat()
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
        viewConstraints?.apply {
            // add initial constraints for left and right thumbs
            leftThumbMoved(selectedRange.lower.toFloat())
            rightThumbMoved(selectedRange.upper.toFloat())
        }
    }

    private fun Constraints.updateTextLabels() {
        setTime(selectedRange.width.times(multiplier), bottom_label)
        setTime((current - selectedRange.lower).times(multiplier), left_label)
        setTime((selectedRange.upper - current).times(multiplier), right_label)

        viewConstraints?.apply {
            bottom_label.centerX = selectedRange.center.toFloat()
        }

        right_label.centerX = right_thumb.centerX
        left_label.centerX = left_thumb.centerX
        translateIfTextLabelsOverlapsWithIndicator()
    }

    // Getter for tag icon. It may change in future so there is no reason to use tag_icon directly
    val tagIcon
        get() = tag_icon

    private fun setTime(time: Float, into: TextView) {
        val endTime = SpannableStringBuilder()
                .bold { append(String.format("%.1f", time)) }
                .append(" ")
                .append(resources.getString(R.string.time_unit))
        into.text = endTime
    }

    private fun convertToXY(constraints: Constraints, viewRange: Range): Constraints {
        with(constraints) {
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
}