package com.finndog.locatenext.server;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Every player's navigation state, saved into the world.
 *
 * <p>Attached to the overworld rather than kept globally because most of what it holds is only
 * meaningful in one world: the variant histories are literal block positions, and the
 * fresh-only mode they lean on is backed by that world's structure references. Carrying them into
 * a different save would point you at coordinates that mean nothing there.
 */
public final class LocateNextSavedData extends SavedData {

    private static final String FILE_ID = "locatenext_navigation";

    private final Map<UUID, NavigationState> states = new HashMap<>();

    private LocateNextSavedData() {
    }

    public static LocateNextSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), FILE_ID);
    }

    // Null DataFixTypes: this is the mod's own data, so vanilla's fixers have nothing to say
    // about it.
    private static SavedData.Factory<LocateNextSavedData> factory() {
        return new SavedData.Factory<>(LocateNextSavedData::new, LocateNextSavedData::load, null);
    }

    private static LocateNextSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LocateNextSavedData data = new LocateNextSavedData();
        CompoundTag players = tag.getCompound("players");
        for (String key : players.getAllKeys()) {
            try {
                data.states.put(UUID.fromString(key), NavigationState.load(players.getCompound(key)));
            } catch (IllegalArgumentException malformedUuid) {
                // A key that isn't a UUID can only come from a hand-edited file; drop that entry
                // rather than fail the whole load and lose everyone else's state.
                LocateNextSavedData.warn(key);
            }
        }
        return data;
    }

    private static void warn(String key) {
        com.finndog.locatenext.LocateNext.LOGGER.warn("Ignoring malformed player key '{}' in {}", key, FILE_ID);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag players = new CompoundTag();
        this.states.forEach((uuid, state) -> players.put(uuid.toString(), state.save()));
        tag.put("players", players);
        return tag;
    }

    public NavigationState state(UUID uuid) {
        return this.states.computeIfAbsent(uuid, ignored -> new NavigationState());
    }
}
