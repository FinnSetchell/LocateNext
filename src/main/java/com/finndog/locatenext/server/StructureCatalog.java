package com.finndog.locatenext.server;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Reads the server's dynamic {@link Structure} registry and groups every entry by namespace.
 *
 * <p>Rebuilt on demand rather than cached, because the registry is datapack-driven: a
 * {@code /reload} or a world with different datapacks changes it. Callers that need it repeatedly
 * inside one operation should hold the returned map.
 */
public final class StructureCatalog {

    private StructureCatalog() {
    }

    public static Registry<Structure> registry(MinecraftServer server) {
        return server.registryAccess().registryOrThrow(Registries.STRUCTURE);
    }

    /** Namespace -> structure ids, both sorted, so index N always means the same structure. */
    public static Map<String, List<ResourceLocation>> byNamespace(MinecraftServer server) {
        return group(registry(server).keySet());
    }

    public static Map<String, List<ResourceLocation>> group(Iterable<ResourceLocation> ids) {
        Map<String, List<ResourceLocation>> grouped = new TreeMap<>();
        for (ResourceLocation id : ids) {
            grouped.computeIfAbsent(id.getNamespace(), k -> new ArrayList<>()).add(id);
        }
        Map<String, List<ResourceLocation>> sorted = new LinkedHashMap<>();
        grouped.forEach((namespace, list) -> {
            list.sort(Comparator.comparing(ResourceLocation::getPath));
            sorted.put(namespace, List.copyOf(list));
        });
        return sorted;
    }

    /** Flat, sorted id list — the wire format for the client-side index. */
    public static List<ResourceLocation> allIds(MinecraftServer server) {
        List<ResourceLocation> ids = new ArrayList<>(registry(server).keySet());
        ids.sort(Comparator.comparing(ResourceLocation::toString));
        return ids;
    }

    /**
     * Whether {@code structure} has any of its allowed biomes in this level's biome source.
     *
     * <p>Same check vanilla's {@code /locate structure} does before searching — without it, asking
     * for a Nether structure from the Overworld burns a full radius search to find nothing.
     */
    public static boolean canGenerateIn(ServerLevel level, Structure structure) {
        Set<Holder<Biome>> possible = level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
        return structure.biomes().stream().anyMatch(possible::contains);
    }

    /**
     * The first loaded dimension that could contain {@code structure}, preferring the one the
     * player is already in.
     */
    public static Optional<ServerLevel> findLevelFor(MinecraftServer server, ServerLevel preferred, Structure structure) {
        if (canGenerateIn(preferred, structure)) {
            return Optional.of(preferred);
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level != preferred && canGenerateIn(level, structure)) {
                return Optional.of(level);
            }
        }
        return Optional.empty();
    }
}
