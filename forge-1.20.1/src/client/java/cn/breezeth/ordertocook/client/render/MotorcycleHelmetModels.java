package cn.breezeth.ordertocook.client.render;

import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Resolves the side-loaded helmet models used by rider render layers.
 */
public final class MotorcycleHelmetModels {
    private MotorcycleHelmetModels() {
    }

    public static BakedModel getForMotorcycleColor(int motorcycleColor) {
        ResourceLocation id = switch (Math.max(0, Math.min(3, motorcycleColor))) {
            case 1 -> ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "helmet_red");
            case 2 -> ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "helmet_blue");
            case 3 -> ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "helmet_yellow");
            default -> ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "helmet_white");
        };
        var manager = Minecraft.getInstance().getModelManager();
        BakedModel model = manager.getModel(new net.minecraft.client.resources.model.ModelResourceLocation(id, "inventory"));
        BakedModel missing = manager.getMissingModel();
        if (model == null || model == missing) {
            model = manager.getModel(new net.minecraft.client.resources.model.ModelResourceLocation(id, "inventory"));
        }
        return model != null ? model : missing;
    }
}
