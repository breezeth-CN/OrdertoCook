package cn.breezeth.ordertocook.client.gecko;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

import java.util.Set;

/**
 * GeckoLib 4.8.x：第一人称下只绘制手臂相关骨骼；与 {@link RiderGeoRenderer} 使用同一 {@link RiderModel}。
 */
public final class RiderFirstPersonArmRenderer extends GeoObjectRenderer<RiderAnimatable> {
    private boolean renderLeft = false;
    private static final Set<String> LEFT_BONES = Set.of("body", "left_arm", "left_hand", "left_item");
    private static final Set<String> RIGHT_BONES = Set.of("body", "right_arm", "right_hand", "right_item");

    public RiderFirstPersonArmRenderer() {
        super(new RiderModel());
    }

    public void renderArm(MatrixStack matrices, RiderAnimatable animatable, VertexConsumerProvider vertexConsumers, float tickDelta, boolean leftHand) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options == null || !client.options.getPerspective().isFirstPerson()) {
            return;
        }
        if (client.getCameraEntity() != client.player || animatable.getPlayer() != client.player) {
            return;
        }

        this.renderLeft = leftHand;
        matrices.push();
        matrices.translate(0.0, FirstPersonArmCalibration.ROOT_Y, 0.0);
        RenderLayer layer = RenderLayer.getEntityCutout(animatable.getPlayer().getSkinTexture());
        VertexConsumer consumer = vertexConsumers.getBuffer(layer);
        Vec3d eye = animatable.getPlayer().getCameraPosVec(tickDelta);
        int packedLight = WorldRenderer.getLightmapCoordinates(animatable.getPlayer().getWorld(), BlockPos.ofFloored(eye));
        this.render(matrices, animatable, vertexConsumers, layer, consumer, packedLight);
        matrices.pop();
    }

    @Override
    public void renderRecursively(MatrixStack poseStack, RiderAnimatable animatable, GeoBone bone, RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null || !client.options.getPerspective().isFirstPerson()) {
            return;
        }
        if (renderLeft) {
            if (!LEFT_BONES.contains(bone.getName())) {
                return;
            }
        } else {
            if (!RIGHT_BONES.contains(bone.getName())) {
                return;
            }
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
