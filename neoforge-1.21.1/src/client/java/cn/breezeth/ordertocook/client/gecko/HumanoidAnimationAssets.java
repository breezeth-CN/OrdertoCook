package cn.breezeth.ordertocook.client.gecko;

import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

public final class HumanoidAnimationAssets {
    private HumanoidAnimationAssets() {
    }

    public static ResourceLocation getModel(String group, PlayerSkin.Model model) {
        if (model == PlayerSkin.Model.SLIM) {
            return ResourceLocation.fromNamespaceAndPath("ordertocook", "geo/drive_idle_alex.geo.json");
        }
        return ResourceLocation.fromNamespaceAndPath("ordertocook", "geo/drive_idle.geo.json");
    }

    public static ResourceLocation getAnimation(String group) {
        return ResourceLocation.fromNamespaceAndPath("ordertocook", "animations/" + group + ".animation.json");
    }
}
