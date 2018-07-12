package com.application.seekbar

import android.view.View

/**
 * Created on 12.07.18.
 */
var View.centerX: Float
    get() = this.x + this.width / 2
    set(value) {
        this.x = value - this.width / 2
    }
