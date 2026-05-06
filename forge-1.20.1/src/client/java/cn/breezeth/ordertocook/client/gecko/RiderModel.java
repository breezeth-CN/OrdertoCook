package cn.breezeth.ordertocook.client.gecko;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class RiderModel extends GeoModel<RiderAnimatable> {
    @Override
    public ResourceLocation getModelResource(RiderAnimatable animatable) {
        return HumanoidAnimationAssets.getModel(animatable.getAnimationGroup(), animatable.getPlayer().getModelName());
    }

    @Override
    public ResourceLocation getTextureResource(RiderAnimatable animatable) {
        return animatable.getPlayer().getSkinTextureLocation();
    }

    @Override
    public ResourceLocation getAnimationResource(RiderAnimatable animatable) {
        return HumanoidAnimationAssets.getAnimation(animatable.getAnimationGroup());
    }
}

