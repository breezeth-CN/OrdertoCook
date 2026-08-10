package cn.breezeth.ordertocook.mixin.client.compat.touhoulittlemaid;

import cn.breezeth.ordertocook.compat.TouhouLittleMaidCustomerCompat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MaidEatingSwingStatusMixin {
    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void oc$handleMaidEatingArm(byte event, CallbackInfo ci) {
        if (event != TouhouLittleMaidCustomerCompat.EAT_LEFT_ARM_EVENT
                && event != TouhouLittleMaidCustomerCompat.EAT_RIGHT_ARM_EVENT) {
            return;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!TouhouLittleMaidCustomerCompat.isTouhouMaid(entity)) return;
        MaidEatingAnimationState.start(entity, event == TouhouLittleMaidCustomerCompat.EAT_LEFT_ARM_EVENT);
        entity.swing(event == TouhouLittleMaidCustomerCompat.EAT_LEFT_ARM_EVENT
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        ci.cancel();
    }
}
