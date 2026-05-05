package cn.breezeth.ordertocook.mixin;

import cn.breezeth.ordertocook.core.OrderNpcManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void oc$limitPickupOnSeat(Player player, CallbackInfo ci) {
        Entity vehicle = player.getVehicle();
        if (vehicle == null) return;
        if (!vehicle.getTags().contains(OrderNpcManager.TAG_CHAIR_SEAT)) return;

        ItemEntity self = (ItemEntity) (Object) this;
        double max = 2.0;
        if (self.distanceToSqr(player) > max * max) {
            ci.cancel();
        }
    }
}
