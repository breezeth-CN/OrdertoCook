package cn.breezeth.ordertocook.client.gecko;

import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Mirrors fabric-1.21.1 {@code HumanoidAnimationAssets}: drive_idle vs drive_idle_alex by skin model,
 * animation file per {@link RiderAnimatable#getAnimationGroup()}.
 */
public final class HumanoidAnimationAssets {
    private HumanoidAnimationAssets() {
    }

    public static Identifier getModel(String group, AbstractClientPlayerEntity player) {
        if ("slim".equals(player.getModel())) {
            return new Identifier(ModConstants.MOD_ID, "geo/drive_idle_alex.geo.json");
        }
        return new Identifier(ModConstants.MOD_ID, "geo/drive_idle.geo.json");
    }

    public static Identifier getAnimation(String group) {
        return new Identifier(ModConstants.MOD_ID, "animations/" + group + ".animation.json");
    }
}
