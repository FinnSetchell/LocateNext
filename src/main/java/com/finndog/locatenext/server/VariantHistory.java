package com.finndog.locatenext.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
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

        public static final Codec<Landing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Landing::dimension),
                BlockPos.CODEC.fieldOf("structure").forGetter(Landing::structurePos),
                BlockPos.CODEC.fieldOf("landing").forGetter(Landing::landing)
        ).apply(instance, Landing::new));
    }

    public static final Codec<VariantHistory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Landing.CODEC.listOf().optionalFieldOf("landings", List.of()).forGetter(VariantHistory::landings),
            Codec.INT.optionalFieldOf("cursor", -1).forGetter(VariantHistory::cursor)
    ).apply(instance, VariantHistory::new));

    private final List<Landing> landings = new ArrayList<>();
    private int cursor = -1;

    public VariantHistory() {
    }

    private VariantHistory(List<Landing> landings, int cursor) {
        this.landings.addAll(landings);
        // Clamped rather than trusted: a hand-edited or truncated file could otherwise leave the
        // cursor pointing past the end.
        this.cursor = Math.min(cursor, this.landings.size() - 1);
    }

    private int cursor() {
        return this.cursor;
    }

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
