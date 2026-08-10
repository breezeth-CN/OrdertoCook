package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.screen.RefrigeratorScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class RefrigeratorBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final int UPPER_SIZE = 27;
    private static final int LOWER_SIZE = 54;
    private static final int MAX_STACK_SIZE = 128;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final NonNullList<ItemStack> upperInventory = NonNullList.withSize(UPPER_SIZE, ItemStack.EMPTY);
    private final NonNullList<ItemStack> lowerInventory = NonNullList.withSize(LOWER_SIZE, ItemStack.EMPTY);
    private final RefrigeratorContainer upperContainer = new RefrigeratorContainer(true, upperInventory);
    private final RefrigeratorContainer lowerContainer = new RefrigeratorContainer(false, lowerInventory);
    private final Set<UUID> upperViewers = new HashSet<>();
    private final Set<UUID> lowerViewers = new HashSet<>();

    // Door state machine: 0=closed, 1=open, 2=closing. Only inventory persists; door animation state is runtime-only.
    private int upperState = 0;
    private int lowerState = 0;
    private int lastUpperState = -1;
    private int lastLowerState = -1;
    private int upperCloseTicks = 0;
    private int lowerCloseTicks = 0;

    public RefrigeratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REFRIGERATOR.get(), pos, state);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, RefrigeratorBlockEntity blockEntity) {
        blockEntity.tickDoorStates();
    }

    public static boolean isAllowedFoodStorage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (ConfigManager.getCustomMenuNutrition(stack) > 0) {
            return true;
        }
        return stack.isEdible();
    }

    public void requestOpen(ServerPlayer player, boolean upper) {
        if (player == null || level == null || level.isClientSide) {
            return;
        }
        openMenu(player, upper);
    }

    public int countItem(String itemId) {
        return countItem(upperInventory, itemId) + countItem(lowerInventory, itemId);
    }

    public int consumeItem(String itemId, int amount) {
        if (amount <= 0) {
            return 0;
        }
        int consumed = consumeItem(upperInventory, itemId, amount);
        consumed += consumeItem(lowerInventory, itemId, amount - consumed);
        if (consumed > 0) {
            setChanged();
            sync();
        }
        return consumed;
    }

    public boolean isUpperOpenForDisplay() {
        return upperState != 0;
    }

    public boolean isLowerOpenForDisplay() {
        return lowerState != 0;
    }

        public java.util.List<ItemStack> getDisplayItems(boolean upper, int limit) {
        NonNullList<ItemStack> source = upper ? upperInventory : lowerInventory;
        java.util.List<ItemStack> result = new java.util.ArrayList<>(limit);
        for (ItemStack stack : source) {
            if (!stack.isEmpty()) {
                result.add(stack.copyWithCount(Math.min(stack.getCount(), 1)));
                if (result.size() >= limit) break;
            }
        }
        return result;
    }

    public int getOccupiedSlotCount(boolean upper) {
        int occupied = 0;
        for (ItemStack stack : upper ? upperInventory : lowerInventory) {
            if (!stack.isEmpty()) occupied++;
        }
        return occupied;
    }

    public int getStorageSlotCount(boolean upper) {
        return upper ? UPPER_SIZE : LOWER_SIZE;
    }

    public void dropContents(Level world, BlockPos pos) {
        SimpleContainer upper = new SimpleContainer(UPPER_SIZE);
        SimpleContainer lower = new SimpleContainer(LOWER_SIZE);
        for (int i = 0; i < UPPER_SIZE; i++) {
            upper.setItem(i, upperInventory.get(i));
            upperInventory.set(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < LOWER_SIZE; i++) {
            lower.setItem(i, lowerInventory.get(i));
            lowerInventory.set(i, ItemStack.EMPTY);
        }
        Containers.dropContents(world, pos, upper);
        Containers.dropContents(world, pos, lower);
        setChanged();
    }

    private void openMenu(ServerPlayer player, boolean upper) {
        player.openMenu(new RefrigeratorMenuProvider(upper));
    }

    private void playOpenAnimation(boolean upper) {
        if (upper) {
            upperState = 1;
            upperCloseTicks = 0;
            lastUpperState = -1;
            playSound(cn.breezeth.ordertocook.registry.ModSounds.DISHWASHER_OUT.get(), 0.35f, 2.0f);
        } else {
            lowerState = 1;
            lowerCloseTicks = 0;
            lastLowerState = -1;
            playSound(cn.breezeth.ordertocook.registry.ModSounds.DISHWASHER_OUT.get(), 0.35f, 1.7f);
        }
        setChanged();
        sync();
    }

    private void playCloseAnimation(boolean upper) {
        if (upper) {
            upperState = 2;
            upperCloseTicks = 20;
            lastUpperState = -1;
            playSound(cn.breezeth.ordertocook.registry.ModSounds.DISHWASHER_IN.get(), 0.35f, 1.1f);
        } else {
            lowerState = 2;
            lowerCloseTicks = 20;
            lastLowerState = -1;
            playSound(cn.breezeth.ordertocook.registry.ModSounds.DISHWASHER_IN.get(), 0.35f, 0.9f);
        }
        setChanged();
        sync();
    }

    private void tickDoorStates() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean changed = false;
        if (upperState == 2 && upperCloseTicks > 0 && --upperCloseTicks <= 0) {
            upperState = 0;
            lastUpperState = -1;
            changed = true;
        }
        if (lowerState == 2 && lowerCloseTicks > 0 && --lowerCloseTicks <= 0) {
            lowerState = 0;
            lastLowerState = -1;
            changed = true;
        }
        if (changed) {
            setChanged();
            sync();
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            var chunk = level.getChunkAt(worldPosition);
            chunk.setBlockEntity(this);
            Packet<?> packet = getUpdatePacket();
            if (packet != null) {
                for (ServerPlayer player : ((ServerLevel) level).getServer().getPlayerList().getPlayers()) {
                    if (player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 64.0 * 64.0) {
                        player.connection.send(packet);
                    }
                }
            }
        }
    }

    private static int countItem(NonNullList<ItemStack> items, String itemId) {
        int total = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int consumeItem(NonNullList<ItemStack> items, String itemId, int amount) {
        int consumed = 0;
        for (ItemStack stack : items) {
            if (consumed >= amount) {
                break;
            }
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) {
                int consume = Math.min(stack.getCount(), amount - consumed);
                stack.shrink(consume);
                consumed += consume;
            }
        }
        return consumed;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "upper_door", 0, state -> {
            if (upperState != lastUpperState) { lastUpperState = upperState; state.getController().forceAnimationReset(); }
            if (upperState == 1) return state.setAndContinue(RawAnimation.begin().thenPlay("up_open"));
            if (upperState == 2) return state.setAndContinue(RawAnimation.begin().thenPlay("up_close"));
            return PlayState.STOP;
        }));
        controllers.add(new AnimationController<>(this, "lower_door", 0, state -> {
            if (lowerState != lastLowerState) { lastLowerState = lowerState; state.getController().forceAnimationReset(); }
            if (lowerState == 1) return state.setAndContinue(RawAnimation.begin().thenPlay("down_open"));
            if (lowerState == 2) return state.setAndContinue(RawAnimation.begin().thenPlay("down_close"));
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private static int stringToState(String anim) {
        if (anim == null || anim.isEmpty()) return 0;
        if (anim.contains("open")) return 1;
        if (anim.contains("close")) return 2;
        return 0;
    }

    private void playSound(SoundEvent sound, float volume, float pitch) {
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("UpperItems", writeSimpleStackList(upperInventory));
        nbt.put("LowerItems", writeSimpleStackList(lowerInventory));
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("UpperItems")) {
            readSimpleStackList(nbt.getList("UpperItems", net.minecraft.nbt.Tag.TAG_COMPOUND), upperInventory);
        }
        if (nbt.contains("LowerItems")) {
            readSimpleStackList(nbt.getList("LowerItems", net.minecraft.nbt.Tag.TAG_COMPOUND), lowerInventory);
        }
        if (level != null && level.isClientSide) {
            if (nbt.contains("UpperState")) {
                int incoming = nbt.getInt("UpperState");
                if (upperState != incoming) {
                    upperState = incoming;
                    lastUpperState = -1;
                }
            }
            if (nbt.contains("LowerState")) {
                int incoming = nbt.getInt("LowerState");
                if (lowerState != incoming) {
                    lowerState = incoming;
                    lastLowerState = -1;
                }
            }
            // Legacy migration from old animation string format (one-time)
            if (nbt.contains("UpperAnim")) { upperState = stringToState(nbt.getString("UpperAnim")); lastUpperState = -1; }
            if (nbt.contains("LowerAnim")) { lowerState = stringToState(nbt.getString("LowerAnim")); lastLowerState = -1; }
        } else {
            upperState = 0;
            lowerState = 0;
            upperCloseTicks = 0;
            lowerCloseTicks = 0;
            lastUpperState = 0;
            lastLowerState = 0;
        }
    }

    private static net.minecraft.nbt.ListTag writeSimpleStackList(net.minecraft.core.NonNullList<ItemStack> items) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                CompoundTag tag = new CompoundTag();
                tag.putByte("Slot", (byte) i);
                tag.putString("id", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                tag.putShort("Count", (short) stack.getCount());
                list.add(tag);
            }
        }
        return list;
    }

    private static void readSimpleStackList(net.minecraft.nbt.ListTag list, net.minecraft.core.NonNullList<ItemStack> items) {
        items.replaceAll(stack -> ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            int slot = tag.getByte("Slot") & 0xFF;
            if (slot >= 0 && slot < items.size()) {
                ResourceLocation id = new ResourceLocation(tag.getString("id"));
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
                if (item == null) continue;
                int count = tag.getShort("Count") & 0xFFFF;
                if (count <= 0) continue;
                ItemStack stack = new ItemStack(item, 1);
                stack.setCount(count);
                items.set(slot, stack);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = saveWithoutMetadata();
        tag.putInt("UpperState", upperState);
        tag.putInt("LowerState", lowerState);
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

    private final class RefrigeratorMenuProvider implements MenuProvider {
        private final boolean upper;

        private RefrigeratorMenuProvider(boolean upper) {
            this.upper = upper;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable(upper ? "container.ordertocook.refrigerator_upper" : "container.ordertocook.refrigerator_lower");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
            return upper
                    ? RefrigeratorScreenHandler.upper(syncId, playerInventory, upperContainer, new RefrigeratorCounts(upperInventory))
                    : RefrigeratorScreenHandler.lower(syncId, playerInventory, lowerContainer, new RefrigeratorCounts(lowerInventory));
        }
    }

    private static final class RefrigeratorCounts implements ContainerData {
        private final NonNullList<ItemStack> items;

        private RefrigeratorCounts(NonNullList<ItemStack> items) {
            this.items = items;
        }

        @Override
        public int get(int index) {
            return index >= 0 && index < items.size() ? items.get(index).getCount() : 0;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return items.size();
        }
    }

    private final class RefrigeratorContainer implements Container {
        private final boolean upper;
        private final NonNullList<ItemStack> items;

        private RefrigeratorContainer(boolean upper, NonNullList<ItemStack> items) {
            this.upper = upper;
            this.items = items;
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
                sync();
            }
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(items, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (!stack.isEmpty() && !isAllowedFoodStorage(stack)) {
                stack = ItemStack.EMPTY;
            }
            if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
                stack.setCount(getMaxStackSize());
            }
            items.set(slot, stack);
            setChanged();
            sync();
        }

        @Override
        public void setChanged() {
            RefrigeratorBlockEntity.this.setChanged();
            RefrigeratorBlockEntity.this.sync();
        }

        @Override
        public boolean stillValid(Player player) {
            return RefrigeratorBlockEntity.this.level != null
                    && !RefrigeratorBlockEntity.this.isRemoved()
                    && player.distanceToSqr(
                    RefrigeratorBlockEntity.this.worldPosition.getX() + 0.5,
                    RefrigeratorBlockEntity.this.worldPosition.getY() + 0.5,
                    RefrigeratorBlockEntity.this.worldPosition.getZ() + 0.5) <= 64.0;
        }

        @Override
        public void startOpen(Player player) {
            if (player.level().isClientSide) {
                return;
            }
            Set<UUID> viewers = upper ? upperViewers : lowerViewers;
            if (viewers.add(player.getUUID()) && viewers.size() == 1) {
                playOpenAnimation(upper);
            }
        }

        @Override
        public void stopOpen(Player player) {
            if (player.level().isClientSide) {
                return;
            }
            Set<UUID> viewers = upper ? upperViewers : lowerViewers;
            if (viewers.remove(player.getUUID()) && viewers.isEmpty()) {
                playCloseAnimation(upper);
            }
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return isAllowedFoodStorage(stack);
        }

        @Override
        public int getMaxStackSize() {
            return MAX_STACK_SIZE;
        }

        @Override
        public void clearContent() {
            items.clear();
            setChanged();
        }
    }

}
