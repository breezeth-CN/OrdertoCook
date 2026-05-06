package cn.breezeth.ordertocook.mixin;

import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class MotorcycleFallDamageMixin {
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void ordertocook$cancelMotorcycleFallDamage(float fallDistance, float damagePerDistance, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (MotorcycleEntity.fromVehicle(self.getVehicle()) != null) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void ordertocook$cancelMotorcycleContactDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (MotorcycleEntity.fromVehicle(self.getVehicle()) == null) {
            return;
        }
        if (source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH)) {
            cir.setReturnValue(false);
        }
    }
}
