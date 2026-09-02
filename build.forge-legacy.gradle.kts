plugins {
    // Forge below 1.20.5 runs on SRG-named Minecraft, so the jar has to be reobfuscated out of the
    // official names the source is written against. ForgeGradle 7 has no reobfuscation step at all,
    // so those versions build cleanly and then die at runtime with NoSuchMethodError the moment
    // they touch a Minecraft method. ModDevGradle's legacy Forge plugin does reobfuscate, and wires
    // it into `jar` directly, so the rest of this script matches build.forge.gradle.kts.
    id("net.neoforged.moddev.legacyforge") version "2.0.141"
    id("minecraft-mutex")
}

// Fleet artifact convention: {ModName}-{loader}-{mc}-{version}.jar — see build.fabric.gradle.kts.
// The node is still named `-forge`, so the loader constant, the `//? if forge` conditionals and the
// jar name are all identical to the ForgeGradle nodes. Only the toolchain underneath differs.
version = sc.properties.get<String>("mod.version")
base.archivesName = "${sc.properties.get<String>("mod.archive_name")}-forge-${sc.current.version}"

// Declared per version in stonecutter.properties.toml.
val requiredJava: JavaVersion = JavaVersion.toVersion(sc.properties.get<String>("mod.java"))

val forgeVersion: String = sc.properties.get<String>("deps.forge_version")

legacyForge {
    version = "${sc.current.version}-$forgeVersion"

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
            "loader_dep" to "forge",
            "loader_dep_range" to "[$forgeVersion,)",
            "pack_format" to sc.properties.get<String>("mod.pack_format"),
            "java" to requiredJava.majorVersion,
        )
        props.forEach { (k, v) -> inputs.property(k, v) }

        filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) { expand(props) }

        // Only Forge ships META-INF/mods.toml with a "forge" dependency; the other loaders' own
        // metadata must not ship in this jar.
        exclude("META-INF/neoforge.mods.toml")
        exclude("fabric.mod.json")
    }

    // Same ordering requirement as the other loaders: Minecraft artifacts must not be created
    // before Stonecutter has written the processed sources.
    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    withType<JavaCompile>().configureEach {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to build/libs/{mod version}/"
        // Load-bearing: collect reobfJar, NOT jar. Under this plugin `jar` keeps the development
        // artifact, in official names, and its archive is redirected to build/devlibs; reobfJar is
        // the SRG-mapped one that a real 1.20.1 server can load. Collecting `jar` here produces a
        // jar that loads and then dies with NoSuchMethodError on the first Minecraft call, which is
        // precisely the bug this toolchain swap exists to fix.
        val reobf = named("reobfJar")
        dependsOn(reobf)
        from(reobf.map { it.outputs.files }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${sc.properties.get<String>("mod.version")}"))
    }
}
