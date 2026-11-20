package com.jaspergameengine.editorprototype;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.content.Intent;
import android.content.ContentResolver;
import android.provider.DocumentsContract;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

/** First touch-layout prototype. Scene data is intentionally in-memory, not the engine format yet. */
public final class MainActivity extends Activity {
    private SceneView scene;
    private TextView selectionLabel;
    private EditText nameInput;
    private Button playButton;
    private TextView projectLabel;
    private Uri workspaceRootUri;
    private Uri activeProjectUri;
    private String activeProjectName = "";
    private static final int REQUEST_WORKSPACE = 4105;
    private static final int REQUEST_OPEN_SCENE = 4101;
    private static final int REQUEST_SAVE_SCENE = 4102;
    private static final int REQUEST_EXPORT_PROJECT = 4103;
    private static final int REQUEST_OPEN_PROJECT = 4104;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(20, 23, 30));
        getWindow().setNavigationBarColor(Color.rgb(20, 23, 30));
        String savedWorkspace = getSharedPreferences("editor", MODE_PRIVATE).getString("workspace", null);
        if (savedWorkspace != null) workspaceRootUri = Uri.parse(savedWorkspace);
        showHome();
    }

    private void buildEditor() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(20, 23, 30));

        TextView title = text("JasperEngine  ·  Editor móvil", 19, Color.WHITE);
        title.setPadding(dp(16), dp(12), dp(12), dp(2));
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));
        projectLabel = text("Prototipo 2D · escena, Lua y física", 12, Color.rgb(166, 177, 194));
        projectLabel.setPadding(dp(16), 0, dp(12), dp(8));
        root.addView(projectLabel, new LinearLayout.LayoutParams(-1, dp(25)));

        LinearLayout actions = row();
        Button open = button("Abrir JSON");
        Button save = button("Guardar JSON");
        Button add = button("+ Objeto");
        Button export = button("Guardar proyecto");
        playButton = button("▶ Play");
        actions.addView(playButton, weighted());
        actions.addView(open, weighted());
        actions.addView(save, weighted());
        actions.addView(add, weighted());
        actions.addView(export, weighted());
        root.addView(actions, new LinearLayout.LayoutParams(-1, dp(52)));
        LinearLayout projectActions = row();
        Button openProject = button("Abrir proyecto JE");
        projectActions.addView(openProject, new LinearLayout.LayoutParams(-1, dp(44)));
        Button backHome = button("‹ Proyectos");
        projectActions.addView(backHome, new LinearLayout.LayoutParams(-1, dp(44)));
        root.addView(projectActions, new LinearLayout.LayoutParams(-1, dp(46)));
        backHome.setOnClickListener(v -> { saveActiveProject(); activeProjectUri = null; showHome(); });
        openProject.setVisibility(View.GONE);
        open.setOnClickListener(v -> openScenePicker());
        save.setOnClickListener(v -> saveScenePicker());

        scene = new SceneView(this);
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
        editRow.addView(rename, new LinearLayout.LayoutParams(dp(76), dp(48)));
        Button editCode = button("Lua");
        editRow.addView(editCode, new LinearLayout.LayoutParams(dp(56), dp(48)));
        panel.addView(editRow, new LinearLayout.LayoutParams(-1, dp(50)));
        editCode.setOnClickListener(v -> editLuaForSelected());
        Button physicsButton = button("Física 2D / Collider");
        panel.addView(physicsButton, new LinearLayout.LayoutParams(-1, dp(42)));
        physicsButton.setOnClickListener(v -> editPhysicsForSelected());
        panel.setPadding(dp(12), dp(5), dp(12), dp(6));
        panelTitle.setText("ESCENA  /  PROPIEDADES · LUA · FÍSICA");
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

        export.setOnClickListener(v -> saveActiveProject());
        add.setOnClickListener(v -> {
            scene.addObject();
            refreshSelection();
        });
        playButton.setOnClickListener(v -> togglePlayMode());
        scene.setSelectionChanged(this::refreshSelection);
        editorRoot = root;
        setContentView(root);
    }

    private boolean playing = false;
    private long lastPlayTick;
    private final Handler playHandler = new Handler(Looper.getMainLooper());
    private final Runnable playLoop = new Runnable() {
        @Override public void run() {
            if (!playing) return;
            long now = SystemClock.uptimeMillis();
            float dt = Math.min(0.05f, (now - lastPlayTick) / 1000f);
            lastPlayTick = now;
            scene.advancePreview(dt);
            playHandler.postDelayed(this, 16);
        }
    };

    private void togglePlayMode() {
        playing = !playing;
        if (playing) {
            scene.setPreviewMode(true);
            lastPlayTick = SystemClock.uptimeMillis();
            playButton.setText("■ Stop");
            playButton.setTextColor(Color.rgb(255, 145, 145));
            projectLabel.setText("VISTA PREVIA · prototipo 2D · pulsa Stop para volver al editor");
            playHandler.post(playLoop);
            note("Vista previa iniciada. Usa las flechas en pantalla para mover el objeto seleccionado.");
        } else {
            playHandler.removeCallbacks(playLoop);
            scene.setPreviewMode(false);
            playButton.setText("▶ Play");
            playButton.setTextColor(Color.WHITE);
            projectLabel.setText("Prototipo 2D · escena, Lua y física");
            note("Vista previa detenida; volviste al editor.");
        }
    }

    @Override protected void onPause() {
        if (playing) togglePlayMode();
        super.onPause();
    }

    private void showHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(18));
        root.setBackgroundColor(Color.rgb(20, 23, 30));
        TextView title = text("JasperEngine · Proyectos", 22, Color.WHITE);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(54)));
        TextView info = text(workspaceRootUri == null
                ? "Primero elige o crea una carpeta raíz. Después los proyectos se guardarán dentro de ella sin volver a preguntarte."
                : "Tus proyectos quedan guardados dentro de la carpeta que elegiste.",
                14, Color.rgb(166, 177, 194));
        root.addView(info, new LinearLayout.LayoutParams(-1, dp(72)));
        Button chooseFolder = button(workspaceRootUri == null ? "Elegir o crear carpeta de proyectos" : "Cambiar carpeta raíz");
        root.addView(chooseFolder, new LinearLayout.LayoutParams(-1, dp(52)));
        chooseFolder.setOnClickListener(v -> workspacePicker());
        if (workspaceRootUri != null) {
            Button newProject = button("＋  Crear proyecto nuevo");
            LinearLayout.LayoutParams newParams = new LinearLayout.LayoutParams(-1, dp(54));
            newParams.topMargin = dp(14);
            root.addView(newProject, newParams);
            newProject.setOnClickListener(v -> askNewProjectName());
            TextView listTitle = text("PROYECTOS", 12, Color.rgb(186, 199, 220));
            LinearLayout.LayoutParams listTitleParams = new LinearLayout.LayoutParams(-1, dp(40));
            listTitleParams.topMargin = dp(15);
            root.addView(listTitle, listTitleParams);
            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            try {
                Uri rootDoc = workspaceDocumentRoot();
                Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(rootDoc,
                        DocumentsContract.getDocumentId(rootDoc));
                String[] columns = {DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE};
                try (android.database.Cursor cursor = getContentResolver().query(children, columns, null, null, null)) {
                    if (cursor != null) {
                        int idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
                        int nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                        int typeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);
                        while (cursor.moveToNext()) {
                            if (!DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(typeCol))) continue;
                            String name = cursor.getString(nameCol);
                            Uri childDoc = DocumentsContract.buildDocumentUriUsingTree(workspaceRootUri, cursor.getString(idCol));
                            if (findChild(childDoc, "Main.scene") == null) continue;
                            Uri childTree = DocumentsContract.buildTreeDocumentUri(childDoc.getAuthority(),
                                    DocumentsContract.getDocumentId(childDoc));
                            Button item = button("▣  " + name + "   ›");
                            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(-1, dp(54));
                            itemParams.bottomMargin = dp(6);
                            list.addView(item, itemParams);
                            item.setOnClickListener(v -> {
                                activeProjectName = name;
                                openProject(childTree);
                                projectLabel.setText("Proyecto: " + activeProjectName);
                                setContentView(editorRoot);
                            });
                        }
                    }
                }
            } catch (Exception e) { note("No pude leer la lista de proyectos: " + e.getMessage()); }
            if (list.getChildCount() == 0) {
                TextView empty = text("Aún no hay proyectos. Pulsa Crear proyecto nuevo.", 14, Color.rgb(166, 177, 194));
                list.addView(empty, new LinearLayout.LayoutParams(-1, dp(52)));
            }
            scroll.addView(list);
            root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        }
        setContentView(root);
    }

    private View editorRoot;

    private Uri workspaceDocumentRoot() {
        return DocumentsContract.buildDocumentUriUsingTree(workspaceRootUri,
                DocumentsContract.getTreeDocumentId(workspaceRootUri));
    }

    private void workspacePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(intent, REQUEST_WORKSPACE); }
        catch (Exception e) { note("No se pudo abrir el selector de carpetas."); }
    }

    private void askNewProjectName() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText("Mi proyecto");
        new AlertDialog.Builder(this).setTitle("Nombre del proyecto").setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Crear", (dialog, which) -> createProject(input.getText().toString()))
                .show();
    }

    private void createProject(String requestedName) {
        try {
            String name = requestedName == null ? "" : requestedName.trim().replaceAll("[/\\\\:*?\\\"<>|]", "_");
            if (name.isEmpty()) name = "Mi proyecto";
            Uri root = workspaceDocumentRoot();
            String candidate = name;
            int number = 2;
            while (findChild(root, candidate) != null) candidate = name + " " + number++;
            Uri folder = createChildDirectory(root, candidate);
            activeProjectUri = DocumentsContract.buildTreeDocumentUri(folder.getAuthority(),
                    DocumentsContract.getDocumentId(folder));
            activeProjectName = candidate;
            buildEditor();
            projectLabel.setText("Proyecto: " + activeProjectName);
            saveActiveProject();
            note("Proyecto creado dentro de tu carpeta de proyectos.");
        } catch (Exception e) { note("No pude crear el proyecto: " + e.getMessage()); }
    }

    private void saveActiveProject() {
        if (activeProjectUri == null) { note("Crea o abre un proyecto primero."); return; }
        exportProject(activeProjectUri);
    }

    @Override public void onBackPressed() {
        if (activeProjectUri != null) {
            saveActiveProject();
            activeProjectUri = null;
            showHome();
        } else super.onBackPressed();
    }

    private void openScenePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "Abrir escena");
        try {
            startActivityForResult(intent, REQUEST_OPEN_SCENE);
        } catch (Exception e) {
            note("No se pudo abrir el selector de archivos.");
        }
    }

    private void openProjectPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_OPEN_PROJECT);
        } catch (Exception e) {
            note("No se pudo abrir el selector de proyectos.");
        }
    }

    private void exportProjectPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_EXPORT_PROJECT);
        } catch (Exception e) {
            note("No se pudo abrir el selector de carpetas.");
        }
    }

    private void saveScenePicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "MiEscena.jasper-scene.json");
        try {
            startActivityForResult(intent, REQUEST_SAVE_SCENE);
        } catch (Exception e) {
            note("No se pudo abrir el selector para guardar.");
        }
    }

    @Override @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_WORKSPACE) {
            try {
                int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, flags);
                workspaceRootUri = uri;
                getSharedPreferences("editor", MODE_PRIVATE).edit().putString("workspace", uri.toString()).apply();
                showHome();
            } catch (Exception e) { note("No pude guardar el permiso de esa carpeta."); }
        } else if (requestCode == REQUEST_SAVE_SCENE) saveScene(uri);
        else if (requestCode == REQUEST_OPEN_SCENE) loadScene(uri);
        else if (requestCode == REQUEST_EXPORT_PROJECT) exportProject(uri);
        else if (requestCode == REQUEST_OPEN_PROJECT) openProject(uri);
    }

    private void exportProject(Uri treeUri) {
        try {
            String treeId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri root = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId);
            String sceneGuid = java.util.UUID.randomUUID().toString().replace("-", "");
            long sceneId = (System.currentTimeMillis() & 0x7fffffffL);
            String sceneName = "Main";
            writeChild(root, "project.yaml", "application/yaml", projectYaml(sceneId, sceneGuid));
            writeChild(root, sceneName + ".scene", "application/yaml", jasperSceneYaml(sceneId, sceneGuid, sceneName));
            createChildDirectory(root, "assets");
            createChildDirectory(root, "lua");
            Uri audio = createChildDirectory(root, "audio");
            writeChild(audio, "Main.mixer", "application/json", mixerJson());
            Uri scripts = createChildDirectory(root, "scripts");
            int scriptIndex = 0;
            for (SceneView.Node node : scene.snapshot()) {
                if (node.luaCode != null && !node.luaCode.trim().isEmpty()) {
                    writeChild(scripts, "Script_" + scriptIndex + ".lua", "text/plain", node.luaCode);
                }
                scriptIndex++;
            }
            note("Proyecto exportado con escena y scripts Lua adjuntos a sus objetos.");
        } catch (Exception e) {
            note("No se pudo exportar el proyecto: " + e.getMessage());
        }
    }

    private void openProject(Uri treeUri) {
        try {
            activeProjectUri = treeUri;
            if (scene == null) buildEditor();
            Uri root = DocumentsContract.buildDocumentUriUsingTree(treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri));
            Uri sceneFile = findChild(root, "Main.scene");
            if (sceneFile == null) {
                note("No encontré Main.scene en esa carpeta. Elige la carpeta del proyecto JasperEngine.");
                return;
            }
            StringBuilder contents = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    getContentResolver().openInputStream(sceneFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) contents.append(line).append('\n');
            }
            ArrayList<SceneView.Node> nodes = parseProjectScene(contents.toString(), root);
            if (nodes == null) {
                note("No pude leer Main.scene: el formato no coincide con el exportador de este prototipo.");
                return;
            }
            scene.replaceScene(nodes);
            refreshSelection();
            note("Proyecto abierto en la vista de prueba. No es el editor completo.");
        } catch (Exception e) {
            note("No se pudo abrir el proyecto: " + e.getMessage());
        }
    }

    private ArrayList<SceneView.Node> parseProjectScene(String yaml, Uri projectRoot) throws Exception {
        ArrayList<SceneView.Node> nodes = new ArrayList<>();
        Uri scriptsFolder = findChild(projectRoot, "scripts");
        if (scriptsFolder != null) scriptsFolder = DocumentsContract.buildDocumentUriUsingTree(projectRoot,
                DocumentsContract.getDocumentId(scriptsFolder));
        final Uri scriptDirectory = scriptsFolder;
        boolean inEntities = false;
        String pendingName = null;
        boolean inRigidBody = false;
        boolean inColliders = false;
        for (String line : yaml.split("\\r?\\n")) {
            if (line.equals("entities:")) { inEntities = true; continue; }
            if (!inEntities) continue;
            String trimmed = line.trim();
            if (trimmed.equals("rigidbody2d:")) {
                if (!nodes.isEmpty()) nodes.get(nodes.size() - 1).physicsEnabled = true;
                inRigidBody = true;
                inColliders = false;
                continue;
            }
            if (trimmed.startsWith("name: ")) {
                inRigidBody = false;
                inColliders = false;
                String encoded = trimmed.substring("name: ".length());
                pendingName = new JSONArray("[" + encoded + "]").getString(0);
            } else if (trimmed.startsWith("position: [") && pendingName != null) {
                String values = trimmed.substring("position: [".length(), trimmed.lastIndexOf(']'));
                String[] xy = values.split(",");
                if (xy.length < 2) return null;
                float x = Float.parseFloat(xy[0].trim()) / 1280f;
                float y = Float.parseFloat(xy[1].trim()) / 720f;
                nodes.add(new SceneView.Node(pendingName, Math.max(0.08f, Math.min(0.92f, x)),
                        Math.max(0.08f, Math.min(0.92f, y))));
                pendingName = null;
            } else if (inRigidBody && trimmed.equals("colliders:")) {
                inColliders = true;
            } else if (inRigidBody && !nodes.isEmpty() && inColliders && trimmed.startsWith("- type: ")) {
                nodes.get(nodes.size() - 1).colliderType = trimmed.substring("- type: ".length()).trim();
            } else if (inRigidBody && !nodes.isEmpty() && !inColliders && trimmed.startsWith("type: ")) {
                nodes.get(nodes.size() - 1).bodyType = trimmed.substring("type: ".length()).trim();
            } else if (inRigidBody && !nodes.isEmpty() && trimmed.startsWith("sensor: ")) {
                nodes.get(nodes.size() - 1).sensor = Boolean.parseBoolean(trimmed.substring("sensor: ".length()));
            } else if (trimmed.startsWith("path: scripts/") && !nodes.isEmpty() && scriptDirectory != null) {
                String fileName = trimmed.substring("path: scripts/".length()).trim();
                Uri luaFile = findChild(scriptDirectory, fileName);
                if (luaFile != null) {
                    StringBuilder code = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                            getContentResolver().openInputStream(luaFile), StandardCharsets.UTF_8))) {
                        String luaLine;
                        while ((luaLine = reader.readLine()) != null) code.append(luaLine).append('\n');
                    }
                    nodes.get(nodes.size() - 1).luaCode = code.toString();
                }
            }
        }
        if (nodes.isEmpty()) return null;
        return nodes;
    }

    private Uri createChildDirectory(Uri parent, String name) throws Exception {
        Uri existing = findChild(parent, name);
        if (existing != null) return existing;
        Uri created = DocumentsContract.createDocument(getContentResolver(), parent,
                DocumentsContract.Document.MIME_TYPE_DIR, name);
        if (created == null) throw new IllegalStateException("No se pudo crear la carpeta " + name);
        return created;
    }

    private void writeChild(Uri parent, String name, String mime, String contents) throws Exception {
        Uri file = findChild(parent, name);
        if (file == null) file = DocumentsContract.createDocument(getContentResolver(), parent, mime, name);
        if (file == null) throw new IllegalStateException("No se pudo crear " + name);
        try (OutputStream out = getContentResolver().openOutputStream(file, "wt")) {
            if (out == null) throw new IllegalStateException("No se pudo escribir " + name);
            out.write(contents.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    private Uri findChild(Uri parent, String name) throws Exception {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(parent,
                DocumentsContract.getDocumentId(parent));
        String[] columns = {DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME};
        try (android.database.Cursor cursor = getContentResolver().query(children, columns, null, null, null)) {
            if (cursor == null) return null;
            int idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            while (cursor.moveToNext()) {
                if (name.equals(cursor.getString(nameColumn))) {
                    return DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(idColumn));
                }
            }
        }
        return null;
    }

    private String projectYaml(long sceneId, String sceneGuid) {
        return "formatVersion: 1\n" +
                "name: JasperMobileProject\n" +
                "canvasWidth: 1280\ncanvasHeight: 720\n" +
                "scalingMode: letterbox\ntextureStrategy: none\nvsync: true\n" +
                "foliageQuality:\n  desktop: high\n  mobile: low\n  web: low\n" +
                "htmlUiMsaaSamples: 4\nhtmlUiGlyphAtlasMiB: 0\n" +
                "htmlUiSdfPrimitives: true\nhtmlUiTransformAwareText: true\n" +
                "graphics:\n  terrainQuality:\n    macos: high\n    ios: low\n    web: medium\n    android: low\n    linux: high\n    windows: high\n  compressTerrainLayers: true\n" +
                "windowMode: windowed\nwindowWidth: 1280\nwindowHeight: 720\nwindowResizable: true\n" +
                "luaDir: .\nversionControl:\n  lfs: true\n" +
                "startSceneId: " + sceneId + "\nmesh:\n  animationEntities: auto\n" +
                "audio:\n  mixer: audio/Main.mixer\n  outputStage: linear\n  iosSessionCategory: ambient\n" +
                "scenes:\n  - filepath: Main.scene\n    filepathGuid: " + sceneGuid + "\n";
    }

    private String mixerJson() throws Exception {
        JSONObject params = new JSONObject();
        params.put("threshold", -1);
        params.put("release", 0.1);
        params.put("lookahead", 1.5);
        params.put("truePeak", 1);
        JSONObject limiter = new JSONObject();
        limiter.put("id", "74af815b");
        limiter.put("type", "limiter");
        limiter.put("params", params);
        JSONArray effects = new JSONArray().put(limiter);
        JSONObject master = new JSONObject();
        master.put("id", "93fe5fa7");
        master.put("name", "Master");
        master.put("parent", JSONObject.NULL);
        master.put("volume", 0);
        master.put("effects", effects);
        JSONObject mixer = new JSONObject();
        mixer.put("format", "jasperengine.mixer");
        mixer.put("version", 2);
        mixer.put("updateMode", "scaled");
        mixer.put("groups", new JSONArray().put(master));
        return mixer.toString(2) + "\n";
    }


    private String jasperSceneYaml(long sceneId, String sceneGuid, String sceneName) {
        StringBuilder y = new StringBuilder();
        y.append("formatVersion: 8\nguid: ").append(sceneGuid).append("\nid: ").append(sceneId)
                .append("\nname: ").append(yamlQuote(sceneName)).append("\nscene:\n")
                .append("  backgroundColor: [0.25, 0.45, 0.65, 1]\n")
                .append("  shadowQuality: auto\n  meshLodEnabled: true\n  meshLodThreshold: 1\n")
                .append("  depthPrepassEnabled: false\n  lightState: off\n")
                .append("  globalIlluminationIntensity: 0.1\n  globalIlluminationColor: [1, 1, 1]\n")
                .append("  litQuality: high\n  ambientLight2DIntensity: 1\n  ambientLight2DColor: [1, 1, 1]\n")
                .append("  shadow2DQuality: low\n  ssaoEnabled: false\n  ssaoRadius: 0.5\n")
                .append("  ssaoIntensity: 1\n  ssaoBias: 0.025\n  ssrEnabled: false\n")
                .append("  ssrMaxDistance: 8\n  ssrThickness: 0.5\n  ssrMaxSteps: 48\n")
                .append("  ssrIntensity: 1\n  ssrBlur: 0\n  gravity2D: [0, -9.81]\n")
                .append("  gravity3D: [0, -9.81, 0]\n  fixedResolutionEnabled: false\n")
                .append("  fixedResolutionWidth: 640\n  fixedResolutionHeight: 360\n  fixedResolutionFilter: nearest\n")
                .append("  enableUIEvents: not_set\nsceneType: scene_2d\nmainCamera: 0\n")
                .append("shaderKeys:\n  - type: mesh\n    properties: 3075\n  - type: blit\n  - type: blit\n    properties: 4096\n")
                .append("entities:\n");
        ArrayList<SceneView.Node> nodes = scene.snapshot();
        if (nodes.isEmpty()) return y.append("  ~\n").toString();
        long entityId = 1001;
        int index = 0;
        for (SceneView.Node n : nodes) {
            y.append("  - type: Entity\n    entity: ").append(entityId++).append("\n    name: ")
                    .append(yamlQuote(n.name)).append("\n    components:\n      transform:\n")
                    .append("        position: [").append(Math.round(n.x * 1280)).append(", ")
                    .append(Math.round(n.y * 720)).append(", 0]\n")
                    .append("        rotation: [1, 0, 0, 0]\n        scale: [1, 1, 1]\n")
                    .append("        visible: true\n        billboardRotation: [1, 0, 0, 0]\n")
                    .append("        billboard: false\n        fakeBillboard: false\n")
                    .append("        cylindricalBillboard: false\n");
            if (n.physicsEnabled) y.append(rigidbody2DYaml(n));
            if (n.luaCode != null && !n.luaCode.trim().isEmpty()) {
                String guid = java.util.UUID.randomUUID().toString().replace("-", "");
                y.append("      script:\n        scripts:\n          - type: lua\n")
                        .append("            path: scripts/Script_").append(index).append(".lua\n")
                        .append("            pathGuid: ").append(guid).append("\n")
                        .append("            className: ").append(scriptClassName(n.name, n.luaCode)).append("\n")
                        .append("            enabled: true\n");
            }
            index++;
        }
        return y.toString();
    }

    private String rigidbody2DYaml(SceneView.Node node) {
        StringBuilder y = new StringBuilder("      rigidbody2d:\\n")
                .append("        enabled: true\\n")
                .append("        type: ").append(node.bodyType).append("\\n")
                .append("        numColliders: 1\\n")
                .append("        gravityScale: 1\\n        linearDamping: 0\\n        angularDamping: 0\\n")
                .append("        fixedRotation: false\\n        collisionDetection: discrete\\n")
                .append("        sleepingMode: start_awake\\n        useAutoMass: true\\n        mass: 1\\n")
                .append("        autoCenterOfMass: true\\n        centerOfMass: [0, 0]\\n")
                .append("        autoInertia: true\\n        inertia: 0\\n        material: \\\"\\\"\\n")
                .append("        colliders:\\n          - type: ").append(node.colliderType).append("\\n")
                .append("            pointA: [0, 0]\\n            pointB: [0, 0]\\n")
                .append("            radius: ").append("circle".equals(node.colliderType) ? "32" : "0").append("\\n")
                .append("            loop: false\\n            density: 1\\n            friction: 0.6\\n")
                .append("            restitution: 0\\n            sensor: ").append(node.sensor).append("\\n")
                .append("            enableHitEvents: false\\n            contactEvents: true\\n")
                .append("            preSolveEvents: false\\n            sensorEvents: false\\n")
                .append("            categoryBits: 1\\n            maskBits: 65535\\n            groupIndex: 0\\n")
                .append("            points: ");
        if ("circle".equals(node.colliderType)) y.append("[]\\n");
        else y.append("[[-32, -24], [32, -24], [32, 24], [-32, 24]]\\n");
        return y.toString();
    }

    private String scriptClassName(String objectName, String luaCode) {
        if (luaCode != null) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("local\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*=").matcher(luaCode);
            if (matcher.find()) return matcher.group(1);
        }
        String name = objectName == null ? "ObjectScript" : objectName.replaceAll("[^A-Za-z0-9_]", "");
        if (name.isEmpty() || Character.isDigit(name.charAt(0))) name = "ObjectScript";
        return name;
    }

    private String yamlQuote(String value) {
        String safe = value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
        return "\"" + safe + "\"";
    }

    private void saveScene(Uri uri) {
        try {
            JSONArray objects = new JSONArray();
            for (SceneView.Node node : scene.snapshot()) {
                JSONObject item = new JSONObject();
                item.put("name", node.name);
                item.put("x", node.x);
                item.put("y", node.y);
                if (node.luaCode != null) item.put("luaCode", node.luaCode);
                item.put("physicsEnabled", node.physicsEnabled);
                item.put("bodyType", node.bodyType);
                item.put("colliderType", node.colliderType);
                item.put("sensor", node.sensor);
                objects.put(item);
            }
            JSONObject document = new JSONObject();
            document.put("format", "jasperengine-mobile-scene");
            document.put("version", 1);
            document.put("objects", objects);
            try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                if (out == null) throw new IllegalStateException("No se pudo abrir el archivo");
                out.write(document.toString(2).getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            note("Escena guardada. Puedes encontrarla en la carpeta que elegiste.");
        } catch (Exception e) {
            note("No se pudo guardar la escena: " + e.getMessage());
        }
    }

    private void loadScene(Uri uri) {
        try {
            StringBuilder json = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    getContentResolver().openInputStream(uri), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
            }
            JSONObject document = new JSONObject(json.toString());
            if (!"jasperengine-mobile-scene".equals(document.optString("format"))) {
                note("Ese archivo no parece una escena de este prototipo.");
                return;
            }
            JSONArray objects = document.getJSONArray("objects");
            ArrayList<SceneView.Node> nodes = new ArrayList<>();
            for (int i = 0; i < objects.length(); i++) {
                JSONObject item = objects.getJSONObject(i);
                SceneView.Node node = new SceneView.Node(item.optString("name", "Objeto " + (i + 1)),
                        (float) item.optDouble("x", 0.5), (float) item.optDouble("y", 0.5));
                node.luaCode = item.optString("luaCode", null);
                node.physicsEnabled = item.optBoolean("physicsEnabled", false);
                node.bodyType = item.optString("bodyType", "static");
                node.colliderType = item.optString("colliderType", "polygon");
                node.sensor = item.optBoolean("sensor", false);
                nodes.add(node);
            }
            if (nodes.isEmpty()) nodes.add(new SceneView.Node("Objeto 1", 0.5f, 0.5f));
            scene.replaceScene(nodes);
            refreshSelection();
            note("Escena abierta correctamente.");
        } catch (Exception e) {
            note("No se pudo abrir la escena: " + e.getMessage());
        }
    }

    private void editPhysicsForSelected() {
        SceneView.Node node = scene.selected();
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(4));
        TextView explanation = text("El cuerpo define cómo responde el objeto; el collider es su forma para tocar otros cuerpos.",
                13, Color.WHITE);
        form.addView(explanation, new LinearLayout.LayoutParams(-1, dp(54)));
        android.widget.Spinner bodyType = new android.widget.Spinner(this);
        String[] bodyTypes = {"Estático (no se mueve)", "Dinámico (responde a física)", "Cinématico (movido por script)"};
        bodyType.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, bodyTypes));
        bodyType.setSelection("dynamic".equals(node.bodyType) ? 1 : ("kinematic".equals(node.bodyType) ? 2 : 0));
        form.addView(bodyType, new LinearLayout.LayoutParams(-1, dp(48)));
        android.widget.Spinner colliderType = new android.widget.Spinner(this);
        String[] shapes = {"Polígono (caja básica)", "Círculo"};
        colliderType.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, shapes));
        colliderType.setSelection("circle".equals(node.colliderType) ? 1 : 0);
        form.addView(colliderType, new LinearLayout.LayoutParams(-1, dp(48)));
        android.widget.CheckBox sensor = new android.widget.CheckBox(this);
        sensor.setText("Sensor / trigger (atraviesa y detecta)");
        sensor.setTextColor(Color.WHITE);
        sensor.setChecked(node.sensor);
        form.addView(sensor, new LinearLayout.LayoutParams(-1, dp(48)));
        new AlertDialog.Builder(this)
                .setTitle("Física 2D · " + node.name)
                .setView(form)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar ajustes", (dialog, which) -> {
                    node.physicsEnabled = true;
                    node.bodyType = bodyType.getSelectedItemPosition() == 1 ? "dynamic" :
                            (bodyType.getSelectedItemPosition() == 2 ? "kinematic" : "static");
                    node.colliderType = colliderType.getSelectedItemPosition() == 1 ? "circle" : "polygon";
                    node.sensor = sensor.isChecked();
                    note("Cuerpo y collider guardados para exportarlos con la escena.");
                })
                .show();
    }

    private void editLuaForSelected() {
        SceneView.Node node = scene.selected();
        EditText code = new EditText(this);
        code.setGravity(Gravity.TOP | Gravity.START);
        code.setSingleLine(false);
        code.setMinLines(12);
        code.setTextSize(13);
        code.setTypeface(android.graphics.Typeface.MONOSPACE);
        code.setTextColor(Color.WHITE);
        code.setHintTextColor(Color.rgb(150, 160, 176));
        code.setHint("Escribe el script Lua de este objeto");
        code.setText(node.luaCode == null ? defaultLuaScript(node.name) : node.luaCode);
        int padding = dp(12);
        code.setPadding(padding, padding, padding, padding);
        new AlertDialog.Builder(this)
                .setTitle("Script Lua · " + node.name)
                .setView(code)
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Plantilla", (dialog, which) -> {
                    node.luaCode = defaultLuaScript(node.name);
                    note("Plantilla Lua puesta. Pulsa Lua otra vez para editarla.");
                })
                .setPositiveButton("Guardar código", (dialog, which) -> {
                    node.luaCode = code.getText().toString();
                    note("Código Lua guardado en este objeto.");
                })
                .show();
    }

    private String defaultLuaScript(String objectName) {
        String className = objectName == null ? "ObjectScript" : objectName.replaceAll("[^A-Za-z0-9_]", "");
        if (className.isEmpty() || Character.isDigit(className.charAt(0))) className = "ObjectScript";
        return "---@class " + className + " : ScriptInstance\\n" +
                "local " + className + " = {}\\n\\n" +
                "function " + className + ":init()\\n" +
                "    self.go = GameObject(self.scene, self.entity)\\n" +
                "    RegisterEngineEvent(self, \\\"onUpdate\\\")\\n" +
                "end\\n\\n" +
                "function " + className + ":onUpdate()\\n" +
                "    -- Lógica por cuadro (si este script está activo en el motor)\\n" +
                "end\\n\\n" +
                "function " + className + ":onCollisionEnter2D(collision)\\n" +
                "    -- Contacto sólido: requiere Rigidbody2D y collider en la escena\\n" +
                "end\\n\\n" +
                "function " + className + ":onTriggerEnter2D(other)\\n" +
                "    -- Trigger: requiere cuerpo/collider sensor configurado en la escena\\n" +
                "end\\n\\n" +
                "return " + className + "\\n";
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
        b.setTextSize(11);
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

    private static final class SceneView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ArrayList<Node> nodes = new ArrayList<>();
        private int selectedIndex = 0;
        private float downX, downY;
        private boolean dragging;
        private boolean previewMode = false;
        private Runnable onSelectionChanged = () -> {};

        SceneView(android.content.Context context) {
            super(context);
            nodes.add(new Node("Objeto 1", 0.5f, 0.5f));
            setBackgroundColor(Color.rgb(15, 18, 24));
        }

        void setSelectionChanged(Runnable listener) { onSelectionChanged = listener; }
        Node selected() { return nodes.get(selectedIndex); }
        ArrayList<Node> snapshot() { return new ArrayList<>(nodes); }

        void replaceScene(ArrayList<Node> replacement) {
            nodes.clear();
            nodes.addAll(replacement);
            selectedIndex = 0;
            invalidate();
        }

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

        void setPreviewMode(boolean enabled) {
            previewMode = enabled;
            invalidate();
        }

        void advancePreview(float dt) {
            if (!previewMode) return;
            for (Node n : nodes) {
                if ("dynamic".equals(n.bodyType)) {
                    n.y = clamp(n.y - 0.25f * dt);
                }
            }
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
            c.drawText(previewMode ? "PLAY · vista previa 2D (sin motor Lua/física real)" :
                    "Editor · arrastra un objeto", dp(12), dp(20), paint);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX(), y = e.getY();
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                downX = x; downY = y; dragging = false;
                for (int i = nodes.size() - 1; i >= 0; i--) {
                    Node n = nodes.get(i);
                    if (Math.abs(x - n.x * getWidth()) < dp(44) && Math.abs(y - n.y * getHeight()) < dp(34)) {
                        selectedIndex = i;
                        if (!previewMode) dragging = true;
                        onSelectionChanged.run(); break;
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
            String luaCode;
            boolean physicsEnabled = false;
            String bodyType = "static";
            String colliderType = "polygon";
            boolean sensor = false;
            float x, y;
            Node(String name, float x, float y) { this.name = name; this.x = x; this.y = y; }
        }
    }
}
