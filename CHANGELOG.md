# Changelog

## [1.1.0] - 2026-08-11

Adds 1.21.11, 26.1.2 and 26.2 alongside 1.21.1. One source tree, one jar per version.

- Persistence now uses a single Codec shared by every version, replacing the hand-rolled NBT.
  26.1 dropped `SavedData`'s tag methods entirely in favour of a Codec, and keeping two
  serializations in step would have been a standing source of save-format drift. Existing saves
  are unaffected — the field names and encoding are unchanged, verified by loading a
  pre-1.1.0 world.
- Mod Menu integration is 1.21.1-only for now: its newer artifacts still carry intermediary
  references that Loom will not remap against an un-obfuscated Minecraft. Everything else works
  on every version.

Known gap: 1.21.1 and 1.21.11 were tested in-game end to end. 26.1.2 and 26.2 are verified to
build and boot with the mod loaded, but nothing beyond that — no bot speaks those protocols yet.

---

## [1.0.0] - 2026-08-09

Initial release.

- Pick a mod with `/locatenext mod <modid>` or the `\` menu, then walk its structures with the
  arrow keys: left/right across the mod's structure list, up/down through instances of the one
  you're on.
- Fresh-only by default, so every search lands on a structure nobody has been sent to before.
- Auto-dimension: finds the dimension whose biome source accepts a structure, scales your
  coordinates across the portal ratio, and teleports you there.
- Reports distance, bearing and search time per jump; coordinates are click-to-copy.
- `/locatenext sweep` surveys every structure in a mod and reports which ones won't place.
- Selection, cursor, settings and instance history are saved into the world.
- Mod Menu's config button opens the picker.

---
