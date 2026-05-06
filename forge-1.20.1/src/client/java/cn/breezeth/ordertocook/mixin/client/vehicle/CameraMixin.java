package cn.breezeth.ordertocook.mixin.client.vehicle;

import cn.breezeth.ordertocook.entity.SeatEntity;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow(remap = false, aliases = "m_90584_")
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = {
            "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            "m_90575_(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"
    }, at = @At("TAIL"), remap = false)
    private void ordertocook$lowerChairFirstPersonCamera(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (thirdPerson) {
            return;
        }
        if (!(focusedEntity.getVehicle() instanceof SeatEntity seat) || seat.resolveParent() != null) {
            return;
        }

        double offset = 8.0 / 16.0;
        Camera self = (Camera) (Object) this;
        this.setPosition(self.getPosition().x, self.getPosition().y - offset, self.getPosition().z);
    }
}
