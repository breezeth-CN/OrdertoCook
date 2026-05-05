package cn.breezeth.ordertocook.client.gecko;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public final class RiderFirstPersonArmRenderer extends GeoObjectRenderer<RiderAnimatable> {
    private boolean renderLeft = false;
    private static final Set<String> LEFT_BONES = Set.of("body", "left_arm", "left_hand", "left_item");
    private static final Set<String> RIGHT_BONES = Set.of("body", "right_arm", "right_hand", "right_item");

    public RiderFirstPersonArmRenderer() {
        super(new RiderModel());
    }

    public void renderArm(PoseStack matrices, RiderAnimatable animatable, MultiBufferSource vertexConsumers, float tickDelta, boolean leftHand) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options == null || !client.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (client.getCameraEntity() != client.player || animatable.getPlayer() != client.player) {
            return;
        }

        this.renderLeft = leftHand;
        matrices.pushPose();
        matrices.translate(0.0, FirstPersonArmCalibration.ROOT_Y, 0.0);
        RenderType layer = RenderType.entityCutout(animatable.getPlayer().getSkin().texture());
        VertexConsumer consumer = vertexConsumers.getBuffer(layer);
        int packedLight = LevelRenderer.getLightColor(animatable.getPlayer().level(), animatable.getPlayer().blockPosition());
        this.render(matrices, animatable, vertexConsumers, layer, consumer, packedLight, tickDelta);
        matrices.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, RiderAnimatable animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int renderColor) {
        Minecraft client = Minecraft.getInstance();
        if (client.options == null || !client.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (renderLeft) {
            if (!LEFT_BONES.contains(bone.getName())) return;
        } else {
            if (!RIGHT_BONES.contains(bone.getName())) return;
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, renderColor);
    }
}

