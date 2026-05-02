package cn.breezeth.ordertocook.client.gecko;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public final class RiderModel extends GeoModel<RiderAnimatable> {
    @Override
    public Identifier getModelResource(RiderAnimatable animatable) {
        return HumanoidAnimationAssets.getModel(animatable.getAnimationGroup(), animatable.getPlayer().getSkinTextures().model());
    }

    @Override
    public Identifier getTextureResource(RiderAnimatable animatable) {
        return animatable.getPlayer().getSkinTextures().texture();
    }

    @Override
    public Identifier getAnimationResource(RiderAnimatable animatable) {
        return HumanoidAnimationAssets.getAnimation(animatable.getAnimationGroup());
    }
}

