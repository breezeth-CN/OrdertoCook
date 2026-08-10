package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.TakeoutBagBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class TakeoutBagPlacedGeoModel extends GeoModel<TakeoutBagBlockEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "geo/block/takeout_bag_placed.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/block/takeout_bag_placed.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "animations/takeout_bag_placed.animation.json");

    @Override
    public ResourceLocation getModelResource(TakeoutBagBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TakeoutBagBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(TakeoutBagBlockEntity animatable) {
        return ANIMATION;
    }
}

