package cn.breezeth.ordertocook.mixin;

import cn.breezeth.ordertocook.core.OrderNpcManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void oc$limitPickupOnSeat(PlayerEntity player, CallbackInfo ci) {
        Entity vehicle = player.getVehicle();
        if (vehicle == null) return;
        if (!vehicle.getCommandTags().contains(OrderNpcManager.TAG_CHAIR_SEAT)) return;

        ItemEntity self = (ItemEntity) (Object) this;
        double max = 2.0;
        if (self.squaredDistanceTo(player) > max * max) {
            ci.cancel();
        }
    }
}
