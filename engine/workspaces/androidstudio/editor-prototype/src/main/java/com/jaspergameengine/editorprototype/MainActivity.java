package com.jaspergameengine.editorprototype;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

/** First touch-layout prototype. Scene data is intentionally in-memory, not the engine format yet. */
public final class MainActivity extends Activity {
    private SceneView scene;
    private TextView selectionLabel;
    private EditText nameInput;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(20, 23, 30));
        getWindow().setNavigationBarColor(Color.rgb(20, 23, 30));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(20, 23, 30));

        TextView title = text("JasperEngine  ·  Editor móvil", 19, Color.WHITE);
        title.setPadding(dp(16), dp(12), dp(12), dp(2));
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));
        TextView project = text("Prototipo de interfaz · escena temporal", 12, Color.rgb(166, 177, 194));
        project.setPadding(dp(16), 0, dp(12), dp(8));
        root.addView(project, new LinearLayout.LayoutParams(-1, dp(25)));

        LinearLayout actions = row();
        Button open = button("Abrir");
        Button save = button("Guardar");
        Button add = button("+ Objeto");
        actions.addView(open, weighted());
        actions.addView(save, weighted());
        actions.addView(add, weighted());
        root.addView(actions, new LinearLayout.LayoutParams(-1, dp(52)));
        open.setOnClickListener(v -> note("Abrir proyecto aún no está conectado."));
        save.setOnClickListener(v -> note("Guardar proyecto aún no está conectado."));

        scene = new SceneView();
        root.addView(scene, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(9), dp(12), dp(10));
        panel.setBackgroundColor(Color.rgb(31, 36, 46));
        TextView panelTitle = text("ESCENA  /  PROPIEDADES", 12, Color.rgb(186, 199, 220));
        panel.addView(panelTitle, new LinearLayout.LayoutParams(-1, dp(28)));
        selectionLabel = text("Seleccionado: Objeto 1", 15, Color.WHITE);
        panel.addView(selectionLabel, new LinearLayout.LayoutParams(-1, dp(28)));

        LinearLayout editRow = row();
        nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setText("Objeto 1");
        nameInput.setHint("Nombre del objeto");
        nameInput.setTextColor(Color.WHITE);
        nameInput.setHintTextColor(Color.rgb(150, 160, 176));
        editRow.addView(nameInput, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button rename = button("Aplicar");
        editRow.addView(rename, new LinearLayout.LayoutParams(dp(96), dp(48)));
        panel.addView(editRow, new LinearLayout.LayoutParams(-1, dp(50)));
        rename.setOnClickListener(v -> {
            scene.renameSelected(nameInput.getText().toString().trim());
            refreshSelection();
        });

        LinearLayout moveRow = row();
        TextView moveLabel = text("Mover:", 14, Color.WHITE);
        moveRow.addView(moveLabel, new LinearLayout.LayoutParams(dp(66), dp(44)));
        addMoveButton(moveRow, "←", -0.06f, 0);
        addMoveButton(moveRow, "↑", 0, -0.06f);
        addMoveButton(moveRow, "↓", 0, 0.06f);
        addMoveButton(moveRow, "→", 0.06f, 0);
        panel.addView(moveRow, new LinearLayout.LayoutParams(-1, dp(46)));
        root.addView(panel, new LinearLayout.LayoutParams(-1, dp(205)));

        add.setOnClickListener(v -> {
            scene.addObject();
            refreshSelection();
        });
        scene.setSelectionChanged(this::refreshSelection);
        setContentView(root);
    }

    private void addMoveButton(LinearLayout row, String label, float dx, float dy) {
        Button b = button(label);
        row.addView(b, new LinearLayout.LayoutParams(0, dp(42), 1));
        b.setOnClickListener(v -> scene.moveSelected(dx, dy));
    }

    private void refreshSelection() {
        SceneView.Node n = scene.selected();
        selectionLabel.setText("Seleccionado: " + n.name);
        if (!nameInput.hasFocus()) nameInput.setText(n.name);
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, dp(48), 1);
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(13);
        b.setAllCaps(false);
        return b;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void note(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private final class SceneView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ArrayList<Node> nodes = new ArrayList<>();
        private int selectedIndex = 0;
        private float downX, downY;
        private boolean dragging;
        private Runnable onSelectionChanged = () -> {};

        SceneView() {
            super(MainActivity.this);
            nodes.add(new Node("Objeto 1", 0.5f, 0.5f));
            setBackgroundColor(Color.rgb(15, 18, 24));
        }

        void setSelectionChanged(Runnable listener) { onSelectionChanged = listener; }
        Node selected() { return nodes.get(selectedIndex); }

        void addObject() {
            int number = nodes.size() + 1;
            nodes.add(new Node("Objeto " + number, 0.38f + (number % 3) * 0.1f, 0.42f + (number % 4) * 0.08f));
            selectedIndex = nodes.size() - 1;
            invalidate();
        }

        void renameSelected(String name) {
            if (!name.isEmpty()) selected().name = name;
            invalidate();
        }

        void moveSelected(float dx, float dy) {
            Node n = selected();
            n.x = clamp(n.x + dx);
            n.y = clamp(n.y + dy);
            invalidate();
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w = getWidth(), h = getHeight();
            paint.setColor(Color.rgb(15, 18, 24));
            c.drawRect(0, 0, w, h, paint);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.rgb(37, 43, 54));
            float step = dp(28);
            for (float x = 0; x < w; x += step) c.drawLine(x, 0, x, h, paint);
            for (float y = 0; y < h; y += step) c.drawLine(0, y, w, y, paint);
            paint.setColor(Color.rgb(68, 83, 104));
            paint.setStrokeWidth(dp(1.5f));
            c.drawLine(w * 0.5f, 0, w * 0.5f, h, paint);
            c.drawLine(0, h * 0.5f, w, h * 0.5f, paint);

            for (int i = 0; i < nodes.size(); i++) {
                Node n = nodes.get(i);
                float cx = n.x * w, cy = n.y * h;
                RectF box = new RectF(cx - dp(34), cy - dp(24), cx + dp(34), cy + dp(24));
                paint.setColor(i == selectedIndex ? Color.rgb(74, 133, 220) : Color.rgb(75, 91, 112));
                c.drawRoundRect(box, dp(7), dp(7), paint);
                if (i == selectedIndex) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(2));
                    paint.setColor(Color.rgb(163, 203, 255));
                    c.drawRoundRect(new RectF(box.left - dp(4), box.top - dp(4), box.right + dp(4), box.bottom + dp(4)), dp(9), dp(9), paint);
                    paint.setStyle(Paint.Style.FILL);
                }
                paint.setColor(Color.WHITE);
                paint.setTextSize(dp(12));
                paint.setTextAlign(Paint.Align.CENTER);
                c.drawText(n.name, cx, cy + dp(4), paint);
            }
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTextSize(dp(11));
            paint.setColor(Color.rgb(170, 185, 205));
            c.drawText("Vista de prueba · arrastra un objeto", dp(12), dp(20), paint);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX(), y = e.getY();
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                downX = x; downY = y; dragging = false;
                for (int i = nodes.size() - 1; i >= 0; i--) {
                    Node n = nodes.get(i);
                    if (Math.abs(x - n.x * getWidth()) < dp(44) && Math.abs(y - n.y * getHeight()) < dp(34)) {
                        selectedIndex = i; dragging = true; onSelectionChanged.run(); break;
                    }
                }
                invalidate();
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_MOVE && dragging) {
                Node n = selected();
                n.x = clamp(x / Math.max(1, getWidth()));
                n.y = clamp(y / Math.max(1, getHeight()));
                invalidate();
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_UP) {
                if (!dragging && Math.hypot(x - downX, y - downY) < dp(12)) {
                    onSelectionChanged.run();
                }
                return true;
            }
            return true;
        }

        private float clamp(float v) { return Math.max(0.08f, Math.min(0.92f, v)); }
        private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }

        private static final class Node {
            String name;
            float x, y;
            Node(String name, float x, float y) { this.name = name; this.x = x; this.y = y; }
        }
    }
}
