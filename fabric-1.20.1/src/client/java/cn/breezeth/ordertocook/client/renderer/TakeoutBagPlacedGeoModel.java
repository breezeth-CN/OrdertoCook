package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.TakeoutBagBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public final class TakeoutBagPlacedGeoModel extends GeoModel<TakeoutBagBlockEntity> {
    private static final Identifier MODEL = new Identifier(ModConstants.MOD_ID, "geo/block/takeout_bag_placed.geo.json");
    private static final Identifier TEXTURE = new Identifier(ModConstants.MOD_ID, "textures/block/takeout_bag_placed.png");
    private static final Identifier ANIMATION = new Identifier(ModConstants.MOD_ID, "animations/takeout_bag_placed.animation.json");

    @Override
    public Identifier getModelResource(TakeoutBagBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(TakeoutBagBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(TakeoutBagBlockEntity animatable) {
        return ANIMATION;
    }
}
