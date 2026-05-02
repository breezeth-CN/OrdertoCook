package cn.breezeth.ordertocook.core;

import cn.breezeth.ordertocook.block.WashingTableBlock;
import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WashingTableManager {
    private static final long REQUIRED_HOLD_TICKS = 80L;
    private static final Map<UUID, Session> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    private WashingTableManager() {
    }

    public static void start(ServerPlayerEntity player, BlockPos pos) {
        ItemStack stack = player.getMainHandStack();
        if (stack.getItem() == ModItems.DIRTY_PLATE) {
            BlockState state = player.getWorld().getBlockState(pos);
            int currentPlates = state.contains(WashingTableBlock.PLATES) ? state.get(WashingTableBlock.PLATES) : 0;
            if (currentPlates < 10) {
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                player.getWorld().setBlockState(pos, state.with(WashingTableBlock.PLATES, currentPlates + 1), 3);
            }
            return;
        }

        if (!canWash(player, pos)) {
            ACTIVE_SESSIONS.remove(player.getUuid());
            return;
        }

        ACTIVE_SESSIONS.put(player.getUuid(), new Session(pos.toImmutable(), player.getServerWorld().getTime()));
    }

    public static void stop(ServerPlayerEntity player) {
        ACTIVE_SESSIONS.remove(player.getUuid());
    }

    public static boolean isWashing(ServerPlayerEntity player) {
        return ACTIVE_SESSIONS.containsKey(player.getUuid());
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Session>> iterator = ACTIVE_SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Session> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            Session session = entry.getValue();
            if (!canWash(player, session.pos())) {
                iterator.remove();
                continue;
            }

            ServerWorld world = player.getServerWorld();
            long elapsedTicks = world.getTime() - session.startTick();
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

    private static boolean canWash(ServerPlayerEntity player, BlockPos pos) {
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        if (!player.getMainHandStack().isEmpty() || !player.getOffHandStack().isEmpty()) {
            return false;
        }
        if (player.squaredDistanceTo(Vec3d.ofCenter(pos)) > 16.0) {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof WashingTableBlock && state.get(WashingTableBlock.PLATES) > 0;
    }

    private static void spawnWashEffects(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Direction facing = state.getBlock() instanceof WashingTableBlock ? state.get(WashingTableBlock.FACING) : Direction.NORTH;
        double y = pos.getY() + 0.94;
        for (int i = 0; i < 10 + world.random.nextInt(6); i++) {
            Vec3d randomSurfacePos = getRandomWaterSurfacePos(pos, facing, world.random.nextDouble(), world.random.nextDouble());
            world.spawnParticles(
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
            Vec3d randomSurfacePos = getRandomWaterSurfacePos(pos, facing, world.random.nextDouble(), world.random.nextDouble());
            world.spawnParticles(
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
                SoundEvents.ENTITY_FISHING_BOBBER_SPLASH,
                SoundCategory.BLOCKS,
                0.22f,
                0.9f + world.random.nextFloat() * 0.2f
        );
    }

    private static void completeWash(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof WashingTableBlock)) {
            return;
        }

        int plates = state.get(WashingTableBlock.PLATES);
        if (plates <= 0) {
            return;
        }

        world.setBlockState(pos, state.with(WashingTableBlock.PLATES, plates - 1), 3);
        world.playSound(null, pos, ModSounds.PLATE_PLACE, SoundCategory.BLOCKS, 0.75f, 0.95f + world.random.nextFloat() * 0.1f);

        ItemStack cleanPlate = new ItemStack(ModItems.CLEAN_PLATE);
        PlayerInventory inventory = player.getInventory();
        if (!inventory.insertStack(cleanPlate)) {
            if (player.getMainHandStack().isEmpty()) {
                player.setStackInHand(Hand.MAIN_HAND, cleanPlate);
            } else if (player.getOffHandStack().isEmpty()) {
                player.setStackInHand(Hand.OFF_HAND, cleanPlate);
            }
        }
    }

    private static Vec3d getRandomWaterSurfacePos(BlockPos pos, Direction facing, double randomX, double randomZ) {
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

        return new Vec3d(pos.getX() + rotatedX, pos.getY(), pos.getZ() + rotatedZ);
    }

    private record Session(BlockPos pos, long startTick) {
    }
}
