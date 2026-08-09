package com.finndog.locatenext.server;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One player's cursor into one mod's structure list, plus their per-session search settings. */
public final class NavigationState {

    /** Vanilla {@code /locate}'s default search radius, in chunks. */
    public static final int DEFAULT_RADIUS = 100;

    private String namespace = "";
    private List<ResourceLocation> structures = List.of();
    /** -1 means "nothing visited yet", so the first `next` lands on index 0. */
    private int index = -1;

    private int radius = DEFAULT_RADIUS;
    /**
     * Vanilla's {@code skipKnownStructures}. On, the generator refuses instances that have already
     * been referenced and marks each hit as referenced on the way out — so every search returns a
     * structure nobody has been sent to before. Default on: this is a tool for looking at fresh
     * generation, and repeatedly landing on the same instance is exactly what makes that tedious.
     */
    private boolean unexploredOnly = true;
    /** Hop to the dimension a structure actually generates in instead of failing. */
    private boolean autoDimension = true;

    private final Set<ResourceLocation> visited = new HashSet<>();
    /** Per-structure instance history, so ↑/↓ can walk variations of the same structure. */
    private final Map<ResourceLocation, VariantHistory> variants = new HashMap<>();

    @Nullable private BlockPos homePos;
    @Nullable private ResourceKey<Level> homeDimension;

    public String namespace() {
        return this.namespace;
    }

    public List<ResourceLocation> structures() {
        return this.structures;
    }

    public int index() {
        return this.index;
    }

    public int size() {
        return this.structures.size();
    }

    public boolean hasSelection() {
        return !this.structures.isEmpty();
    }

    public void select(String namespace, List<ResourceLocation> structures) {
        this.namespace = namespace;
        this.structures = List.copyOf(structures);
        this.index = -1;
        this.visited.clear();
        this.variants.clear();
    }

    public void clear() {
        this.namespace = "";
        this.structures = List.of();
        this.index = -1;
        this.visited.clear();
        this.variants.clear();
    }

    public VariantHistory variants(ResourceLocation id) {
        return this.variants.computeIfAbsent(id, key -> new VariantHistory());
    }

    /** Wraps, so holding right arrow loops the list rather than dead-ending. */
    public int step(int delta) {
        int size = this.structures.size();
        if (size == 0) {
            return -1;
        }
        // -1 + 1 -> 0 gives a clean first `next`; -1 - 1 -> size - 1 makes the first `prev` wrap
        // to the end, which is how you get at the tail of a long list quickly.
        this.index = Math.floorMod(this.index + delta, size);
        return this.index;
    }

    public boolean setIndex(int index) {
        if (index < 0 || index >= this.structures.size()) {
            return false;
        }
        this.index = index;
        return true;
    }

    @Nullable
    public ResourceLocation current() {
        return this.index >= 0 && this.index < this.structures.size() ? this.structures.get(this.index) : null;
    }

    public void markVisited(ResourceLocation id) {
        this.visited.add(id);
    }

    public boolean isVisited(ResourceLocation id) {
        return this.visited.contains(id);
    }

    public int visitedCount() {
        return this.visited.size();
    }

    public int radius() {
        return this.radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public boolean unexploredOnly() {
        return this.unexploredOnly;
    }

    public void setUnexploredOnly(boolean unexploredOnly) {
        this.unexploredOnly = unexploredOnly;
    }

    public boolean autoDimension() {
        return this.autoDimension;
    }

    public void setAutoDimension(boolean autoDimension) {
        this.autoDimension = autoDimension;
    }

    /** Recorded once, on the first teleport, so `/ln home` can undo a whole session of hopping. */
    public void rememberHome(ResourceKey<Level> dimension, BlockPos pos) {
        if (this.homePos == null) {
            this.homeDimension = dimension;
            this.homePos = pos;
        }
    }

    @Nullable
    public BlockPos homePos() {
        return this.homePos;
    }

    @Nullable
    public ResourceKey<Level> homeDimension() {
        return this.homeDimension;
    }

    public void clearHome() {
        this.homePos = null;
        this.homeDimension = null;
    }
}
