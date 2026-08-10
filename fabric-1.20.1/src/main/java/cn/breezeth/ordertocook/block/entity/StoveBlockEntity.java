package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.screen.StoveScreenHandler;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmokingRecipe;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class StoveBlockEntity extends BlockEntity implements GeoBlockEntity, Inventory, NamedScreenHandlerFactory {
    public static final int SLOT_COUNT = 7;
    public static final int DATA_COUNT = 4;
    private static final int INPUT_START = 0;
    private static final int FUEL_SLOT = 3;
    private static final int OUTPUT_START = 4;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final RecipeManager.MatchGetter<Inventory, SmokingRecipe> quickCheck =
            RecipeManager.createCachedMatchGetter(RecipeType.SMOKING);
    private final PropertyDelegate dataAccess = new PropertyDelegate() {
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
        public int size() {
            return DATA_COUNT;
        }
    };

    private DefaultedList<ItemStack> items = DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY);
    private int cookProgress = 0;
    private int cookTotalTime = 200;
    private int litTime = 0;
    private int litDuration = 0;
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
        super(ModBlockEntities.STOVE, pos, state);
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T blockEntity) {
        if (blockEntity instanceof StoveBlockEntity stove && !world.isClient) {
            stove.serverTick();
        }
    }

    public int getFireFrame() {
        if (firePhase == 2) {
            return -1;
        }
        if (firePhase == 3) {
            return cycleFrame + 1;
        }
        return 0;
    }

    private void serverTick() {
        if (world == null || world.isClient) {
            return;
        }
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
                firePhase = 2;
                fireTicks = 0;
                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    Vec3d v = particlePos();
                    serverWorld.spawnParticles(ParticleTypes.FLAME, v.x, v.y, v.z, 12, 0.15, 0.08, 0.05, 0.04);
                    serverWorld.spawnParticles(ParticleTypes.SMOKE, v.x, v.y, v.z, 6, 0.12, 0.06, 0.04, 0.02);
                }
                changed = true;
                sync();
            } else if (firePhase == 2 && fireTicks >= BURST_DURATION) {
                firePhase = 3;
                fireTicks = 0;
                cycleFrame = 0;
                changed = true;
                sync();
            } else if (firePhase == 3) {
                if (fireTicks % CYCLE_INTERVAL == 0 && world instanceof ServerWorld serverWorld) {
                    Vec3d v = particlePos();
                    serverWorld.spawnParticles(ParticleTypes.FLAME, v.x, v.y, v.z, 3, 0.06, 0.02, 0.01, 0.01);
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
            markDirty();
        }
    }

    private Vec3d particlePos() {
        Direction dir = getCachedState().contains(net.minecraft.state.property.Properties.HORIZONTAL_FACING)
                ? getCachedState().get(net.minecraft.state.property.Properties.HORIZONTAL_FACING)
                : Direction.NORTH;
        return new Vec3d(
                pos.getX() + 0.5 + dir.getOffsetX() * 0.375,
                pos.getY() + 0.4,
                pos.getZ() + 0.5 + dir.getOffsetZ() * 0.375
        );
    }

    private boolean isLit() {
        return litTime > 0;
    }

    @Nullable
    private SmokingRecipe getRecipe(int lane) {
        ItemStack input = items.get(INPUT_START + lane);
        if (input.isEmpty() || world == null) {
            return null;
        }
        return quickCheck.getFirstMatch(new SimpleInventory(input), world).orElse(null);
    }

    private boolean canCook(int lane, @Nullable SmokingRecipe recipe) {
        if (recipe == null || items.get(INPUT_START + lane).isEmpty() || world == null) {
            return false;
        }
        ItemStack result = recipe.craft(new SimpleInventory(items.get(INPUT_START + lane)), world.getRegistryManager());
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = items.get(OUTPUT_START + lane);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.canCombine(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= Math.min(output.getMaxCount(), getMaxCountPerStack());
    }

    private void cook(int lane, @Nullable SmokingRecipe recipe) {
        if (!canCook(lane, recipe) || world == null) {
            return;
        }
        ItemStack result = recipe.craft(new SimpleInventory(items.get(INPUT_START + lane)), world.getRegistryManager());
        ItemStack output = items.get(OUTPUT_START + lane);
        if (output.isEmpty()) {
            items.set(OUTPUT_START + lane, result.copy());
        } else {
            output.increment(result.getCount());
        }
        items.get(INPUT_START + lane).decrement(1);
    }

    private int getBurnDuration(ItemStack stack) {
        Integer burnTime = FuelRegistry.INSTANCE.get(stack.getItem());
        return burnTime == null ? 0 : burnTime;
    }

    private void consumeFuel() {
        ItemStack fuel = items.get(FUEL_SLOT);
        if (fuel.isEmpty()) {
            return;
        }
        ItemStack remainder = fuel.getRecipeRemainder();
        fuel.decrement(1);
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
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
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
    public int size() {
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
    public ItemStack getStack(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(items, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        ItemStack previous = items.get(slot);
        boolean same = !stack.isEmpty() && ItemStack.canCombine(previous, stack);
        items.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        if (slot >= INPUT_START && slot < INPUT_START + 3 && !same) {
            cookProgress = 0;
        }
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return world != null && !isRemoved() && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (slot >= OUTPUT_START) {
            return false;
        }
        if (slot == FUEL_SLOT) {
            return getBurnDuration(stack) > 0;
        }
        return true;
    }

    @Override
    public void clear() {
        items.clear();
        markDirty();
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.ordertocook.stove");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new StoveScreenHandler(syncId, playerInventory, this, dataAccess);
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putInt("BurnTime", litTime);
        nbt.putInt("BurnDuration", litDuration);
        nbt.putInt("CookProgress", cookProgress);
        nbt.putInt("CookTotal", cookTotalTime);
        nbt.putInt("FirePhase", firePhase);
        nbt.putInt("CycleFrame", cycleFrame);
        nbt.putInt("FireTicks", fireTicks);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        items = DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY);
        Inventories.readNbt(nbt, items);
        litTime = nbt.getInt("BurnTime");
        litDuration = nbt.getInt("BurnDuration");
        cookProgress = nbt.getInt("CookProgress");
        cookTotalTime = nbt.getInt("CookTotal");
        firePhase = nbt.getInt("FirePhase");
        cycleFrame = nbt.getInt("CycleFrame");
        fireTicks = nbt.getInt("FireTicks");
        if (world != null && world.isClient && nbt.contains("FireAnim")) {
            int incoming = nbt.getInt("FireAnim");
            if (incoming != fireAnimation) {
                fireAnimation = incoming;
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
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound tag = createNbt();
        tag.putInt("FireAnim", fireAnimation);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
