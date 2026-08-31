plugins {
    id("net.neoforged.moddev") version "2.0.141"
    id("minecraft-mutex")
}

// Fleet artifact convention: {ModName}-{loader}-{mc}-{version}.jar — see build.fabric.gradle.kts.
version = sc.properties.get<String>("mod.version")
base.archivesName = "${sc.properties.get<String>("mod.archive_name")}-neoforge-${sc.current.version}"

// Declared per version in stonecutter.properties.toml.
val requiredJava: JavaVersion = JavaVersion.toVersion(sc.properties.get<String>("mod.java"))

// 1.20.4 predates vanilla's own CustomPacketPayload (added in 1.20.5) and NeoForge's
// neoforge.mods.toml (added alongside it) — see net/Net.java and the manifest split below.
val legacy: Boolean = sc.current.version == "1.20.4"

neoForge {
    version = sc.properties.get<String>("deps.neo_loader")

    mods {
        register(sc.properties.get<String>("mod.id")) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        // Per-node game directory — see the matching comment in build.fabric.gradle.kts.
        register("client") {
            client()
            gameDirectory = rootProject.file("run/${project.name}")
        }
        register("server") {
            server()
            gameDirectory = rootProject.file("run/${project.name}")
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        val props = mapOf(
            "id" to sc.properties.get<String>("mod.id"),
            "name" to sc.properties.get<String>("mod.name"),
            "version" to sc.properties.get<String>("mod.version"),
            "description" to sc.properties.get<String>("mod.description"),
            "author" to sc.properties.get<String>("mod.author"),
            "license" to sc.properties.get<String>("mod.license"),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "neo_loader" to sc.properties.get<String>("deps.neo_loader"),
            "pack_format" to sc.properties.get<String>("mod.pack_format"),
            "java" to requiredJava.majorVersion,
            // Only consumed by the legacy (1.20.4) mods.toml below — shared with Forge, which
            // needs its own loader modid and range in the same template. See that file's comment.
            "loader_dep" to "neoforge",
            "loader_dep_range" to "[${sc.properties.get<String>("deps.neo_loader")},)",
        )
        props.forEach { (k, v) -> inputs.property(k, v) }

        // 1.20.4 ships the classic mods.toml; 1.20.5+ ships neoforge.mods.toml. Only one of the
        // two is ever templated or shipped — the other is dropped so its unreplaced `${}`
        // placeholders never reach a jar.
        if (legacy) {
            filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) { expand(props) }
            exclude("META-INF/neoforge.mods.toml")
        } else {
            filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) { expand(props) }
            exclude("META-INF/mods.toml")
        }

        // Fabric-only metadata must not ship in the NeoForge jar.
        exclude("fabric.mod.json")
    }

    // Required: moddev's Minecraft artifacts must not be created before Stonecutter has written
    // the processed sources, or Gradle fails with an implicit-dependency validation error.
    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    // Same ordering requirement as above, for the same reason — see build.fabric.gradle.kts.
    withType<JavaCompile>().configureEach {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to build/libs/{mod version}/"
        from(jar.flatMap { it.archiveFile }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${sc.properties.get<String>("mod.version")}"))
    }
}
