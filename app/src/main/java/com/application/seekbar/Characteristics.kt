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
        var visibleRange:Range<Int>)