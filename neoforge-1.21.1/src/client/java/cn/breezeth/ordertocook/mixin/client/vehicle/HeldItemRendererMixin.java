package cn.breezeth.ordertocook.mixin.client.vehicle;

import cn.breezeth.ordertocook.client.gecko.RiderRenderBridge;
import cn.breezeth.ordertocook.client.gecko.RiderFirstPersonArmRenderer;
import cn.breezeth.ordertocook.client.gecko.RiderAnimatable;
import cn.breezeth.ordertocook.client.gecko.FirstPersonArmCalibration;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {
    private static final RiderFirstPersonArmRenderer FIRST_PERSON_ARM_RENDERER = new RiderFirstPersonArmRenderer();

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void ordertocook$adjustRidingArmPose(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.options == null || !client.options.getCameraType().isFirstPerson()) {
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

        matrices.pushPose();
        if (hand == InteractionHand.MAIN_HAND) {
            matrices.translate(FirstPersonArmCalibration.mainX(profile), FirstPersonArmCalibration.mainY(profile), FirstPersonArmCalibration.mainZ(profile));
            matrices.mulPose(com.mojang.math.Axis.XP.rotationDegrees(FirstPersonArmCalibration.mainXRot(profile)));
            matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(FirstPersonArmCalibration.mainYRot(profile)));
            matrices.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(FirstPersonArmCalibration.mainZRot(profile)));
        } else {
            matrices.translate(FirstPersonArmCalibration.offX(profile), FirstPersonArmCalibration.offY(profile), FirstPersonArmCalibration.offZ(profile));
            matrices.mulPose(com.mojang.math.Axis.XP.rotationDegrees(FirstPersonArmCalibration.offXRot(profile)));
            matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(FirstPersonArmCalibration.offYRot(profile)));
            matrices.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(FirstPersonArmCalibration.offZRot(profile)));
        }

        if (motorcycle != null && hand == InteractionHand.MAIN_HAND && hornAnimating) {
            if (hornRepeat) {
                matrices.translate(FirstPersonArmCalibration.HORN_REPEAT_X, FirstPersonArmCalibration.HORN_REPEAT_Y, FirstPersonArmCalibration.HORN_REPEAT_Z);
                matrices.mulPose(com.mojang.math.Axis.XP.rotationDegrees(FirstPersonArmCalibration.HORN_REPEAT_X_ROT));
                matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(FirstPersonArmCalibration.HORN_REPEAT_Y_ROT));
                matrices.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(FirstPersonArmCalibration.HORN_REPEAT_Z_ROT));
            } else {
                matrices.translate(FirstPersonArmCalibration.HORN_MAIN_X, FirstPersonArmCalibration.HORN_MAIN_Y, FirstPersonArmCalibration.HORN_MAIN_Z);
                matrices.mulPose(com.mojang.math.Axis.XP.rotationDegrees(FirstPersonArmCalibration.HORN_MAIN_X_ROT));
                matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(FirstPersonArmCalibration.HORN_MAIN_Y_ROT));
                matrices.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(FirstPersonArmCalibration.HORN_MAIN_Z_ROT));
            }
        }

        RiderAnimatable animatable = RiderRenderBridge.getOrCreateAnimatable(player);
        FIRST_PERSON_ARM_RENDERER.renderArm(matrices, animatable, vertexConsumers, tickDelta, hand == InteractionHand.OFF_HAND);
        matrices.popPose();
        ci.cancel();
    }
}

