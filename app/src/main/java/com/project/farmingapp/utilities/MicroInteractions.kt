package com.project.farmingapp.utilities

import android.view.HapticFeedbackConstants
import android.view.View

fun View.pulseTapFeedback() {
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    animate().scaleX(0.97f).scaleY(0.97f).setDuration(70).withEndAction {
        animate().scaleX(1f).scaleY(1f).setDuration(90).start()
    }.start()
}
