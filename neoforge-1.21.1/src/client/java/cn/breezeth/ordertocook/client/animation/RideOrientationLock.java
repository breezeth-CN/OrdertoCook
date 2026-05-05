package cn.breezeth.ordertocook.client.animation;

import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class RideOrientationLock {
    private static boolean registered;

    private RideOrientationLock() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(RideOrientationLock::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        var client = net.minecraft.client.Minecraft.getInstance();
        var player = client.player;
        if (player == null) {
            return;
        }

        MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
        if (motorcycle == null) {
            return;
        }

        float bodyYaw = motorcycle.getYRot() + 180.0f;
        player.yBodyRot = bodyYaw;
        player.yBodyRotO = bodyYaw;
    }
}
