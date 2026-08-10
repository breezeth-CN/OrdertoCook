package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.FoodPlateBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public final class FoodPlatePlacedGeoModel extends GeoModel<FoodPlateBlockEntity> {
    private static final Identifier MODEL = Identifier.of(ModConstants.MOD_ID, "geo/block/food_plate_placed.geo.json");
    private static final Identifier TEXTURE = Identifier.of(ModConstants.MOD_ID, "textures/block/food_plate_placed.png");
    private static final Identifier ANIMATION = Identifier.of(ModConstants.MOD_ID, "animations/food_plate_placed.animation.json");

    @Override
    public Identifier getModelResource(FoodPlateBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(FoodPlateBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(FoodPlateBlockEntity animatable) {
        return ANIMATION;
    }
}
