package com.application.seekbar

import kotlin.math.max
import kotlin.math.min

/**
 *
 * Current implementation with [Int] value.
 * Contains general operations as [android.util.Range] but is mutable for performance reason.
 *
 * Important: not thread-safe so don't treat it as such.
 *
 * Created on 10.07.2018.
 */
class Range constructor(lower: Int, upper: Int) {

    var lower: Int = lower
        private set
    var upper: Int = upper
        private set

    var width: Int = 0
        private set

    var listener: ((range: Range) -> Unit)? = null

    init {
        checkInvariance(lower, upper)
        this.width = upper - lower
    }

    private fun checkInvariance(lower: Int, upper: Int) {
        if (lower > upper) {
            throw IllegalStateException("Lower can not be bigger than upper")
        }
    }

    fun shift(delta: Int) {
        set(lower + delta, upper + delta)
    }

    fun set(lower: Int, upper: Int) {
        checkInvariance(lower, upper)
        if (lower != this.lower || upper != this.upper) {
            this.lower = lower
            this.upper = upper
            this.width = upper - lower
            listener?.invoke(this)
        }
    }

    fun shiftImmutable(delta: Int): Range {
        return Range(lower + delta, upper + delta)
    }

    fun contains(value: Int): Boolean {
        return value in lower..upper
    }

    fun contains(value: Float): Boolean {
        return value in lower..upper
    }

    fun contains(range: Range): Boolean {
        return range.lower in lower..upper && range.upper in lower..upper
    }

    /**
     * Returns value which is clamped inside of this range
     */
    fun clamp(value: Int): Int {
        if (value <= lower) {
            return lower
        }
        if (value >= upper) {
            return upper
        }
        return value
    }

    fun clamp(value: Float): Float {
        if (value <= lower) {
            return lower.toFloat()
        }
        if (value >= upper) {
            return upper.toFloat()
        }
        return value
    }

    /**
     * Changes current range to fit the range
     */
    fun clamp(range: Range) {
        set(max(lower, range.lower), min(upper, range.upper))
//        if (lower <= range.lower) {
//            lower = range.lower
//        }
//        if (upper >= range.upper) {
//            val upper = range.upper
//        }
    }

    val center
        get() = (upper + lower) / 2


    override fun toString() = "Range[$lower: $upper]."

    companion object {
        val EMPTY = Range(0, 1)
    }
}