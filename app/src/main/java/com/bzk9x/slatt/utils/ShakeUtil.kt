package com.bzk9x.slatt.utils

import android.view.View
import android.view.animation.CycleInterpolator
import android.view.animation.TranslateAnimation

object ShakeUtil {
    fun shake(view: View) {
        val animation = TranslateAnimation(0f, 20f, 0f, 0f)
        animation.duration = 150
        animation.interpolator = CycleInterpolator(2f)

        view.startAnimation(animation)
    }
}