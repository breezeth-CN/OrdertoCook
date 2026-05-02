package cn.breezeth.ordertocook.mixin;

import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class MotorcycleFallDamageMixin {
    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void ordertocook$cancelMotorcycleFallDamage(float fallDistance, float damagePerDistance, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (MotorcycleEntity.fromVehicle(self.getVehicle()) != null) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void ordertocook$cancelMotorcycleContactDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (MotorcycleEntity.fromVehicle(self.getVehicle()) == null) {
            return;
        }
        if (source.isOf(DamageTypes.CACTUS) || source.isOf(DamageTypes.SWEET_BERRY_BUSH)) {
            cir.setReturnValue(false);
        }
    }
}
