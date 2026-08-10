package cn.breezeth.ordertocook.mixin.client.compat.touhoulittlemaid;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity", remap = false)
public abstract class GeckoMaidEatingAnimationMixin {
    @Inject(method = "setCustomAnimations", at = @At("TAIL"), remap = false, require = 0)
    private void oc$applyEatingPose(@Coerce Object context, @Coerce Object event,
                                    CallbackInfoReturnable<Boolean> cir) {
        Object maidApi = oc$invoke(this, "getMaid");
        Object maidObject = maidApi == null ? null : oc$invoke(maidApi, "asEntity");
        if (!(maidObject instanceof LivingEntity maid)) return;
        MaidEatingAnimationState.Sample sample = MaidEatingAnimationState.sample(maid);
        if (sample == null) return;
        Object model = oc$invoke(this, "getCurrentModel");
        if (model == null) return;
        Object arm = oc$invoke(model, sample.left() ? "leftArm" : "rightArm");
        oc$poseArm(arm, sample.progress(), sample.left());
    }

    private static void oc$poseArm(Object arm, float progress, boolean left) {
        if (arm == null) return;
        try {
            Class<?> type = arm.getClass();
            float strength = (float) Math.sin(progress * Math.PI);
            float x = ((Number) type.getMethod("getRotationX").invoke(arm)).floatValue();
            float y = ((Number) type.getMethod("getRotationY").invoke(arm)).floatValue();
            float z = ((Number) type.getMethod("getRotationZ").invoke(arm)).floatValue();
            type.getMethod("setRotationX", float.class).invoke(arm, x - strength * 1.25f);
            type.getMethod("setRotationY", float.class).invoke(arm, y + strength * (left ? 0.20f : -0.20f));
            type.getMethod("setRotationZ", float.class).invoke(arm, z + strength * (left ? -0.12f : 0.12f));
        } catch (ReflectiveOperationException exception) {
            MaidEatingAnimationState.reportFailure("gecko arm", exception);
        }
    }

    private static Object oc$invoke(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            MaidEatingAnimationState.reportFailure("gecko " + name, exception);
            return null;
        }
    }
}
