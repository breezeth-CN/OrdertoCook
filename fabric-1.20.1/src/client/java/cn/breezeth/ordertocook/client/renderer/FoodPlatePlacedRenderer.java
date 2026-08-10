package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.FoodPlateBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class FoodPlatePlacedRenderer extends GeoBlockRenderer<FoodPlateBlockEntity> {
    public FoodPlatePlacedRenderer() {
        super(new FoodPlatePlacedGeoModel());
    }

    public void renderRecursively(MatrixStack poseStack, FoodPlateBlockEntity animatable, GeoBone bone, RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (!animatable.shouldRenderPlacedAnimation()) {
            return;
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
