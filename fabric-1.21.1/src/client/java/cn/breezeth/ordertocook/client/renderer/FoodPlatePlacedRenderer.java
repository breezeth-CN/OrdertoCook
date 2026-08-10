package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.FoodPlateBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class FoodPlatePlacedRenderer extends GeoBlockRenderer<FoodPlateBlockEntity> {
    public FoodPlatePlacedRenderer() {
        super(new FoodPlatePlacedGeoModel());
    }

    @Override
    public void render(FoodPlateBlockEntity animatable, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight, int packedOverlay) {
        if (!animatable.shouldRenderPlacedAnimation()) {
            return;
        }
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
