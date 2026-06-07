package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.block.FoodPlateBlock;
import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FoodPlateBlockEntity extends BlockEntity {
    private static final int VARIANT_COUNT = 10;
    private static final int STAGE_ONE_TICK = 15;
    private static final int STAGE_TWO_TICK = 60;
    private static final int STAGE_THREE_TICK = 80;
    private static final int DIRTY_PLATE_TICK = 100;

    private ItemStack plateStack = ItemStack.EMPTY;
    private int eatingTicks = -1;
    private int plateVariant = 1;

    public FoodPlateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOOD_PLATE_DISPLAY.get(), pos, state);
    }

    public ItemStack getPlateStack() {
        return plateStack;
    }

    public void setPlateStack(ItemStack stack) {
        this.plateStack = stack;
        setChanged();
    }

    public void startEatingSequence(ItemStack stack) {
        this.plateStack = stack.copyWithCount(1);
        this.eatingTicks = 0;
        this.plateVariant = randomVariant();
        updateStage(0);
        setChanged();
    }

    public static void tick(Level world, BlockPos pos, BlockState state, FoodPlateBlockEntity blockEntity) {
        if (!(world instanceof ServerLevel serverWorld)) {
            return;
        }
        if (blockEntity.eatingTicks < 0) {
            return;
        }
        blockEntity.eatingTicks++;
        if (blockEntity.eatingTicks == STAGE_ONE_TICK) {
            blockEntity.updateStage(serverWorld, pos, blockEntity.stageForPhase(1));
        } else if (blockEntity.eatingTicks == STAGE_TWO_TICK) {
            blockEntity.updateStage(serverWorld, pos, blockEntity.stageForPhase(2));
        } else if (blockEntity.eatingTicks == STAGE_THREE_TICK) {
            blockEntity.updateStage(serverWorld, pos, blockEntity.stageForPhase(3));
        } else if (blockEntity.eatingTicks >= DIRTY_PLATE_TICK) {
            blockEntity.plateStack = new ItemStack(ModItems.DIRTY_PLATE.get());
            blockEntity.updateStage(serverWorld, pos, blockEntity.stageForPhase(4));
            blockEntity.eatingTicks = -1;
            blockEntity.setChanged();
        }
    }

    private int randomVariant() {
        if (this.level != null) {
            return this.level.random.nextInt(VARIANT_COUNT) + 1;
        }
        return java.util.concurrent.ThreadLocalRandom.current().nextInt(1, VARIANT_COUNT + 1);
    }

    private int stageForPhase(int phase) {
        int variant = Math.max(1, Math.min(VARIANT_COUNT, this.plateVariant));
        int clampedPhase = Math.max(1, Math.min(4, phase));
        return (variant - 1) * 4 + clampedPhase;
    }

    private void updateStage(int stage) {
        if (this.level instanceof ServerLevel serverWorld) {
            updateStage(serverWorld, this.worldPosition, stage);
        }
    }

    private void updateStage(ServerLevel world, BlockPos pos, int stage) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof FoodPlateBlock && state.hasProperty(FoodPlateBlock.STAGE) && state.getValue(FoodPlateBlock.STAGE) != stage) {
            world.setBlock(pos, state.setValue(FoodPlateBlock.STAGE, stage), 3);
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        if (!plateStack.isEmpty()) {
            nbt.put("PlateStack", plateStack.save(registryLookup));
        }
        nbt.putInt("EatingTicks", this.eatingTicks);
        nbt.putInt("PlateVariant", this.plateVariant);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        if (nbt.contains("PlateStack")) {
            this.plateStack = ItemStack.parse(registryLookup, nbt.getCompound("PlateStack")).orElse(ItemStack.EMPTY);
        } else {
            this.plateStack = ItemStack.EMPTY;
        }
        this.eatingTicks = nbt.contains("EatingTicks") ? nbt.getInt("EatingTicks") : -1;
        this.plateVariant = nbt.contains("PlateVariant") ? Math.max(1, Math.min(VARIANT_COUNT, nbt.getInt("PlateVariant"))) : 1;
    }
}
