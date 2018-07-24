package com.application.seekbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Created on 23.07.18.
 */
class RangeTest {

    @Test
    fun `doesn't overlap`() {
        val first = Range(0, 10)
        val second = Range(11, 15)

        assertNull(first.overlap(second))
    }

    @Test
    fun `edge overlap`() {
        val first = Range(0, 10)
        val second = Range(10, 15)

        assertEquals(first.overlap(second), Range(10, 10))
    }

    @Test
    fun `one contains another`() {
        val first = Range(0, 10)
        val second = Range(5, 8)

        assertEquals(first.overlap(second), second)
        assertEquals(second.overlap(first), second)
    }

    @Test
    fun `normal overlap`() {
        val first = Range(0, 10)
        val second = Range(8, 15)

        assertEquals(first.overlap(second), Range(8, 10))
        assertEquals(second.overlap(first), Range(8, 10))
    }
}