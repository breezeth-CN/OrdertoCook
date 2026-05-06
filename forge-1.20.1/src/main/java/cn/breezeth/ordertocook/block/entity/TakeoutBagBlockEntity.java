package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TakeoutBagBlockEntity extends BlockEntity {
    private ItemStack bagStack = ItemStack.EMPTY;

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
}
