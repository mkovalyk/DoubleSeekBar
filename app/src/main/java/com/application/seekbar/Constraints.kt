package com.application.seekbar

import android.util.Log
import kotlin.math.min

/**
 * Created on 06.07.18.
 */
data class Constraints(
        val allowedRange: Range,
        val totalRange: Range,
        val selectedRange: Range,
        var current: Int,
        var visibleRange: Range,
        var minRange: Int = 0,
        val tolerate: Boolean = false) {

    init {
        if (tolerate) {
            Log.d("Constraints", "Before: $this")
            visibleRange.clamp(totalRange)
            allowedRange.clamp(totalRange)
            selectedRange.clamp(allowedRange)
            current = selectedRange.clamp(current)
            minRange = min(minRange, selectedRange.width)
            Log.d("Constraints", "After: $this")
        } else {
            if (!totalRange.contains(allowedRange)) {
                throw IllegalStateException("Total range ${totalRange}should be equal to or bigger than allowed range: $allowedRange")
            }
            if (!allowedRange.contains(selectedRange)) {
                throw IllegalStateException("Allowed range:$allowedRange should be equal to or bigger than selected range: $selectedRange")
            }
            if (!totalRange.contains(visibleRange)) {
                throw IllegalStateException("Total range:$totalRange should be equal to or bigger than visible range:$visibleRange")
            }
            if (!selectedRange.contains(current)) {
                throw IllegalStateException("Selected range $selectedRange should contains current value: $current")
            }
            if (minRange > selectedRange.width) {
                throw IllegalStateException("MinRange: $minRange is bigger that width of selected area")
            }
        }
    }

    companion object {
        val EMPTY = Constraints(Range.EMPTY, Range.EMPTY, Range.EMPTY, 0, Range.EMPTY)
    }
}

/**
 *
 * Schematic representation of the ranges used in this class
 *
 *                                          C - current
 *
 *   #########++++++++++++++++++++****************C************++++++++++++++#######################
 *                                |<---selectedRange--------->|
 *            |<-----------------------allowedRange------------------------>|
 *   |<--------------------------------totalRange------------------------------------------------->|
 *   |<-----------............---------visibleRange----------------------.......................-->|
 *
 *   Visible range can be changed according to the scale.
 */