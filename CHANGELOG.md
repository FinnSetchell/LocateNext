# Changelog

## [1.3.0] - 2026-08-31

Adds NeoForge and Forge alongside Fabric, and fills in the missing Minecraft versions, taking the
build from 5 jars to 25.

Fabric gains 1.20.4, 1.20.6, 1.21.3, 1.21.4 and 1.21.5. NeoForge covers 1.20.4 through 1.21.11.
Forge covers 1.20.1 through 1.21.11. 26.1.2 and 26.2 stay Fabric-only.

Networking is where the loaders diverge most, so it stays in `Net`/`ClientNet` rather than at the
call sites, the same split 1.2.0 introduced for the payload-API boundary. Each loader needs its own
registration and send path, and within a loader those paths changed again mid-range: NeoForge 1.20.4
uses its fork's older `RegisterPayloadHandlerEvent`, 1.20.6 onward uses `PayloadRegistrar`, and
1.21.11 moves client-to-server sends onto `ClientPacketDistributor`. Forge keeps `SimpleChannel`
throughout, but 1.20.1 predates `CustomPacketPayload` entirely, and 1.21.11's EventBus 7 moves
command, keybind, tick and datapack-sync events off the shared bus onto dedicated ones.

Two version boundaries in the existing source turn out to have been wrong, and adding versions in the
1.20.2 to 1.20.6 window is what exposed them. `ResourceLocation`'s named factory arrived in 1.21, not
1.20.5, so 1.20.5 and 1.20.6 still take the public constructor. `SavedData.Factory` arrived in 1.20.2
but did not take a registry lookup until 1.20.5, which is three eras rather than the two the code
assumed, and both NeoForge and Forge ship their own 1.20.4 variants on top of that. Neither could
fire before, because no node existed in that window.

Loader-specific entrypoints live in flat per-loader files rather than nested conditionals, because
Stonecutter does not resolve `//? if` markers nested inside another conditional's commented-out
region.

Known gap: every jar builds, but the NeoForge and Forge nodes have not been run in-game. Fabric
1.21.1 and 1.21.11 remain the only paths tested end to end. Forge 1.21.11 is the least proven of the
set, since EventBus 7 is a large change and it has only been compiled against.

---

## [1.2.0] - 2026-08-12

Adds 1.20.1, bringing the supported set to 1.20.1, 1.21.1, 1.21.11, 26.1.2 and 26.2.

1.20.1 predates the payload networking system entirely — `CustomPacketPayload`, `StreamCodec` and
the codec registry all arrived in 1.20.5 — so it needed a second networking path rather than a
rename. Each payload now owns its buffer read/write, which is identical on every version, and only
registration and sending differ. That split lives in `Net`/`ClientNet` instead of at each call
site. One difference there is load-bearing: the old API hands packets to the netty thread, so those
handlers hop to the server thread explicitly before touching any world state.

Persistence is a three-way split now (1.20.1, 1.21.1-era, 1.21.5+), all sharing the one Codec.

---

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
