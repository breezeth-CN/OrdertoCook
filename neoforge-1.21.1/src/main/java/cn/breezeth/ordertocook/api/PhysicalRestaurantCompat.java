package cn.breezeth.ordertocook.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

public interface PhysicalRestaurantCompat {
    boolean isAvailable();

    BlockPos toWorldPosition(Level level, BlockPos pos);

    @Nullable
    UUID getStructureId(Level level, BlockPos pos);

    Set<UUID> getConnectedStructureIds(Level level, BlockPos pos);

    default void runOrderMachineMaintenance(ServerLevel level) {
    }
}
