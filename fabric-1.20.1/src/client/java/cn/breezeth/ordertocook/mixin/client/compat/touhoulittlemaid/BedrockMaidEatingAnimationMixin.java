package cn.breezeth.ordertocook.mixin.client.compat.touhoulittlemaid;

import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.registry.ModEntities;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.HashMap;

@Pseudo
@Mixin(targets = "com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.BedrockModel", remap = false)
public abstract class BedrockMaidEatingAnimationMixin {
    @Shadow(remap = false) @Final
    protected HashMap<String, Object> modelMapWrapper;

    @Inject(method = "setupAnim", at = @At("TAIL"), remap = false, require = 0)
    private void oc$applyEatingArm(@Coerce Object target, float limbSwing, float limbSwingAmount,
                                   float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(target instanceof LivingEntity maid)) return;
        MaidEatingAnimationState.Sample sample = MaidEatingAnimationState.sample(maid);
        if (sample == null) return;
        oc$poseArm(modelMapWrapper.get(sample.left() ? "armLeft" : "armRight"), sample.progress(), sample.left());
    }

    private static void oc$poseArm(Object arm, float progress, boolean left) {
        if (arm == null) return;
        try {
            Class<?> type = arm.getClass();
            float strength = (float) Math.sin(progress * Math.PI);
            float x = ((Number) type.getMethod("getRotateAngleX").invoke(arm)).floatValue();
            float y = ((Number) type.getMethod("getRotateAngleY").invoke(arm)).floatValue();
            float z = ((Number) type.getMethod("getRotateAngleZ").invoke(arm)).floatValue();
            type.getMethod("setRotateAngleX", float.class).invoke(arm, x - strength * 1.25f);
            type.getMethod("setRotateAngleY", float.class).invoke(arm, y + strength * (left ? 0.20f : -0.20f));
            type.getMethod("setRotateAngleZ", float.class).invoke(arm, z + strength * (left ? -0.12f : 0.12f));
        } catch (ReflectiveOperationException exception) {
            MaidEatingAnimationState.reportFailure("bedrock arm", exception);
        }
    }

    private static Boolean oc$invokeBoolean(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target) instanceof Boolean value ? value : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
