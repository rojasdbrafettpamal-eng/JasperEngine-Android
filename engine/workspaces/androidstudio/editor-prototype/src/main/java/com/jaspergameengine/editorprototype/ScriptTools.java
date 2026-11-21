package com.jaspergameengine.editorprototype;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;

/** Small object/script browser for the mobile prototype. */
final class ScriptTools {
    interface Listener {
        void selectObject(MainActivity.SceneView.Node node);
        void edit(MainActivity.SceneView.Node node, SceneScript script);
        void changed();
    }
    private final Activity activity;
    private final ArrayList<MainActivity.SceneView.Node> objects;
    private final Listener listener;
    private final float density;
    ScriptTools(Activity activity, ArrayList<MainActivity.SceneView.Node> objects, Listener listener) {
        this.activity=activity; this.objects=objects; this.listener=listener;
        density=activity.getResources().getDisplayMetrics().density;
    }
    private int dp(float x){return (int)(x*density+0.5f);}
    private Button button(String value){Button b=new Button(activity);b.setText(value);b.setAllCaps(false);b.setTextSize(13);b.setMinHeight(dp(46));b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(53,64,82)));return b;}
    private TextView label(String value){TextView t=new TextView(activity);t.setText(value);t.setTextSize(14);t.setTextColor(Color.rgb(205,215,230));t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    void show(){
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(10),dp(6),dp(10),dp(6));
        content.addView(label("Cada objeto puede tener varios scripts. Se ejecutan uno tras otro y comparten escena.objetos."),new LinearLayout.LayoutParams(-1,dp(58)));
        ScrollView scroll=new ScrollView(activity);LinearLayout list=new LinearLayout(activity);list.setOrientation(LinearLayout.VERTICAL);
        for(MainActivity.SceneView.Node node:objects){
            TextView heading=label("▣  "+node.name);heading.setTextColor(Color.WHITE);heading.setTextSize(15);list.addView(heading,new LinearLayout.LayoutParams(-1,dp(42)));
            if(node.scripts.isEmpty() && node.jsCode!=null && !node.jsCode.trim().isEmpty()) node.scripts.add(new SceneScript(node.scriptName,node.jsCode));
            for(int i=0;i<node.scripts.size();i++){
                SceneScript script=node.scripts.get(i);Button edit=button("✎  "+script.name+"  ·  editar");LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(46));ep.setMargins(0,dp(2),0,dp(2));list.addView(edit,ep);
                edit.setOnClickListener(v->{((AlertDialog)content.getTag()).dismiss();listener.selectObject(node);listener.edit(node,script);});
            }
            Button add=button("＋ Añadir script a "+node.name);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(46));ap.setMargins(0,dp(3),0,dp(8));list.addView(add,ap);
            add.setOnClickListener(v->{int n=node.scripts.size()+1;String base=(node.name==null?"Objeto":node.name).replaceAll("[^A-Za-z0-9_-]","_");SceneScript script=new SceneScript(base+"_Script"+n+".js", "// Este script pertenece a: "+node.name+"\n// Usa escena.objetos para encontrar y coordinar otros objetos.\nfunction init(escena, obj) {\n}\n\nfunction update(escena, dt, obj) {\n}\n");node.scripts.add(script);listener.changed();((AlertDialog)content.getTag()).dismiss();listener.selectObject(node);listener.edit(node,script);});
        }
        scroll.addView(list);content.addView(scroll,new LinearLayout.LayoutParams(-1,dp(430)));
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Objetos y scripts").setView(content).setNegativeButton("Cerrar",null).create();content.setTag(dialog);dialog.show();
    }
}
