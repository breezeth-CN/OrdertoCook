package cn.breezeth.ordertocook.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PhysicalRestaurantCompatApi {
    private static final CopyOnWriteArrayList<PhysicalRestaurantCompat> COMPATS = new CopyOnWriteArrayList<>();
    private static final Set<BlockPos> MOVING_BLOCKS = new HashSet<>();
    private static final Set<BlockPos> MOVED_TARGETS = new HashSet<>();

    private PhysicalRestaurantCompatApi() {
    }

    public static void register(PhysicalRestaurantCompat compat) {
        if (compat != null && !COMPATS.contains(compat)) {
            COMPATS.add(compat);
        }
    }

    public static boolean hasAvailableCompat() {
        for (PhysicalRestaurantCompat compat : COMPATS) {
            if (compat.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    public static BlockPos toWorldPosition(Level level, BlockPos pos) {
        if (level == null || pos == null) return pos;
        for (PhysicalRestaurantCompat compat : COMPATS) {
            if (!compat.isAvailable()) continue;
            BlockPos projected = compat.toWorldPosition(level, pos);
            if (projected != null && !projected.equals(pos)) {
                return projected;
            }
        }
        return pos;
    }

    @Nullable
    public static UUID getStructureId(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        for (PhysicalRestaurantCompat compat : COMPATS) {
            if (!compat.isAvailable()) continue;
            UUID id = compat.getStructureId(level, pos);
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    public static Set<UUID> getConnectedStructureIds(Level level, BlockPos pos) {
        if (level == null || pos == null) return Collections.emptySet();
        Set<UUID> ids = new HashSet<>();
        for (PhysicalRestaurantCompat compat : COMPATS) {
            if (!compat.isAvailable()) continue;
            ids.addAll(compat.getConnectedStructureIds(level, pos));
        }
        return ids;
    }

    public static void runOrderMachineMaintenance(ServerLevel level) {
        if (level == null) return;
        for (PhysicalRestaurantCompat compat : COMPATS) {
            if (compat.isAvailable()) {
                compat.runOrderMachineMaintenance(level);
            }
        }
    }

    public static void markAssemblyMoving(BlockPos source, BlockPos target) {
        if (source != null) {
            MOVING_BLOCKS.add(source.immutable());
        }
        if (target != null) {
            MOVED_TARGETS.add(target.immutable());
        }
    }

    public static void unmarkAssemblyMoving(BlockPos pos) {
        if (pos != null) {
            MOVING_BLOCKS.remove(pos);
        }
    }

    public static boolean isAssemblyMoving(BlockPos pos) {
        return pos != null && MOVING_BLOCKS.contains(pos);
    }

    public static boolean consumeMovedTarget(BlockPos pos) {
        return pos != null && MOVED_TARGETS.remove(pos);
    }
}
