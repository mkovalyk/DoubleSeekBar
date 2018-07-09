package com.application.seekbar

import android.util.Range

/**
 * Created on 06.07.18.
 */
data class Characteristics(
        val allowedRange: Range<Int>,
        val totalRange: Range<Int>,
        var selectedRange: Range<Int>,
        var current: Int,
        var visibleRange: Range<Int>) {

    init {
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