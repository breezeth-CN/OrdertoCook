package cn.breezeth.ordertocook.client.render;

import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.Identifier;

/**
 * Resolves the side-loaded 3D helmet models used by rider render layers.
 */
public final class MotorcycleHelmetModels {
    private MotorcycleHelmetModels() {
    }

    public static BakedModel getForMotorcycleColor(int motorcycleColor) {
        Identifier id = switch (Math.max(0, Math.min(3, motorcycleColor))) {
            case 1 -> Identifier.of(ModConstants.MOD_ID, "item/helmet_red");
            case 2 -> Identifier.of(ModConstants.MOD_ID, "item/helmet_blue");
            case 3 -> Identifier.of(ModConstants.MOD_ID, "item/helmet_yellow");
            default -> Identifier.of(ModConstants.MOD_ID, "item/helmet_white");
        };
        var manager = MinecraftClient.getInstance().getBakedModelManager();
        BakedModel model = manager.getModel(id);
        BakedModel missing = manager.getMissingModel();
        if (model == null || model == missing) {
            model = manager.getModel(Identifier.of(ModConstants.MOD_ID, "item/helmet"));
        }
        return model != null ? model : missing;
    }
}
