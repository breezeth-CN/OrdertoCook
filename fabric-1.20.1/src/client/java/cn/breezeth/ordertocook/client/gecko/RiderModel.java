package cn.breezeth.ordertocook.client.gecko;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public final class RiderModel extends GeoModel<RiderAnimatable> {
    @Override
    public Identifier getModelResource(RiderAnimatable animatable) {
        // 与 fabric-1.21.1 一致：宽/细臂由 AbstractClientPlayerEntity#getModel()（"default"/"slim"）驱动 geo 选择。
        return HumanoidAnimationAssets.getModel(animatable.getAnimationGroup(), animatable.getPlayer());
    }

    @Override
    public Identifier getTextureResource(RiderAnimatable animatable) {
        // 1.21 为 getSkinTextures().texture()；此处为 1.20.1 等价 API。
        return animatable.getPlayer().getSkinTexture();
    }

    @Override
    public Identifier getAnimationResource(RiderAnimatable animatable) {
        return HumanoidAnimationAssets.getAnimation(animatable.getAnimationGroup());
    }
}
