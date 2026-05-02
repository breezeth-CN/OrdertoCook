package cn.breezeth.ordertocook.mixin.client.vehicle;

import cn.breezeth.ordertocook.client.gecko.RiderRenderBridge;
import cn.breezeth.ordertocook.client.gecko.RiderFirstPersonArmRenderer;
import cn.breezeth.ordertocook.client.gecko.RiderAnimatable;
import cn.breezeth.ordertocook.client.gecko.FirstPersonArmCalibration;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    private static final RiderFirstPersonArmRenderer FIRST_PERSON_ARM_RENDERER = new RiderFirstPersonArmRenderer();

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void ordertocook$adjustRidingArmPose(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null || !client.options.getPerspective().isFirstPerson()) {
            return;
        }
        MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
        boolean chairClapping = player.getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null && RiderRenderBridge.isChairClapping(player);
        boolean washing = RiderRenderBridge.isWashing(player);
        if (motorcycle == null && !chairClapping && !washing) {
            return;
        }
        FirstPersonArmCalibration.Profile profile = RiderRenderBridge.getFirstPersonProfile(player);

        boolean hornAnimating = RiderRenderBridge.isHornAnimating(player);
        boolean hornRepeat = RiderRenderBridge.isRepeatHornAnimating(player);

        matrices.push();
        if (hand == Hand.MAIN_HAND) {
            matrices.translate(FirstPersonArmCalibration.mainX(profile), FirstPersonArmCalibration.mainY(profile), FirstPersonArmCalibration.mainZ(profile));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(FirstPersonArmCalibration.mainXRot(profile)));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(FirstPersonArmCalibration.mainYRot(profile)));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(FirstPersonArmCalibration.mainZRot(profile)));
        } else {
            matrices.translate(FirstPersonArmCalibration.offX(profile), FirstPersonArmCalibration.offY(profile), FirstPersonArmCalibration.offZ(profile));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(FirstPersonArmCalibration.offXRot(profile)));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(FirstPersonArmCalibration.offYRot(profile)));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(FirstPersonArmCalibration.offZRot(profile)));
        }

        if (motorcycle != null && hand == Hand.MAIN_HAND && hornAnimating) {
            if (hornRepeat) {
                matrices.translate(FirstPersonArmCalibration.HORN_REPEAT_X, FirstPersonArmCalibration.HORN_REPEAT_Y, FirstPersonArmCalibration.HORN_REPEAT_Z);
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(FirstPersonArmCalibration.HORN_REPEAT_X_ROT));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(FirstPersonArmCalibration.HORN_REPEAT_Y_ROT));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(FirstPersonArmCalibration.HORN_REPEAT_Z_ROT));
            } else {
                matrices.translate(FirstPersonArmCalibration.HORN_MAIN_X, FirstPersonArmCalibration.HORN_MAIN_Y, FirstPersonArmCalibration.HORN_MAIN_Z);
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(FirstPersonArmCalibration.HORN_MAIN_X_ROT));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(FirstPersonArmCalibration.HORN_MAIN_Y_ROT));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(FirstPersonArmCalibration.HORN_MAIN_Z_ROT));
            }
        }

        RiderAnimatable animatable = RiderRenderBridge.getOrCreateAnimatable(player);
        FIRST_PERSON_ARM_RENDERER.renderArm(matrices, animatable, vertexConsumers, tickDelta, hand == Hand.OFF_HAND);
        matrices.pop();
        ci.cancel();
    }
}

