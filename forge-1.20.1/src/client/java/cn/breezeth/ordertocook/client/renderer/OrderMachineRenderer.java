package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class OrderMachineRenderer extends GeoBlockRenderer<OrderMachineBlockEntity> {
    private static final String ORDER_LAYER_BONE = "group";

    public OrderMachineRenderer() {
        super(new OrderMachineGeoModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, OrderMachineBlockEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (ORDER_LAYER_BONE.equals(bone.getName())) {
            if (!animatable.shouldRenderOrderLayer()) {
                return;
            }
            if (animatable.shouldHoldNewOrderLastFrame()) {
                bone.updatePosition(0.0f, 7.0f, 3.0f);
                bone.updateRotation(0.0f, 0.0f, 0.0f);
                bone.updateScale(1.0f, 1.0f, 1.0f);
            }
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}

