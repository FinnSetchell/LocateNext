pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
    // Applies the Loom variant matching each node's Minecraft version. Only Loom 1.17+ handles the
    // 26.1 un-obfuscation boundary, so this is what makes adding a 26.x node later a one-liner.
    id("dev.kikugie.loom-back-compat") version "0.4"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        /**
         * Registers one node per loader for a Minecraft version, as `versions/{project}-{loader}`
         * built by `build.{loader}.gradle.kts`.
         *
         * The two-argument [version] call is load-bearing: passing `"1.21.1-fabric"` as a single
         * argument would make SemVer read `fabric` as a pre-release tag, ordering it
         * `1.21 < 1.21.1-fabric < 1.21.1` and silently breaking `//? if >=1.21.1` conditions.
         */
        fun match(project: String, vararg loaders: String, version: String = project) {
            for (loader in loaders) {
                version("$project-$loader", version).buildscript("build.$loader.gradle.kts")
            }
        }

        match("1.20.1", "fabric")
        // 1.21.1 is the reference node — the one that has been tested in-game end to end.
        match("1.21.1", "fabric")
        match("1.21.11", "fabric")
        match("26.1.2", "fabric")
        match("26.2", "fabric")

        // NeoForge nodes. 1.20.4 is the earliest one NeoForge itself supports — it forked from
        // Forge at 1.20.4, so there is no `net.neoforged:neoforge` artifact below it.
        match("1.20.4", "neoforge")
        match("1.20.6", "neoforge")
        match("1.21.1", "neoforge")
        match("1.21.3", "neoforge")
        match("1.21.4", "neoforge")
        match("1.21.5", "neoforge")
        match("1.21.11", "neoforge")

        vcsVersion = "1.21.1-fabric"
    }
}

rootProject.name = "LocateNext"
