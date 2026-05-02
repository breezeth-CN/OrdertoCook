package cn.breezeth.ordertocook.mixin.client.vehicle;

import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    private static boolean ordertocook$isRidingMotorcycle() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && MotorcycleEntity.fromVehicle(client.player.getVehicle()) != null;
    }

    private static boolean ordertocook$isSittingChair() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null
                && client.player.getVehicle() instanceof SeatEntity seat
                && seat.resolveParent() == null;
    }

    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void ordertocook$cancelAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (ordertocook$isRidingMotorcycle() || ordertocook$isSittingChair()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"), cancellable = true)
    private void ordertocook$cancelBreaking(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (ordertocook$isRidingMotorcycle() || ordertocook$isSittingChair()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void ordertocook$cancelAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (ordertocook$isRidingMotorcycle() || ordertocook$isSittingChair()) {
            ci.cancel();
        }
    }
}
