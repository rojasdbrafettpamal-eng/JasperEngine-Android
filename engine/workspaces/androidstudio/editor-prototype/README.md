# JasperEngine Android editor UI prototype

This is an isolated first-stage Android UI shell. It is deliberately separate from the Android game/player app, so it does not alter or claim to replace game export. The scene is temporary in-memory mock data: add/select/rename/drag objects and move them with touch controls. Open/save are clearly marked as not connected yet.

Next integration step: replace the mock scene with JasperEngine editor/engine data and implement Android lifecycle, project storage, and touch-aware editor tools. This prototype is a UI test, not the complete editor and not yet validated on a device.

Build from the repository root with:

```sh
cd engine/workspaces/androidstudio
./gradlew --no-daemon :editor-prototype:assembleDebug
```
