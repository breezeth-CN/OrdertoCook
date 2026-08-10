package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class OrderMachineRenderer extends GeoBlockRenderer<OrderMachineBlockEntity> {
    private static final String ORDER_LAYER_BONE = "group";

    public OrderMachineRenderer() {
        super(new OrderMachineGeoModel());
    }

    @Override
    public void renderRecursively(MatrixStack poseStack, OrderMachineBlockEntity animatable, GeoBone bone, RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int renderColor) {
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
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, renderColor);
    }
}
