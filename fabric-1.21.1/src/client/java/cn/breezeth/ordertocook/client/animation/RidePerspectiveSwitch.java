package cn.breezeth.ordertocook.client.animation;

import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.Perspective;

public final class RidePerspectiveSwitch {
    private static boolean wasRidingMotorcycle = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(client.player.getVehicle());
                boolean isRidingMotorcycle = motorcycle != null;

                if (isRidingMotorcycle && !wasRidingMotorcycle) {
                    client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                } else if (!isRidingMotorcycle && wasRidingMotorcycle) {
                    client.options.setPerspective(Perspective.FIRST_PERSON);
                }

                wasRidingMotorcycle = isRidingMotorcycle;
            }
        });
    }
}

