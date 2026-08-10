package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.RefrigeratorBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public final class RefrigeratorGeoModel extends GeoModel<RefrigeratorBlockEntity> {
    private static final Identifier MODEL = new Identifier(ModConstants.MOD_ID, "geo/block/refrigerator.geo.json");
    private static final Identifier TEXTURE = new Identifier(ModConstants.MOD_ID, "textures/block/refrigerator.png");
    private static final Identifier ANIMATION = new Identifier(ModConstants.MOD_ID, "animations/refrigerator.animation.json");

    @Override
    public Identifier getModelResource(RefrigeratorBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(RefrigeratorBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(RefrigeratorBlockEntity animatable) {
        return ANIMATION;
    }
}
