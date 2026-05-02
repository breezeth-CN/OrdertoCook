package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
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
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (!bagStack.isEmpty()) {
            nbt.put("BagStack", bagStack.encode(registryLookup));
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("BagStack")) {
            this.bagStack = ItemStack.fromNbt(registryLookup, nbt.getCompound("BagStack")).orElse(ItemStack.EMPTY);
        } else {
            this.bagStack = ItemStack.EMPTY;
        }
    }
}
