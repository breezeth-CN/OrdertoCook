package cn.breezeth.ordertocook.client.animation;

import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/**
 * 与 fabric-1.21.1 回滚版一致：骑摩托时同步身体朝向与车头。
 */
public final class RideOrientationLock {
    private RideOrientationLock() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(RideOrientationLock::onEndClientTick);
    }

    private static void onEndClientTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(client.player.getVehicle());
        if (motorcycle == null) {
            return;
        }
        float currentMotoYaw = motorcycle.getYaw();
        float bodyYaw = currentMotoYaw + 180.0f;
        client.player.setBodyYaw(bodyYaw);
    }
}
