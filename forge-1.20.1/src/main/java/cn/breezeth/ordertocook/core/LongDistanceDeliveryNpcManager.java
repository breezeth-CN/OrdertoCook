package cn.breezeth.ordertocook.core;

import cn.breezeth.ordertocook.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class LongDistanceDeliveryNpcManager {
    private LongDistanceDeliveryNpcManager() {}

    public static final String TAG_DELIVERY_LONG = "otc_delivery_long";

    public static boolean spawn(ServerLevel world, Player player, BlockPos targetPos, String orderId, String customerName, long expiryTick, long expirySys) {
        return spawn(world, player, targetPos, orderId, customerName, expiryTick, expirySys, null);
    }

    public static boolean spawn(ServerLevel world, Player player, BlockPos targetPos, String orderId, String customerName, long expiryTick, long expirySys, CompoundTag customerData) {
        OrderNpcManager.FailureCounters counters = new OrderNpcManager.FailureCounters();
        BlockPos pos = OrderNpcManager.findSpawnPosDelivery(world, targetPos, counters);
        if (pos == null) {
            player.displayClientMessage(Component.translatable("message.ordertocook.npc_respawn_try").withStyle(ChatFormatting.AQUA), false);
            pos = tryFindDrySpot(world, targetPos, 10, 64, counters);
            if (pos == null) pos = tryFindDrySpot(world, targetPos, 50, 256, counters);
            if (pos == null) {
                if (ConfigManager.isDevModeEnabled()) {
                    player.displayClientMessage(Component.translatable("message.ordertocook.npc_spawn_fail_delivery", counters.describeForDelivery()).withStyle(ChatFormatting.RED), false);
                }
                return false;
            }
        }
        net.minecraft.world.entity.LivingEntity npc = OrderNpcManager.createNpc(world, pos, customerName, customerData);
        if (npc == null) return false;
        npc.addTag(TAG_DELIVERY_LONG);
        OrderNpcManager.setupOrderTeam(world, npc);
        OrderNpcManager.tagNpc(player, orderId, expiryTick, expirySys, npc);
        OrderNpcManager.addMapping(player, orderId, npc);
        player.displayClientMessage(Component.translatable("message.ordertocook.npc_respawn_success", customerName, String.valueOf(pos.getX()), String.valueOf(pos.getZ())).withStyle(ChatFormatting.AQUA), false);
        if (ConfigManager.isDevModeEnabled()) {
            Component typeName = Component.translatable(npc.getType().getDescriptionId());
            player.displayClientMessage(Component.translatable("message.ordertocook.npc_spawn_delivery", orderId, typeName, pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.AQUA), false);
        }
        return true;
    }

    private static BlockPos tryFindDrySpot(ServerLevel world, BlockPos targetPos, int radius, int attempts, OrderNpcManager.FailureCounters counters) {
        int x = targetPos.getX();
        int z = targetPos.getZ();
        for (int i = 0; i < attempts; i++) {
            int dx = world.random.nextIntBetweenInclusive(-radius, radius);
            int dz = world.random.nextIntBetweenInclusive(-radius, radius);
            int candX = x + dx;
            int candZ = z + dz;
            int candY = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candX, candZ);
            int spawnY = candY + 1;
            if (candY <= world.getMinBuildHeight() + 1) {
                counters.lowY++;
            }
            BlockPos pos = new BlockPos(candX, spawnY, candZ);
            if (!(world.isEmptyBlock(pos) && world.isEmptyBlock(pos.above()))) {
                counters.space++;
                continue;
            }
            if (!(world.getBlockState(pos).getFluidState().isEmpty() && 
                  (pos.below().getY() < world.getMinBuildHeight() || world.getBlockState(pos.below()).getFluidState().isEmpty()))) {
                counters.water++;
                continue;
            }
            return pos;
        }
        return null;
    }
}
