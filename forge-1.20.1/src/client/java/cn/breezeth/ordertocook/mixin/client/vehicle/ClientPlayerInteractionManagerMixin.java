package cn.breezeth.ordertocook.mixin.client.vehicle;

import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {
    private static boolean ordertocook$isRidingMotorcycle() {
        Minecraft client = Minecraft.getInstance();
        return client.player != null && MotorcycleEntity.fromVehicle(client.player.getVehicle()) != null;
    }

    private static boolean ordertocook$isSittingChair() {
        Minecraft client = Minecraft.getInstance();
        return client.player != null && client.player.getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null;
    }

    @Inject(method = {
            "startDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",
            "m_105269_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"
    }, at = @At("HEAD"), cancellable = true, remap = false)
    private void ordertocook$cancelAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (ordertocook$isRidingMotorcycle() || ordertocook$isSittingChair()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = {
            "continueDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",
            "m_105283_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"
    }, at = @At("HEAD"), cancellable = true, remap = false)
    private void ordertocook$cancelBreaking(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (ordertocook$isRidingMotorcycle() || ordertocook$isSittingChair()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = {
            "attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V",
            "m_105223_(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V"
    }, at = @At("HEAD"), cancellable = true, remap = false)
    private void ordertocook$cancelAttackEntity(Player player, Entity target, CallbackInfo ci) {
        if (ordertocook$isRidingMotorcycle() || ordertocook$isSittingChair()) {
            ci.cancel();
        }
    }
}

