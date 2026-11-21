# JasperEngine Android editor prototype — arcade preview

The Android editor is still a limited prototype, separate from JasperEngine's native editor and Box2D runtime.

- Play opens a full-screen preview instead of leaving the editor grid and property panels visible. It includes a starfield, layered hills, ground strip, score/time HUD, exit control and restart after a collision.
- Objects named `Cohete`, `Avion`/`Avión`, `Aeronave`, `Personaje` or `Player` are treated as the aircraft. `Meteorito`, `Meteoro` or `Meteor` identifies the obstacle. A `Suelo` / `Piso` / `Ground` / `Floor` object is reserved as the ground marker. Matching is case-insensitive and ignores accents.
- Without custom sprites, the game draws a small spaceship and rocky meteor; assigned pixel-art skins are used as-is.
- One meteor falls through a random horizontal lane, speeds up gradually and returns above the screen in a new lane when it passes the bottom. The score counts dodged meteors. Hitting the aircraft ends the run; tap to restart.
- Existing arrow scripts can control the aircraft. Tapping and holding an arrow exposes `obj.presionado=true`; release resets it. Scene scripts still execute on the prototype runtime.
- This is a lightweight Java preview with approximate collision detection, not a native JasperEngine runtime or Box2D. 3D remains unavailable.

Build: `cd engine/workspaces/androidstudio && ./gradlew --no-daemon :editor-prototype:assembleDebug`
