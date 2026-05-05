package cn.breezeth.ordertocook.client.animation;

import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.client.CameraType;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class RidePerspectiveSwitch {
    private static boolean registered;
    private static boolean wasRidingMotorcycle;

    private RidePerspectiveSwitch() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(RidePerspectiveSwitch::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        var client = net.minecraft.client.Minecraft.getInstance();
        if (client.player == null) {
            wasRidingMotorcycle = false;
            return;
        }

        boolean isRidingMotorcycle = MotorcycleEntity.fromVehicle(client.player.getVehicle()) != null;
        if (isRidingMotorcycle && !wasRidingMotorcycle) {
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (!isRidingMotorcycle && wasRidingMotorcycle) {
            client.options.setCameraType(CameraType.FIRST_PERSON);
        }

        wasRidingMotorcycle = isRidingMotorcycle;
    }
}
