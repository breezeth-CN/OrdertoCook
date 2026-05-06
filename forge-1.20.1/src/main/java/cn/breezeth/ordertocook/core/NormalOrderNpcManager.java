package cn.breezeth.ordertocook.core;

import cn.breezeth.ordertocook.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class NormalOrderNpcManager {
    private NormalOrderNpcManager() {}

    public static void spawn(ServerLevel world, Player player, BlockPos machinePos, String orderId, String customerName, long expiryTick, long expirySys, CompoundTag customerData) {
        OrderNpcManager.FailureCounters counters = new OrderNpcManager.FailureCounters();
        BlockPos pos = OrderNpcManager.findSpawnPosNormal(world, machinePos, counters);
        if (pos == null) {
            if (ConfigManager.isDevModeEnabled()) {
                player.displayClientMessage(Component.translatable("message.ordertocook.npc_spawn_fail_normal", counters.describe()).withStyle(ChatFormatting.RED), false);
            }
            return;
        }
        net.minecraft.world.entity.LivingEntity npc = OrderNpcManager.createNpc(world, pos, customerName, customerData);
        if (npc == null) return;
        OrderNpcManager.setupOrderTeam(world, npc);
        OrderNpcManager.tagNpc(player, orderId, expiryTick, expirySys, npc);
        OrderNpcManager.addMapping(player, orderId, npc);
        if (ConfigManager.isDevModeEnabled()) {
            Component typeName = Component.translatable(npc.getType().getDescriptionId());
            player.displayClientMessage(Component.translatable("message.ordertocook.npc_spawn_normal", orderId, typeName, pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN), false);
        }
    }
}
