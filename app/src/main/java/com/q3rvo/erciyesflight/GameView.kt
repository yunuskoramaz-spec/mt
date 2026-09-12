package com.q3rvo.erciyesflight

import android.content.Context
import android.graphics.*
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.random.Random

class GameView(context: Context) : View(context), Choreographer.FrameCallback {
    companion object {
        private const val DESIGN_WIDTH = 1080f
        private const val DESIGN_HEIGHT = 1920f
        private const val GRAVITY = 2500f
        private const val FLAP_VELOCITY = -820f
        private const val PIPE_SPEED = 430f
        private const val PIPE_GAP = 610f
        private const val BIRD_SIZE = 125f
        private const val PIPE_WIDTH = 190f
        private const val PIPE_SPACING = 620f
        private const val FIRST_PIPE_X = 1550f
        private const val PLAY_BOTTOM = 1690f
        private const val MAX_PIPES = 8
    }
    private enum class State { MENU, PLAYING, GAMEOVER }
    private data class Pipe(var x: Float = 0f, var gapCenter: Float = 0f, var passed: Boolean = false)

    private val bird: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.player_bird)
    private val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pipePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pipeHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val birdDst = RectF()
    private val buttonRect = RectF()
    private val scoreRect = RectF()
    private val mountainPath = Path()
    private val snowPath = Path()
    private val roofPath = Path()
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
    private val choreographer = Choreographer.getInstance()
    private val pipes = Array(MAX_PIPES) { Pipe() }

    private var pipeCount = 0
    private var high = prefs.getInt("high", 0)
    private var muted = prefs.getBoolean("muted", false)
    private var state = State.MENU
    private var score = 0
    private var birdX = 300f
    private var birdY = 820f
    private var velocity = 0f
    private var lastFrameTimeNanos = 0L
    private var running = false
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private var cloudOffset = 0f
    private var recordPulse = 0f
    private var newRecordShown = false

    init {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        hudPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pipePaint.shader = LinearGradient(0f, 0f, PIPE_WIDTH, 0f, Color.rgb(73, 196, 78), Color.rgb(39, 145, 55), Shader.TileMode.CLAMP)
        pipeHighlightPaint.color = Color.argb(95, 220, 255, 205)
        shadowPaint.color = Color.argb(80, 0, 45, 55)
        mountainPath.moveTo(0f,760f); mountainPath.lineTo(210f,610f); mountainPath.lineTo(360f,720f); mountainPath.lineTo(540f,350f); mountainPath.lineTo(730f,720f); mountainPath.lineTo(910f,585f); mountainPath.lineTo(1080f,750f); mountainPath.lineTo(1080f,980f); mountainPath.lineTo(0f,980f); mountainPath.close()
        snowPath.moveTo(465f,505f); snowPath.lineTo(540f,350f); snowPath.lineTo(625f,520f); snowPath.lineTo(580f,482f); snowPath.lineTo(540f,420f); snowPath.lineTo(505f,492f); snowPath.close()
        roofPath.moveTo(75f,885f); roofPath.lineTo(120f,815f); roofPath.lineTo(165f,885f); roofPath.close()
        startLoop()
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); startLoop() }
    override fun onDetachedFromWindow() { stopLoop(); tone.release(); super.onDetachedFromWindow() }

    private fun startLoop() {
        if (running) return
        running = true
        lastFrameTimeNanos = System.nanoTime()
        choreographer.postFrameCallback(this)
    }
    private fun stopLoop() {
        if (!running) return
        running = false
        choreographer.removeFrameCallback(this)
    }
    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        val dt = if (lastFrameTimeNanos == 0L) 0f else ((frameTimeNanos-lastFrameTimeNanos)/1_000_000_000f).coerceIn(0f,0.033f)
        lastFrameTimeNanos = frameTimeNanos
        if (state == State.PLAYING) update(dt)
        cloudOffset = (cloudOffset + dt*8f) % 1080f
        if (recordPulse > 0f) recordPulse -= dt
        invalidate()
        choreographer.postFrameCallback(this)
    }

    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) {
        super.onSizeChanged(w,h,oldw,oldh)
        if(w<=0||h<=0)return
        scale=min(w/DESIGN_WIDTH,h/DESIGN_HEIGHT)
        offsetX=(w-DESIGN_WIDTH*scale)*.5f
        offsetY=(h-DESIGN_HEIGHT*scale)*.5f
        if(state==State.MENU){birdX=300f;birdY=820f}
    }

    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        canvas.save()
        try {
            canvas.drawColor(Color.rgb(210,232,240))
            canvas.translate(offsetX,offsetY)
            canvas.scale(scale,scale)
            drawSky(canvas); drawClouds(canvas); drawMountain(canvas); drawCity(canvas)
            drawPipes(canvas); drawGround(canvas); drawBird(canvas)
            when(state){State.MENU->drawMenu(canvas);State.PLAYING->drawHud(canvas);State.GAMEOVER->{drawHud(canvas);drawGameOver(canvas)}}
        } finally { canvas.restore() }
    }
    private fun drawSky(c:Canvas){
        paint.shader=LinearGradient(0f,0f,0f,DESIGN_HEIGHT,Color.rgb(71,184,237),Color.rgb(245,225,188),Shader.TileMode.CLAMP)
        c.drawRect(0f,0f,DESIGN_WIDTH,DESIGN_HEIGHT,paint);paint.shader=null
    }
    private fun drawClouds(c:Canvas){drawCloud(c,140f-cloudOffset,220f,115f);drawCloud(c,780f-cloudOffset*.55f,320f,150f);drawCloud(c,1190f-cloudOffset*.35f,170f,90f)}
    private fun drawCloud(c:Canvas,x:Float,y:Float,size:Float){
        val xx=((x+1300f)%1300f)-100f
        paint.color=Color.argb(190,255,255,255)
        c.drawCircle(xx,y,size*.34f,paint);c.drawCircle(xx+size*.28f,y-size*.08f,size*.42f,paint);c.drawCircle(xx+size*.60f,y,size*.30f,paint)
        c.drawRoundRect(xx-size*.35f,y,xx+size*.78f,y+size*.25f,size*.12f,size*.12f,paint)
    }
    private fun drawMountain(c:Canvas){paint.color=Color.rgb(102,154,190);c.drawPath(mountainPath,paint);paint.color=Color.argb(235,255,255,255);c.drawPath(snowPath,paint)}
    private fun drawCity(c:Canvas){
        val base=1030f
        paint.color=Color.argb(220,90,117,130);c.drawRect(0f,850f,1080f,base,paint)
        var x=0f;var i=0
        while(x<DESIGN_WIDTH){val bw=70f+(i%4)*28f;val bh=90f+(i%5)*35f;paint.color=if(i%2==0)Color.rgb(92,121,132)else Color.rgb(108,135,143);c.drawRoundRect(x,base-bh,x+bw,base,10f,10f,paint);x+=bw+10f;i++}
        paint.color=Color.rgb(215,197,170);c.drawRoundRect(445f,830f,635f,base,20f,20f,paint);c.drawOval(460f,735f,620f,875f,paint)
        paint.color=Color.rgb(225,205,175);c.drawRect(485f,710f,505f,base,paint);c.drawRect(575f,710f,595f,base,paint);c.drawCircle(495f,700f,12f,paint);c.drawCircle(585f,700f,12f,paint)
        paint.color=Color.rgb(180,155,125);c.drawRect(90f,885f,150f,base,paint);c.drawPath(roofPath,paint)
    }
    private fun drawPipes(c:Canvas){var i=0;while(i<pipeCount){val p=pipes[i];val gapTop=p.gapCenter-PIPE_GAP*.5f;val gapBottom=p.gapCenter+PIPE_GAP*.5f;drawPipe(c,p.x,0f,gapTop,true);drawPipe(c,p.x,gapBottom,PLAY_BOTTOM,false);i++}}
    private fun drawPipe(c:Canvas,x:Float,top:Float,bottom:Float,isTop:Boolean){
        if(bottom<=top)return
        paint.shader=pipePaint.shader;c.drawRect(x,top,x+PIPE_WIDTH,bottom,paint);paint.shader=null
        c.drawRect(x+24f,top,x+43f,bottom,pipeHighlightPaint)
        val capH=58f;val capLeft=x-15f;val capRight=x+PIPE_WIDTH+15f
        if(isTop)c.drawRoundRect(capLeft,bottom-capH,capRight,bottom,18f,18f,pipePaint) else c.drawRoundRect(capLeft,top,capRight,top+capH,18f,18f,pipePaint)
    }
    private fun drawGround(c:Canvas){
        paint.color=Color.rgb(69,168,79);c.drawRect(0f,PLAY_BOTTOM,DESIGN_WIDTH,PLAY_BOTTOM+36f,paint)
        paint.color=Color.rgb(45,127,62);c.drawRect(0f,PLAY_BOTTOM,DESIGN_WIDTH,PLAY_BOTTOM+8f,paint)
        paint.color=Color.rgb(227,197,140);c.drawRect(0f,PLAY_BOTTOM+36f,DESIGN_WIDTH,DESIGN_HEIGHT,paint)
        paint.color=Color.rgb(194,160,107);var x=0f;while(x<DESIGN_WIDTH){c.drawCircle(x+30f,PLAY_BOTTOM+80f,4f,paint);c.drawCircle(x+90f,PLAY_BOTTOM+125f,3f,paint);x+=140f}
    }
    private fun drawBird(c:Canvas){
        val halfW=BIRD_SIZE*.5f;val halfH=BIRD_SIZE*(bird.height.toFloat()/bird.width.toFloat())*.5f
        birdDst.set(birdX-halfW,birdY-halfH,birdX+halfW,birdY+halfH)
        val angle=(velocity/18f).coerceIn(-22f,24f);c.save();c.rotate(angle,birdX,birdY);c.drawBitmap(bird,null,birdDst,paint);c.restore()
    }
    private fun drawHud(c:Canvas){
        scoreRect.set(430f,52f,650f,190f);shadowPaint.color=Color.argb(65,0,35,50);c.drawRoundRect(scoreRect.left+6f,scoreRect.top+8f,scoreRect.right+6f,scoreRect.bottom+8f,36f,36f,shadowPaint)
        hudPaint.color=Color.argb(220,255,255,255);c.drawRoundRect(scoreRect,36f,36f,hudPaint);hudPaint.color=Color.rgb(35,86,104);hudPaint.textAlign=Paint.Align.CENTER;hudPaint.textSize=92f;c.drawText(score.toString(),540f,145f,hudPaint)
    }
    private fun drawMenu(c:Canvas){
        paint.color=Color.argb(90,8,50,72);c.drawRect(0f,0f,DESIGN_WIDTH,DESIGN_HEIGHT,paint);hudPaint.color=Color.WHITE;hudPaint.textAlign=Paint.Align.CENTER;hudPaint.textSize=105f;hudPaint.typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD);c.drawText("ERCİYES",540f,360f,hudPaint);hudPaint.textSize=72f;c.drawText("UÇUŞU",540f,445f,hudPaint)
        drawButton(c,330f,1240f,750f,1390f,"OYUNA BAŞLA",true);drawButton(c,390f,1450f,690f,1560f,if(muted)"SESİ AÇ" else "SESİ KAPAT",false);hudPaint.textSize=42f;hudPaint.color=Color.WHITE;c.drawText("EN YÜKSEK SKOR  $high",540f,1660f,hudPaint)
    }
    private fun drawButton(c:Canvas,l:Float,t:Float,r:Float,b:Float,label:String,primary:Boolean){
        buttonRect.set(l,t,r,b);buttonPaint.color=Color.argb(70,0,35,45);c.drawRoundRect(l+4f,t+8f,r+4f,b+8f,42f,42f,buttonPaint);buttonPaint.color=if(primary)Color.rgb(255,184,62)else Color.argb(235,43,105,124);c.drawRoundRect(buttonRect,42f,42f,buttonPaint);hudPaint.color=Color.WHITE;hudPaint.textSize=if(primary)52f else 38f;hudPaint.textAlign=Paint.Align.CENTER;c.drawText(label,540f,(t+b)*.5f+hudPaint.textSize*.35f,hudPaint)
    }
    private fun drawGameOver(c:Canvas){
        paint.color=Color.argb(165,4,25,38);c.drawRect(0f,0f,DESIGN_WIDTH,DESIGN_HEIGHT,paint);buttonPaint.color=Color.argb(245,247,251,247);c.drawRoundRect(165f,420f,915f,1280f,56f,56f,buttonPaint);hudPaint.color=Color.rgb(34,72,84);hudPaint.textAlign=Paint.Align.CENTER;hudPaint.textSize=70f;c.drawText("OYUN BİTTİ",540f,555f,hudPaint);hudPaint.textSize=44f;c.drawText("Skor",540f,690f,hudPaint);hudPaint.textSize=92f;c.drawText(score.toString(),540f,790f,hudPaint);hudPaint.textSize=38f;c.drawText("En yüksek skor: $high",540f,875f,hudPaint)
        if(newRecordShown){hudPaint.color=Color.rgb(233,148,31);hudPaint.textSize=36f;c.drawText("YENİ REKOR!",540f,935f,hudPaint)}
        drawButton(c,255f,1000f,825f,1105f,"TEKRAR OYNA",true);drawButton(c,255f,1140f,825f,1245f,"ANA MENÜ",false)
    }
    private fun resetPipe(p:Pipe,x:Float,first:Boolean){p.x=x;p.gapCenter=if(first)835f else Random.nextInt(650,1030).toFloat();p.passed=false}
    private fun startGame(){state=State.PLAYING;score=0;newRecordShown=false;recordPulse=0f;pipeCount=0;birdX=300f;birdY=820f;velocity=0f;resetPipe(pipes[0],FIRST_PIPE_X,true);pipeCount=1;lastFrameTimeNanos=System.nanoTime()}

    private fun update(dt:Float){
        velocity+=GRAVITY*dt;birdY+=velocity*dt
        var i=0
        while(i<pipeCount){
            val p=pipes[i];p.x-=PIPE_SPEED*dt
            if(i==pipeCount-1&&p.x<DESIGN_WIDTH-PIPE_SPACING&&pipeCount<MAX_PIPES){resetPipe(pipes[pipeCount],p.x+PIPE_SPACING,false);pipeCount++}
            if(!p.passed&&p.x+PIPE_WIDTH<birdX-BIRD_SIZE*.5f){p.passed=true;score++;if(score>high){high=score;newRecordShown=true;recordPulse=.25f;prefs.edit().putInt("high",high).apply()};play(ToneGenerator.TONE_PROP_ACK)}
            i++
        }
        val halfW=BIRD_SIZE*.5f;val halfH=BIRD_SIZE*(bird.height.toFloat()/bird.width.toFloat())*.5f
        val birdLeft=birdX-halfW*.82f;val birdRight=birdX+halfW*.82f;val birdTop=birdY-halfH*.82f;val birdBottom=birdY+halfH*.82f
        var hitPipe=false;i=0
        while(i<pipeCount){
            val p=pipes[i]
            if(p.x+PIPE_WIDTH>birdLeft&&p.x<birdRight){
                val gapTop=p.gapCenter-PIPE_GAP*.5f;val gapBottom=p.gapCenter+PIPE_GAP*.5f
                val topPipeHitboxTop=0f;val topPipeHitboxBottom=gapTop
                val bottomPipeHitboxTop=gapBottom;val bottomPipeHitboxBottom=PLAY_BOTTOM
                val overlapTop=birdBottom>topPipeHitboxTop&&birdTop<topPipeHitboxBottom
                val overlapBottom=birdBottom>bottomPipeHitboxTop&&birdTop<bottomPipeHitboxBottom
                hitPipe=overlapTop||overlapBottom
                if(hitPipe)break
            }
            i++
        }
        if(hitPipe||birdTop<30f||birdBottom>PLAY_BOTTOM){state=State.GAMEOVER;play(ToneGenerator.TONE_PROP_NACK)}
    }
    private fun play(id:Int){if(!muted)tone.startTone(id,80)}
    private fun gamePointX(screenX:Float):Float=(screenX-offsetX)/scale
    private fun gamePointY(screenY:Float):Float=(screenY-offsetY)/scale
    override fun onTouchEvent(event:MotionEvent):Boolean{
        if(event.action!=MotionEvent.ACTION_DOWN)return true
        val x=gamePointX(event.x);val y=gamePointY(event.y)
        when(state){
            State.MENU->{if(y in 1240f..1390f&&x in 330f..750f){startGame();velocity=FLAP_VELOCITY;play(ToneGenerator.TONE_PROP_BEEP)}else if(y in 1450f..1560f&&x in 390f..690f){muted=!muted;prefs.edit().putBoolean("muted",muted).apply()}}
            State.PLAYING->{velocity=FLAP_VELOCITY;play(ToneGenerator.TONE_PROP_BEEP)}
            State.GAMEOVER->{if(y in 1000f..1105f&&x in 255f..825f){startGame();velocity=FLAP_VELOCITY;play(ToneGenerator.TONE_PROP_BEEP)}else if(y in 1140f..1245f&&x in 255f..825f){state=State.MENU;birdY=820f;velocity=0f;pipeCount=0;lastFrameTimeNanos=System.nanoTime()}}
        }
        invalidate();return true
    }
}
