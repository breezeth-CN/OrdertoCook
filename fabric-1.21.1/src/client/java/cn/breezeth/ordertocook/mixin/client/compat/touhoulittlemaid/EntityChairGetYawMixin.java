package cn.breezeth.ordertocook.mixin.client.compat.touhoulittlemaid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair", remap = false)
public abstract class EntityChairGetYawMixin {
    @Inject(method = "getYaw()F", at = @At("HEAD"), cancellable = true)
    private void oc$fixGetYaw(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(0.0f);
    }
}
