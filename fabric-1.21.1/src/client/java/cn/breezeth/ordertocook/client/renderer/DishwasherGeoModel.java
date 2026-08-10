package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.DishwasherBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public final class DishwasherGeoModel extends GeoModel<DishwasherBlockEntity> {
    private static final Identifier MODEL = Identifier.of(ModConstants.MOD_ID, "geo/block/dishwasher.geo.json");
    private static final Identifier TEXTURE = Identifier.of(ModConstants.MOD_ID, "textures/block/dishwasher.png");
    private static final Identifier ANIMATION = Identifier.of(ModConstants.MOD_ID, "animations/dishwasher.animation.json");

    @Override
    public Identifier getModelResource(DishwasherBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(DishwasherBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(DishwasherBlockEntity animatable) {
        return ANIMATION;
    }
}
