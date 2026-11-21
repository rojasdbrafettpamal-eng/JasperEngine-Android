# JasperEngine Android editor UI prototype

This is a limited Android editor and preview prototype, separate from JasperEngine's native editor/Box2D runtime.

- The bottom inspector is a draggable, scrollable drawer. It can expand/collapse to show its controls.
- The inspector's manual move arrows were removed to leave room for project behaviours. Physics/collider settings remain available for setting up the prototype's dynamic body.
- **Objetos y scripts** lists every scene object and all named JavaScript scripts attached to it. Add, edit or delete multiple scripts per object. Scripts persist in `MobileScene.json` and export as separate `.js` files.
- At Play each object's scripts run in list order with the same scene state. Each gets `obj` for its attached object and `escena.objetos` to find other objects by name and read/write positions and velocities. Tapping an object in Play sets its `tocado` property for that update, enabling tap-driven arrows and inter-object behaviour. This is prototype-level synchronization, not native engine events or full collision callbacks.
- Prototype Play has simple gravity and approximate contacts. The canvas edge is not a floor; dynamic objects can fall below the visible area. It is not Box2D/JasperEngine native physics.
- **Sprites / skins** provides ready-made pixel sprites and a 16×16 painter. Pixel data is stored in the scene and exported under `assets/sprites/`, and can be reused on other objects in the project.
- 3D remains project metadata only; no 3D editor, rendering, physics or Play runtime in this stage.
- YAML export remains a minimal prototype and is not a complete JasperEngine project.

Build:

```sh
cd engine/workspaces/androidstudio
./gradlew --no-daemon :editor-prototype:assembleDebug
```
