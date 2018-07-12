package com.application.seekbar

import android.view.View

/**
 * Created on 12.07.18.
 */
var View.centerX: Float
    get() = this.x + this.width / 2
    set(value) {
        if (value != (x + this.width / 2)) {
            this.x = value - this.width / 2
        }
    }
