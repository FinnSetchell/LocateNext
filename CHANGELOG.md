# Changelog

## [1.3.2] - 2026-09-02

Fixes the mod refusing to load on NeoForge and Forge installs that sit a few patches behind the one
it was built against.

Every jar declared its loader dependency as the exact version it was compiled with, so an install
running anything older was rejected outright before the mod loaded at all. A NeoForge 21.1.230
install refused a jar built against 21.1.249 over a 19-patch gap that changes nothing this mod uses.
Four of the seven NeoForge versions were affected in practice. The declared floor is now the base of
the loader's minor line, so any 21.1.x accepts the 1.21.1 jar. Forge carried the same fault and gets
the same fix, though it happened to bite less often.

This is the same mistake the Fabric side already had a note about, where declaring the development
loader version had locked the mod out of an install pinned slightly behind it.

---

## [1.3.1] - 2026-09-02

Fixes two jars that 1.3.0 shipped broken, and drops a third that cannot currently be built. Every
jar in 1.3.0 compiled, but compiling was never evidence that it would load, and three of them did
not.

NeoForge 1.20.6 was rejected before any of its code ran. The mod asked for language provider javafml
4 or newer while NeoForge 20.6.139 supplies 3.0.45, so the loader refused the file outright. That
floor is now declared per version rather than once for the whole matrix, which is what let a value
valid everywhere else go unnoticed here.

Forge 1.20.1 loaded, announced itself, and then crashed the server the moment a world registered its
commands. Forge only moved its production runtime to official Minecraft names at 1.20.5; below that
it still runs on the older internal names, so a jar built against the official ones has to be
translated on the way out. It never was, so every Minecraft call it made pointed at a method the
running game does not have. That version now builds through a toolchain that performs the
translation.

Forge 1.20.4 is no longer built. It needs the same translation 1.20.1 does, and no available
toolchain performs it for that exact version: one cannot translate at all, and the other stops at
1.20.1. A jar that loads and then crashes is worse than an absent one, so the version is left out
rather than shipped broken. Fabric and NeoForge both still cover 1.20.4.

Every jar in this release has been booted on a real dedicated server and confirmed to load.

---

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
