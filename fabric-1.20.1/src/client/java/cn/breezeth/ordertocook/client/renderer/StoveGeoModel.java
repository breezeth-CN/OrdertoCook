package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.StoveBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public final class StoveGeoModel extends GeoModel<StoveBlockEntity> {
    private static final Identifier MODEL = new Identifier(ModConstants.MOD_ID, "geo/block/stove.geo.json");
    private static final Identifier TEXTURE = new Identifier(ModConstants.MOD_ID, "textures/block/stove.png");
    private static final Identifier FIRE_BURST = new Identifier(ModConstants.MOD_ID, "textures/block/stove_fire.png");
    private static final Identifier[] FIRE_CYCLE = {
            new Identifier(ModConstants.MOD_ID, "textures/block/stove_fire_0.png"),
            new Identifier(ModConstants.MOD_ID, "textures/block/stove_fire_1.png"),
            new Identifier(ModConstants.MOD_ID, "textures/block/stove_fire_2.png"),
            new Identifier(ModConstants.MOD_ID, "textures/block/stove_fire_3.png")
    };
    private static final Identifier ANIMATION = new Identifier(ModConstants.MOD_ID, "animations/stove.animation.json");

    @Override
    public Identifier getModelResource(StoveBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(StoveBlockEntity animatable) {
        int frame = animatable.getFireFrame();
        if (frame == -1) {
            return FIRE_BURST;
        }
        if (frame >= 1 && frame <= FIRE_CYCLE.length) {
            return FIRE_CYCLE[frame - 1];
        }
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(StoveBlockEntity animatable) {
        return ANIMATION;
    }
}
