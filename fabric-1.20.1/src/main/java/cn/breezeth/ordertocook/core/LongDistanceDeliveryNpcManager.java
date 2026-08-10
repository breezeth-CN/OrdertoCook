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
            if (ConfigManager.isDevModeEnabled()) player.sendMessage(Text.translatable("message.ordertocook.npc_spawn_fail_delivery", counters.describeForDelivery()).formatted(Formatting.RED), false);
            return false;
        }
        net.minecraft.entity.LivingEntity npc = OrderNpcManager.createNpc(world, pos, customerName, customerData);
        if (npc == null) return false;
        npc.addCommandTag(TAG_DELIVERY_LONG);
        OrderNpcManager.setupOrderTeam(world, npc);
        OrderNpcManager.tagNpc(player, orderId, expiryTick, expirySys, npc);
        OrderNpcManager.addMapping(player, orderId, npc);
        if (pos.getX() != targetPos.getX() || pos.getZ() != targetPos.getZ()) {
            player.sendMessage(Text.translatable("message.ordertocook.npc_respawn_success", customerName, String.valueOf(pos.getX()), String.valueOf(pos.getZ())).formatted(Formatting.AQUA), false);
        }
        if (ConfigManager.isDevModeEnabled()) {
            Text typeName = Text.translatable(npc.getType().getTranslationKey());
            player.sendMessage(Text.translatable("message.ordertocook.npc_spawn_delivery", orderId, typeName, pos.getX(), pos.getY(), pos.getZ()).formatted(Formatting.AQUA), false);
        }
        return true;
    }

}
