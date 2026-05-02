package cn.breezeth.ordertocook.mixin;

import cn.breezeth.ordertocook.core.OrderNpcRegistry;
import cn.breezeth.ordertocook.core.OrderNpcVisualAccess;
import cn.breezeth.ordertocook.mixin.accessor.MobEntityAccessor;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombieEntity.class)
public class ZombieEntityMixin implements OrderNpcVisualAccess {
    @Unique private boolean oc$goalsAdded = false;
    @Unique private static final TrackedData<Integer> oc$SKIN_VARIANT = DataTracker.registerData(ZombieEntity.class, TrackedDataHandlerRegistry.INTEGER);

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void oc$initDataTracker(CallbackInfo ci) {
        ZombieEntity self = (ZombieEntity)(Object)this;
        self.getDataTracker().startTracking(oc$SKIN_VARIANT, 0);
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void oc$onTickMovement(CallbackInfo ci) {
        ZombieEntity self = (ZombieEntity)(Object)this;
        if (self.getWorld().isClient()) return;
        if (OrderNpcRegistry.isNpc(self.getUuid())) {
            if (!oc$goalsAdded) {
                MobEntityAccessor accessor = (MobEntityAccessor) (Object) self;
                accessor.oc$getGoalSelector().add(8, new LookAtEntityGoal(self, PlayerEntity.class, 8.0f));
                accessor.oc$getGoalSelector().add(9, new LookAroundGoal(self));
                oc$goalsAdded = true;
            }
        }
    }

    @Override
    public void oc$setSkinVariant(int variant) {
        ZombieEntity self = (ZombieEntity)(Object)this;
        self.getDataTracker().set(oc$SKIN_VARIANT, variant);
    }

    @Override
    public int oc$getSkinVariant() {
        ZombieEntity self = (ZombieEntity)(Object)this;
        return self.getDataTracker().get(oc$SKIN_VARIANT);
    }
}
