package com.application.seekbar

/**
 * Created on 06.07.18.
 */
data class Characteristics(
        val allowedRange: Range,
        val totalRange: Range,
        val selectedRange: Range,
        var current: Int,
        var visibleRange: Range, val tolerate: Boolean = false) {

    init {
        if (tolerate) {
            visibleRange.clamp(totalRange)
            allowedRange.clamp(totalRange)
            selectedRange.clamp(allowedRange)
            current = selectedRange.clamp(current)
        } else {
            if (!totalRange.contains(allowedRange)) {
                throw IllegalStateException("Total range should be equal to or bigger than allowed range")
            }
            if (!allowedRange.contains(selectedRange)) {
                throw IllegalStateException("Allowed range should be equal to or bigger than selected range")
            }
            if (!totalRange.contains(visibleRange)) {
                throw IllegalStateException("Total range should be equal to or bigger than visible range")
            }
            if (!selectedRange.contains(current)) {
                throw IllegalStateException("Selected range  should contains current value")
            }
        }
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