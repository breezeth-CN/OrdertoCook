package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.DishwasherBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class DishwasherGeoModel extends GeoModel<DishwasherBlockEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "geo/block/dishwasher.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/block/dishwasher.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "animations/dishwasher.animation.json");

    @Override
    public ResourceLocation getModelResource(DishwasherBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DishwasherBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DishwasherBlockEntity animatable) {
        return ANIMATION;
    }
}

