package cn.breezeth.ordertocook.client.animation;

import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class RideOrientationLock {
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var player = client.player;
            if (player == null) return;

            MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
            if (motorcycle == null) {
                return;
            }

            float currentMotoYaw = motorcycle.getYaw();
            float bodyYaw = currentMotoYaw + 180.0f;
            player.bodyYaw = bodyYaw;
            player.prevBodyYaw = bodyYaw;
        });
    }
}

