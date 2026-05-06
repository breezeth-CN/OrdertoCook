package cn.breezeth.ordertocook.core;

import cn.breezeth.ordertocook.block.WashingTableBlock;
import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WashingTableManager {
    private static final long REQUIRED_HOLD_TICKS = 80L;
    private static final Map<UUID, Session> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    private WashingTableManager() {
    }

    public static void start(ServerPlayer player, BlockPos pos) {
        if (!canWash(player, pos)) {
            ACTIVE_SESSIONS.remove(player.getUUID());
            return;
        }

        ACTIVE_SESSIONS.put(player.getUUID(), new Session(pos.immutable(), player.serverLevel().getGameTime()));
    }

    public static void stop(ServerPlayer player) {
        ACTIVE_SESSIONS.remove(player.getUUID());
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Session>> iterator = ACTIVE_SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Session> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            Session session = entry.getValue();
            if (!canWash(player, session.pos())) {
                iterator.remove();
                continue;
            }

            ServerLevel world = player.serverLevel();
            long elapsedTicks = world.getGameTime() - session.startTick();
            if (elapsedTicks >= 10L && elapsedTicks < REQUIRED_HOLD_TICKS && elapsedTicks % 20L == 10L) {
                spawnWashEffects(world, session.pos());
            }
            if (elapsedTicks < REQUIRED_HOLD_TICKS) {
                continue;
            }

            completeWash(player, world, session.pos());
            iterator.remove();
        }
    }

    private static boolean canWash(ServerPlayer player, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
            return false;
        }
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 16.0) {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof WashingTableBlock && state.getValue(WashingTableBlock.PLATES) > 0;
    }

    private static void spawnWashEffects(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Direction facing = state.getBlock() instanceof WashingTableBlock ? state.getValue(WashingTableBlock.FACING) : Direction.NORTH;
        double y = pos.getY() + 0.94;
        for (int i = 0; i < 10 + world.random.nextInt(6); i++) {
            Vec3 randomSurfacePos = getRandomWaterSurfacePos(pos, facing, world.random.nextDouble(), world.random.nextDouble());
            world.sendParticles(
                    ParticleTypes.BUBBLE_POP,
                    randomSurfacePos.x,
                    y + world.random.nextDouble() * 0.05,
                    randomSurfacePos.z,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
        for (int i = 0; i < 2; i++) {
            Vec3 randomSurfacePos = getRandomWaterSurfacePos(pos, facing, world.random.nextDouble(), world.random.nextDouble());
            world.sendParticles(
                    ParticleTypes.SPLASH,
                    randomSurfacePos.x,
                    y,
                    randomSurfacePos.z,
                    1,
                    0.0,
                    0.01,
                    0.0,
                    0.0
            );
        }
        world.playSound(
                null,
                pos,
                SoundEvents.FISHING_BOBBER_SPLASH,
                SoundSource.BLOCKS,
                0.22f,
                0.9f + world.random.nextFloat() * 0.2f
        );
    }

    private static void completeWash(ServerPlayer player, ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof WashingTableBlock)) {
            return;
        }

        int plates = state.getValue(WashingTableBlock.PLATES);
        if (plates <= 0) {
            return;
        }

        world.setBlock(pos, state.setValue(WashingTableBlock.PLATES, plates - 1), 3);
        world.playSound(null, pos, ModSounds.PLATE_PLACE.get(), SoundSource.BLOCKS, 0.75f, 0.95f + world.random.nextFloat() * 0.1f);

        ItemStack cleanPlate = new ItemStack(ModItems.CLEAN_PLATE.get());
        Inventory inventory = player.getInventory();
        if (!inventory.add(cleanPlate)) {
            if (player.getMainHandItem().isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, cleanPlate);
            } else if (player.getOffhandItem().isEmpty()) {
                player.setItemInHand(InteractionHand.OFF_HAND, cleanPlate);
            }
        }
    }

    private static Vec3 getRandomWaterSurfacePos(BlockPos pos, Direction facing, double randomX, double randomZ) {
        double localX = 3.0 / 16.0 + randomX * (10.0 / 16.0);
        double localZ = 3.0 / 16.0 + randomZ * (9.0 / 16.0);
        double rotatedX;
        double rotatedZ;

        switch (facing) {
            case EAST -> {
                rotatedX = 1.0 - localZ;
                rotatedZ = localX;
            }
            case SOUTH -> {
                rotatedX = 1.0 - localX;
                rotatedZ = 1.0 - localZ;
            }
            case WEST -> {
                rotatedX = localZ;
                rotatedZ = 1.0 - localX;
            }
            default -> {
                rotatedX = localX;
                rotatedZ = localZ;
            }
        }

        return new Vec3(pos.getX() + rotatedX, pos.getY(), pos.getZ() + rotatedZ);
    }

    private record Session(BlockPos pos, long startTick) {
    }
}
