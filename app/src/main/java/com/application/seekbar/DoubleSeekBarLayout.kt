package com.application.seekbar

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
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
            updateConstraints(value!!)
        }

    private var viewConstraints: Constraints? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_double_seek_bar, this)
        left_thumb.listener = { newValue, byUser ->
            leftThumbMoved(newValue, byUser)
        }

        right_thumb.listener = { newValue, byUser ->
            rightThumbMoved(newValue, byUser)
        }
        bar_with_limit.viewRange.listener = { _ ->
            if (constraints != null) {
                updateConstraints(constraints!!)
            }
        }
        bar_with_limit.currentListener = {
            val value = translatePointNoClamping(it, bar_with_limit.absoluteConstraints.visibleRange, constraints!!.visibleRange)
            text_value.text = value.toString()
            updateMaxRangesForThumbs()
        }
    }

    private fun rightThumbMoved(newValue: Float, byUser: Boolean) {
        viewConstraints?.apply {
            val intValue = newValue.toInt()
            if (byUser) {
                bar_with_limit.updateSelectedRange(Range(selectedRange.lower, intValue))
//                selectedRange.set(selectedRange.lower, intValue)
            }
            // update maximal value for left thumb
            val value = min(current, intValue - minRange)
            val lower = left_thumb.range.lower
            left_thumb.range.set(lower, max(lower, value))
        }
    }

    private fun leftThumbMoved(newValue: Float, byUser: Boolean) {
        viewConstraints?.apply {
            val intValue = newValue.toInt()
            if (byUser) {
                bar_with_limit.updateSelectedRange(Range(intValue, selectedRange.upper))
//                selectedRange.set(intValue, selectedRange.upper)
            }
            // update minimal value for right thumb
            val value = max(current, intValue + minRange)
            val upper = right_thumb.range.upper
            right_thumb.range.set(min(upper, value), upper)
        }
    }

    private fun updateMaxRangesForThumbs() {
        viewConstraints?.apply {
            right_thumb.range.set(right_thumb.range.lower, allowedRange.upper)
            left_thumb.range.set(allowedRange.lower, left_thumb.range.upper)
        }
    }

    private fun translateIfTextLabelsOverlapsWithIndicator() {
        val distanceToLeft = tag_icon.x - left_label.width - left_label.x
        val distanceToRight = right_label.x - tag_icon.x - tag_icon.width
        tag_icon.translationY = minOf(0f, distanceToLeft, distanceToRight)
    }

    private fun updateConstraints(newValue: Constraints) {
        bar_with_limit.absoluteConstraints = convertToXY(newValue, bar_with_limit.viewRange)
        bar_with_limit.absoluteConstraints.selectedRange.listener = ::updateSelectedRange
        viewConstraints = bar_with_limit.relativeConstraints
//        Log.d(TAG, "updateConstraints: viewConstraints:$viewConstraints")
        // post to make sure all layout operations are already performed
        post {
            viewConstraints?.apply {
                left_thumb.range = Range(allowedRange.clamp(visibleRange.lower), current)
                right_thumb.range = Range(current, allowedRange.clamp(visibleRange.upper))

                left_thumb.centerX = selectedRange.lower.toFloat()
                right_thumb.centerX = selectedRange.upper.toFloat()

                constraints?.updateTextLabels()
                tag_icon.centerX = current.toFloat()
                vertical_line.centerX = current.toFloat()
            }
        }
    }

    private fun updateSelectedRange(newRange: Range) {
        Log.d(TAG, "updateSelectedRange: $newRange")
        with(constraints!!) {
            // convert to origin coordinate
            val lower = translatePoint(newRange.lower, bar_with_limit.absoluteConstraints.visibleRange, visibleRange)
            val upper = translatePoint(newRange.upper, bar_with_limit.absoluteConstraints.visibleRange, visibleRange)
            selectedRange.set(lower, upper)
            // Even though it is limited at UI level, it is required to be limited here to prevent
            // such edge cases: translatePoint: 871 from Range[72: 1848]. to Range[-27000: 33000].. Result -7
            selectedRange.clamp(allowedRange)
            updateTextLabels()

            bar_with_limit.invalidate()
        }
        viewConstraints?.apply {
            // add initial constraints for left and right thumbs
            leftThumbMoved(selectedRange.lower.toFloat(), false)
            rightThumbMoved(selectedRange.upper.toFloat(), false)
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
    val tagIcon: ImageView
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
            val allowedRange = Range(translatePointNoClamping(allowedRange.lower, visibleRange, viewRange),
                    translatePointNoClamping(allowedRange.upper, visibleRange, viewRange))

            val totalRange = Range(translatePointNoClamping(totalRange.lower, visibleRange, viewRange),
                    translatePointNoClamping(totalRange.upper, visibleRange, viewRange))

            val selectedRange = Range(translatePointNoClamping(selectedRange.lower, visibleRange, viewRange),
                    translatePointNoClamping(selectedRange.upper, visibleRange, viewRange))

            val current = translatePointNoClamping(current, visibleRange, viewRange)

            val range = Range(translatePointNoClamping(visibleRange.lower, visibleRange, viewRange),
                    translatePointNoClamping(visibleRange.upper, visibleRange, viewRange))

            val minDuration = minRange * viewRange.width / visibleRange.width
            val result = Constraints(allowedRange, totalRange, selectedRange, current, range, minDuration, true)
            Log.d(TAG, "convertToXY: Result: $result")
            return result
        }
    }

    private fun translatePoint(x: Int, range: Range, translatedRange: Range): Int {
        // converting to long to prevent overflows
        val result = translatedRange.clamp((translatedRange.lower + translatedRange.width.toLong() * (x - range.lower) / range.width).toInt())
        Log.d(TAG, "translatePoint: $x from $range to $translatedRange. Result $result")
        return result
    }

    private fun translatePointNoClamping(x: Int, range: Range, translatedRange: Range): Int {
        // converting to long to prevent overflows
        val result = (translatedRange.width.toLong() * x / range.width).toInt()
//        Log.d(TAG, "translatePointNoClamping: $x from $range to $translatedRange. Result $result")
        return result
    }

    companion object {
        const val TAG = "DoubleSeekBarLayout"
    }
}