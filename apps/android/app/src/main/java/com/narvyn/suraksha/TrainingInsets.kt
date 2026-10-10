package com.narvyn.suraksha

import android.app.Activity
import android.view.View
import android.view.WindowInsets

/** Keep system bars and the keyboard outside bounded learning content, including API 36. */
fun Activity.trainingInsets(root:View) {
    root.setOnApplyWindowInsetsListener { view,insets ->
        if(android.os.Build.VERSION.SDK_INT>=30) {
            val bars=insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
            view.setPadding(dp(16)+bars.left,dp(12)+bars.top,dp(16)+bars.right,dp(12)+bars.bottom)
        } else view.setPadding(dp(16)+insets.systemWindowInsetLeft,dp(12)+insets.systemWindowInsetTop,dp(16)+insets.systemWindowInsetRight,dp(12)+insets.systemWindowInsetBottom)
        insets
    }
    root.requestApplyInsets()
}
