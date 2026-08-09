package com.finndog.locatenext.server;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
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
}
