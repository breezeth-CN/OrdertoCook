package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.screen.StoveScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class StoveBlockEntity extends BlockEntity implements GeoBlockEntity, Container, MenuProvider {
    public static final int SLOT_COUNT = 7;
    public static final int DATA_COUNT = 4;
    private static final int INPUT_START = 0;
    private static final int FUEL_SLOT = 3;
    private static final int OUTPUT_START = 4;
    private static final int FIRE_TEXTURE_DELAY_TICKS = 15;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final RecipeManager.CachedCheck<Container, SmokingRecipe> quickCheck = RecipeManager.createCheck(RecipeType.SMOKING);
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> litTime;
                case 1 -> litDuration;
                case 2 -> cookProgress;
                case 3 -> cookTotalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> litTime = value;
                case 1 -> litDuration = value;
                case 2 -> cookProgress = value;
                case 3 -> cookTotalTime = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int cookProgress = 0;
    private int cookTotalTime = 200;
    private int litTime = 0;
    private int litDuration = 0;
    // firePhase: 0=off, 1=ignition delay, 2=burst, 3=cycling
    private int firePhase = 0;
    private int fireTicks = 0;
    private int cycleFrame = 0;
    private int fireAnimation = 0;
    private int fireAnimationTicks = 0;
    private int lastFireAnimation = 0;
    private static final int BURST_DELAY = 15;
    private static final int BURST_DURATION = 5;
    private static final int CYCLE_INTERVAL = 8;

    public StoveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STOVE.get(), pos, state);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, StoveBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    /** -1=burst, 0=off, 1-4=cycling stove_fire_{0-3} */
    public int getFireFrame() {
        if (firePhase == 2) return -1; // burst: stove_fire.png
        if (firePhase == 3) return cycleFrame + 1; // cycling: 1-4 → stove_fire_{0-3}.png
        return 0; // off: stove.png
    }

    private void serverTick() {
        if (level == null || level.isClientSide) return;

        boolean changed = false;
        if (fireAnimationTicks > 0) {
            fireAnimationTicks--;
            if (fireAnimationTicks == 0) {
                fireAnimation = 0;
                sync();
            }
        }
        if (isLit()) {
            litTime--;
            fireTicks++;
            if (firePhase == 1 && fireTicks >= BURST_DELAY) {
                // 0.75s: burst phase
                firePhase = 2;
                fireTicks = 0;
                if (level instanceof ServerLevel sw) {
                    sw.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                    Vec3 v = particlePos();
                    sw.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, v.x, v.y, v.z, 12, 0.15, 0.08, 0.05, 0.04);
                    sw.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, v.x, v.y, v.z, 6, 0.12, 0.06, 0.04, 0.02);
                }
                changed = true;
                sync();
            } else if (firePhase == 2 && fireTicks >= BURST_DURATION) {
                // After burst duration: start cycling
                firePhase = 3;
                fireTicks = 0;
                cycleFrame = 0;
                changed = true;
                sync();
            } else if (firePhase == 3) {
                if (fireTicks % CYCLE_INTERVAL == 0 && level instanceof ServerLevel sw) {
                    Vec3 v = particlePos();
                    sw.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, v.x, v.y, v.z, 3, 0.06, 0.02, 0.01, 0.01);
                }
                if (fireTicks >= CYCLE_INTERVAL) {
                    fireTicks = 0;
                    cycleFrame = (cycleFrame + 1) % 4;
                    changed = true;
                    sync();
                }
            }
        } else if (firePhase > 0) {
            firePhase = 0;
            fireTicks = 0;
            cycleFrame = 0;
            changed = true;
            sync();
        }

        boolean hasCookable = false;
        for (int lane = 0; lane < 3; lane++) {
            if (canCook(lane, getRecipe(lane))) {
                hasCookable = true;
                break;
            }
        }

        if (!isLit() && hasCookable) {
            int burnTime = getBurnDuration(items.get(FUEL_SLOT));
            if (burnTime > 0) {
                litTime = burnTime;
                litDuration = burnTime;
                consumeFuel();
                triggerFireAnimation();
                changed = true;
            }
        }

        boolean anyCanCook = false;
        if (isLit()) {
            for (int lane = 0; lane < 3; lane++) {
                if (canCook(lane, getRecipe(lane))) {
                    anyCanCook = true;
                    break;
                }
            }
            if (anyCanCook) {
                cookProgress++;
                changed = true;
                if (cookProgress >= cookTotalTime) {
                    cookProgress = 0;
                    for (int lane = 0; lane < 3; lane++) {
                        SmokingRecipe recipe = getRecipe(lane);
                        if (canCook(lane, recipe)) {
                            cook(lane, recipe);
                        }
                    }
                }
            }
        } else if (cookProgress > 0) {
            cookProgress = Math.max(0, cookProgress - 2);
            changed = true;
        }

        if (changed) {
            setChanged(level, worldPosition, getBlockState());
        }
    }

    private Vec3 particlePos() {
        Direction dir = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.4;
        double cz = worldPosition.getZ() + 0.5;
        return new Vec3(
            cx + dir.getStepX() * 0.375,
            cy,
            cz + dir.getStepZ() * 0.375
        );
    }

    private boolean isLit() {
        return litTime > 0;
    }

    private SmokingRecipe getRecipe(int lane) {
        ItemStack input = items.get(INPUT_START + lane);
        if (input.isEmpty()) {
            return null;
        }
        return quickCheck.getRecipeFor(new SimpleContainer(input), level).orElse(null);
    }

    private boolean canCook(int lane, @Nullable SmokingRecipe recipe) {
        if (recipe == null || items.get(INPUT_START + lane).isEmpty()) {
            return false;
        }
        ItemStack result = recipe.assemble(new SimpleContainer(items.get(INPUT_START + lane)), level.registryAccess());
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = items.get(OUTPUT_START + lane);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameTags(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= Math.min(output.getMaxStackSize(), getMaxStackSize());
    }

    private void cook(int lane, @Nullable SmokingRecipe recipe) {
        if (!canCook(lane, recipe)) {
            return;
        }
        ItemStack result = recipe.assemble(new SimpleContainer(items.get(INPUT_START + lane)), level.registryAccess());
        ItemStack output = items.get(OUTPUT_START + lane);
        if (output.isEmpty()) {
            items.set(OUTPUT_START + lane, result.copy());
        } else {
            output.grow(result.getCount());
        }
        items.get(INPUT_START + lane).shrink(1);
    }

    private int getTotalCookTime(@Nullable SmokingRecipe recipe) {
        return recipe == null ? 200 : recipe.getCookingTime();
    }

    private int getBurnDuration(ItemStack stack) {
        return stack.isEmpty() ? 0 : ForgeHooks.getBurnTime(stack, RecipeType.SMOKING);
    }

    private void consumeFuel() {
        ItemStack fuel = items.get(FUEL_SLOT);
        if (fuel.isEmpty()) {
            return;
        }
        ItemStack remainder = fuel.hasCraftingRemainingItem() ? fuel.getCraftingRemainingItem() : ItemStack.EMPTY;
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            items.set(FUEL_SLOT, remainder);
        }
    }

    private void triggerFireAnimation() {
        fireAnimation++;
        lastFireAnimation = -1;
        fireAnimationTicks = 20;
        firePhase = 1;
        fireTicks = 0;
        cycleFrame = 0;
        sync();
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            var chunk = level.getChunkAt(worldPosition);
            chunk.setBlockEntity(this);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "fire", 0, state -> {
            if (fireAnimation <= 0) {
                return PlayState.STOP;
            }
            if (fireAnimation != lastFireAnimation) {
                lastFireAnimation = fireAnimation;
                state.getController().forceAnimationReset();
            }
            state.setAndContinue(RawAnimation.begin().thenPlay("fire"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack previous = items.get(slot);
        boolean same = !stack.isEmpty() && ItemStack.isSameItemSameTags(previous, stack);
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        if (slot >= INPUT_START && slot < INPUT_START + 3 && !same) {
            cookProgress = 0;
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && !isRemoved()
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= OUTPUT_START) {
            return false;
        }
        if (slot == FUEL_SLOT) {
            return ForgeHooks.getBurnTime(stack, RecipeType.SMOKING) > 0;
        }
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.ordertocook.stove");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new StoveScreenHandler(syncId, playerInventory, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        ContainerHelper.saveAllItems(nbt, items);
        nbt.putInt("BurnTime", litTime);
        nbt.putInt("BurnDuration", litDuration);
        nbt.putInt("CookProgress", cookProgress);
        nbt.putInt("CookTotal", cookTotalTime);
        nbt.putInt("FirePhase", firePhase);
        nbt.putInt("CycleFrame", cycleFrame);
        nbt.putInt("FireTicks", fireTicks);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, items);
        litTime = nbt.getInt("BurnTime");
        litDuration = nbt.getInt("BurnDuration");
        cookProgress = nbt.getInt("CookProgress");
        cookTotalTime = nbt.getInt("CookTotal");
        firePhase = nbt.getInt("FirePhase");
        cycleFrame = nbt.getInt("CycleFrame");
        fireTicks = nbt.getInt("FireTicks");
        if (level != null && level.isClientSide && nbt.contains("FireAnim")) {
            int incomingFireAnimation = nbt.getInt("FireAnim");
            if (incomingFireAnimation != fireAnimation) {
                fireAnimation = incomingFireAnimation;
                fireAnimationTicks = fireAnimation > 0 ? 20 : 0;
                lastFireAnimation = -1;
            }
        } else {
            fireAnimation = 0;
            fireAnimationTicks = 0;
            lastFireAnimation = 0;
        }
        if (litTime > 0 && firePhase > 0) {
            firePhase = 3;
            fireTicks = 0;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = saveWithoutMetadata();
        tag.putInt("FireAnim", fireAnimation);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }
}
