package com.jaspergameengine.editorprototype;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;

/** Pixel-art presets and the small 16x16 sprite painter used by the Android prototype. */
final class SpriteSkinTools {
    static final int SIZE = 16;
    private static final int[] PALETTE = {Color.TRANSPARENT, Color.WHITE, Color.BLACK,
            Color.rgb(60,130,245), Color.rgb(240,75,80), Color.rgb(75,200,120),
            Color.rgb(255,205,65), Color.rgb(160,105,235), Color.rgb(130,80,45)};
    private final Activity activity;
    private final MainActivity.SceneView.Node target;
    private final ArrayList<MainActivity.SceneView.Node> sceneNodes;
    private final Runnable onOpen, onClose, onApply;
    private float density;

    SpriteSkinTools(Activity activity, MainActivity.SceneView.Node target,
                    ArrayList<MainActivity.SceneView.Node> sceneNodes,
                    Runnable onOpen, Runnable onClose, Runnable onApply) {
        this.activity=activity; this.target=target; this.sceneNodes=sceneNodes;
        this.onOpen=onOpen; this.onClose=onClose; this.onApply=onApply;
        density=activity.getResources().getDisplayMetrics().density;
    }
    private int dp(float n){return (int)(n*density+0.5f);}
    private Button button(String title){Button b=new Button(activity);b.setText(title);b.setAllCaps(false);b.setTextSize(12);b.setMinHeight(dp(44));b.setTextColor(Color.WHITE);b.setPadding(dp(8),dp(5),dp(8),dp(5));b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(53,64,82)));return b;}
    private LinearLayout row(){LinearLayout r=new LinearLayout(activity);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);return r;}
    private TextView label(String text,int size,int color){TextView t=new TextView(activity);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}

    void show(){
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(12),dp(6),dp(12),dp(6));
        content.addView(label("Elige una skin lista o dibuja un sprite pixel por pixel.",13,Color.rgb(205,215,230)),new LinearLayout.LayoutParams(-1,dp(42)));
        ScrollView scroll=new ScrollView(activity);LinearLayout list=new LinearLayout(activity);list.setOrientation(LinearLayout.VERTICAL);
        String[] presets={"Pixel azul","Pixel rojo","Pixel verde"};
        for(String preset:presets){Button b=button("Usar "+preset);list.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));b.setOnClickListener(v->apply(preset,makePreset(preset)));}
        HashSet<String> seen=new HashSet<>();
        for(MainActivity.SceneView.Node other:sceneNodes){if(other.spritePixels==null||other.spriteName==null||!seen.add(other.spriteName))continue;String name=other.spriteName;int[] pixels=other.spritePixels.clone();Button b=button("Usar sprite: "+name);list.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));b.setOnClickListener(v->apply(name,pixels));}
        Button create=button("＋ Crear sprite pixel art");LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(50));cp.topMargin=dp(6);list.addView(create,cp);
        create.setOnClickListener(v->{((android.app.Dialog)content.getTag()).dismiss();paint();});
        scroll.addView(list);content.addView(scroll);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Sprite / Skin · "+target.name).setView(content).setNegativeButton("Cerrar",null).create();content.setTag(dialog);dialog.show();
    }
    private void apply(String name,int[] pixels){target.setSprite(name,pixels);onApply.run();}
    private int[] makePreset(String name){
        int[] p=new int[SIZE*SIZE];int fill=name.contains("Rojo")?Color.rgb(230,65,70):name.contains("Verde")?Color.rgb(70,195,105):Color.rgb(65,130,240);
        int shade=name.contains("Rojo")?Color.rgb(140,35,45):name.contains("Verde")?Color.rgb(30,115,65):Color.rgb(35,65,150);
        for(int y=2;y<14;y++)for(int x=2;x<14;x++)p[y*SIZE+x]=fill;
        for(int x=2;x<14;x++){p[13*SIZE+x]=shade;p[12*SIZE+x]=shade;}
        for(int y=4;y<7;y++){p[y*SIZE+5]=Color.WHITE;p[y*SIZE+10]=Color.WHITE;}
        for(int y=5;y<7;y++){p[y*SIZE+6]=Color.BLACK;p[y*SIZE+11]=Color.BLACK;}
        return p;
    }
    private void paint(){
        LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(12),dp(8),dp(12),dp(8));root.setBackgroundColor(Color.rgb(20,23,30));
        TextView title=label("PINTOR PIXEL ART · 16 × 16",16,Color.WHITE);root.addView(title,new LinearLayout.LayoutParams(-1,dp(42)));
        EditText filename=new EditText(activity);filename.setSingleLine(true);filename.setText(target.spriteName==null?target.name+"_skin":target.spriteName);filename.setHint("Nombre del sprite");filename.setTextColor(Color.WHITE);filename.setHintTextColor(Color.rgb(150,160,176));root.addView(filename,new LinearLayout.LayoutParams(-1,dp(50)));
        PaintView canvas=new PaintView(activity,target.spritePixels);LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,0,1);vp.setMargins(dp(4),dp(8),dp(4),dp(8));root.addView(canvas,vp);
        TextView paletteLabel=label("Color de pintura · transparente borra",13,Color.rgb(190,205,225));root.addView(paletteLabel,new LinearLayout.LayoutParams(-1,dp(32)));
        LinearLayout colors=row();for(int i=0;i<PALETTE.length;i++){final int index=i;Button swatch=button(i==0?"□":"●");swatch.setTextColor(PALETTE[i]==Color.TRANSPARENT?Color.WHITE:PALETTE[i]);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(44),1);sp.setMargins(dp(2),0,dp(2),0);colors.addView(swatch,sp);swatch.setOnClickListener(v->{canvas.color=PALETTE[index];paletteLabel.setText(index==0?"Transparente · borrar pixeles":"Color seleccionado · toca la cuadrícula");});}
        root.addView(colors,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout actions=row();Button cancel=button("Cancelar"),clear=button("Limpiar"),save=button("Guardar y usar");for(Button b:new Button[]{cancel,clear,save}){LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(48),1);ap.setMargins(dp(2),0,dp(2),0);actions.addView(b,ap);}root.addView(actions,new LinearLayout.LayoutParams(-1,dp(54)));
        cancel.setOnClickListener(v->onClose.run());clear.setOnClickListener(v->canvas.clear());
        save.setOnClickListener(v->{String name=filename.getText().toString().trim().replaceAll("[^A-Za-z0-9_-]","_");if(name.isEmpty())name="Sprite";target.setSprite(name,canvas.pixels.clone());onClose.run();onApply.run();});
        onOpen.run();activity.setContentView(root);
    }
    private static final class PaintView extends View{
        final Paint paint=new Paint();final int[] pixels=new int[SIZE*SIZE];int color=Color.BLACK;
        PaintView(Activity activity,int[] initial){super(activity);if(initial!=null&&initial.length==pixels.length)System.arraycopy(initial,0,pixels,0,pixels.length);setBackgroundColor(Color.rgb(38,43,54));}
        void clear(){java.util.Arrays.fill(pixels,Color.TRANSPARENT);invalidate();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float size=Math.min(getWidth(),getHeight()),left=(getWidth()-size)/2f,top=(getHeight()-size)/2f,cell=size/SIZE;paint.setStyle(Paint.Style.FILL);for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){int col=pixels[y*SIZE+x];paint.setColor(col==Color.TRANSPARENT?(((x+y)&1)==0?Color.rgb(68,74,86):Color.rgb(48,53,63)):col);c.drawRect(left+x*cell,top+y*cell,left+(x+1)*cell,top+(y+1)*cell,paint);}paint.setColor(Color.rgb(100,110,128));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);for(int i=0;i<=SIZE;i++){float pos=left+i*cell;c.drawLine(pos,top,pos,top+size,paint);pos=top+i*cell;c.drawLine(left,pos,left+size,pos,paint);}paint.setStyle(Paint.Style.FILL);}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_DOWN&&e.getAction()!=MotionEvent.ACTION_MOVE)return true;float size=Math.min(getWidth(),getHeight()),left=(getWidth()-size)/2f,top=(getHeight()-size)/2f,cell=size/SIZE;int x=(int)((e.getX()-left)/cell),y=(int)((e.getY()-top)/cell);if(x>=0&&x<SIZE&&y>=0&&y<SIZE){pixels[y*SIZE+x]=color;invalidate();}return true;}
    }
}
