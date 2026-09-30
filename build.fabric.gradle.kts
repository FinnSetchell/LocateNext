plugins {
    // Applies the Loom variant matching this node's Minecraft version.
    id("dev.kikugie.loom-back-compat")
}

// Do not set `group` — Loom derives coordinates itself.
// Fleet artifact convention: {ModName}-{loader}-{mc}-{version}.jar. Setting it via archivesName
// (rather than the jar task's archiveFileName) keeps the sources jar on the same stem.
version = sc.properties.get<String>("mod.version")
base.archivesName = "${sc.properties.get<String>("mod.archive_name")}-fabric-${sc.current.version}"

// Declared per version in stonecutter.properties.toml.
val requiredJava: JavaVersion = JavaVersion.toVersion(sc.properties.get<String>("mod.java"))

// Blank means "no Mod Menu on this version" — it drops both the dependency and the entrypoint,
// so the shipped fabric.mod.json never names a class that isn't in the jar.
val modmenuVersion: String = sc.properties.get<String>("deps.modmenu")
val modmenuEntrypoint: String =
    if (modmenuVersion.isBlank()) "" else "\"com.finndog.locatenext.client.LocateNextModMenu\""

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // No-op on the un-obfuscated versions; applies Mojang mappings on the obfuscated ones.
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${sc.properties.get<String>("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")

    // Only the ModMenuApi interface is needed, and Fabric loads that entrypoint solely when Mod
    // Menu asks for it — so this stays off both the runtime classpath and the shipped jar.
    // Blank on versions with no usable Mod Menu artifact; see stonecutter.properties.toml.
    if (modmenuVersion.isNotBlank()) {
        modCompileOnly("com.terraformersmc:modmenu:$modmenuVersion")
    }
}

repositories {
    maven("https://maven.terraformersmc.com/releases") { name = "TerraformersMC" }
}

loom {
    runs {
        // Per-node game directory. A shared one would put every version's worlds, options and
        // config in the same folder, so opening a 1.21.1 world under a newer node would upgrade it
        // irreversibly. `project.name` is the node name, e.g. "1.21.1-fabric".
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("../../run/${project.name}")
        }
        named("server") {
            server()
            configName = "Fabric Server"
            ideConfigGenerated(true)
            runDir("../../run/${project.name}")
        }
    }

    mods {
        register(sc.properties.get<String>("mod.id")) {
            sourceSet(sourceSets.main.get())
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
            // Declared floor, not the dev loader — see the note in stonecutter.properties.toml.
            "loader" to sc.properties.get<String>("mod.loader_min"),
            "pack_format" to sc.properties.get<String>("mod.pack_format"),
            "java" to requiredJava.majorVersion,
            "modmenu_entrypoint" to modmenuEntrypoint,
        )
        props.forEach { (k, v) -> inputs.property(k, v) }

        filesMatching(listOf("fabric.mod.json", "pack.mcmeta")) { expand(props) }

        // NeoForge-only metadata must not ship in the Fabric jar.
        exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to build/libs/{mod version}/"
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${sc.properties.get<String>("mod.version")}"))
    }
}

// IntelliJ 2026.2 turns the dots in a node's name into underscores when it names modules
// (Root.1_21_1-fabric.main), but Loom writes its run configurations for Root.1.21.1-fabric.main,
// which then don't run. Once the IDE has imported the project, point them at the name it actually uses.
tasks.named("ideaSyncTask") {
    val ideaDir = rootProject.file(".idea")
    val dotted = "${rootProject.name}.${project.name}."
    val underscored = "${rootProject.name}.${project.name.replace('.', '_')}."
    doLast {
        val modules = File(ideaDir, "modules.xml")
        if (dotted == underscored || !modules.exists() || underscored !in modules.readText()) return@doLast
        File(ideaDir, "runConfigurations").listFiles { f -> f.extension == "xml" }?.forEach { f ->
            val text = f.readText()
            val fixed = text.replace("<module name=\"$dotted", "<module name=\"$underscored")
            if (fixed != text) f.writeText(fixed)
        }
    }
}
