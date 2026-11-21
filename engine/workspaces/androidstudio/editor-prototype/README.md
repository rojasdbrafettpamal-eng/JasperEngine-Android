# JasperEngine Android editor UI prototype

Isolated mobile editor UI prototype, separate from JasperEngine's game player/exporter.

- Add, select, drag, rename, and nudge simple 2D placeholder objects.
- Save/Open JSON scene snapshots using Android's file picker (prototype interchange only).
- At launch the prototype shows a **Proyectos** home screen. The user chooses or creates a workspace folder once with Android's folder picker; its permission is persisted. Each new project gets its own subfolder automatically, and the home screen lists saved projects for reopening.
- **Guardar proyecto** writes the current project's minimal folder (`project.yaml`, `Main.scene`, `assets/`, `lua/`, and `audio/Main.mixer`) into its selected workspace subfolder, without showing the folder picker every time.
- Opening a listed project loads its placeholder names, positions, and linked Lua text back into the prototype view. It does not run JasperEngine or become the full editor.
- The **Lua** button opens a plain text editor for the selected object. Its code is included in the JSON snapshot and exported as `scripts/Script_N.lua`, linked from the scene. The template exposes JasperEngine's `init` and `onUpdate` lifecycle plus 2D collision callback stubs. This Android prototype does not execute or validate Lua.
- **Física 2D / Collider** adds a serialized `rigidbody2d` component to the selected object, with static/dynamic/kinematic body type, a polygon or circle collider, and an optional sensor/trigger. The settings are stored in prototype JSON and emitted in the JasperEngine scene YAML; the prototype itself still does not simulate physics or run scripts.

The scene YAML follows the repository's current template shape and maps placeholder positions into a 1280x720 2D scene. This remains an experimental limited export: images, scripts, materials, full scene properties, native engine integration, and complete project editing are not implemented.

Build from the repository root:

```sh
cd engine/workspaces/androidstudio
./gradlew --no-daemon :editor-prototype:assembleDebug
```
- Projects autosave in their own folder on edits (debounced), on Android activity pause, and when leaving for the Projects screen. The home screen offers a “Continue project” shortcut for the most recently opened project, and each saved folder remains in the Projects list after restarting the app.
- New-project dialog includes 2D and 3D choices. The chosen dimension is stored in `MobileScene.json`, shown on the home screen and on the editor status label. 3D is only a project-type marker in this stage; the 3D editor, rendering, physics and Play mode are explicitly not implemented yet. The 2D preview does not run for a 3D project.

- Each saved JavaScript script now receives the object it belongs to as the third parameter `obj` in `update(scene, dt, obj)` (and the second parameter in `init(scene, obj)`). It can directly read/write `obj.nombre`, `obj.x`, `obj.y`, `obj.vx`, `obj.vy`, `obj.dinamico`, and `obj.fisica`; `scene.objetos` remains available to find or affect other objects. Coordinates are normalized (0–1), with `y` increasing downward. Script runtime errors are surfaced as a Toast and in the preview.
