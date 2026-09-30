package com.finndog.locatenext.server.compat;

import com.finndog.locatenext.LocateNext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Optional bridge to Moog's Paths' locate API, called by reflection only so LocateNext has no
 * compile or runtime dependency on it.
 *
 * <p>Methods are found by name and parameter count, never by parameter type, because the Minecraft
 * classes in those signatures are renamed per node while Moog's Paths' own names are not. Every
 * name that has to match the other mod is a constant below.
 *
 * <p>An absent, old or mismatched Moog's Paths yields empty results, so callers behave exactly as
 * if it were not installed. At most one warning is ever logged.
 */
public final class MoogsPathsCompat {

    private static final String LOCATOR_CLASS = "com.finndog.moogs_paths.api.MoogsPathsLocator";
    private static final String NETWORKS_METHOD = "networks";
    private static final int NETWORKS_ARITY = 1;
    private static final String LOCATE_METHOD = "locate";
    private static final int LOCATE_ARITY = 4;
    private static final String ORIGIN_ACCESSOR = "origin";
    private static final String LANDING_ACCESSOR = "landing";
    private static final String DISTANCE_ACCESSOR = "distance";

    /** origin is the path's stable identity, landing is where to teleport. */
    public record Found(BlockPos origin, BlockPos landing, int distance) {
    }

    private record Api(Method networks, Method locate) {
    }

    private record Accessors(Method origin, Method landing, Method distance) {
    }

    private static boolean resolved;
    private static Api current;
    private static Accessors accessors;
    private static boolean warned;

    private MoogsPathsCompat() {
    }

    /** Network ids as Moog's Paths reports them, or empty when it or its API is not there. */
    public static List<ResourceLocation> networks(MinecraftServer server) {
        Api api = api();
        if (api == null) {
            return List.of();
        }
        try {
            Object result = api.networks().invoke(null, server.overworld());
            if (!(result instanceof Collection<?> collection)) {
                disable("networks did not return a list");
                return List.of();
            }
            List<ResourceLocation> ids = new ArrayList<>(collection.size());
            for (Object element : collection) {
                if (element instanceof ResourceLocation id) {
                    ids.add(id);
                }
            }
            return ids;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            warnOnce("networks failed", e);
            return List.of();
        }
    }

    public static boolean isNetwork(MinecraftServer server, ResourceLocation id) {
        return networks(server).contains(id);
    }

    /**
     * The nearest path of {@code network} to {@code fromInOverworld} whose origin is not in
     * {@code excludeOrigins}. Also makes Moog's Paths queue that path's placement.
     */
    public static Optional<Found> locate(MinecraftServer server, ResourceLocation network,
                                         BlockPos fromInOverworld, Set<BlockPos> excludeOrigins) {
        Api api = api();
        if (api == null) {
            return Optional.empty();
        }
        try {
            Object result = api.locate().invoke(null, server.overworld(), network, fromInOverworld, excludeOrigins);
            if (!(result instanceof Optional<?> optional)) {
                disable("locate did not return an Optional");
                return Optional.empty();
            }
            if (optional.isEmpty()) {
                return Optional.empty();
            }
            Object path = optional.get();
            Accessors read = accessors(path.getClass());
            if (read == null) {
                return Optional.empty();
            }
            BlockPos origin = (BlockPos) read.origin().invoke(path);
            BlockPos landing = (BlockPos) read.landing().invoke(path);
            int distance = ((Number) read.distance().invoke(path)).intValue();
            return Optional.of(new Found(origin, landing, distance));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            warnOnce("locate failed", e);
            return Optional.empty();
        }
    }

    private static synchronized Api api() {
        if (!resolved) {
            resolved = true;
            current = resolve();
        }
        return current;
    }

    private static Api resolve() {
        Class<?> locator;
        try {
            locator = Class.forName(LOCATOR_CLASS, true, MoogsPathsCompat.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        } catch (LinkageError e) {
            warnOnce("locator class could not be loaded", e);
            return null;
        }
        Method networks = find(locator, NETWORKS_METHOD, NETWORKS_ARITY);
        Method locate = find(locator, LOCATE_METHOD, LOCATE_ARITY);
        if (networks == null || locate == null) {
            warnOnce("expected " + NETWORKS_METHOD + "/" + NETWORKS_ARITY + " and "
                    + LOCATE_METHOD + "/" + LOCATE_ARITY + " on " + LOCATOR_CLASS, null);
            return null;
        }
        return new Api(networks, locate);
    }

    private static Method find(Class<?> type, String name, int arity) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == arity
                    && Modifier.isStatic(method.getModifiers())) {
                return method;
            }
        }
        return null;
    }

    private static synchronized Accessors accessors(Class<?> type) {
        if (accessors == null || accessors.origin().getDeclaringClass() != type) {
            try {
                accessors = new Accessors(type.getMethod(ORIGIN_ACCESSOR), type.getMethod(LANDING_ACCESSOR),
                        type.getMethod(DISTANCE_ACCESSOR));
            } catch (NoSuchMethodException e) {
                disable("path result has no " + ORIGIN_ACCESSOR + "/" + LANDING_ACCESSOR + "/"
                        + DISTANCE_ACCESSOR + " accessors");
                return null;
            }
        }
        return accessors;
    }

    private static synchronized void disable(String reason) {
        current = null;
        warnOnce(reason, null);
    }

    private static synchronized void warnOnce(String reason, Throwable cause) {
        if (warned) {
            return;
        }
        warned = true;
        Throwable root = cause instanceof InvocationTargetException && cause.getCause() != null
                ? cause.getCause() : cause;
        LocateNext.LOGGER.warn("Moog's Paths compat: {}{}", reason,
                root == null ? "" : " (" + root + ")");
        if (root != null) {
            LocateNext.LOGGER.debug("Moog's Paths compat failure", root);
        }
    }
}
