package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.FoodPlateBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class FoodPlatePlacedRenderer extends GeoBlockRenderer<FoodPlateBlockEntity> {
    public FoodPlatePlacedRenderer() {
        super(new FoodPlatePlacedGeoModel());
    }

    @Override
    public void render(FoodPlateBlockEntity animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!animatable.shouldRenderPlacedAnimation()) {
            return;
        }
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }
}

