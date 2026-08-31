plugins {
    id("dev.kikugie.stonecutter")
}

// Must be a string literal — Stonecutter rewrites this line when switching versions, and an
// assignment from a variable would compile but silently never update.
stonecutter active "1.21.1-fabric"

// Registers each node's buildAndCollect with Stonecutter so it sequences them across the tree.
// `./gradlew buildAndCollect` (unqualified, so Gradle runs it in every node project) then builds
// the whole matrix in one invocation.
stonecutter tasks {
    named("buildAndCollect")
}

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    // Lets `[fabric."1.21.1"]`-style tables in stonecutter.properties.toml resolve for this node.
    properties {
        tags(version, loader)
    }

    // Provides `//? if fabric { ... //?}` conditionals so a NeoForge or Forge node can share this
    // source tree without restructuring it.
    constants {
        match(loader, "fabric", "neoforge", "forge")
    }

    replacements {
        // 1.21.11 renamed ResourceLocation to Identifier. It appears in ~60 places across the
        // catalog, payloads, persistence and client index, so it is handled as a whole-tree
        // replacement rather than by wrapping every site in a conditional.
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }

        // Do NOT add a `regex(...)` replacement here, for any rename. The IntelliJ plugin's model
        // requires `phase` and `identifier` on RegexReplacement, and Stonecutter 0.9.7's
        // RegexReplacementSpec exposes neither, so the IDE gives up on the whole project model
        // and version switching stops working.
        //
        // 26.1's GuiGraphics -> GuiGraphicsExtractor rename is deliberately NOT handled here
        // either: replacements rewrite prose in comments as readily as code, and the name is a
        // substring of unrelated accessors. It is covered by ordinary `//? if >=26.1`
        // conditionals at its six sites in LocateNextScreen.
    }
}
