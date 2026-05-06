package cn.breezeth.ordertocook.client.gecko;

import net.minecraft.resources.ResourceLocation;

public final class HumanoidAnimationAssets {
    private HumanoidAnimationAssets() {
    }

    public static ResourceLocation getModel(String group, String modelName) {
        if ("slim".equals(modelName)) {
            return new ResourceLocation("ordertocook", "geo/drive_idle_alex.geo.json");
        }
        return new ResourceLocation("ordertocook", "geo/drive_idle.geo.json");
    }

    public static ResourceLocation getAnimation(String group) {
        return new ResourceLocation("ordertocook", "animations/" + group + ".animation.json");
    }
}
