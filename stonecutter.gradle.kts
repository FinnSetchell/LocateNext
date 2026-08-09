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

    // Provides `//? if fabric { ... //?}` conditionals so a NeoForge node can share this source
    // tree later without restructuring it.
    constants {
        match(loader, "fabric", "neoforge")
    }
}
