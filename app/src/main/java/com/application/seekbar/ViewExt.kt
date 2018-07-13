package com.application.seekbar

import android.view.View

/**
 * Extension method for View.
 *
 * Created on 12.07.18.
 */
/**
 * Get and set horizontal center of the view
 */
var View.centerX: Float
    get() = this.x + this.width / 2
    set(value) {
        // check if there is need to update
        if (value != (x + this.width / 2)) {
            this.x = value - this.width / 2
        }
    }
