package com.application.seekbar

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity.BOTTOM
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val layout = DoubleSeekBarLayout(this)

        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        params.gravity = BOTTOM
        layout.layoutParams = params
        baseLayout.addView(layout)

        // video - 5 minutes
        val videoDuration = 300 * 1000

        // in seconds
        val width = 60 * 1000
        val current = 15 * 1000
        val selectedOffset = 5 * 1000
        val minRange = 4 * 1000

        layout.post {
            layout.constraints = Constraints(Range(0, videoDuration), Range(-width / 2, videoDuration + width / 2),
                    Range(current - selectedOffset, current + selectedOffset), current,
                    Range(current - width / 2, current + width / 2), minRange, true, 0.001f)
                    .apply {
                        selectedRange.listener = { range ->
                            Log.d(TAG, "SelectedRange Changed: $range")
                        }
                    }
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
