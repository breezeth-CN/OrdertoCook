package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.RefrigeratorBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class RefrigeratorGeoModel extends GeoModel<RefrigeratorBlockEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "geo/block/refrigerator.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/block/refrigerator.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "animations/refrigerator.animation.json");

    @Override
    public ResourceLocation getModelResource(RefrigeratorBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(RefrigeratorBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(RefrigeratorBlockEntity animatable) {
        return ANIMATION;
    }
}
