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
            if (ConfigManager.isDevModeEnabled()) player.displayClientMessage(Component.translatable("message.ordertocook.npc_spawn_fail_delivery", counters.describeForDelivery()).withStyle(ChatFormatting.RED), false);
            return false;
        }
        net.minecraft.world.entity.LivingEntity npc = OrderNpcManager.createNpc(world, pos, customerName, customerData);
        if (npc == null) return false;
        npc.addTag(TAG_DELIVERY_LONG);
        OrderNpcManager.setupOrderTeam(world, npc);
        OrderNpcManager.tagNpc(player, orderId, expiryTick, expirySys, npc);
        OrderNpcManager.addMapping(player, orderId, npc);
        if (pos.getX() != targetPos.getX() || pos.getZ() != targetPos.getZ()) {
            player.displayClientMessage(Component.translatable("message.ordertocook.npc_respawn_success", customerName, String.valueOf(pos.getX()), String.valueOf(pos.getZ())).withStyle(ChatFormatting.AQUA), false);
        }
        if (ConfigManager.isDevModeEnabled()) {
            Component typeName = Component.translatable(npc.getType().getDescriptionId());
            player.displayClientMessage(Component.translatable("message.ordertocook.npc_spawn_delivery", orderId, typeName, pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.AQUA), false);
        }
        return true;
    }

}
