# JasperEngine Android editor UI prototype

Isolated mobile editor UI prototype, separate from JasperEngine's game player/exporter.

- Add, select, drag, rename, and nudge simple 2D placeholder objects.
- Save/Open JSON scene snapshots using Android's file picker (prototype interchange only).
- **Proyecto JE** exports a minimal project folder with `project.yaml`, `Main.scene`, `assets/`, `lua/`, and a default `audio/Main.mixer`. The scene YAML follows the repository's current template shape and converts placeholder positions to a 1280x720 scene. This is an experimental export, not a complete JasperEngine editor project: images, scripts, materials, full scene properties and direct project importing are not implemented.

Build from the repository root:

```sh
cd engine/workspaces/androidstudio
./gradlew --no-daemon :editor-prototype:assembleDebug
```
