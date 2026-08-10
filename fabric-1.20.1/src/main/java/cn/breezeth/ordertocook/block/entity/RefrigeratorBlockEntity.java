package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.registry.ModSounds;
import cn.breezeth.ordertocook.screen.RefrigeratorScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
    private final DefaultedList<ItemStack> upperInventory = DefaultedList.ofSize(UPPER_SIZE, ItemStack.EMPTY);
    private final DefaultedList<ItemStack> lowerInventory = DefaultedList.ofSize(LOWER_SIZE, ItemStack.EMPTY);
    private final RefrigeratorInventory upperContainer = new RefrigeratorInventory(true, upperInventory);
    private final RefrigeratorInventory lowerContainer = new RefrigeratorInventory(false, lowerInventory);
    private final Set<UUID> upperViewers = new HashSet<>();
    private final Set<UUID> lowerViewers = new HashSet<>();

    private int upperState = 0;
    private int lowerState = 0;
    private int lastUpperState = -1;
    private int lastLowerState = -1;
    private int upperCloseTicks = 0;
    private int lowerCloseTicks = 0;

    public RefrigeratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REFRIGERATOR, pos, state);
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T blockEntity) {
        if (blockEntity instanceof RefrigeratorBlockEntity refrigerator && !world.isClient) {
            refrigerator.tickDoorStates();
        }
    }

    public static boolean isAllowedFoodStorage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (ConfigManager.getCustomMenuNutrition(stack) > 0) {
            return true;
        }
        return stack.isFood();
    }

    public void requestOpen(ServerPlayerEntity player, boolean upper) {
        if (world == null || world.isClient) {
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
            markDirty();
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
        DefaultedList<ItemStack> source = upper ? upperInventory : lowerInventory;
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

    public void dropContents(World world, BlockPos pos) {
        SimpleInventory upper = new SimpleInventory(UPPER_SIZE);
        SimpleInventory lower = new SimpleInventory(LOWER_SIZE);
        for (int i = 0; i < UPPER_SIZE; i++) {
            upper.setStack(i, upperInventory.get(i));
            upperInventory.set(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < LOWER_SIZE; i++) {
            lower.setStack(i, lowerInventory.get(i));
            lowerInventory.set(i, ItemStack.EMPTY);
        }
        ItemScatterer.spawn(world, pos, upper);
        ItemScatterer.spawn(world, pos, lower);
        markDirty();
    }

    private void openMenu(ServerPlayerEntity player, boolean upper) {
        player.openHandledScreen(new RefrigeratorMenuProvider(upper));
    }

    private void playOpenAnimation(boolean upper) {
        if (upper) {
            upperState = 1;
            upperCloseTicks = 0;
            lastUpperState = -1;
            playSound(ModSounds.DISHWASHER_OUT, 0.35f, 2.0f);
        } else {
            lowerState = 1;
            lowerCloseTicks = 0;
            lastLowerState = -1;
            playSound(ModSounds.DISHWASHER_OUT, 0.35f, 1.7f);
        }
        markDirty();
        sync();
    }

    private void playCloseAnimation(boolean upper) {
        if (upper) {
            upperState = 2;
            upperCloseTicks = 20;
            lastUpperState = -1;
            playSound(ModSounds.DISHWASHER_IN, 0.35f, 1.1f);
        } else {
            lowerState = 2;
            lowerCloseTicks = 20;
            lastLowerState = -1;
            playSound(ModSounds.DISHWASHER_IN, 0.35f, 0.9f);
        }
        markDirty();
        sync();
    }

    private void tickDoorStates() {
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
            markDirty();
            sync();
        }
    }

    private void sync() {
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
            Packet<?> packet = toUpdatePacket();
            if (packet != null) {
                for (ServerPlayerEntity player : ((ServerWorld) world).getServer().getPlayerManager().getPlayerList()) {
                    if (player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) < 64.0 * 64.0) {
                        player.networkHandler.sendPacket(packet);
                    }
                }
            }
        }
    }

    private static int countItem(DefaultedList<ItemStack> items, String itemId) {
        int total = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(itemId)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int consumeItem(DefaultedList<ItemStack> items, String itemId, int amount) {
        int consumed = 0;
        for (ItemStack stack : items) {
            if (consumed >= amount) {
                break;
            }
            if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(itemId)) {
                int consume = Math.min(stack.getCount(), amount - consumed);
                stack.decrement(consume);
                consumed += consume;
            }
        }
        return consumed;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "upper_door", 0, state -> {
            if (upperState != lastUpperState) {
                lastUpperState = upperState;
                state.getController().forceAnimationReset();
            }
            if (upperState == 1) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("up_open"));
            }
            if (upperState == 2) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("up_close"));
            }
            return PlayState.STOP;
        }));
        controllers.add(new AnimationController<>(this, "lower_door", 0, state -> {
            if (lowerState != lastLowerState) {
                lastLowerState = lowerState;
                state.getController().forceAnimationReset();
            }
            if (lowerState == 1) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("down_open"));
            }
            if (lowerState == 2) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("down_close"));
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private void playSound(net.minecraft.sound.SoundEvent sound, float volume, float pitch) {
        if (world != null && !world.isClient) {
            world.playSound(null, pos, sound, SoundCategory.BLOCKS, volume, pitch);
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.put("UpperItems", writeSimpleStackList(upperInventory));
        nbt.put("LowerItems", writeSimpleStackList(lowerInventory));
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("UpperItems")) {
            readSimpleStackList(nbt.getList("UpperItems", 10), upperInventory);
        }
        if (nbt.contains("LowerItems")) {
            readSimpleStackList(nbt.getList("LowerItems", 10), lowerInventory);
        }
        if (world != null && world.isClient) {
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
        } else {
            upperState = 0;
            lowerState = 0;
            upperCloseTicks = 0;
            lowerCloseTicks = 0;
            lastUpperState = 0;
            lastLowerState = 0;
        }
    }

    private static NbtList writeSimpleStackList(DefaultedList<ItemStack> items) {
        NbtList list = new NbtList();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                NbtCompound tag = new NbtCompound();
                tag.putByte("Slot", (byte) i);
                tag.putString("id", Registries.ITEM.getId(stack.getItem()).toString());
                tag.putShort("Count", (short) stack.getCount());
                list.add(tag);
            }
        }
        return list;
    }

    private static void readSimpleStackList(NbtList list, DefaultedList<ItemStack> items) {
        items.replaceAll(stack -> ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound tag = list.getCompound(i);
            int slot = tag.getByte("Slot") & 0xFF;
            if (slot < 0 || slot >= items.size()) {
                continue;
            }
            Identifier id = Identifier.tryParse(tag.getString("id"));
            if (id == null) {
                continue;
            }
            var item = Registries.ITEM.get(id);
            int count = tag.getShort("Count") & 0xFFFF;
            if (count <= 0) {
                continue;
            }
            ItemStack stack = new ItemStack(item, 1);
            stack.setCount(count);
            items.set(slot, stack);
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound tag = createNbt();
        tag.putInt("UpperState", upperState);
        tag.putInt("LowerState", lowerState);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    private final class RefrigeratorMenuProvider implements NamedScreenHandlerFactory {
        private final boolean upper;

        private RefrigeratorMenuProvider(boolean upper) {
            this.upper = upper;
        }

        @Override
        public Text getDisplayName() {
            return Text.translatable(upper ? "container.ordertocook.refrigerator_upper" : "container.ordertocook.refrigerator_lower");
        }

        @Nullable
        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return upper
                    ? RefrigeratorScreenHandler.upper(syncId, playerInventory, upperContainer, new RefrigeratorCounts(upperInventory))
                    : RefrigeratorScreenHandler.lower(syncId, playerInventory, lowerContainer, new RefrigeratorCounts(lowerInventory));
        }
    }

    private static final class RefrigeratorCounts implements PropertyDelegate {
        private final DefaultedList<ItemStack> items;

        private RefrigeratorCounts(DefaultedList<ItemStack> items) {
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
        public int size() {
            return items.size();
        }
    }

    private final class RefrigeratorInventory implements Inventory {
        private final boolean upper;
        private final DefaultedList<ItemStack> items;

        private RefrigeratorInventory(boolean upper, DefaultedList<ItemStack> items) {
            this.upper = upper;
            this.items = items;
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
            ItemStack result = net.minecraft.inventory.Inventories.splitStack(items, slot, amount);
            if (!result.isEmpty()) {
                markDirty();
                sync();
            }
            return result;
        }

        @Override
        public ItemStack removeStack(int slot) {
            ItemStack result = net.minecraft.inventory.Inventories.removeStack(items, slot);
            if (!result.isEmpty()) {
                markDirty();
                sync();
            }
            return result;
        }

        @Override
        public void setStack(int slot, ItemStack stack) {
            if (!stack.isEmpty() && !isAllowedFoodStorage(stack)) {
                stack = ItemStack.EMPTY;
            }
            if (!stack.isEmpty() && stack.getCount() > getMaxCountPerStack()) {
                stack.setCount(getMaxCountPerStack());
            }
            items.set(slot, stack);
            markDirty();
            sync();
        }

        @Override
        public void markDirty() {
            RefrigeratorBlockEntity.this.markDirty();
        }

        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            return world != null
                    && !isRemoved()
                    && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }

        @Override
        public void onOpen(PlayerEntity player) {
            if (player.getWorld().isClient) {
                return;
            }
            Set<UUID> viewers = upper ? upperViewers : lowerViewers;
            if (viewers.add(player.getUuid()) && viewers.size() == 1) {
                playOpenAnimation(upper);
            }
        }

        @Override
        public void onClose(PlayerEntity player) {
            if (player.getWorld().isClient) {
                return;
            }
            Set<UUID> viewers = upper ? upperViewers : lowerViewers;
            if (viewers.remove(player.getUuid()) && viewers.isEmpty()) {
                playCloseAnimation(upper);
            }
        }

        @Override
        public boolean isValid(int slot, ItemStack stack) {
            return isAllowedFoodStorage(stack);
        }

        @Override
        public int getMaxCountPerStack() {
            return MAX_STACK_SIZE;
        }

        @Override
        public void clear() {
            items.clear();
            markDirty();
            sync();
        }
    }

}
