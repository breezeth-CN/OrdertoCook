package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.FoodPlateBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class FoodPlatePlacedGeoModel extends GeoModel<FoodPlateBlockEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(ModConstants.MOD_ID, "geo/block/food_plate_placed.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(ModConstants.MOD_ID, "textures/block/food_plate_placed.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(ModConstants.MOD_ID, "animations/food_plate_placed.animation.json");

    @Override
    public ResourceLocation getModelResource(FoodPlateBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(FoodPlateBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(FoodPlateBlockEntity animatable) {
        return ANIMATION;
    }
}

