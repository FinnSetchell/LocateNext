package com.finndog.locatenext.server;

import com.finndog.locatenext.LocateNext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
//? if >=1.21.5 {
/*import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;
*///?} else {
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
//?}
//? if >=1.20.5 && <1.21.5 {
import net.minecraft.core.HolderLookup;
//?}

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
 *
 * <p>Serialization is one {@link Codec} for every version. 1.21.5 replaced {@code SavedData}'s
 * {@code save(CompoundTag)} with a {@code SavedDataType} carrying a Codec, so the older path
 * simply runs the same Codec through {@code NbtOps}. Only the wiring below is version-specific;
 * the actual shape of the saved data is declared once, in {@link NavigationState#CODEC}.
 */
public final class LocateNextSavedData extends SavedData {

    private static final String FILE_ID = "locatenext_navigation";

    public static final Codec<LocateNextSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, NavigationState.CODEC)
                    .optionalFieldOf("players", Map.of()).forGetter(data -> data.states)
    ).apply(instance, LocateNextSavedData::new));

    private final Map<UUID, NavigationState> states = new HashMap<>();

    private LocateNextSavedData() {
    }

    private LocateNextSavedData(Map<UUID, NavigationState> states) {
        this.states.putAll(states);
    }

    // SavedDataType's id was a plain String until 26.1 made it an Identifier.
    //? if >=26.1 {
    /*private static final net.minecraft.resources.Identifier TYPE_ID = LocateNext.id(FILE_ID);
    *///?} else {
    private static final String TYPE_ID = FILE_ID;
    //?}

    //? if >=1.21.5 {
    /*private static final SavedDataType<LocateNextSavedData> TYPE = new SavedDataType<>(
            TYPE_ID, LocateNextSavedData::new, CODEC, DataFixTypes.LEVEL);

    public static LocateNextSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
    *///?}

    // Three independent, mutually exclusive conditions rather than an if/elif/else chain: the
    // chain silently selected the wrong arm here, and non-overlapping guards cannot.
    //? if >=1.20.5 && <1.21.5 {
    public static LocateNextSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), FILE_ID);
    }

    // Null DataFixTypes: this is the mod's own data, so vanilla's fixers have nothing to say
    // about it.
    private static SavedData.Factory<LocateNextSavedData> factory() {
        return new SavedData.Factory<>(LocateNextSavedData::new, LocateNextSavedData::load, null);
    }

    private static LocateNextSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return decode(tag);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return encode(tag);
    }
    //?}

    // 1.20.2 introduced SavedData.Factory; before it, the storage takes the loader and the
    // constructor as separate arguments and neither side sees a registry lookup.
    //? if <1.20.2 {
    /*public static LocateNextSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(LocateNextSavedData::decode, LocateNextSavedData::new, FILE_ID);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        return encode(tag);
    }
    *///?}

    // 1.20.2 through 1.20.4: SavedData.Factory exists, but its loader and SavedData#save still
    // predate the registry lookup 1.20.5 added to both.
    //? if >=1.20.2 && <1.20.5 {
    /*public static LocateNextSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), FILE_ID);
    }

    private static SavedData.Factory<LocateNextSavedData> factory() {
        return new SavedData.Factory<>(LocateNextSavedData::new, LocateNextSavedData::decode, null);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        return encode(tag);
    }
    *///?}

    //? if <1.21.5 {
    private static LocateNextSavedData decode(CompoundTag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(error -> LocateNext.LOGGER.error("Discarding unreadable {}: {}", FILE_ID, error))
                .orElseGet(LocateNextSavedData::new);
    }

    private CompoundTag encode(CompoundTag tag) {
        CODEC.encodeStart(NbtOps.INSTANCE, this)
                .resultOrPartial(error -> LocateNext.LOGGER.error("Failed to write {}: {}", FILE_ID, error))
                .ifPresent(encoded -> tag.merge((CompoundTag) encoded));
        return tag;
    }
    //?}

    public NavigationState state(UUID uuid) {
        return this.states.computeIfAbsent(uuid, ignored -> new NavigationState());
    }
}
