# JasperEngine Android editor UI prototype

This remains a limited Android editor/preview prototype separate from JasperEngine's native editor and Box2D runtime.

- Create projects in a persisted user-selected folder, autosave changes into each project, and resume the last project from the home screen.
- Choose project type 2D or 3D at creation. 3D is stored as metadata only; its editor, rendering, physics and Play mode are a later stage.
- Add, select, drag, rename and reposition simple 2D objects. Named JavaScript files execute in an isolated preview and receive their attached object as `obj` in `init(scene,obj)` / `update(scene,dt,obj)`.
- Prototype Play applies simple gravity and approximate contacts. The drawing area/frame is not a physics object or floor: dynamic objects fall beyond the visible canvas and disappear from view. Only configured objects can catch them.
- **Sprite / Skin** offers ready-made blue/red/green pixel characters and a 16×16 paint editor with color palette, transparency/erase, clear, naming, saving and applying to the selected object. Pixel data persists in `MobileScene.json` and is exported under `assets/sprites/`; sprites can be reused on other objects in the scene.
- The main controls are arranged in spaced rows with padding and a distinct Play button.
- YAML export is experimental and limited; this is not JasperEngine's native editor/runtime and does not provide full Box2D or native script integration.

Build:

```sh
cd engine/workspaces/androidstudio
./gradlew --no-daemon :editor-prototype:assembleDebug
```
