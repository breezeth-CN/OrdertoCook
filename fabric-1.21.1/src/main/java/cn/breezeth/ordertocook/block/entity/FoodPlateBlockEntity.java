package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.block.FoodPlateBlock;
import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.registry.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FoodPlateBlockEntity extends BlockEntity {
    private static final int STAGE_ONE_TICK = 15;
    private static final int STAGE_TWO_TICK = 60;
    private static final int STAGE_THREE_TICK = 80;
    private static final int DIRTY_PLATE_TICK = 100;

    private ItemStack plateStack = ItemStack.EMPTY;
    private int eatingTicks = -1;

    public FoodPlateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOOD_PLATE_DISPLAY, pos, state);
    }

    public ItemStack getPlateStack() {
        return plateStack;
    }

    public void setPlateStack(ItemStack stack) {
        this.plateStack = stack;
        markDirty();
    }

    public void startEatingSequence(ItemStack stack) {
        this.plateStack = stack.copyWithCount(1);
        this.eatingTicks = 0;
        updateStage(0);
        markDirty();
    }

    public static void tick(World world, BlockPos pos, BlockState state, FoodPlateBlockEntity blockEntity) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        if (blockEntity.eatingTicks < 0) {
            return;
        }
        blockEntity.eatingTicks++;
        if (blockEntity.eatingTicks == STAGE_ONE_TICK) {
            blockEntity.updateStage(serverWorld, pos, 1);
        } else if (blockEntity.eatingTicks == STAGE_TWO_TICK) {
            blockEntity.updateStage(serverWorld, pos, 2);
        } else if (blockEntity.eatingTicks == STAGE_THREE_TICK) {
            blockEntity.updateStage(serverWorld, pos, 3);
        } else if (blockEntity.eatingTicks >= DIRTY_PLATE_TICK) {
            blockEntity.plateStack = new ItemStack(ModItems.DIRTY_PLATE);
            blockEntity.updateStage(serverWorld, pos, 4);
            blockEntity.eatingTicks = -1;
            blockEntity.markDirty();
        }
    }

    private void updateStage(int stage) {
        if (this.world instanceof ServerWorld serverWorld) {
            updateStage(serverWorld, this.pos, stage);
        }
    }

    private void updateStage(ServerWorld world, BlockPos pos, int stage) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof FoodPlateBlock && state.contains(FoodPlateBlock.STAGE) && state.get(FoodPlateBlock.STAGE) != stage) {
            world.setBlockState(pos, state.with(FoodPlateBlock.STAGE, stage), 3);
        }
        markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (!plateStack.isEmpty()) {
            nbt.put("PlateStack", plateStack.encode(registryLookup));
        }
        nbt.putInt("EatingTicks", this.eatingTicks);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("PlateStack")) {
            this.plateStack = ItemStack.fromNbt(registryLookup, nbt.getCompound("PlateStack")).orElse(ItemStack.EMPTY);
        } else {
            this.plateStack = ItemStack.EMPTY;
        }
        this.eatingTicks = nbt.contains("EatingTicks") ? nbt.getInt("EatingTicks") : -1;
    }
}
