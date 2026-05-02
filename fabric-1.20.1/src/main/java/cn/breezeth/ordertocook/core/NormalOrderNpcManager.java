package cn.breezeth.ordertocook.core;

import cn.breezeth.ordertocook.config.ConfigManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class NormalOrderNpcManager {
    private NormalOrderNpcManager() {}

    public static void spawn(ServerWorld world, PlayerEntity player, BlockPos machinePos, String orderId, String customerName, long expiryTick, long expirySys, NbtCompound customerData) {
        OrderNpcManager.FailureCounters counters = new OrderNpcManager.FailureCounters();
        BlockPos pos = OrderNpcManager.findSpawnPosNormal(world, machinePos, counters);
        if (pos == null) {
            if (ConfigManager.isDevModeEnabled()) {
                player.sendMessage(Text.translatable("message.ordertocook.npc_spawn_fail_normal", counters.describe()).formatted(Formatting.RED), false);
            }
            return;
        }
        net.minecraft.entity.LivingEntity npc = OrderNpcManager.createNpc(world, pos, customerName, customerData);
        if (npc == null) return;
        OrderNpcManager.setupOrderTeam(world, npc);
        OrderNpcManager.tagNpc(player, orderId, expiryTick, expirySys, npc);
        OrderNpcManager.addMapping(player, orderId, npc);
        if (ConfigManager.isDevModeEnabled()) {
            Text typeName = Text.translatable(npc.getType().getTranslationKey());
            player.sendMessage(Text.translatable("message.ordertocook.npc_spawn_normal", orderId, typeName, pos.getX(), pos.getY(), pos.getZ()).formatted(Formatting.GREEN), false);
        }
    }
}
