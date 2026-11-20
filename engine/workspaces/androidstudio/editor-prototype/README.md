# JasperEngine Android editor UI prototype

Isolated mobile editor UI prototype, separate from JasperEngine's game player/exporter.

- Add, select, drag, rename, and nudge simple 2D placeholder objects.
- Save/Open JSON scene snapshots using Android's file picker (prototype interchange only).
- **Proyecto JE** exports a minimal project folder with `project.yaml`, `Main.scene`, `assets/`, `lua/`, and a default `audio/Main.mixer`.
- **Abrir proyecto JE** selects one of those exported folders and loads its placeholder names, positions, and linked Lua text back into the prototype view. It does not run JasperEngine or become the full editor.
- The **Lua** button opens a plain text editor for the selected object. Its code is included in the JSON snapshot and exported as `scripts/Script_N.lua`, linked from the scene. The template exposes JasperEngine's `init` and `onUpdate` lifecycle; users can edit it for the engine's documented APIs, including physics callbacks, but this Android prototype does not execute or validate Lua.

The scene YAML follows the repository's current template shape and maps placeholder positions into a 1280x720 2D scene. This remains an experimental limited export: images, scripts, materials, full scene properties, native engine integration, and complete project editing are not implemented.

Build from the repository root:

```sh
cd engine/workspaces/androidstudio
./gradlew --no-daemon :editor-prototype:assembleDebug
```
