package cn.breezeth.ordertocook.mixin.client.compat.touhoulittlemaid;

import cn.breezeth.ordertocook.compat.TouhouLittleMaidCustomerCompat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MaidEatingSwingStatusMixin {
    @Inject(method = "handleStatus", at = @At("HEAD"), cancellable = true)
    private void oc$handleMaidEatingArm(byte event, CallbackInfo ci) {
        if (event != TouhouLittleMaidCustomerCompat.EAT_LEFT_ARM_EVENT
                && event != TouhouLittleMaidCustomerCompat.EAT_RIGHT_ARM_EVENT) {
            return;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!TouhouLittleMaidCustomerCompat.isTouhouMaid(entity)) return;
        MaidEatingAnimationState.start(entity, event == TouhouLittleMaidCustomerCompat.EAT_LEFT_ARM_EVENT);
        entity.swingHand(event == TouhouLittleMaidCustomerCompat.EAT_LEFT_ARM_EVENT
                ? Hand.OFF_HAND : Hand.MAIN_HAND);
        ci.cancel();
    }
}
