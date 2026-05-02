package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public class TakeoutBagBlockEntity extends BlockEntity {
    private ItemStack bagStack = ItemStack.EMPTY;

    public TakeoutBagBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TAKEOUT_BAG, pos, state);
    }

    public ItemStack getBagStack() {
        return bagStack;
    }

    public void setBagStack(ItemStack stack) {
        this.bagStack = stack;
        markDirty();
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (!bagStack.isEmpty()) {
            nbt.put("BagStack", bagStack.writeNbt(new NbtCompound()));
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("BagStack")) {
            this.bagStack = ItemStack.fromNbt(nbt.getCompound("BagStack"));
        } else {
            this.bagStack = ItemStack.EMPTY;
        }
    }
}
