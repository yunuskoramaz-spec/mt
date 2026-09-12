package com.q3rvo.erciyesflight

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager

class MainActivity : Activity() {
    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.rgb(17, 42, 65)
        window.navigationBarColor = Color.rgb(17, 42, 65)
        gameView = GameView(this)
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
