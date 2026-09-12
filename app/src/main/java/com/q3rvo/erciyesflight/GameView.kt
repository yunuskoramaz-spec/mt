package com.q3rvo.erciyesflight

import android.content.Context
import android.graphics.*
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameView(context: Context) : View(context) {
    private val bird = BitmapFactory.decodeResource(resources, R.drawable.player_bird)
    private val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
    private var high = prefs.getInt("high", 0)
    private var muted = prefs.getBoolean("muted", false)
    private var state = State.MENU
    private var score = 0
    private var birdY = 0f
    private var velocity = 0f
    private var spawnTimer = 0f
    private var lastTimeNs = System.nanoTime()
    private var newRecordShown = false

    private var gravity = 0f
    private var flapVelocity = 0f
    private var pipeSpeed = 0f
    private var pipeGap = 0f
    private var birdWidth = 0f
    private var birdHeight = 0f
    private var pipeWidth = 0f
    private var playBottom = 0f
    private var birdX = 0f

    private val pipes = mutableListOf<Pipe>()
    private enum class State { MENU, PLAYING, GAMEOVER }
    private data class Pipe(var x: Float, val gapTop: Float, val gapBottom: Float, var passed: Boolean = false)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        val density = resources.displayMetrics.density
        val minBird = min(w * 0.18f, 64f * density)
        val maxBird = min(w * 0.24f, 96f * density)
        birdWidth = max(minBird, maxBird)
        val ratio = if (bird.width > 0) bird.height.toFloat() / bird.width.toFloat() else 0.67f
        birdHeight = birdWidth * ratio
        pipeWidth = w * 0.13f
        playBottom = h * 0.88f
        pipeGap = max(birdHeight * 3f, h * 0.24f)
        gravity = h * 1.10f
        flapVelocity = -h * 0.40f
        pipeSpeed = w * 0.40f
        birdX = w * 0.20f
        if (state == State.MENU) birdY = h * 0.43f
    }

    override fun onDetachedFromWindow() {
        tone.release()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        val dt = min((now - lastTimeNs) / 1_000_000_000f, 0.033f)
        lastTimeNs = now
        canvas.save()
        try {
            drawBackground(canvas)
            when (state) {
                State.MENU -> drawMenu(canvas)
                State.PLAYING -> { update(dt); drawGame(canvas) }
                State.GAMEOVER -> { drawGame(canvas); drawGameOver(canvas) }
            }
        } finally {
            canvas.restore()
        }
        postInvalidateOnAnimation()
    }

    private fun drawBackground(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        paint.shader = LinearGradient(0f, 0f, 0f, h * .72f, Color.rgb(55,180,245), Color.rgb(205,225,235), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint); paint.shader = null
        drawCloud(c, w*.12f, h*.13f, w*.13f); drawCloud(c, w*.83f, h*.19f, w*.15f); drawCloud(c, w*.28f, h*.27f, w*.10f)
        val m = Path().apply { moveTo(0f,h*.45f); lineTo(w*.22f,h*.38f); lineTo(w*.40f,h*.43f); lineTo(w*.52f,h*.27f); lineTo(w*.64f,h*.42f); lineTo(w*.80f,h*.34f); lineTo(w,h*.44f); lineTo(w,h*.62f); lineTo(0f,h*.62f); close() }
        paint.color = Color.rgb(78,150,215); c.drawPath(m,paint)
        val s = Path().apply { moveTo(w*.37f,h*.42f); lineTo(w*.52f,h*.27f); lineTo(w*.63f,h*.43f); lineTo(w*.56f,h*.39f); lineTo(w*.51f,h*.33f); lineTo(w*.46f,h*.40f); close() }
        paint.color = Color.WHITE; c.drawPath(s,paint); drawCity(c,w,h)
        paint.color = Color.rgb(45,150,62); c.drawRect(0f,h*.76f,w,h*.82f,paint)
        for (x in -30..w.toInt() step 70) { paint.color = Color.rgb(85,210,65); c.drawCircle(x.toFloat(),h*.79f,42f,paint) }
        paint.color = Color.rgb(75,185,55); c.drawRect(0f,h*.82f,w,h*.845f,paint)
        paint.color = Color.rgb(242,211,150); c.drawRect(0f,h*.845f,w,h,paint)
        paint.color = Color.rgb(35,105,40); c.drawRect(0f,h*.82f,w,h*.835f,paint)
    }

    private fun drawCloud(c:Canvas,x:Float,y:Float,s:Float){paint.color=Color.argb(235,255,255,255);c.drawCircle(x,y,s*.34f,paint);c.drawCircle(x+s*.28f,y-s*.08f,s*.42f,paint);c.drawCircle(x+s*.58f,y,s*.30f,paint);c.drawRoundRect(x-s*.35f,y,x+s*.78f,y+s*.25f,s*.12f,s*.12f,paint)}
    private fun drawCity(c:Canvas,w:Float,h:Float){val base=h*.74f;var x=0f;val rnd=Random(12);while(x<w){val bw=(32+rnd.nextInt(40)).toFloat();val bh=(70+rnd.nextInt(130)).toFloat();paint.color=Color.rgb(215,205,185);c.drawRect(x,base-bh,x+bw,base,paint);paint.color=Color.rgb(105,150,170);var wx=x+8f;while(wx<x+bw-5f){var wy=base-bh+14f;while(wy<base-10f){if(rnd.nextFloat()>.3f)c.drawRect(wx,wy,wx+7f,wy+10f,paint);wy+=24f};wx+=16f};x+=bw+6};paint.color=Color.rgb(205,195,175);c.drawRect(w*.40f,base-h*.12f,w*.62f,base,paint);c.drawOval(w*.41f,base-h*.25f,w*.61f,base-h*.02f,paint);for(mx in listOf(w*.36f,w*.64f)){paint.color=Color.rgb(195,185,165);c.drawRect(mx,base-h*.30f,mx+18f,base,paint);val p=Path().apply{moveTo(mx-8f,base-h*.30f);lineTo(mx+9f,base-h*.37f);lineTo(mx+26f,base-h*.30f);close()};c.drawPath(p,paint)};paint.color=Color.rgb(135,115,95);c.drawRect(w*.07f,base-h*.12f,w*.16f,base,paint);val tp=Path().apply{moveTo(w*.06f,base-h*.12f);lineTo(w*.115f,base-h*.24f);lineTo(w*.17f,base-h*.12f);close()};c.drawPath(tp,paint);paint.color=Color.rgb(242,238,220);c.drawCircle(w*.115f,base-h*.18f,22f,paint);paint.color=Color.DKGRAY;c.drawLine(w*.115f,base-h*.18f,w*.115f,base-h*.205f,paint);c.drawLine(w*.115f,base-h*.18f,w*.14f,base-h*.17f,paint);paint.color=Color.rgb(150,130,105);c.drawRect(w*.76f,base-h*.20f,w*.96f,base,paint);var xx=w*.76f;while(xx<w*.96f){c.drawRect(xx,base-h*.24f,xx+22f,base-h*.20f,paint);xx+=36f}}

    private fun drawMenu(c:Canvas){val w=width.toFloat();val h=height.toFloat();paint.color=Color.argb(120,0,25,55);c.drawRect(0f,0f,w,h,paint);text(c,"ERCİYES UÇUŞU",w/2,h*.22f,h*.065f,true);text(c,"Kayseri'nin üzerinde ne kadar uçabilirsin?",w/2,h*.29f,h*.027f,false);drawButton(c,h*.46f,h*.56f,"OYUNA BAŞLA");drawButton(c,h*.60f,h*.69f,if(muted)"SESİ AÇ" else "SESİ KAPAT");text(c,"EN YÜKSEK SKOR  $high",w/2,h*.79f,h*.028f,true);text(c,"Ekrana dokun ve uç!",w/2,h*.88f,h*.023f,false)}
    private fun drawButton(c:Canvas,top:Float,bottom:Float,label:String){val w=width.toFloat();val l=w*.18f;val r=w*.82f;paint.color=Color.rgb(255,193,7);c.drawRoundRect(l,top,r,bottom,32f,32f,paint);paint.color=Color.WHITE;paint.style=Paint.Style.STROKE;paint.strokeWidth=3f;c.drawRoundRect(l,top,r,bottom,32f,32f,paint);paint.style=Paint.Style.FILL;text(c,label,w/2,(top+bottom)/2+(bottom-top)*.13f,(bottom-top)*.30f,true)}
    private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,bold:Boolean){paint.color=Color.WHITE;paint.textAlign=Paint.Align.CENTER;paint.textSize=size;paint.typeface=Typeface.create(Typeface.DEFAULT,if(bold)Typeface.BOLD else Typeface.NORMAL);c.drawText(s,x,y,paint)}

    private fun update(dt:Float){
        val w=width.toFloat(); val h=height.toFloat()
        velocity += gravity*dt; birdY += velocity*dt; spawnTimer -= dt
        if(spawnTimer<=0f){
            val first=pipes.isEmpty()
            spawnTimer=if(first)1.20f else max(.95f,1.40f-score*.010f)
            val center=if(first) birdY else Random.nextFloat()*(h*.56f-h*.34f)+h*.34f
            pipes += Pipe(if(first)w*1.30f else w*1.08f,center-pipeGap/2f,center+pipeGap/2f)
        }
        pipeSpeed=w*(.40f+min(score,40)*.004f)
        pipes.forEach{p->
            p.x-=pipeSpeed*dt
            if(!p.passed&&p.x+pipeWidth<birdX-birdWidth*.50f){
                p.passed=true;score++
                if(score>high){high=score;newRecordShown=true;prefs.edit().putInt("high",high).apply()}
                play(ToneGenerator.TONE_PROP_ACK)
            }
        }
        pipes.removeAll{it.x+pipeWidth<-pipeWidth}

        val birdHitbox=RectF(birdX-birdWidth*.35f,birdY-birdHeight*.35f,birdX+birdWidth*.35f,birdY+birdHeight*.35f)
        val hitPipe=pipes.any{p->
            val topPipeHitbox=RectF(p.x,0f,p.x+pipeWidth,p.gapTop)
            val bottomPipeHitbox=RectF(p.x,p.gapBottom,p.x+pipeWidth,playBottom)
            RectF.intersects(birdHitbox,topPipeHitbox)||RectF.intersects(birdHitbox,bottomPipeHitbox)
        }
        val hitBounds=birdHitbox.top<0f||birdHitbox.bottom>playBottom
        if(hitBounds||hitPipe){state=State.GAMEOVER;play(ToneGenerator.TONE_PROP_NACK)}
    }

    private fun play(id:Int){if(!muted)tone.startTone(id,90)}

    private fun drawGame(c:Canvas){
        val w=width.toFloat();val h=height.toFloat()
        pipes.forEach{p->drawPipe(c,p.x,0f,p.gapTop,true);drawPipe(c,p.x,p.gapBottom,playBottom,false)}
        val dst=RectF(birdX-birdWidth/2f,birdY-birdHeight/2f,birdX+birdWidth/2f,birdY+birdHeight/2f)
        val angle=min(25f,max(-25f,velocity/h*80f))
        c.save();try{c.rotate(angle,birdX,birdY);c.drawBitmap(bird,null,dst,paint)}finally{c.restore()}
        text(c,score.toString(),w/2,h*.095f,h*.065f,true)
    }

    private fun drawPipe(c:Canvas,x:Float,top:Float,bottom:Float,isTop:Boolean){
        if(bottom<=top)return
        paint.color=Color.rgb(54,205,42);c.drawRect(x,top,x+pipeWidth,bottom,paint)
        paint.color=Color.rgb(98,238,55);c.drawRect(x+pipeWidth*.16f,top,x+pipeWidth*.27f,bottom,paint)
        paint.color=Color.rgb(25,145,24);c.drawRect(x+pipeWidth*.82f,top,x+pipeWidth,bottom,paint)
        val capH=min(pipeWidth*.22f,bottom-top);val l=x-pipeWidth*.10f;val r=x+pipeWidth*1.10f
        val capTop=if(isTop)bottom-capH else top;val capBottom=if(isTop)bottom else top+capH
        paint.color=Color.rgb(67,224,45);c.drawRect(l,capTop,r,capBottom,paint)
        paint.color=Color.rgb(20,110,20);paint.style=Paint.Style.STROKE;paint.strokeWidth=3f;c.drawRect(l,capTop,r,capBottom,paint);paint.style=Paint.Style.FILL
    }

    private fun drawGameOver(c:Canvas){val w=width.toFloat();val h=height.toFloat();paint.color=Color.argb(170,0,20,35);c.drawRect(0f,0f,w,h,paint);text(c,"OYUN BİTTİ",w/2,h*.29f,h*.062f,true);text(c,"Skor: $score",w/2,h*.38f,h*.033f,false);text(c,"Rekor: $high",w/2,h*.425f,h*.033f,true);if(newRecordShown)text(c,"YENİ REKOR!",w/2,h*.475f,h*.035f,true);drawButton(c,h*.52f,h*.62f,"TEKRAR OYNA");drawButton(c,h*.66f,h*.76f,"ANA MENÜ")}

    private fun startGame(){state=State.PLAYING;score=0;pipes.clear();spawnTimer=0.01f;birdY=height*.43f;velocity=0f;newRecordShown=false;lastTimeNs=System.nanoTime()}

    override fun onTouchEvent(event:MotionEvent):Boolean{
        if(event.action!=MotionEvent.ACTION_DOWN)return true
        val h=height.toFloat()
        when(state){
            State.MENU->when{event.y in h*.46f..h*.56f->startGame();event.y in h*.60f..h*.69f->{muted=!muted;prefs.edit().putBoolean("muted",muted).apply()}}
            State.PLAYING->{velocity=flapVelocity;play(ToneGenerator.TONE_PROP_BEEP)}
            State.GAMEOVER->when{event.y in h*.52f..h*.62f->startGame();event.y in h*.66f..h*.76f->{state=State.MENU;lastTimeNs=System.nanoTime()}}
        }
        return true
    }
}
