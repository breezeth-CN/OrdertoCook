package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.StoveBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class StoveGeoModel extends GeoModel<StoveBlockEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "geo/block/stove.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/block/stove.png");
    private static final ResourceLocation FIRE_BURST = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/block/stove_fire.png");
    private static final ResourceLocation[] FIRE_CYCLE = {
        ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/block/stove_fire_0.png"),
        ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/block/stove_fire_1.png"),
        ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/block/stove_fire_2.png"),
        ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/block/stove_fire_3.png"),
    };
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "animations/stove.animation.json");

    @Override
    public ResourceLocation getModelResource(StoveBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(StoveBlockEntity animatable) {
        int frame = animatable.getFireFrame();
        if (frame == -1) return FIRE_BURST;
        if (frame >= 1 && frame <= FIRE_CYCLE.length) return FIRE_CYCLE[frame - 1];
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(StoveBlockEntity animatable) {
        return ANIMATION;
    }
}
