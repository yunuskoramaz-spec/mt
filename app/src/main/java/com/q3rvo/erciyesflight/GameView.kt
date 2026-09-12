package com.q3rvo.erciyesflight

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.random.Random

class GameView(context: Context) : View(context) {
    private val bg = BitmapFactory.decodeStream(context.assets.open("background.png"))
    private val bird = BitmapFactory.decodeStream(context.assets.open("bird.png"))
    private val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var high = prefs.getInt("high", 0)
    private var state = State.MENU
    private var score = 0
    private var y = 0f
    private var vy = 0f
    private var spawn = 0f
    private var lastNs = System.nanoTime()
    private var muted = false
    private val pipes = mutableListOf<Pipe>()

    private enum class State { MENU, PLAYING, GAMEOVER }
    private data class Pipe(var x: Float, val gapY: Float, var passed: Boolean = false)

    override fun onDraw(canvas: Canvas) {
        val dt = min((System.nanoTime() - lastNs) / 1_000_000_000f, 0.033f)
        lastNs = System.nanoTime()
        drawBackground(canvas)
        when (state) {
            State.MENU -> drawMenu(canvas)
            State.PLAYING -> { update(dt); drawGame(canvas) }
            State.GAMEOVER -> { drawGame(canvas); drawGameOver(canvas) }
        }
        postInvalidateOnAnimation()
    }

    private fun drawBackground(c: Canvas) {
        c.drawBitmap(bg, null, Rect(0, 0, width, height), paint)
        paint.color = Color.argb(45, 255, 255, 255)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun drawMenu(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        paint.color = Color.argb(150, 0, 0, 0); c.drawRect(0f, 0f, w, h, paint)
        text(c, "ERCİYES UÇUŞU", w/2, h*.22f, h*.075f, true)
        text(c, "Kayseri'nin üzerinde ne kadar uçabilirsin?", w/2, h*.29f, h*.031f, false)
        button(c, h*.48f, h*.60f, "OYUNA BAŞLA")
        button(c, h*.64f, h*.74f, if (muted) "🔇 SES KAPALI" else "🔊 SES AÇIK")
        text(c, "EN YÜKSEK SKOR  $high", w/2, h*.84f, h*.031f, true)
    }

    private fun button(c: Canvas, top: Float, bottom: Float, label: String) {
        val w = width.toFloat(); val l = w*.2f; val r = w*.8f
        paint.color = Color.rgb(255, 193, 7); c.drawRoundRect(l, top, r, bottom, 28f, 28f, paint)
        paint.color = Color.WHITE; paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f
        c.drawRoundRect(l, top, r, bottom, 28f, 28f, paint); paint.style = Paint.Style.FILL
        text(c, label, w/2, (top+bottom)/2 + 11f, (bottom-top)*.34f, true)
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, bold: Boolean) {
        paint.color = Color.WHITE; paint.textAlign = Paint.Align.CENTER; paint.textSize = size
        paint.typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        c.drawText(s, x, y, paint)
    }

    private fun update(dt: Float) {
        val w = width.toFloat(); val h = height.toFloat()
        vy += h*1.25f*dt; y += vy*dt
        spawn -= dt
        if (spawn <= 0f) {
            spawn = 1.45f
            val minY = h*.23f; val maxY = h*.61f
            pipes += Pipe(w+80f, Random.nextFloat()*(maxY-minY)+minY)
        }
        val speed = w*.42f + score*w*.006f
        pipes.forEach { p ->
            p.x -= speed*dt
            if (!p.passed && p.x < w*.18f) { p.passed = true; score++; saveHigh() }
        }
        pipes.removeAll { it.x < -140f }

        val bw = w*.18f; val bh = h*.10f
        val bx = w*.19f
        val br = RectF(bx-bw/2, y-bh/2, bx+bw/2, y+bh/2)
        if (y < h*.035f || y > h*.82f || pipes.any { p ->
                val gap = h*.135f
                RectF.intersects(br, RectF(p.x, 0f, p.x+70f, p.gapY-gap)) ||
                RectF.intersects(br, RectF(p.x, p.gapY+gap, p.x+70f, h*.84f))
            }) state = State.GAMEOVER
    }

    private fun saveHigh() {
        if (score > high) { high = score; prefs.edit().putInt("high", high).apply() }
    }

    private fun drawGame(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val pw = 70f
        pipes.forEach { p ->
            val gap = h*.135f
            paint.color = Color.rgb(48, 220, 35)
            c.drawRect(p.x, 0f, p.x+pw, p.gapY-gap, paint)
            c.drawRect(p.x, p.gapY+gap, p.x+pw, h*.84f, paint)
            paint.color = Color.rgb(85, 240, 40)
            c.drawRect(p.x-12f, p.gapY-gap-20f, p.x+pw+12f, p.gapY-gap, paint)
            c.drawRect(p.x-12f, p.gapY+gap, p.x+pw+12f, p.gapY+gap+20f, paint)
        }
        val bw = w*.22f; val bh = h*.145f; val bx = w*.19f
        c.drawBitmap(bird, null, RectF(bx-bw/2, y-bh/2, bx+bw/2, y+bh/2), paint)
        text(c, score.toString(), w/2, h*.105f, h*.075f, true)
    }

    private fun drawGameOver(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        paint.color = Color.argb(165, 0, 0, 0); c.drawRect(0f, 0f, w, h, paint)
        text(c, "OYUN BİTTİ", w/2, h*.30f, h*.07f, true)
        text(c, "Skor: $score   Rekor: $high", w/2, h*.39f, h*.035f, false)
        button(c, h*.50f, h*.61f, "TEKRAR OYNA")
        button(c, h*.65f, h*.75f, "ANA MENÜ")
    }

    private fun start() {
        state = State.PLAYING; score = 0; pipes.clear(); spawn = .2f; y = height*.45f; vy = 0f
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_DOWN) return true
        val h = height.toFloat()
        when (state) {
            State.MENU -> when {
                e.y in h*.48f..h*.60f -> start()
                e.y in h*.64f..h*.74f -> muted = !muted
            }
            State.PLAYING -> vy = -h*.48f
            State.GAMEOVER -> when {
                e.y in h*.50f..h*.61f -> start()
                e.y in h*.65f..h*.75f -> state = State.MENU
            }
        }
        return true
    }
}
