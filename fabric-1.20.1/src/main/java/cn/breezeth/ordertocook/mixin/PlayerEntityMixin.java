package cn.breezeth.ordertocook.mixin;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.core.PrestigeManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class PlayerEntityMixin implements PrestigeManager.IPrestigeData {
    @Unique
    private int prestige = 0;

    @Override
    public int getPrestige() {
        return prestige;
    }

    @Override
    public void setPrestige(int prestige) {
        this.prestige = prestige;
    }

    @Override
    public void addPrestige(int amount) {
        this.prestige += amount;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt(ModConstants.NBT_PRESTIGE_KEY, prestige);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(ModConstants.NBT_PRESTIGE_KEY)) {
            prestige = nbt.getInt(ModConstants.NBT_PRESTIGE_KEY);
        }
    }
}
