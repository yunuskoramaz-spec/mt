package com.q3rvo.erciyesflight

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Log
import android.view.Choreographer
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class GameView(context: Context) : View(context), Choreographer.FrameCallback {
    companion object {
        private const val DESIGN_WIDTH = 1080f
        private const val DESIGN_HEIGHT = 1920f
        private const val GRAVITY = 2500f
        private const val FLAP_VELOCITY = -820f
        private const val PIPE_SPEED = 430f
        private const val PIPE_GAP = 610f
        private const val PIPE_WIDTH = 190f
        private const val PIPE_SPACING = 620f
        private const val FIRST_PIPE_X = 1550f
        private const val PLAY_BOTTOM = 1690f
        private const val SPAWN_TRIGGER_DISTANCE = 480f
        private const val INITIAL_GAP_CENTER = 835f
        private const val MIN_GAP_CENTER = 690f
        private const val MAX_GAP_CENTER = 1000f
        private const val BIRD_W = 154f
        private const val BIRD_H = 122f
        private const val MENU_BIRD_X = 540f
        private const val MENU_BIRD_Y = 745f

        private val PrimaryColor = Color.rgb(21, 54, 82)
        private val SecondaryColor = Color.rgb(72, 125, 153)
        private val AccentColor = Color.rgb(239, 178, 70)
        private val BackgroundColor = Color.rgb(17, 42, 65)
        private val SurfaceColor = Color.rgb(248, 250, 250)
        private val TextPrimaryColor = Color.rgb(25, 48, 65)
        private val TextSecondaryColor = Color.rgb(101, 121, 132)
        private val SuccessColor = Color.rgb(61, 153, 105)
        private val DangerColor = Color.rgb(205, 82, 77)
    }

    private enum class State { MENU, SETTINGS, PLAYING, GAMEOVER }

    private val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
    private val birdSource = BitmapFactory.decodeResource(resources, R.drawable.player_bird)
    private val birdFrames = createBirdFrames(birdSource)
    private val spawner = PipeSpawner(
        designWidth = DESIGN_WIDTH,
        designHeight = DESIGN_HEIGHT,
        spacing = PIPE_SPACING,
        pipeWidth = PIPE_WIDTH,
        firstX = FIRST_PIPE_X,
        spawnTriggerDistance = SPAWN_TRIGGER_DISTANCE,
        minGapCenter = MIN_GAP_CENTER,
        maxGapCenter = MAX_GAP_CENTER,
        initialGapCenter = INITIAL_GAP_CENTER
    )
    private val pipes get() = spawner.pipes
    private val sounds = PremiumSoundEngine()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stonePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stoneHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val birdDst = RectF()
    private val birdCollision = RectF()
    private val topCollision = RectF()
    private val bottomCollision = RectF()
    private val scoreRect = RectF()
    private val playButton = RectF()
    private val secondaryButton = RectF()
    private val settingsButton = RectF()
    private val panelRect = RectF()
    private val newRecordRect = RectF()
    private val cityPath = Path()
    private val logoPath = Path()
    private val mountainPath = Path()
    private val snowPath = Path()
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boldTypeface = Typeface.create("sans-serif", Typeface.BOLD)
    private val normalTypeface = Typeface.create("sans-serif", Typeface.NORMAL)

    private val skyShader = LinearGradient(
        0f, 0f, 0f, DESIGN_HEIGHT,
        Color.rgb(25, 76, 111), Color.rgb(191, 220, 224), Shader.TileMode.CLAMP
    )
    private val stoneShader = LinearGradient(
        0f, 0f, PIPE_WIDTH, 0f,
        Color.rgb(185, 151, 116), Color.rgb(121, 91, 72), Shader.TileMode.CLAMP
    )
    private val glassShader = LinearGradient(
        0f, 0f, 0f, 180f,
        Color.argb(235, 255, 255, 255), Color.argb(205, 234, 241, 243), Shader.TileMode.CLAMP
    )
    private val choreographer = Choreographer.getInstance()

    private var state = State.MENU
    private var high = prefs.getInt("high", 0)
    private var muted = prefs.getBoolean("muted", false)
    private var hapticEnabled = prefs.getBoolean("haptic", true)
    private var score = 0
    private var birdY = 835f
    private var velocity = 0f
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private var safeTop = 0f
    private var safeBottom = 0f
    private var cloudOffset = 0f
    private var menuTime = 0f
    private var gameOverTime = 1f
    private var transitionTime = 1f
    private var buttonPressTime = 0f
    private var pressedButton = 0
    private var newRecordShown = false
    private var collisionFlash = 0f
    private var gameOverSoundPlayed = false

    private var frameCallbackPosted = false
    private var isRunning = false
    private var lastFrameTimeNanos = 0L
    private var debugFrameCount = 0L
    private var debugFrameSeconds = 0f
    private var debugMaxFrameSeconds = 0f
    private var debugCallbackExecutions = 0L

    private val particleX = FloatArray(18)
    private val particleY = FloatArray(18)
    private val particleVX = FloatArray(18)
    private val particleVY = FloatArray(18)
    private val particleLife = FloatArray(18)

    init {
        isFocusable = true
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        textPaint.typeface = boldTypeface
        shadowPaint.color = Color.argb(55, 4, 26, 39)
        stoneHighlightPaint.color = Color.argb(65, 255, 245, 224)
        iconPaint.style = Paint.Style.STROKE
        iconPaint.strokeWidth = 7f
        iconPaint.strokeCap = Paint.Cap.ROUND

        mountainPath.moveTo(0f, 790f)
        mountainPath.cubicTo(180f, 675f, 280f, 760f, 410f, 690f)
        mountainPath.lineTo(540f, 335f)
        mountainPath.cubicTo(655f, 650f, 740f, 720f, 860f, 650f)
        mountainPath.cubicTo(960f, 600f, 1020f, 700f, 1080f, 730f)
        mountainPath.lineTo(1080f, 1070f)
        mountainPath.lineTo(0f, 1070f)
        mountainPath.close()

        snowPath.moveTo(452f, 575f)
        snowPath.lineTo(540f, 335f)
        snowPath.lineTo(632f, 575f)
        snowPath.lineTo(585f, 536f)
        snowPath.lineTo(540f, 426f)
        snowPath.lineTo(501f, 520f)
        snowPath.close()

        cityPath.moveTo(0f, 1025f)
        cityPath.lineTo(80f, 965f)
        cityPath.lineTo(150f, 1025f)
        cityPath.lineTo(220f, 910f)
        cityPath.lineTo(300f, 1025f)
        cityPath.lineTo(385f, 945f)
        cityPath.lineTo(470f, 1025f)
        cityPath.lineTo(560f, 895f)
        cityPath.lineTo(650f, 1025f)
        cityPath.lineTo(740f, 930f)
        cityPath.lineTo(825f, 1025f)
        cityPath.lineTo(900f, 875f)
        cityPath.lineTo(980f, 1025f)
        cityPath.lineTo(1080f, 950f)
        cityPath.lineTo(1080f, 1120f)
        cityPath.lineTo(0f, 1120f)
        cityPath.close()

        logoPath.moveTo(470f, 292f)
        logoPath.lineTo(540f, 220f)
        logoPath.lineTo(610f, 292f)
        logoPath.moveTo(492f, 278f)
        logoPath.lineTo(540f, 245f)
        logoPath.lineTo(584f, 278f)

        startLoop()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startLoop()
    }

    override fun onDetachedFromWindow() {
        stopLoop()
        sounds.release()
        birdFrames.forEach { if (it !== birdSource && !it.isRecycled) it.recycle() }
        if (!birdSource.isRecycled) birdSource.recycle()
        super.onDetachedFromWindow()
    }

    fun pauseLoop() = stopLoop()

    fun resumeLoop() {
        lastFrameTimeNanos = 0L
        startLoop()
    }

    fun setSafeInsets(top: Int, bottom: Int) {
        safeTop = top.toFloat()
        safeBottom = bottom.toFloat()
        recalculateViewport(width, height)
        invalidate()
    }

    private fun startLoop() {
        if (isRunning) return
        isRunning = true
        lastFrameTimeNanos = 0L
        postFrameIfNeeded()
    }

    private fun stopLoop() {
        isRunning = false
        if (frameCallbackPosted) {
            choreographer.removeFrameCallback(this)
            frameCallbackPosted = false
        }
        lastFrameTimeNanos = 0L
    }

    private fun postFrameIfNeeded() {
        if (!isRunning || frameCallbackPosted) return
        frameCallbackPosted = true
        choreographer.postFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        frameCallbackPosted = false
        if (!isRunning) return

        val deltaSeconds = if (lastFrameTimeNanos == 0L) {
            0f
        } else {
            ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0f, 0.033f)
        }
        val rawSeconds = if (lastFrameTimeNanos == 0L) 0f else (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
        lastFrameTimeNanos = frameTimeNanos
        if (BuildConfig.DEBUG) {
            debugFrameCount++
            debugFrameSeconds += rawSeconds.coerceAtLeast(0f)
            debugMaxFrameSeconds = max(debugMaxFrameSeconds, rawSeconds)
            debugCallbackExecutions++
            if (debugFrameCount % 600L == 0L) {
                val avg = debugFrameSeconds / debugFrameCount
                val fps = if (avg > 0f) 1f / avg else 0f
                Log.d("ErciyesPerf", "avgFPS=%.1f maxFrame=%.1fms callbacks=%d pipes=%d lastSpawnTime=%.2fs lastSpawnX=%.1f".format(fps, debugMaxFrameSeconds * 1000f, debugCallbackExecutions, pipes.size, spawner.lastSpawnTimeSeconds, spawner.lastSpawnX))
            }
        }

        updateGame(deltaSeconds)
        invalidate()
        postFrameIfNeeded()
    }

    private fun updateGame(dt: Float) {
        menuTime += dt
        cloudOffset = (cloudOffset + dt * 11f) % 1400f
        buttonPressTime = max(0f, buttonPressTime - dt)
        collisionFlash = max(0f, collisionFlash - dt * 4f)

        when (state) {
            State.MENU, State.SETTINGS -> {
                transitionTime = min(1f, transitionTime + dt * 3f)
            }
            State.PLAYING -> {
                transitionTime = min(1f, transitionTime + dt * 5f)
                velocity += GRAVITY * dt
                birdY += velocity * dt
                spawner.update(dt, PIPE_SPEED)

                for (pipe in pipes) {
                    if (!pipe.passed && pipe.x + PIPE_WIDTH < 300f) {
                        if (spawner.markPassed(pipe)) {
                            score++
                            if (score > high) {
                                high = score
                                newRecordShown = true
                                prefs.edit().putInt("high", high).apply()
                            }
                            playSound(2)
                            haptic(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    }
                }

                if (checkCollision()) endGame()
                spawner.ensureFuturePipe()
            }
            State.GAMEOVER -> {
                gameOverTime = min(1f, gameOverTime + dt * 3.8f)
                updateParticles(dt)
            }
        }
    }

    private fun checkCollision(): Boolean {
        val birdCenterX = 300f
        val left = birdCenterX - BIRD_W * 0.34f
        val right = birdCenterX + BIRD_W * 0.34f
        val top = birdY - BIRD_H * 0.36f
        val bottom = birdY + BIRD_H * 0.36f
        birdCollision.set(left, top, right, bottom)

        if (top < 45f || bottom > PLAY_BOTTOM) return true

        val gapHalf = PIPE_GAP * 0.5f
        for (pipe in pipes) {
            topCollision.set(pipe.x, 0f, pipe.x + PIPE_WIDTH, pipe.gapCenter - gapHalf)
            bottomCollision.set(pipe.x, pipe.gapCenter + gapHalf, pipe.x + PIPE_WIDTH, PLAY_BOTTOM)
            if (RectF.intersects(birdCollision, topCollision) || RectF.intersects(birdCollision, bottomCollision)) return true
        }
        return false
    }

    private fun endGame() {
        if (state != State.PLAYING) return
        state = State.GAMEOVER
        gameOverTime = 0f
        collisionFlash = 1f
        gameOverSoundPlayed = false
        spawnCollisionParticles()
        playSound(4)
        haptic(HapticFeedbackConstants.LONG_PRESS)
        if (!gameOverSoundPlayed) {
            playSound(6)
            gameOverSoundPlayed = true
        }
    }

    private fun startGame() {
        state = State.PLAYING
        score = 0
        newRecordShown = false
        gameOverSoundPlayed = false
        gameOverTime = 0f
        transitionTime = 0f
        birdY = 835f
        velocity = 0f
        cloudOffset = 0f
        spawner.reset()
        resetParticles()
        playSound(5)
        haptic(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun goMenu() {
        state = State.MENU
        spawner.clear()
        resetParticles()
        transitionTime = 0f
        buttonPressTime = 0f
        pressedButton = 0
    }

    private fun recalculateViewport(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val usableHeight = (h.toFloat() - safeTop - safeBottom).coerceAtLeast(1f)
        scale = min(w / DESIGN_WIDTH, usableHeight / DESIGN_HEIGHT)
        offsetX = (w - DESIGN_WIDTH * scale) * 0.5f
        offsetY = safeTop + (usableHeight - DESIGN_HEIGHT * scale) * 0.5f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateViewport(w, h)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) return true
        val p = toDesignPoint(event.x, event.y)
        when (state) {
            State.MENU -> handleMenuTap(p.first, p.second)
            State.SETTINGS -> handleSettingsTap(p.first, p.second)
            State.PLAYING -> flap()
            State.GAMEOVER -> handleGameOverTap(p.first, p.second)
        }
        return true
    }

    private fun handleMenuTap(x: Float, y: Float) {
        if (playButton.contains(x, y)) {
            pressButton(1)
            startGame()
        } else if (settingsButton.contains(x, y)) {
            pressButton(3)
            state = State.SETTINGS
            playSound(0)
            haptic(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    private fun handleSettingsTap(x: Float, y: Float) {
        if (secondaryButton.contains(x, y)) {
            muted = !muted
            prefs.edit().putBoolean("muted", muted).apply()
            pressButton(2)
            playSound(0)
            haptic(HapticFeedbackConstants.VIRTUAL_KEY)
        } else if (playButton.contains(x, y)) {
            hapticEnabled = !hapticEnabled
            prefs.edit().putBoolean("haptic", hapticEnabled).apply()
            pressButton(1)
            playSound(0)
            haptic(HapticFeedbackConstants.VIRTUAL_KEY)
        } else if (settingsButton.contains(x, y)) {
            pressButton(3)
            goMenu()
        }
    }

    private fun handleGameOverTap(x: Float, y: Float) {
        if (playButton.contains(x, y)) {
            pressButton(1)
            startGame()
        } else if (secondaryButton.contains(x, y)) {
            pressButton(2)
            goMenu()
            playSound(0)
            haptic(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    private fun flap() {
        velocity = FLAP_VELOCITY
        playSound(1)
        haptic(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun pressButton(which: Int) {
        pressedButton = which
        buttonPressTime = 0.11f
        playSound(0)
        haptic(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun playSound(index: Int) {
        if (!muted) sounds.play(index)
    }

    private fun haptic(type: Int) {
        if (hapticEnabled && isHapticFeedbackEnabled) performHapticFeedback(type)
    }

    private fun toDesignPoint(x: Float, y: Float): Pair<Float, Float> {
        return ((x - offsetX) / scale) to ((y - offsetY) / scale)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(BackgroundColor)
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        drawWorld(canvas)
        when (state) {
            State.MENU -> drawMenu(canvas)
            State.SETTINGS -> drawSettings(canvas)
            State.PLAYING -> drawHud(canvas)
            State.GAMEOVER -> {
                drawHud(canvas)
                drawGameOver(canvas)
                drawParticles(canvas)
            }
        }
        canvas.restore()
    }

    private fun drawWorld(c: Canvas) {
        paint.shader = skyShader
        c.drawRect(0f, 0f, DESIGN_WIDTH, DESIGN_HEIGHT, paint)
        paint.shader = null
        drawClouds(c)
        paint.color = Color.rgb(74, 119, 147)
        c.drawPath(mountainPath, paint)
        paint.color = Color.argb(235, 242, 247, 246)
        c.drawPath(snowPath, paint)
        paint.color = Color.rgb(47, 75, 89)
        c.drawPath(cityPath, paint)
        drawCityDetails(c)
        drawPipes(c)
        drawGround(c)
        if (state == State.PLAYING || state == State.GAMEOVER) drawBird(c, false)
    }

    private fun drawClouds(c: Canvas) {
        drawCloud(c, 115f - cloudOffset, 230f, 125f)
        drawCloud(c, 720f - cloudOffset * 0.56f, 345f, 160f)
        drawCloud(c, 1250f - cloudOffset * 0.35f, 150f, 95f)
        drawCloud(c, 1760f - cloudOffset * 0.44f, 270f, 135f)
    }

    private fun drawCloud(c: Canvas, x: Float, y: Float, size: Float) {
        val xx = ((x + 1550f) % 1550f) - 220f
        cloudPaint.color = Color.argb(145, 250, 255, 255)
        c.drawCircle(xx, y, size * .31f, cloudPaint)
        c.drawCircle(xx + size * .30f, y - size * .10f, size * .40f, cloudPaint)
        c.drawCircle(xx + size * .61f, y, size * .28f, cloudPaint)
        c.drawRoundRect(xx - size * .30f, y, xx + size * .76f, y + size * .22f, size * .10f, size * .10f, cloudPaint)
    }

    private fun drawCityDetails(c: Canvas) {
        paint.color = Color.argb(105, 229, 205, 169)
        c.drawRoundRect(445f, 895f, 635f, 1035f, 22f, 22f, paint)
        c.drawOval(462f, 790f, 618f, 930f, paint)
        c.drawRect(486f, 755f, 506f, 1035f, paint)
        c.drawRect(574f, 755f, 594f, 1035f, paint)
        c.drawCircle(496f, 745f, 11f, paint)
        c.drawCircle(584f, 745f, 11f, paint)
        paint.color = Color.argb(120, 229, 205, 169)
        c.drawRoundRect(74f, 920f, 164f, 1035f, 12f, 12f, paint)
        c.drawCircle(119f, 904f, 35f, paint)
    }

    private fun drawPipes(c: Canvas) {
        val gapHalf = PIPE_GAP * .5f
        for (pipe in pipes) {
            drawTower(c, pipe.x, 0f, pipe.gapCenter - gapHalf, true)
            drawTower(c, pipe.x, pipe.gapCenter + gapHalf, PLAY_BOTTOM, false)
        }
    }

    private fun drawTower(c: Canvas, x: Float, top: Float, bottom: Float, topCap: Boolean) {
        if (bottom <= top) return
        stonePaint.shader = stoneShader
        c.drawRoundRect(x, top, x + PIPE_WIDTH, bottom, 18f, 18f, stonePaint)
        stonePaint.shader = null
        stoneHighlightPaint.color = Color.argb(70, 255, 240, 210)
        c.drawRoundRect(x + 16f, top + 8f, x + 43f, bottom - 8f, 12f, 12f, stoneHighlightPaint)
        val capH = 64f
        val left = x - 18f
        val right = x + PIPE_WIDTH + 18f
        if (topCap) {
            drawTowerCap(c, left, bottom - capH, right, bottom)
        } else {
            drawTowerCap(c, left, top, right, top + capH)
        }
    }

    private fun drawTowerCap(c: Canvas, l: Float, t: Float, r: Float, b: Float) {
        paint.color = Color.rgb(204, 171, 132)
        c.drawRoundRect(l, t, r, b, 18f, 18f, paint)
        paint.color = Color.argb(85, 255, 247, 228)
        c.drawRoundRect(l + 8f, t + 7f, r - 8f, t + 18f, 8f, 8f, paint)
        paint.color = Color.argb(40, 63, 43, 32)
        c.drawRoundRect(l + 4f, b - 12f, r - 4f, b - 4f, 5f, 5f, paint)
    }

    private fun drawGround(c: Canvas) {
        paint.color = Color.rgb(45, 102, 76)
        c.drawRect(0f, PLAY_BOTTOM, DESIGN_WIDTH, PLAY_BOTTOM + 38f, paint)
        paint.color = Color.rgb(83, 147, 96)
        c.drawRect(0f, PLAY_BOTTOM, DESIGN_WIDTH, PLAY_BOTTOM + 7f, paint)
        paint.color = Color.rgb(207, 174, 124)
        c.drawRect(0f, PLAY_BOTTOM + 38f, DESIGN_WIDTH, DESIGN_HEIGHT, paint)
        paint.color = Color.argb(65, 115, 76, 43)
        var x = 35f
        while (x < DESIGN_WIDTH) {
            c.drawCircle(x, PLAY_BOTTOM + 82f, 4f, paint)
            c.drawCircle(x + 55f, PLAY_BOTTOM + 132f, 3f, paint)
            x += 135f
        }
    }

    private fun drawBird(c: Canvas, menu: Boolean) {
        val centerX = if (menu) MENU_BIRD_X else 300f
        val baseY = if (menu) MENU_BIRD_Y else birdY
        val bob = if (menu) sin(menuTime * 2.0f) * 12f else 0f
        val frame = birdFrames[((menuTime * 9f).toInt() % birdFrames.size + birdFrames.size) % birdFrames.size]
        val flapScale = 1f + if (!menu) sin(menuTime * 18f) * .018f else sin(menuTime * 4f) * .015f
        birdDst.set(centerX - BIRD_W * .5f, baseY + bob - BIRD_H * .5f, centerX + BIRD_W * .5f, baseY + bob + BIRD_H * .5f)
        val cx = birdDst.centerX()
        val cy = birdDst.centerY()
        val angle = if (menu) sin(menuTime * 1.8f) * 5f else (velocity / 24f).coerceIn(-24f, 25f)
        c.save()
        c.rotate(angle, cx, cy)
        c.scale(1f, flapScale, cx, cy)
        shadowPaint.alpha = 42
        c.drawOval(birdDst.left + 12f, birdDst.bottom - 3f, birdDst.right - 8f, birdDst.bottom + 15f, shadowPaint)
        c.drawBitmap(frame, null, birdDst, paint)
        c.restore()
    }

    private fun drawHud(c: Canvas) {
        scoreRect.set(405f, 48f, 675f, 182f)
        shadowPaint.alpha = 52
        c.drawRoundRect(scoreRect.left + 7f, scoreRect.top + 10f, scoreRect.right + 7f, scoreRect.bottom + 10f, 42f, 42f, shadowPaint)
        glassPaint.shader = glassShader
        c.drawRoundRect(scoreRect, 42f, 42f, glassPaint)
        glassPaint.shader = null
        textPaint.color = PrimaryColor
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 86f
        c.drawText(score.toString(), 540f, 138f, textPaint)
        if (newRecordShown && score > 0) {
            textPaint.color = AccentColor
            textPaint.textSize = 24f
            c.drawText("YENİ REKOR", 540f, 170f, textPaint)
        }
    }

    private fun drawMenu(c: Canvas) {
        drawMenuBackdrop(c)
        drawLogo(c)
        drawBird(c, true)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.argb(210, 240, 249, 250)
        textPaint.textSize = 30f
        textPaint.typeface = normalTypeface
        c.drawText("KAYSERİ'NİN ÜZERİNDE UÇ", 540f, 920f, textPaint)

        playButton.set(265f, 1050f, 815f, 1200f)
        secondaryButton.set(345f, 1230f, 735f, 1345f)
        settingsButton.set(455f, 1400f, 625f, 1510f)
        drawPremiumButton(c, playButton, "OYNA", true, pressedButton == 1)
        drawPremiumButton(c, secondaryButton, mutedIconLabel(), false, pressedButton == 2)
        drawGearButton(c, settingsButton, pressedButton == 3)

        textPaint.color = Color.argb(210, 246, 250, 250)
        textPaint.textSize = 27f
        textPaint.typeface = normalTypeface
        c.drawText("EN YÜKSEK SKOR  $high", 540f, 1580f, textPaint)
    }

    private fun drawMenuBackdrop(c: Canvas) {
        paint.color = Color.argb(55, 3, 26, 43)
        c.drawRect(0f, 0f, DESIGN_WIDTH, DESIGN_HEIGHT, paint)
    }

    private fun drawLogo(c: Canvas) {
        val alpha = (255f * min(1f, menuTime * 1.8f)).toInt()
        iconPaint.alpha = alpha
        iconPaint.color = Color.WHITE
        iconPaint.strokeWidth = 9f
        c.drawPath(logoPath, iconPaint)
        c.drawArc(446f, 245f, 518f, 315f, 205f, 105f, false, iconPaint)
        c.drawArc(562f, 245f, 634f, 315f, 230f, 105f, false, iconPaint)
        textPaint.alpha = alpha
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = boldTypeface
        textPaint.color = Color.WHITE
        textPaint.textSize = 104f
        c.drawText("ERCİYES", 540f, 375f, textPaint)
        textPaint.textSize = 62f
        textPaint.typeface = normalTypeface
        textPaint.color = AccentColor
        c.drawText("UÇUŞU", 540f, 448f, textPaint)
        paint.color = Color.argb(150, 255, 255, 255)
        c.drawRoundRect(435f, 482f, 645f, 489f, 4f, 4f, paint)
        textPaint.alpha = 255
        iconPaint.alpha = 255
    }

    private fun mutedIconLabel() = if (muted) "SES KAPALI" else "SES AÇIK"

    private fun drawPremiumButton(c: Canvas, rect: RectF, label: String, primary: Boolean, pressed: Boolean) {
        val s = if (pressed && buttonPressTime > 0f) .96f else 1f
        val cx = rect.centerX()
        val cy = rect.centerY()
        c.save()
        c.scale(s, s, cx, cy)
        shadowPaint.alpha = if (primary) 68 else 42
        c.drawRoundRect(rect.left + 6f, rect.top + 10f, rect.right + 6f, rect.bottom + 10f, 42f, 42f, shadowPaint)
        paint.color = if (primary) AccentColor else Color.argb(210, 245, 249, 249)
        c.drawRoundRect(rect, 42f, 42f, paint)
        paint.color = if (primary) Color.argb(110, 255, 255, 255) else Color.argb(70, 255, 255, 255)
        c.drawRoundRect(rect.left + 7f, rect.top + 7f, rect.right - 7f, rect.top + 28f, 16f, 16f, paint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = if (primary) 48f else 30f
        textPaint.typeface = boldTypeface
        textPaint.color = if (primary) PrimaryColor else TextPrimaryColor
        c.drawText(label, cx, cy + textPaint.textSize * .34f, textPaint)
        c.restore()
    }

    private fun drawGearButton(c: Canvas, rect: RectF, pressed: Boolean) {
        shadowPaint.alpha = 42
        c.drawCircle(rect.centerX() + 5f, rect.centerY() + 8f, 48f, shadowPaint)
        paint.color = Color.argb(215, 245, 249, 249)
        c.drawCircle(rect.centerX(), rect.centerY(), 48f, paint)
        iconPaint.color = PrimaryColor
        val cx = rect.centerX(); val cy = rect.centerY()
        c.drawCircle(cx, cy, 18f, iconPaint)
        c.drawCircle(cx, cy, 6f, iconPaint)
        for (i in 0 until 8) {
            val a = i * (Math.PI.toFloat() / 4f)
            c.drawLine(cx + cos(a) * 24f, cy + sin(a) * 24f, cx + cos(a) * 32f, cy + sin(a) * 32f, iconPaint)
        }
    }

    private fun drawSettings(c: Canvas) {
        drawMenuBackdrop(c)
        drawLogo(c)
        panelRect.set(170f, 650f, 910f, 1390f)
        shadowPaint.alpha = 70
        c.drawRoundRect(panelRect.left + 8f, panelRect.top + 14f, panelRect.right + 8f, panelRect.bottom + 14f, 48f, 48f, shadowPaint)
        paint.color = SurfaceColor
        c.drawRoundRect(panelRect, 48f, 48f, paint)
        textPaint.color = TextPrimaryColor
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = boldTypeface
        textPaint.textSize = 52f
        c.drawText("AYARLAR", 540f, 760f, textPaint)

        playButton.set(275f, 860f, 805f, 1010f)
        secondaryButton.set(275f, 1050f, 805f, 1200f)
        settingsButton.set(455f, 1250f, 625f, 1355f)
        drawPremiumButton(c, playButton, if (hapticEnabled) "TİTREŞİM AÇIK" else "TİTREŞİM KAPALI", true, pressedButton == 1)
        drawPremiumButton(c, secondaryButton, if (muted) "SES KAPALI" else "SES AÇIK", false, pressedButton == 2)
        drawPremiumButton(c, settingsButton, "GERİ", false, pressedButton == 3)
    }

    private fun drawGameOver(c: Canvas) {
        val alpha = (190f * min(1f, gameOverTime * 2.4f)).toInt()
        paint.color = Color.argb(alpha, 5, 22, 35)
        c.drawRect(0f, 0f, DESIGN_WIDTH, DESIGN_HEIGHT, paint)

        val cardW = 780f
        val cardH = 900f
        val left = (DESIGN_WIDTH - cardW) * .5f
        val top = (DESIGN_HEIGHT - cardH) * .5f
        val ease = 1f - (1f - min(1f, gameOverTime * 1.8f)) * (1f - min(1f, gameOverTime * 1.8f))
        val cardScale = .94f + .06f * ease
        c.save()
        c.scale(cardScale, cardScale, 540f, 960f)
        panelRect.set(left, top, left + cardW, top + cardH)
        shadowPaint.alpha = 80
        c.drawRoundRect(panelRect.left + 10f, panelRect.top + 16f, panelRect.right + 10f, panelRect.bottom + 16f, 54f, 54f, shadowPaint)
        paint.color = SurfaceColor
        c.drawRoundRect(panelRect, 54f, 54f, paint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = boldTypeface
        textPaint.color = TextPrimaryColor
        textPaint.textSize = 62f
        c.drawText("OYUN BİTTİ", 540f, top + 125f, textPaint)
        textPaint.typeface = normalTypeface
        textPaint.color = TextSecondaryColor
        textPaint.textSize = 28f
        c.drawText("Uçuş tamamlandı", 540f, top + 172f, textPaint)

        textPaint.typeface = boldTypeface
        textPaint.color = PrimaryColor
        textPaint.textSize = 118f
        c.drawText(score.toString(), 540f, top + 335f, textPaint)
        textPaint.typeface = normalTypeface
        textPaint.color = TextSecondaryColor
        textPaint.textSize = 27f
        c.drawText("SKOR", 540f, top + 380f, textPaint)

        drawScoreLine(c, top + 465f, "EN YÜKSEK", high.toString())
        if (newRecordShown && score > 0) drawRecordBadge(c, top + 560f)

        playButton.set(left + 105f, top + 635f, left + cardW - 105f, top + 755f)
        secondaryButton.set(left + 150f, top + 785f, left + cardW - 150f, top + 880f)
        drawPremiumButton(c, playButton, "TEKRAR OYNA", true, pressedButton == 1)
        drawPremiumButton(c, secondaryButton, "ANA MENÜ", false, pressedButton == 2)
        c.restore()
    }

    private fun drawScoreLine(c: Canvas, y: Float, label: String, value: String) {
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = TextSecondaryColor
        textPaint.textSize = 28f
        textPaint.typeface = normalTypeface
        c.drawText(label, 320f, y, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.typeface = boldTypeface
        textPaint.color = TextPrimaryColor
        c.drawText(value, 760f, y, textPaint)
    }

    private fun drawRecordBadge(c: Canvas, y: Float) {
        newRecordRect.set(350f, y - 38f, 730f, y + 28f)
        paint.color = Color.argb(35, 239, 178, 70)
        c.drawRoundRect(newRecordRect, 32f, 32f, paint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = boldTypeface
        textPaint.textSize = 25f
        textPaint.color = Color.rgb(170, 117, 35)
        c.drawText("✦  YENİ REKOR  ✦", 540f, y + 9f, textPaint)
    }

    private fun createBirdFrames(source: Bitmap): Array<Bitmap> {
        val result = ArrayList<Bitmap>(3)
        val scales = floatArrayOf(.96f, 1f, 1.04f)
        for (s in scales) {
            val matrix = Matrix().apply { setScale(1f, s, source.width / 2f, source.height / 2f) }
            result += Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }
        return result.toTypedArray()
    }

    private fun resetParticles() {
        for (i in particleLife.indices) particleLife[i] = 0f
    }

    private fun spawnCollisionParticles() {
        for (i in particleLife.indices) {
            val angle = i * (Math.PI.toFloat() * 2f / particleLife.size)
            val speed = 180f + (i % 5) * 35f
            particleX[i] = 300f
            particleY[i] = birdY
            particleVX[i] = cos(angle) * speed
            particleVY[i] = sin(angle) * speed
            particleLife[i] = .65f
        }
    }

    private fun updateParticles(dt: Float) {
        for (i in particleLife.indices) {
            if (particleLife[i] <= 0f) continue
            particleLife[i] -= dt
            particleX[i] += particleVX[i] * dt
            particleY[i] += particleVY[i] * dt
            particleVY[i] += 380f * dt
        }
    }

    private fun drawParticles(c: Canvas) {
        for (i in particleLife.indices) {
            val life = particleLife[i]
            if (life <= 0f) continue
            paint.color = Color.argb((255f * (life / .65f)).toInt().coerceIn(0, 255), 239, 178, 70)
            c.drawCircle(particleX[i], particleY[i], 6f + i % 3, paint)
        }
        if (collisionFlash > 0f) {
            paint.color = Color.argb((70f * collisionFlash).toInt(), 255, 255, 255)
            c.drawCircle(300f, birdY, 95f * collisionFlash, paint)
        }
    }
}
