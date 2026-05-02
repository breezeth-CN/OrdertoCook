package cn.breezeth.ordertocook.core;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class PrestigeManager {
    public static int getPlayerPrestige(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            // Retrieve prestige data from player entity mixin
            return ((IPrestigeData) player).getPrestige();
        }
        return 0;
    }

    public static void addPlayerPrestige(PlayerEntity player, int amount) {
        if (player instanceof ServerPlayerEntity) {
            ((IPrestigeData) player).addPrestige(amount);
        }
    }
    
    // Interface to inject into PlayerEntity
    public interface IPrestigeData {
        int getPrestige();
        void setPrestige(int prestige);
        void addPrestige(int amount);
    }
}
