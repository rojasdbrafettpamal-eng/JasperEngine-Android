# JasperEngine Android editor UI prototype

An isolated first-stage Android editor UI shell, separate from the game's Android player/export path. It currently draws a small mock scene that supports adding, selecting, renaming, dragging and nudging objects with touch controls.

The **Abrir** and **Guardar** buttons use Android's system document picker (Storage Access Framework). Save writes a JSON scene file to the folder the user chooses; Open reloads that prototype file. It does not yet use JasperEngine's native project/scene format, and it is not the complete editor.

Build from the repository root:

```sh
cd engine/workspaces/androidstudio
./gradlew --no-daemon :editor-prototype:assembleDebug
```
