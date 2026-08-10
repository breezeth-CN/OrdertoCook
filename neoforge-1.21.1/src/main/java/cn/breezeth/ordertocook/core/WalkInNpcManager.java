package cn.breezeth.ordertocook.core;

import cn.breezeth.ordertocook.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public final class WalkInNpcManager {
    private WalkInNpcManager() {}

    public static boolean spawn(ServerLevel world, BlockPos machinePos, int level) {
        return spawn(world, machinePos, level, Long.MIN_VALUE);
    }

    public static boolean spawn(ServerLevel world, BlockPos machinePos, int level, long boardPos) {
        return spawn(world, machinePos, level, boardPos, CustomerProfileLibrary.createOrderProfile(world));
    }

    public static boolean spawn(ServerLevel world, BlockPos machinePos, int level, CustomerProfileLibrary.CustomerProfile profile) {
        return spawn(world, machinePos, level, Long.MIN_VALUE, profile);
    }

    public static boolean spawn(ServerLevel world, BlockPos machinePos, int level, long boardPos, CustomerProfileLibrary.CustomerProfile profile) {
        BlockPos pos = OrderNpcManager.findEmptyChair(world, machinePos);
        if (pos == null) return false;

        CustomerProfileLibrary.CustomerProfile finalProfile = profile == null
                ? CustomerProfileLibrary.createOrderProfile(world)
                : profile;
        String customerName = finalProfile.displayName();
        net.minecraft.world.entity.LivingEntity npc = OrderNpcManager.createNpc(world, pos, finalProfile, OtcRuntimeIdState.get(world).allocateCustomerId());
        if (npc == null) return false;

        npc.addTag(OrderNpcManager.TAG_WALKIN);
        npc.addTag("otc_level:" + level);
        npc.addTag(OrderNpcManager.TAG_WALKIN_SPAWN_TIME + world.getGameTime());
        npc.addTag(OrderNpcManager.TAG_WALKIN_SPAWN_SYSTEM_TIME + System.currentTimeMillis());
        npc.addTag(OrderNpcManager.TAG_WALKIN_MACHINE_POS_PREFIX + machinePos.asLong());
        if (boardPos != Long.MIN_VALUE) {
            npc.addTag(OrderNpcManager.TAG_WALKIN_BOARD_POS_PREFIX + boardPos);
        }
        OrderNpcManager.setupWalkInTeam(world, npc);

        world.playSound(null, pos, cn.breezeth.ordertocook.registry.ModSounds.ORDER_REFRESH.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);

        if (ConfigManager.isDevModeEnabled()) {
            Component msg = Component.literal("[OTC Dev] Walk-in NPC Spawned at " + pos.toShortString() + " Level: " + level + " Name: " + customerName).withStyle(ChatFormatting.GREEN);
            for (net.minecraft.server.level.ServerPlayer p : world.players()) {
                if (p.hasPermissions(2)) p.displayClientMessage(msg, false);
            }
        }
        return true;
    }

    public static void checkDespawn(ServerLevel world) {
        long nowTick = world.getGameTime();
        long nowSys = System.currentTimeMillis();
        long maxAgeTicks = ModConstants.URGENT_DURATION_TICKS;
        long maxAgeMillis = maxAgeTicks * 50L;
        java.util.List<net.minecraft.world.entity.LivingEntity> toDespawn = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.Entity entity : world.getAllEntities()) {
            if (!(entity instanceof net.minecraft.world.entity.LivingEntity le)) continue;
            if (!le.getTags().contains(OrderNpcManager.TAG_WALKIN)) continue;
            OrderNpcRegistry.register(le.getUUID());
            long spawnTick = -1;
            long spawnSys = -1;
            for (String tag : le.getTags()) {
                if (tag.startsWith(OrderNpcManager.TAG_WALKIN_SPAWN_TIME)) {
                    try {
                        spawnTick = Long.parseLong(tag.substring(OrderNpcManager.TAG_WALKIN_SPAWN_TIME.length()));
                    } catch (NumberFormatException ignored) {
                    }
                } else if (tag.startsWith(OrderNpcManager.TAG_WALKIN_SPAWN_SYSTEM_TIME)) {
                    try {
                        spawnSys = Long.parseLong(tag.substring(OrderNpcManager.TAG_WALKIN_SPAWN_SYSTEM_TIME.length()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (spawnTick < 0L && spawnSys < 0L) {
                toDespawn.add(le);
                continue;
            }
            boolean tickExpired = spawnTick >= 0L && nowTick - spawnTick >= maxAgeTicks;
            boolean systemExpired = spawnSys >= 0L && nowSys - spawnSys >= maxAgeMillis;
            if (tickExpired || systemExpired) {
                toDespawn.add(le);
            }
        }
        for (net.minecraft.world.entity.LivingEntity le : toDespawn) {
            OrderNpcManager.despawnNpcForWalkIn(world, le);
        }
    }

    public static void changeToNormalTeam(ServerLevel world, net.minecraft.world.entity.LivingEntity entity) {
        OrderNpcManager.changeToNormalTeam(world, entity);
    }

    public static void cleanupChairSeats(ServerLevel world) {
        OrderNpcManager.cleanupChairSeats(world);
    }
}
