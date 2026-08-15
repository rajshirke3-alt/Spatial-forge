# SpatialForge — Phase 1 Prototype

A native Android 3D viewport prototype, built to the "First Build Requirement" scope from
the spec: real OpenGL ES 3.0 rendering, orbit/pan/zoom navigation, primitive creation,
select/move/rotate/scale/delete, undo/redo, and save/load. This is the foundation the
later drawing, sculpting, materials, and architectural-tools phases build on top of.

## ⚠️ Status: unbuilt / untested

This was written directly as source in a non-Android environment with no SDK, emulator,
or device available — so it has **not** been compiled or run. Treat it as a strong,
complete starting point, not a verified build. The most likely rough edges on first
compile are minor Gradle/AGP or ViewBinding wiring issues, not the 3D logic itself.

## Opening the project

1. Open the `SpatialForge/` folder in a recent Android Studio (Ladybird/Meerkat or newer).
2. Let Gradle sync (uses AGP 8.6, Kotlin 1.9.24, compileSdk/targetSdk 34, minSdk 26).
3. Run on a real Android device or tablet — the emulator's GL driver is often unreliable
   for OpenGL ES 3.0; a physical device is strongly recommended, per the spec's own
   Section 35 requirement to validate on real hardware before continuing.

## What's implemented

- **Real 3D rendering** — OpenGL ES 3.0, GLSurfaceView, custom vertex/fragment shaders
  (Lambert + specular + rim-highlight for the selected object), a ground grid, one
  directional light.
- **Navigation** — one-finger orbit, two-finger pan, pinch zoom (`OrbitCamera`,
  `GestureController`).
- **Primitives** — cube, sphere, cylinder, generated procedurally with proper normals
  (`MeshFactory`), added via the bottom toolbar.
- **Selection** — tap-to-pick using a screen-to-world ray against each object's bounding
  sphere (`RayPicker`); selected object gets a rim-light highlight.
- **Transform tools** — Move (drag on camera-facing plane), Rotate (drag = yaw/pitch),
  Scale (vertical drag), each a distinct toolbar mode; Delete removes the selection.
- **Undo/redo** — command pattern (`Command`, `UndoManager`); add, delete, and transform
  are all undoable without reloading the scene.
- **Save/load** — scene serialized to JSON in app-internal storage (`.f3dproject`-style
  format: geometry type, transform, color); autosaves on backgrounding.
- **Performance HUD** — live FPS / object count / vertex count, per spec Section 26.

## What's intentionally not here yet (later phases per the spec)

Drawing/curves, extrude/loft, sculpting, materials & lighting editor, camera
save/perspective-ortho toggle, reference images, import/export (GLB/OBJ/STL), grid
snapping & measurement, layers panel, multi-project browsing, onboarding flow. Building
those on a shaky Phase 1 would violate the spec's own "don't build everything at once"
rule (Section 33) — so this stops exactly at the Phase 1 boundary.

## Known simplifications worth knowing about

- Only one save slot right now (`current_project.f3dproject`) — no named multi-project
  browser yet; that's Phase-1-appropriate but you'll want it before Phase 2.
- Rotate/Scale are drag-based rather than on-screen 3D gizmo handles — reliable and
  simple to use, but not the "visible 3D transform handles" from the fuller spec
  (Section 9). Worth upgrading once the core interaction is validated on-device.
- Selection uses bounding-sphere ray tests, not per-triangle — fine at this object
  count/complexity, will need tightening once meshes get denser.
