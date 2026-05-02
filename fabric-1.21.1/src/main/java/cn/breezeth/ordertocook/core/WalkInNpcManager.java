package cn.breezeth.ordertocook.core;

import cn.breezeth.ordertocook.config.ConfigManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class WalkInNpcManager {
    private WalkInNpcManager() {}

    public static boolean spawn(ServerWorld world, BlockPos machinePos, int level) {
        return spawn(world, machinePos, level, Long.MIN_VALUE);
    }

    public static boolean spawn(ServerWorld world, BlockPos machinePos, int level, long boardPos) {
        return spawn(world, machinePos, level, boardPos, CustomerProfileLibrary.createOrderProfile(world));
    }

    public static boolean spawn(ServerWorld world, BlockPos machinePos, int level, CustomerProfileLibrary.CustomerProfile profile) {
        return spawn(world, machinePos, level, Long.MIN_VALUE, profile);
    }

    public static boolean spawn(ServerWorld world, BlockPos machinePos, int level, long boardPos, CustomerProfileLibrary.CustomerProfile profile) {
        BlockPos pos = OrderNpcManager.findEmptyChair(world, machinePos);
        if (pos == null) return false;

        CustomerProfileLibrary.CustomerProfile finalProfile = profile == null
                ? CustomerProfileLibrary.createOrderProfile(world)
                : profile;
        String customerName = finalProfile.displayName();
        net.minecraft.entity.LivingEntity npc = OrderNpcManager.createNpc(world, pos, finalProfile, OtcRuntimeIdState.get(world).allocateCustomerId());
        if (npc == null) return false;

        npc.addCommandTag(OrderNpcManager.TAG_WALKIN);
        npc.addCommandTag("otc_level:" + level);
        npc.addCommandTag(OrderNpcManager.TAG_WALKIN_SPAWN_TIME + world.getTime());
        npc.addCommandTag(OrderNpcManager.TAG_WALKIN_SPAWN_SYSTEM_TIME + System.currentTimeMillis());
        npc.addCommandTag(OrderNpcManager.TAG_WALKIN_MACHINE_POS_PREFIX + machinePos.asLong());
        if (boardPos != Long.MIN_VALUE) {
            npc.addCommandTag(OrderNpcManager.TAG_WALKIN_BOARD_POS_PREFIX + boardPos);
        }
        OrderNpcManager.setupWalkInTeam(world, npc);

        world.playSound(null, pos, cn.breezeth.ordertocook.registry.ModSounds.ORDER_REFRESH, net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 1.0f);

        if (ConfigManager.isDevModeEnabled()) {
            Text msg = Text.literal("[OTC Dev] Walk-in NPC Spawned at " + pos.toShortString() + " Level: " + level + " Name: " + customerName).formatted(Formatting.GREEN);
            for (net.minecraft.server.network.ServerPlayerEntity p : world.getPlayers()) {
                if (p.hasPermissionLevel(2)) p.sendMessage(msg, false);
            }
        }
        return true;
    }

    public static void checkDespawn(ServerWorld world) {
        long nowTick = world.getTime();
        long maxAgeTicks = 10 * 60 * 20;
        java.util.List<net.minecraft.entity.LivingEntity> toDespawn = new java.util.ArrayList<>();
        for (java.util.UUID id : OrderNpcRegistry.ids()) {
            net.minecraft.entity.Entity entity = world.getEntity(id);
            if (!(entity instanceof net.minecraft.entity.LivingEntity le)) continue;
            if (!le.getCommandTags().contains(OrderNpcManager.TAG_WALKIN)) continue;
            long spawnTick = -1;
            for (String tag : le.getCommandTags()) {
                if (tag.startsWith(OrderNpcManager.TAG_WALKIN_SPAWN_TIME)) {
                    try {
                        spawnTick = Long.parseLong(tag.substring(OrderNpcManager.TAG_WALKIN_SPAWN_TIME.length()));
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                }
            }
            if (spawnTick == -1) {
                toDespawn.add(le);
                continue;
            }
            if (nowTick - spawnTick > maxAgeTicks) {
                toDespawn.add(le);
            }
        }
        for (net.minecraft.entity.LivingEntity le : toDespawn) {
            OrderNpcManager.despawnNpcForWalkIn(world, le);
        }
    }

    public static void changeToNormalTeam(ServerWorld world, net.minecraft.entity.LivingEntity entity) {
        OrderNpcManager.changeToNormalTeam(world, entity);
    }

    public static void cleanupChairSeats(ServerWorld world) {
        OrderNpcManager.cleanupChairSeats(world);
    }
}
