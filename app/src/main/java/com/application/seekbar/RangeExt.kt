package com.application.seekbar

import android.util.Range

/**
 * Created on 06.07.18.
 */
/**
 * Couldn't make it generic. Generic type [Number] doesn't have operations.
 */
fun Range<Int>.width(): Int {
    return upper - lower
}

/**
 * Couldn't make it generic. Generic type [Number] doesn't have operations.
 */
fun Range<Int>.shift(delta: Int): Range<Int> {
    return Range(lower + delta, upper + delta)
}
