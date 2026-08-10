package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TakeoutBagBlockEntity extends BlockEntity implements GeoBlockEntity {
    private ItemStack bagStack = ItemStack.EMPTY;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TakeoutBagBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TAKEOUT_BAG.get(), pos, state);
    }

    public ItemStack getBagStack() {
        return bagStack;
    }

    public void setBagStack(ItemStack stack) {
        this.bagStack = stack;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (!bagStack.isEmpty()) {
            nbt.put("BagStack", bagStack.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("BagStack")) {
            this.bagStack = ItemStack.of(nbt.getCompound("BagStack"));
        } else {
            this.bagStack = ItemStack.EMPTY;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            state.setAndContinue(RawAnimation.begin().thenPlay("maked"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
