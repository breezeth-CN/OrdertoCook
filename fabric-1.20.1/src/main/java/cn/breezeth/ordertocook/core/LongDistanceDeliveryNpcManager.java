package cn.breezeth.ordertocook.core;

import cn.breezeth.ordertocook.config.ConfigManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class LongDistanceDeliveryNpcManager {
    private LongDistanceDeliveryNpcManager() {}

    public static final String TAG_DELIVERY_LONG = "otc_delivery_long";

    public static boolean spawn(ServerWorld world, PlayerEntity player, BlockPos targetPos, String orderId, String customerName, long expiryTick, long expirySys) {
        return spawn(world, player, targetPos, orderId, customerName, expiryTick, expirySys, null);
    }

    public static boolean spawn(ServerWorld world, PlayerEntity player, BlockPos targetPos, String orderId, String customerName, long expiryTick, long expirySys, NbtCompound customerData) {
        OrderNpcManager.FailureCounters counters = new OrderNpcManager.FailureCounters();
        BlockPos pos = OrderNpcManager.findSpawnPosDelivery(world, targetPos, counters);
        if (pos == null) {
            player.sendMessage(Text.translatable("message.ordertocook.npc_respawn_try").formatted(Formatting.AQUA), false);
            pos = tryFindDrySpot(world, targetPos, 10, 64, counters);
            if (pos == null) {
                pos = tryFindDrySpot(world, targetPos, 50, 256, counters);
            }
            if (pos == null) {
                if (ConfigManager.isDevModeEnabled()) {
                    player.sendMessage(Text.translatable("message.ordertocook.npc_spawn_fail_delivery", counters.describeForDelivery()).formatted(Formatting.RED), false);
                }
                return false;
            }
        }
        net.minecraft.entity.LivingEntity npc = OrderNpcManager.createNpc(world, pos, customerName, customerData);
        if (npc == null) return false;
        npc.addCommandTag(TAG_DELIVERY_LONG);
        OrderNpcManager.setupOrderTeam(world, npc);
        OrderNpcManager.tagNpc(player, orderId, expiryTick, expirySys, npc);
        OrderNpcManager.addMapping(player, orderId, npc);
        player.sendMessage(Text.translatable("message.ordertocook.npc_respawn_success", customerName, String.valueOf(pos.getX()), String.valueOf(pos.getZ())).formatted(Formatting.AQUA), false);
        if (ConfigManager.isDevModeEnabled()) {
            Text typeName = Text.translatable(npc.getType().getTranslationKey());
            player.sendMessage(Text.translatable("message.ordertocook.npc_spawn_delivery", orderId, typeName, pos.getX(), pos.getY(), pos.getZ()).formatted(Formatting.AQUA), false);
        }
        return true;
    }

    private static BlockPos tryFindDrySpot(ServerWorld world, BlockPos targetPos, int radius, int attempts, OrderNpcManager.FailureCounters counters) {
        int x = targetPos.getX();
        int z = targetPos.getZ();
        for (int i = 0; i < attempts; i++) {
            int dx = world.random.nextBetween(-radius, radius);
            int dz = world.random.nextBetween(-radius, radius);
            int candX = x + dx;
            int candZ = z + dz;
            int candY = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, candX, candZ);
            int spawnY = candY + 1;
            if (candY <= world.getBottomY() + 1) {
                counters.lowY++;
            }
            BlockPos pos = new BlockPos(candX, spawnY, candZ);
            if (!(world.isAir(pos) && world.isAir(pos.up()))) {
                counters.space++;
                continue;
            }
            if (!(world.getBlockState(pos).getFluidState().isEmpty() && 
                  (pos.down().getY() < world.getBottomY() || world.getBlockState(pos.down()).getFluidState().isEmpty()))) {
                counters.water++;
                continue;
            }
            return pos;
        }
        return null;
    }
}
