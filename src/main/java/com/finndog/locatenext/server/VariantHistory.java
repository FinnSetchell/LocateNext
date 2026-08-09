package com.finndog.locatenext.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Every instance of one structure this player has been sent to, in the order they were found,
 * with a cursor into it.
 *
 * <p>This is what makes ↑/↓ work: ↑ walks forward, searching for a fresh instance once it runs off
 * the end, and ↓ walks back through instances already seen without searching again. It also means
 * stepping away with ←/→ and coming back returns you to the same instance rather than re-rolling.
 */
public final class VariantHistory {

    /** One located instance. {@code structurePos} is the raw find; {@code landing} is where we put you. */
    public record Landing(ResourceKey<Level> dimension, BlockPos structurePos, BlockPos landing) {
    }

    private final List<Landing> landings = new ArrayList<>();
    private int cursor = -1;

    public boolean isEmpty() {
        return this.landings.isEmpty();
    }

    public int size() {
        return this.landings.size();
    }

    /** One-based, for display. */
    public int position() {
        return this.cursor + 1;
    }

    @Nullable
    public Landing current() {
        return this.cursor >= 0 && this.cursor < this.landings.size() ? this.landings.get(this.cursor) : null;
    }

    /** @return the entry now under the cursor, or null when already at the oldest. */
    @Nullable
    public Landing back() {
        if (this.cursor <= 0) {
            return null;
        }
        this.cursor--;
        return this.landings.get(this.cursor);
    }

    /**
     * Steps forward into an instance already found. Returns null when the cursor is at the newest
     * entry, which is the caller's signal to go search for a genuinely new one.
     */
    @Nullable
    public Landing forward() {
        if (this.cursor + 1 >= this.landings.size()) {
            return null;
        }
        this.cursor++;
        return this.landings.get(this.cursor);
    }

    public void add(Landing landing) {
        this.landings.add(landing);
        this.cursor = this.landings.size() - 1;
    }

    /** Guards against a search handing back an instance we've already been to. */
    public boolean contains(ResourceKey<Level> dimension, BlockPos structurePos) {
        for (Landing landing : this.landings) {
            if (landing.dimension() == dimension && landing.structurePos().equals(structurePos)) {
                return true;
            }
        }
        return false;
    }

    public List<Landing> landings() {
        return List.copyOf(this.landings);
    }

    // ------------------------------------------------------------------ persistence

    // Positions go in as plain int arrays rather than through NbtUtils, whose block-pos helpers
    // changed shape across 1.20/1.21 and would need a Stonecutter branch per version.
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Landing landing : this.landings) {
            CompoundTag entry = new CompoundTag();
            entry.putString("dimension", landing.dimension().location().toString());
            entry.putIntArray("structure", pack(landing.structurePos()));
            entry.putIntArray("landing", pack(landing.landing()));
            list.add(entry);
        }
        tag.put("landings", list);
        tag.putInt("cursor", this.cursor);
        return tag;
    }

    public static VariantHistory load(CompoundTag tag) {
        VariantHistory history = new VariantHistory();
        ListTag list = tag.getList("landings", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation dimensionId = ResourceLocation.tryParse(entry.getString("dimension"));
            BlockPos structure = unpack(entry.getIntArray("structure"));
            BlockPos landing = unpack(entry.getIntArray("landing"));
            if (dimensionId == null || structure == null || landing == null) {
                continue;
            }
            history.landings.add(new Landing(
                    ResourceKey.create(Registries.DIMENSION, dimensionId), structure, landing));
        }
        // Clamped rather than trusted: entries above can be skipped as malformed, which would
        // otherwise leave the cursor pointing past the end.
        history.cursor = Math.min(tag.getInt("cursor"), history.landings.size() - 1);
        return history;
    }

    private static int[] pack(BlockPos pos) {
        return new int[]{pos.getX(), pos.getY(), pos.getZ()};
    }

    @Nullable
    private static BlockPos unpack(int[] values) {
        return values.length == 3 ? new BlockPos(values[0], values[1], values[2]) : null;
    }
}
