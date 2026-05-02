package cn.breezeth.ordertocook.client.render;

import cn.breezeth.ordertocook.core.ModConstants;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.Identifier;

/**
 * 骑乘/Geo 与 {@link cn.breezeth.ordertocook.client.render.feature.HelmetFeatureRenderer} 共用。
 * {@code ModelLoadingPlugin.addModels} 注册的模型须用 {@link FabricBakedModelManager#getModel(Identifier)} 取烘焙结果；
 * 用 {@code BakedModelManager#getModel(ModelIdentifier)} 会拿不到、呈紫黑 missing（1.21 则可用原版 {@code getModel(Identifier)}）。
 */
public final class MotorcycleHelmetModels {
    private MotorcycleHelmetModels() {
    }

    public static BakedModel getForMotorcycleColor(int motorcycleColor) {
        Identifier id = switch (Math.max(0, Math.min(3, motorcycleColor))) {
            case 1 -> new Identifier(ModConstants.MOD_ID, "item/helmet_red");
            case 2 -> new Identifier(ModConstants.MOD_ID, "item/helmet_blue");
            case 3 -> new Identifier(ModConstants.MOD_ID, "item/helmet_yellow");
            default -> new Identifier(ModConstants.MOD_ID, "item/helmet_white");
        };
        FabricBakedModelManager models = (FabricBakedModelManager) MinecraftClient.getInstance().getBakedModelManager();
        BakedModel model = models.getModel(id);
        BakedModel missing = MinecraftClient.getInstance().getBakedModelManager().getMissingModel();
        if (model == null || model == missing) {
            model = models.getModel(new Identifier(ModConstants.MOD_ID, "item/helmet"));
        }
        return model != null ? model : missing;
    }
}
