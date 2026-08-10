package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.FoodPlateBlockEntity;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class FoodPlatePlacedRenderer extends GeoBlockRenderer<FoodPlateBlockEntity> {
    public FoodPlatePlacedRenderer() {
        super(new FoodPlatePlacedGeoModel());
    }

    public void renderRecursively(PoseStack poseStack, FoodPlateBlockEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (!animatable.shouldRenderPlacedAnimation()) {
            return;
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}

