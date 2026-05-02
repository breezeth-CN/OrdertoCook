package cn.breezeth.ordertocook.mixin.client.vehicle;

import cn.breezeth.ordertocook.entity.SeatEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 与 fabric-1.21.1 回滚版一致：仅椅子无 parent 时第一人称降低镜头；摩托不在这里改摄像机。
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Inject(method = "update", at = @At("TAIL"))
    private void ordertocook$lowerChairFirstPersonCamera(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (thirdPerson) {
            return;
        }
        if (!(focusedEntity.getVehicle() instanceof SeatEntity seat) || seat.resolveParent() != null) {
            return;
        }

        double offset = 8.0 / 16.0;
        Camera self = (Camera) (Object) this;
        this.setPos(self.getPos().x, self.getPos().y - offset, self.getPos().z);
    }
}
