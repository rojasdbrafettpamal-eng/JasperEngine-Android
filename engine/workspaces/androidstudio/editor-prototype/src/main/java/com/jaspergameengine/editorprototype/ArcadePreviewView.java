package com.jaspergameengine.editorprototype;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

/** Full-screen arcade-style presentation for the Android prototype's 2D preview. */
final class ArcadePreviewView extends View {
    private final MainActivity.SceneView editorScene;
    private final Runnable onExit;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random(40293);
    private final float[] starX = new float[80], starY = new float[80], starR = new float[80];
    private final ArrayList<MainActivity.SceneView.Node> nodes;
    private MainActivity.SceneView.Node player, meteor, floor, heldNode;
    private boolean arcadeMode, gameOver, pointerHeld;
    private float elapsed, speed = .52f;
    private int score;

    ArcadePreviewView(Context context, MainActivity.SceneView scene, Runnable onExit) {
        super(context);
        this.editorScene=scene; this.onExit=onExit; this.nodes=scene.snapshot();
        for(int i=0;i<starX.length;i++){starX[i]=random.nextFloat();starY[i]=random.nextFloat();starR[i]=1+random.nextFloat()*2.2f;}
        identifyObjects(); setLayerType(View.LAYER_TYPE_SOFTWARE,null);
    }
    private String norm(String value){return Normalizer.normalize(value==null?"":value.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","");}
    private boolean has(MainActivity.SceneView.Node n,String... parts){String name=norm(n.name);for(String part:parts)if(name.contains(part))return true;return false;}
    private void identifyObjects(){for(MainActivity.SceneView.Node n:nodes){if(has(n,"cohete","avion","aeronave","personaje","player"))player=n;if(has(n,"meteorito","meteoro","meteor"))meteor=n;if(has(n,"suelo","piso","ground","floor"))floor=n;}arcadeMode=player!=null&&meteor!=null;if(arcadeMode){player.x=.5f;player.y=.80f;player.vx=player.vy=0;floorY();resetMeteor();}}
    private void floorY(){if(floor!=null){floor.x=.5f;floor.y=.96f;}}
    MainActivity.SceneView.Node getPlayer(){return player;}
    MainActivity.SceneView.Node getMeteor(){return meteor;}
    void step(float dt){if(!arcadeMode||gameOver)return;elapsed+=dt;speed=Math.min(1.25f,.52f+elapsed*.014f);player.x=clamp(player.x+player.vx*dt,.08f,.92f);player.y=.80f;player.vy=0;meteor.y+=speed*dt;if(meteor.y>1.08f){score++;resetMeteor();}if(Math.abs(player.x-meteor.x)<.075f&&Math.abs(player.y-meteor.y)<.060f){gameOver=true;releaseAll();} }
    private void resetMeteor(){meteor.x=.12f+random.nextFloat()*.76f;meteor.y=-.08f;meteor.vy=0;}
    private float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
    private void restart(){releaseAll();gameOver=false;elapsed=0;score=0;speed=.52f;player.x=.5f;player.y=.80f;player.vx=player.vy=0;resetMeteor();invalidate();}
    private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}

    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();paint.setShader(new LinearGradient(0,0,0,h,Color.rgb(5,11,33),Color.rgb(25,54,91),Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,paint);paint.setShader(null);
        for(int i=0;i<starX.length;i++){float twinkle=(float)(.55+.45*Math.sin(elapsed*2+i));paint.setColor(Color.argb(100+(int)(155*twinkle),160,202,255));c.drawCircle(starX[i]*w,starY[i]*h,dp(starR[i]),paint);}
        paint.setColor(Color.rgb(26,48,77));Path hills=new Path();hills.moveTo(0,h*.92f);hills.lineTo(w*.18f,h*.86f);hills.lineTo(w*.36f,h*.92f);hills.lineTo(w*.58f,h*.84f);hills.lineTo(w*.82f,h*.91f);hills.lineTo(w,h*.85f);hills.lineTo(w,h);hills.lineTo(0,h);hills.close();c.drawPath(hills,paint);
        paint.setColor(Color.rgb(49,83,104));c.drawRect(0,h*.956f,w,h,paint);paint.setColor(Color.rgb(100,165,162));c.drawRect(0,h*.956f,w,h*.960f,paint);
        drawNodes(c,w,h);
        paint.setColor(0xB8071020);c.drawRoundRect(new RectF(dp(14),dp(14),dp(220),dp(66)),dp(15),dp(15),paint);paint.setTextAlign(Paint.Align.LEFT);paint.setColor(Color.WHITE);paint.setTextSize(dp(17));c.drawText("ESQUIVADOS  "+score,dp(27),dp(39),paint);paint.setTextSize(dp(12));paint.setColor(Color.rgb(190,211,239));c.drawText("TIEMPO  "+(int)elapsed+" s",dp(27),dp(57),paint);
        paint.setColor(0xC8324058);c.drawRoundRect(new RectF(w-dp(104),dp(12),w-dp(12),dp(58)),dp(14),dp(14),paint);paint.setTextAlign(Paint.Align.CENTER);paint.setColor(Color.WHITE);paint.setTextSize(dp(13));c.drawText("■  SALIR",w-dp(58),dp(41),paint);
        if(!arcadeMode){paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(dp(15));paint.setColor(Color.WHITE);c.drawText("Para activar el juego, nombra dos objetos:",w/2,h*.48f,paint);c.drawText("Cohete y Meteorito",w/2,h*.48f+dp(27),paint);}
        else {paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(dp(12));paint.setColor(Color.rgb(198,216,240));c.drawText("Mantén las flechas para moverte",w/2,h-dp(24),paint);}
        if(gameOver){paint.setColor(0xCC050A17);c.drawRect(0,0,w,h,paint);paint.setTextAlign(Paint.Align.CENTER);paint.setColor(Color.WHITE);paint.setTextSize(dp(34));c.drawText("¡IMPACTO!",w/2,h*.44f,paint);paint.setTextSize(dp(17));c.drawText("Esquivaste "+score+" meteoritos",w/2,h*.44f+dp(36),paint);paint.setTextSize(dp(14));c.drawText("Toca para volver a intentarlo",w/2,h*.44f+dp(72),paint);}
    }
    private void drawNodes(Canvas c,float w,float h){for(MainActivity.SceneView.Node n:nodes){if(n==floor)continue;float x=n.x*w,y=n.y*h;RectF box=new RectF(x-dp(34),y-dp(24),x+dp(34),y+dp(24));if(arcadeMode&&n==player){if(n.spriteBitmap!=null)drawSprite(c,n.spriteBitmap,box);else drawShip(c,x,y);}else if(arcadeMode&&n==meteor){if(n.spriteBitmap!=null)drawSprite(c,n.spriteBitmap,box);else drawRock(c,x,y);}else if(has(n,"flecha","izquierda","derecha","left","right")){drawArrow(c,n,x,y);}else if(n.spriteBitmap!=null)drawSprite(c,n.spriteBitmap,box);else{paint.setColor(Color.rgb(77,94,120));c.drawRoundRect(box,dp(9),dp(9),paint);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(dp(11));paint.setColor(Color.WHITE);c.drawText(n.name,x,y+dp(4),paint);}}}
    private void drawSprite(Canvas c,Bitmap b,RectF r){paint.setFilterBitmap(false);c.drawBitmap(b,null,r,paint);}
    private void drawShip(Canvas c,float x,float y){paint.setColor(Color.rgb(65,165,224));Path p=new Path();p.moveTo(x,y-dp(31));p.lineTo(x+dp(23),y+dp(20));p.lineTo(x+dp(8),y+dp(13));p.lineTo(x,y+dp(22));p.lineTo(x-dp(8),y+dp(13));p.lineTo(x-dp(23),y+dp(20));p.close();c.drawPath(p,paint);paint.setColor(Color.rgb(220,242,255));c.drawOval(x-dp(5),y-dp(9),x+dp(5),y+dp(8),paint);paint.setColor(Color.rgb(255,153,61));Path flame=new Path();flame.moveTo(x-dp(6),y+dp(19));flame.lineTo(x,y+dp(34));flame.lineTo(x+dp(6),y+dp(19));flame.close();c.drawPath(flame,paint);}
    private void drawRock(Canvas c,float x,float y){paint.setColor(Color.rgb(161,87,73));Path p=new Path();for(int i=0;i<12;i++){double a=Math.PI*2*i/12;float r=dp(20+(i%3)*3);float px=x+(float)Math.cos(a)*r,py=y+(float)Math.sin(a)*r;if(i==0)p.moveTo(px,py);else p.lineTo(px,py);}p.close();c.drawPath(p,paint);paint.setColor(Color.rgb(112,57,51));c.drawCircle(x-dp(6),y-dp(4),dp(4),paint);c.drawCircle(x+dp(8),y+dp(6),dp(3),paint);paint.setColor(Color.rgb(208,132,89));c.drawCircle(x+dp(4),y-dp(10),dp(2),paint);}
    private void drawArrow(Canvas c,MainActivity.SceneView.Node n,float x,float y){paint.setColor(n.pressed?Color.rgb(50,157,213):0xCC273953);c.drawRoundRect(new RectF(x-dp(34),y-dp(27),x+dp(34),y+dp(27)),dp(13),dp(13),paint);boolean left=has(n,"izquierda","left");paint.setColor(Color.WHITE);Path p=new Path();if(left){p.moveTo(x-dp(18),y);p.lineTo(x+dp(10),y-dp(16));p.lineTo(x+dp(10),y+dp(16));}else{p.moveTo(x+dp(18),y);p.lineTo(x-dp(10),y-dp(16));p.lineTo(x-dp(10),y+dp(16));}p.close();c.drawPath(p,paint);}

    @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX(),y=e.getY();int action=e.getActionMasked();if(action==MotionEvent.ACTION_DOWN){if(x>getWidth()-dp(112)&&y<dp(72)){onExit.run();return true;}if(gameOver){restart();return true;}pointerHeld=true;releaseAll();heldNode=findNode(x,y);if(heldNode!=null){heldNode.pressed=true;heldNode.tapped=true;}invalidate();return true;}if(action==MotionEvent.ACTION_MOVE&&pointerHeld){MainActivity.SceneView.Node now=findNode(x,y);if(now!=heldNode){releaseAll();heldNode=now;if(heldNode!=null)heldNode.pressed=true;}invalidate();return true;}if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){pointerHeld=false;releaseAll();invalidate();return true;}return true;}
    private MainActivity.SceneView.Node findNode(float x,float y){for(int i=nodes.size()-1;i>=0;i--){MainActivity.SceneView.Node n=nodes.get(i);if(n==floor)continue;if(Math.abs(x-n.x*getWidth())<dp(48)&&Math.abs(y-n.y*getHeight())<dp(44))return n;}return null;}
    private void releaseAll(){for(MainActivity.SceneView.Node n:nodes)n.pressed=false;heldNode=null;}
}
