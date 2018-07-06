package com.application.seekbar

import android.util.Range

/**
 * Created on 06.07.18.
 */
fun Range<Int>.width(): Int {
    return upper - lower
}

fun Range<Int>.shift(delta: Int): Range<Int> {
    return Range(lower + delta, upper + delta)
}
