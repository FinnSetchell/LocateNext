plugins {
    id("net.minecraftforge.gradle") version "[7.0.29,8.0)"
    id("minecraft-mutex")
}

// Fleet artifact convention: {ModName}-{loader}-{mc}-{version}.jar — see build.fabric.gradle.kts.
version = sc.properties.get<String>("mod.version")
base.archivesName = "${sc.properties.get<String>("mod.archive_name")}-forge-${sc.current.version}"

// Declared per version in stonecutter.properties.toml.
val requiredJava: JavaVersion = JavaVersion.toVersion(sc.properties.get<String>("mod.java"))

val forgeVersion: String = sc.properties.get<String>("deps.forge_version")

minecraft {
    mappings("official", sc.current.version)

    runs {
        // Per-node game directory — see the matching comment in build.fabric.gradle.kts.
        all {
            workingDir.set(rootProject.file("run/${project.name}"))
        }
        create("client")
        create("server") {
            args("--nogui")
        }
    }
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
    mavenCentral()
}

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:${sc.current.version}-$forgeVersion"))
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
            // Shared with NeoForge's legacy mods.toml — see the note in that file.
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

    // Required: Minecraft/Forge artifact setup must not run before Stonecutter has written the
    // processed sources, or Gradle fails with an implicit-dependency validation error — same
    // ordering requirement as NeoForge's createMinecraftArtifacts, see build.neoforge.gradle.kts.
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
