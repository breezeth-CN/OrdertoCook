package cn.breezeth.ordertocook.client.gecko;

import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

public final class HumanoidAnimationAssets {
    private HumanoidAnimationAssets() {
    }

    public static Identifier getModel(String group, SkinTextures.Model model) {
        if (model == SkinTextures.Model.SLIM) {
            return Identifier.of("ordertocook", "geo/drive_idle_alex.geo.json");
        }
        return Identifier.of("ordertocook", "geo/drive_idle.geo.json");
    }

    public static Identifier getAnimation(String group) {
        return Identifier.of("ordertocook", "animations/" + group + ".animation.json");
    }
}
