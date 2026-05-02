package cn.breezeth.ordertocook.client.render;

import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.Identifier;

/**
 * 骑乘/Geo 路径使用 {@code item/helmet_ride_*}（generated、仅 layer0），避免复杂头盔模型在嵌套渲染中呈紫黑块；缺失时回退 {@code item/helmet}。
 */
public final class MotorcycleHelmetModels {
    private MotorcycleHelmetModels() {
    }

    public static BakedModel getForMotorcycleColor(int motorcycleColor) {
        Identifier id = switch (Math.max(0, Math.min(3, motorcycleColor))) {
            case 1 -> Identifier.of(ModConstants.MOD_ID, "item/helmet_ride_red");
            case 2 -> Identifier.of(ModConstants.MOD_ID, "item/helmet_ride_blue");
            case 3 -> Identifier.of(ModConstants.MOD_ID, "item/helmet_ride_yellow");
            default -> Identifier.of(ModConstants.MOD_ID, "item/helmet_ride_white");
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
