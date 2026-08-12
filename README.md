# LocateNext

An internal dev tool for testing structure mods. Pick a mod id, then walk its entire structure list
with the arrow keys — each press locates the next structure and teleports you to it, so you never
type `/locate` or track your place in the list again.

Fabric, built with Stonecutter. One jar per Minecraft version:

| Version | Java | Notes |
| --- | --- | --- |
| 1.20.1 | 17 | Uses the pre-1.20.5 networking API |
| 1.21.1 | 21 | Reference version — includes Mod Menu integration |
| 1.21.11 | 21 | |
| 26.1.2 | 25 | |
| 26.2 | 25 | |

## Usage

1. `/locatenext mod [modid]` — or press `\` and pick it from the menu.
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
[LN] 3/17  [modid]:example_structure  variant 2/3
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

## It remembers where you were

Your selected mod, your place in its list, your settings and every instance you've been sent to are
saved into the world, so they survive quitting to title and restarting the game. Pick a mod once
and pick up where you left off next session.

It's stored per-world rather than globally, because most of what it holds only means something in
one save: the variant histories are literal block positions, and the fresh-only mode they rely on
is backed by that world's structure references.

Two consequences worth knowing:

- The structure *list* is rebuilt from the registry on load rather than stored, so adding or
  removing a structure mod between sessions is handled — your position is clamped to the new list,
  and a selection whose mod is gone is dropped rather than left dangling.
- Like all world data, it's written when the world saves. A hard crash can lose changes since the
  last autosave; `/save-all` forces it.

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

Mod Menu's config button opens the picker, if you have Mod Menu installed. It's a `compileOnly`
dependency, so nothing is bundled and the mod runs fine without it.

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

## Releasing

Tags are `<version>-<label>`, e.g. `1.0.0-fabric`. Pushing one builds the mod and cuts a GitHub
release with the jar attached.

```bash
git tag 1.0.0-fabric && git push origin 1.0.0-fabric
```

Before tagging, make sure `mod.version` in `stonecutter.properties.toml` and the `## [version]`
section in `CHANGELOG.md` both match — the workflow hard-fails on either being stale rather than
shipping the wrong thing quietly.

This is deliberately *not* the release-actions v2 system the other mods use. v2 requires at least
one storefront project (`platforms` is mandatory in its manifest) and cuts the GitHub release
downstream of the CurseForge/Modrinth upload, so it cannot do GitHub-only. Everything here is
shaped like v2 — same tag form, same jar naming, same changelog rules — so moving over once this
mod has storefront projects means deleting `.github/workflows/release.yml`, adding the three thin
caller workflows and a `moogs-publish.yml`. Tags cut now stay valid.

Output lands in `versions/{version}-fabric/build/libs/`.

### Adding a Minecraft version

Add a `match("1.21.4", "fabric")` line to `settings.gradle.kts` and the matching `["1.21.4"]` /
`[fabric."1.21.4"]` tables to `stonecutter.properties.toml`, then build the node and let the
compiler tell you what moved. API differences get `//? if >=1.21.4 { ... //?}` conditionals in the
shared source tree.

Two rules worth keeping, both learned the hard way:

- Use `string(...)` replacements for whole-tree renames, never `regex(...)` — a regex replacement
  emits JSON the IntelliJ plugin can't model, and the IDE then drops the whole project.
- Put the conditional at the version the API *actually* changed at, not the version you happened
  to be porting to. Several changes here look like 26.1 from a 1.21.1 vantage point but really
  landed at 1.21.11; adding the 1.21.11 node is what surfaced them.

## AI usage disclaimer

This mod was written by Claude (Anthropic's Opus 5) working from my specification, in a Claude Code
session. I directed the design and reviewed the result; the code, the comments and this README were
AI-generated.

It is a personal development tool, not a released mod — it is not tuned for performance on busy
servers, and it teleports players and mutates structure-reference state in the world it runs in.
Point it at development worlds, not at anything you care about.

## Licence

MIT.
