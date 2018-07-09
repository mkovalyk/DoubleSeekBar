package com.application.seekbar

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import kotlinx.android.synthetic.main.layout_double_seek_bar.view.*

/**
 * Created on 06.07.18.
 */
class DoubleSeekBarLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        ConstraintLayout(context, attrs, defStyleAttr) {
    var characteristics: Characteristics? = null
        set(value) {
            field = value
            updateCharacteristics(field!!)
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_double_seek_bar, this)
    }

    private fun updateCharacteristics(newValue: Characteristics) {
        barWithLimit.abstractCharacteristics = newValue
        leftThumb.range
    }
}