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
                width = upper - lower
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
        this.width = upper - lower
    }

    fun shift(delta: Int) {
        lower += delta
        upper += delta
    }

    fun shiftImmutable(delta: Int): Range {
        return Range(lower + delta, upper + delta)
    }

    fun contains(value: Int): Boolean {
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

    companion object {
        val EMPTY = Range(0, 0)
    }
}