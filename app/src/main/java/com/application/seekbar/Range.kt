package com.application.seekbar

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

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

    private val internalListener: (property: KProperty<*>, oldValue: Int, newValue: Int) -> Unit =
            { _, _, _ ->
                width = this.upper - this.lower
                listener?.invoke(this)
            }

    var lower: Int by Delegates.observable(lower, internalListener)
        private set
    var upper: Int  by Delegates.observable(upper, internalListener)
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
        lower += delta
        upper += delta
    }

    fun set(lower: Int, upper: Int) {
        checkInvariance(lower, upper)
        this.lower = lower
        this.upper = upper
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
        if (lower <= range.lower) {
            this.lower = range.lower
        }
        if (upper >= range.upper) {
            this.upper = range.upper
        }
    }

    override fun toString() = "Range[$lower: $upper]."

    companion object {
        val EMPTY = Range(0, 1)
    }
}