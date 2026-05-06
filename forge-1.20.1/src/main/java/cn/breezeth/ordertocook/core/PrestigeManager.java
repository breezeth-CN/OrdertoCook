package cn.breezeth.ordertocook.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PrestigeManager {
    public static int getPlayerPrestige(Player player) {
        if (player instanceof ServerPlayer) {
            // Retrieve prestige data from player entity mixin
            return ((IPrestigeData) player).getPrestige();
        }
        return 0;
    }

    public static void addPlayerPrestige(Player player, int amount) {
        if (player instanceof ServerPlayer) {
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
