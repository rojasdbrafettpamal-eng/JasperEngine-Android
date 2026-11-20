# JasperEngine Android editor prototype

An isolated Android editor/preview prototype, separate from JasperEngine's native editor and game runtime.

- On launch, the Projects screen lets the user choose one root folder with Android's document picker; that grant is persisted. Projects are created in subfolders and listed for reopening.
- The editor can add/select/drag/rename simple 2D placeholder objects and save/open JSON.
- **JavaScript** opens a full-screen code editor for the selected object. Each script has a `.js` filename, saves in the project `scripts/` folder, and appears as a named row under that project on the Projects screen. `MobileScene.json` persists the association between object, script filename, source, and physics settings.
- Play runs `init(scene)` once and `update(scene, dt)` for saved scripts using the embedded Rhino interpreter. The script receives `scene.objetos`, with object fields `nombre`, `x`, `y`, `vx`, `vy`, `dinamico`, and `fisica`. Scripts can change an object's position or velocity. Java/Android class access is blocked and Rhino applies an instruction limit.
- Play also runs this prototype's simple 2D simulation: gravity affects dynamic bodies; they land on the screen floor and static body colliders; static/dynamic/sensor settings are read from object data. This is not JasperEngine's native Box2D world, and JavaScript is not a native JasperEngine script component (JasperEngine's native script system is Lua).
- **Física 2D / Collider** configures an object as static/dynamic/kinematic, with a polygon or circle collider and optional sensor. Settings are saved in `MobileScene.json` and exported to the limited JasperEngine scene YAML.
- **Guardar proyecto** writes `project.yaml`, `Main.scene`, `MobileScene.json`, `scripts/`, `assets/`, and `audio/Main.mixer` into the active project without asking for the folder again.

This is still a prototype, not a full JasperEngine/Unity-equivalent editor. It does not run JasperEngine itself, execute arbitrary engine APIs, or provide full Box2D contact callbacks, rich rendering, or complete project import/export.

Build from the repository root:

```sh
cd engine/workspaces/androidstudio
./gradlew --no-daemon :editor-prototype:assembleDebug
```
