package com.q3rvo.erciyesflight

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager

class MainActivity : Activity() {
    private lateinit var gameView: CleanGameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) window.setDecorFitsSystemWindows(true)
        window.statusBarColor = Color.rgb(67, 174, 226)
        window.navigationBarColor = Color.rgb(238, 211, 158)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        gameView = CleanGameView(this)
        setContentView(gameView)
        gameView.setOnApplyWindowInsetsListener { _, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            gameView.setSafeInsets(bars.top, bars.bottom)
            insets
        }
        gameView.requestApplyInsets()
    }

    override fun onPause() {
        if (::gameView.isInitialized) gameView.pauseLoop()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::gameView.isInitialized) gameView.resumeLoop()
    }
}
