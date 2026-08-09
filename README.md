# LocateNext

An internal dev tool for testing structure mods. Pick a mod id, then walk its entire structure list
with the arrow keys — each press locates the next structure and teleports you to it, so you never
type `/locate` or track your place in the list again.

Fabric, Minecraft 1.21.1, built with Stonecutter.

## Usage

1. `/locatenext mod moogsvoyagerstructures` — or press `\` and pick it from the menu.
2. Arrow keys drive everything:

| Key | Does |
| --- | --- |
| `→` | Next structure in the mod's list |
| `←` | Previous structure |
| `↑` | A **different instance** of the structure you're on |
| `↓` | Back to the instance of that structure you were at before |
| `\` | Open the menu |

`←`/`→` move *across* structures, `↑`/`↓` move *through instances* of one — so you can line up
half a dozen variations of the same piece and flick between them.

Each jump reports the index, the structure id, the landing coordinates, the distance and bearing,
and how long the search took:

```
[LN] 3/17  moogsvoyagerstructures:big_bridge  variant 2/3
[LN]   at 1234, 78, -5600   3,412 blocks SE   in 412 ms
[◀ prev] [next ▶]  [↑ new] [↓ back]  [home]
```

Coordinates are click-to-copy; the nav bar buttons are clickable too.

## Always landing somewhere new

The thing that makes repeated `/locate` tedious is that it keeps handing back the same nearest
structure. LocateNext defaults to **fresh-only** mode, which is vanilla's own `skipKnownStructures`
flag — the same mechanism that stops a village cartographer from selling you two maps to the same
monument. The generator refuses instances that have already been referenced, and marks each hit as
referenced on the way out, so every search returns a structure nobody has been sent to yet.

On top of that, LocateNext keeps a per-structure history of everywhere it has sent you:

- `↑` walks forward through that history, and searches for a genuinely new instance once it runs
  off the end. If a search does hand back somewhere you've already been, it marches the search
  origin outward by one placement-grid cell and tries again.
- `↓` walks back through instances you've already seen — no search, so it's instant.
- `←`/`→` return you to the instance of that structure you were last at, rather than re-rolling it.

Turn it off with `/ln fresh false` to get plain vanilla "nearest wins" behaviour.

## Commands

`/locatenext`, aliased to `/ln`. Requires permission level 2.

| Command | What it does |
| --- | --- |
| `mod <modid>` | Select a mod and build its ordered structure list |
| `mods` | List every namespace that registers structures, with counts |
| `list` | Numbered list of the selected mod's structures; visited ones are dimmed, current is green |
| `next` / `prev` | Step across structures and jump |
| `variant next` / `variant prev` | Step through instances of the current structure |
| `variant list` | Every instance of the current structure you've been to |
| `goto <n>` | Jump straight to entry *n* (1-based, matching `list`) |
| `home` | Return to where you were before your first jump |
| `status` | Current mod, position and settings |
| `sweep` | Locate every structure in the mod without teleporting, and report which ones can't be found |
| `radius <chunks>` | Search radius, default 100 (same as vanilla `/locate`) |
| `fresh <true\|false>` | Only land on instances nobody has been sent to. Default **true** |
| `autodim <true\|false>` | Hop to whichever dimension a structure belongs to. Default **true** |
| `clear` | Drop the selection, history and saved home position |

## What it handles for you

- **Dimensions.** A Nether or End structure searched from the Overworld would normally just fail.
  With `autodim` on, LocateNext finds the dimension whose biome source actually accepts the
  structure, scales your coordinates across the portal ratio, searches there, and teleports you.
- **Landing safely.** In dimensions with a ceiling the heightmap returns the bedrock roof, so the
  Nether gets a downward scan for a real air pocket instead.
- **Impossible structures.** If no loaded dimension's biome source overlaps the structure's biome
  list, you get told that immediately rather than waiting out a full-radius search — the same check
  vanilla `/locate` does.
- **Datapack reloads.** The structure registry is datapack-driven, so `/reload` re-pushes the index
  to every client and rebuilds selections in place.

`sweep` is the one worth knowing about: it runs a locate for every structure in the mod and prints
a ✔/✘ per entry. One command tells you which of your structures the generator refuses to place.
It deliberately runs with fresh-only *off*, because marking every structure in the world as
referenced as a side effect of surveying them would poison later navigation.

## Notes

- The search runs on the server thread, exactly like vanilla `/locate`. At large radii it will
  visibly hang the game for a few seconds. `ChunkGenerator#findNearestMapStructure` touches the
  chunk source and isn't safe to run off-thread, so this is deliberate.
- Fresh-only mode is slower than plain `/locate`: it has to generate chunks to `STRUCTURE_STARTS`
  to know whether an instance has been referenced. That's the price of never landing twice.
- The first jump into untouched terrain also has to generate the landing chunk in full before it
  can work out a surface height — measured at ~7 s in testing. Later jumps in the same area are
  near-instant (0–300 ms), and revisiting an instance you've already been to never searches at all.
- Arrow keys are unbound in vanilla, so nothing is stolen. They're rebindable under **LocateNext**
  in Controls, and they only fire when no screen is open.
- Works on a dedicated server too, as long as the client has the mod. Without it, commands still
  work; only the keybinds and menu need the client side.

## Building

```bash
./gradlew build
```

Output lands in `versions/1.21.1-fabric/build/libs/`. `./gradlew buildAndCollect` collects every
node's jar into `build/libs/{version}/`.

### Adding a Minecraft version

Add a `match("1.21.4", "fabric")` line to `settings.gradle.kts` and the matching `["1.21.4"]` /
`[fabric."1.21.4"]` tables to `stonecutter.properties.toml`. Stonecutter generates the node; any
API differences get `//? if >=1.21.4 { ... //?}` comments in the shared source tree.

## AI usage disclaimer

This mod was written by Claude (Anthropic's Opus 5) working from my specification, in a Claude Code
session. I directed the design and reviewed the result; the code, the comments and this README were
AI-generated.

It is a personal development tool, not a released mod — it is not tuned for performance on busy
servers, and it teleports players and mutates structure-reference state in the world it runs in.
Point it at development worlds, not at anything you care about.

## Licence

MIT.
