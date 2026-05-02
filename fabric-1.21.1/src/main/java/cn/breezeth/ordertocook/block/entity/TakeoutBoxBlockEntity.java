package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.screen.TakeoutBoxScreenHandler;
import cn.breezeth.ordertocook.util.ImplementedInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import net.minecraft.screen.ArrayPropertyDelegate;

import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.registry.ModBlocks;
import cn.breezeth.ordertocook.util.DataCompat;
import net.minecraft.entity.ItemEntity;
import net.minecraft.registry.Registries;
import java.util.HashMap;
import java.util.Map;
import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.block.PlateShelfBlock;
import cn.breezeth.ordertocook.block.ShelfBlock;
import cn.breezeth.ordertocook.block.FoodPlateBlock;
import cn.breezeth.ordertocook.block.TakeoutBagBlock;
import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.registry.ModSounds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.state.property.Properties;
import net.minecraft.sound.SoundCategory;

public class TakeoutBoxBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {
    // Slot 0: Input Order
    // Slot 1-12: Input Items
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(13, ItemStack.EMPTY);

    protected final PropertyDelegate propertyDelegate;

    public TakeoutBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COUNTERTOP, pos, state);
        this.propertyDelegate = new ArrayPropertyDelegate(2);
    }

    public void tryPackOrder(PlayerEntity player) {
        ItemStack orderStack = inventory.get(0);
        if (orderStack.isEmpty() || !orderStack.isOf(ModItems.ORDER)) return;

        NbtCompound nbt = DataCompat.copy(orderStack);
        if (nbt == null) return;

        // Check expiry
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
            long expiryTick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
            if (world == null || world.getTime() >= expiryTick) return;
        } else if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
            long expiryTime = nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
            if (System.currentTimeMillis() >= expiryTime) return;
        }

        NbtCompound foodList = getFoodListIfSatisfied(nbt);
        if (foodList == null) return;

        if (ConfigManager.get().countertopConsumeLeather) {
            if (!tryConsumeOnePaper()) {
                if (player instanceof ServerPlayerEntity sp) {
                    sp.closeHandledScreen();
                    sp.sendMessage(Text.translatable("message.ordertocook.countertop.no_packaging").formatted(Formatting.RED), true);
                }
                return;
            }
        }

        // Consume items
        for (String key : foodList.getKeys()) {
            int required = foodList.getInt(key);
            for (int i = 1; i < 13; i++) {
                if (required <= 0) break;
                ItemStack stack = inventory.get(i);
                if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(key)) {
                    int consume = Math.min(stack.getCount(), required);
                    stack.decrement(consume);
                    required -= consume;
                }
            }
        }

        // Create Takeout Bag
        ItemStack bag = new ItemStack(ModItems.TAKEOUT_BAG);
        NbtCompound bagNbt = new NbtCompound();

        var customName = orderStack.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
        if (customName != null) {
            bag.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, customName);
        }
        
        // Transfer Prestige
        if (nbt.contains("Prestige")) {
            bagNbt.putInt("Prestige", nbt.getInt("Prestige"));
        }
        if (nbt.contains(ModConstants.NBT_ORDER_ID)) {
            bagNbt.putString(ModConstants.NBT_ORDER_ID, nbt.getString(ModConstants.NBT_ORDER_ID));
        }
        if (nbt.contains(ModConstants.NBT_CUSTOMER_NAME)) {
            bagNbt.putString(ModConstants.NBT_CUSTOMER_NAME, nbt.getString(ModConstants.NBT_CUSTOMER_NAME));
        }
        if (nbt.contains(ModConstants.NBT_TYPE)) {
            bagNbt.putInt(ModConstants.NBT_TYPE, nbt.getInt(ModConstants.NBT_TYPE));
        }
        if (nbt.contains(ModConstants.NBT_DELIVERY)) {
            bagNbt.putBoolean(ModConstants.NBT_DELIVERY, nbt.getBoolean(ModConstants.NBT_DELIVERY));
        }
        if (nbt.contains(ModConstants.NBT_URGENT)) {
            bagNbt.putBoolean(ModConstants.NBT_URGENT, nbt.getBoolean(ModConstants.NBT_URGENT));
        }
        if (nbt.contains(ModConstants.NBT_IS_LONG_DISTANCE)) {
            bagNbt.putBoolean(ModConstants.NBT_IS_LONG_DISTANCE, nbt.getBoolean(ModConstants.NBT_IS_LONG_DISTANCE));
        }
        // Transfer Delivery Info
        if (nbt.contains("delivery_pos")) {
            bagNbt.put("delivery_pos", nbt.getCompound("delivery_pos"));
        }
        // Transfer Machine Identity (for ranking attribution)
        if (nbt.contains(cn.breezeth.ordertocook.core.ModConstants.NBT_MACHINE_ID)) {
            bagNbt.putInt(cn.breezeth.ordertocook.core.ModConstants.NBT_MACHINE_ID, nbt.getInt(cn.breezeth.ordertocook.core.ModConstants.NBT_MACHINE_ID));
        }
        // Transfer optional machine pos/dim (compat, not used for ranking)
        if (nbt.contains(cn.breezeth.ordertocook.core.ModConstants.NBT_MACHINE_POS)) {
            bagNbt.putLong(cn.breezeth.ordertocook.core.ModConstants.NBT_MACHINE_POS, nbt.getLong(cn.breezeth.ordertocook.core.ModConstants.NBT_MACHINE_POS));
        }
        if (nbt.contains(cn.breezeth.ordertocook.core.ModConstants.NBT_MACHINE_DIM)) {
            bagNbt.putString(cn.breezeth.ordertocook.core.ModConstants.NBT_MACHINE_DIM, nbt.getString(cn.breezeth.ordertocook.core.ModConstants.NBT_MACHINE_DIM));
        }
        // Transfer Delivery Distance (precomputed at accept time)
        if (nbt.contains(cn.breezeth.ordertocook.core.ModConstants.NBT_DELIVERY_DIST)) {
            bagNbt.putInt(cn.breezeth.ordertocook.core.ModConstants.NBT_DELIVERY_DIST, nbt.getInt(cn.breezeth.ordertocook.core.ModConstants.NBT_DELIVERY_DIST));
        }
        // Replace Order_Type by delivery distance if present, otherwise preserve existing Order_Type (walkin=1) or default 0
        int orderType = nbt.contains(cn.breezeth.ordertocook.core.ModConstants.NBT_ORDER_TYPE) ? nbt.getInt(cn.breezeth.ordertocook.core.ModConstants.NBT_ORDER_TYPE) : 0;
        int deliveryDist = nbt.contains(cn.breezeth.ordertocook.core.ModConstants.NBT_DELIVERY_DIST) ? nbt.getInt(cn.breezeth.ordertocook.core.ModConstants.NBT_DELIVERY_DIST) : 0;
        bagNbt.putInt(cn.breezeth.ordertocook.core.ModConstants.NBT_ORDER_TYPE, deliveryDist > 0 ? deliveryDist : orderType);
        // Transfer Expiry Time
        if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
            bagNbt.putLong(ModConstants.NBT_EXPIRY_TIME, nbt.getLong(ModConstants.NBT_EXPIRY_TIME));
        }
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
            bagNbt.putLong(ModConstants.NBT_EXPIRY_TICK, nbt.getLong(ModConstants.NBT_EXPIRY_TICK));
        }
        
        DataCompat.set(bag, bagNbt);
        
        // Consume Order
        inventory.set(0, ItemStack.EMPTY);
        
        // Place Bag above box
        if (world != null && !world.isClient) {
            BlockPos bagPos = pos.up();
            if (world.getBlockState(bagPos).isAir()) {
                Direction facing = Direction.NORTH;
                BlockState selfState = getCachedState();
                if (selfState.contains(Properties.HORIZONTAL_FACING)) {
                    facing = selfState.get(Properties.HORIZONTAL_FACING);
                }
                world.setBlockState(bagPos, ModBlocks.TAKEOUT_BAG.getDefaultState().with(TakeoutBagBlock.FACING, facing), Block.NOTIFY_ALL);
                if (world.getBlockEntity(bagPos) instanceof TakeoutBagBlockEntity bagBe) {
                    bagBe.setBagStack(bag.copy());
                }
            } else {
                ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, bag);
                entity.setVelocity(0, 0.2, 0);
                world.spawnEntity(entity);
            }
        }
        
        refreshAvailableCounts();
        markDirty();
    }

    public void tryPlateOrder(PlayerEntity player) {
        ItemStack orderStack = inventory.get(0);
        if (orderStack.isEmpty() || !orderStack.isOf(ModItems.ORDER) || world == null || world.isClient) return;

        if (isOutputBlocked()) {
            if (player instanceof ServerPlayerEntity sp) {
                sp.closeHandledScreen();
                sp.sendMessage(Text.translatable("message.ordertocook.countertop_blocked").formatted(Formatting.RED), true);
            }
            return;
        }

        NbtCompound nbt = DataCompat.copy(orderStack);
        if (nbt == null) return;
        if (isExpired(nbt)) return;

        if (nbt.getBoolean(ModConstants.NBT_DELIVERY)) {
            if (player instanceof ServerPlayerEntity sp) {
                sp.closeHandledScreen();
                sp.sendMessage(Text.translatable("message.ordertocook.countertop.plating_delivery_forbidden").formatted(Formatting.RED), true);
            }
            return;
        }

        NbtCompound foodList = getFoodListIfSatisfied(nbt);
        if (foodList == null) {
            if (player instanceof ServerPlayerEntity sp) {
                sp.closeHandledScreen();
                sp.sendMessage(Text.translatable("message.ordertocook.countertop.missing_food").formatted(Formatting.RED), true);
            }
            return;
        }

        if (!tryConsumeOnePlate()) {
            if (player instanceof ServerPlayerEntity sp) {
                sp.closeHandledScreen();
                sp.sendMessage(Text.translatable("message.ordertocook.countertop.no_clean_plates").formatted(Formatting.RED), true);
            }
            return;
        }

        consumeFoodList(foodList);

        ItemStack plated = new ItemStack(ModItems.FOOD_PLATE);
        NbtCompound platedNbt = nbt.copy();
        platedNbt.putBoolean("Plated", true);
        platedNbt.putString("Carrier", "plate");
        DataCompat.set(plated, platedNbt);

        var customName = orderStack.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
        if (customName != null) {
            plated.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, customName);
        }

        inventory.set(0, ItemStack.EMPTY);
        BlockPos platePos = pos.up();
        Direction facing = Direction.NORTH;
        BlockState selfState = getCachedState();
        if (selfState.contains(Properties.HORIZONTAL_FACING)) {
            facing = selfState.get(Properties.HORIZONTAL_FACING);
        }
        world.setBlockState(platePos, ModBlocks.FOOD_PLATE_DISPLAY.getDefaultState().with(FoodPlateBlock.FACING, facing), Block.NOTIFY_ALL);
        if (world.getBlockEntity(platePos) instanceof FoodPlateBlockEntity foodPlateBe) {
            foodPlateBe.setPlateStack(plated.copy());
        }
        world.playSound(null, pos, ModSounds.FOOD_PLATE_PLACE, SoundCategory.BLOCKS, 0.9f, 1.0f);

        refreshAvailableCounts();
        markDirty();
    }

    private boolean isOutputBlocked() {
        if (this.world == null) return true;
        BlockPos outputPos = this.pos.up();
        if (!this.world.getBlockState(outputPos).isAir()) {
            return true;
        }
        Box itemBox = new Box(
                outputPos.getX() + 0.2, outputPos.getY(), outputPos.getZ() + 0.2,
                outputPos.getX() + 0.8, outputPos.getY() + 1.0, outputPos.getZ() + 0.8
        );
        return !this.world.getEntitiesByClass(ItemEntity.class, itemBox, entity -> entity.isAlive() && !entity.getStack().isEmpty()).isEmpty();
    }

    private boolean isExpired(NbtCompound nbt) {
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
            long expiryTick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
            return world == null || world.getTime() >= expiryTick;
        }
        if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
            return System.currentTimeMillis() >= nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
        }
        return false;
    }

    private NbtCompound getFoodListIfSatisfied(NbtCompound nbt) {
        if (!nbt.contains("FoodList")) return null;
        NbtCompound foodList = nbt.getCompound("FoodList");
        Map<String, Integer> availableItems = new HashMap<>();
        for (int i = 1; i < 13; i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty()) {
                String id = Registries.ITEM.getId(stack.getItem()).toString();
                availableItems.put(id, availableItems.getOrDefault(id, 0) + stack.getCount());
            }
        }
        for (String key : foodList.getKeys()) {
            if (availableItems.getOrDefault(key, 0) < foodList.getInt(key)) {
                return null;
            }
        }
        return foodList;
    }

    private void consumeFoodList(NbtCompound foodList) {
        for (String key : foodList.getKeys()) {
            int required = foodList.getInt(key);
            for (int i = 1; i < 13 && required > 0; i++) {
                ItemStack stack = inventory.get(i);
                if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(key)) {
                    int consume = Math.min(stack.getCount(), required);
                    stack.decrement(consume);
                    required -= consume;
                }
            }
        }
    }

    private boolean tryConsumeOnePaper() {
        if (this.world == null || this.world.isClient) return false;
        BlockPos best = null;
        int bestDist = Integer.MAX_VALUE;

        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dy = -5; dy <= 5; dy++) {
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    m.set(this.pos.getX() + dx, this.pos.getY() + dy, this.pos.getZ() + dz);
                    BlockState state = this.world.getBlockState(m);
                    if (!(state.getBlock() instanceof ShelfBlock)) continue;
                    int papers = state.get(ShelfBlock.PAPERS);
                    if (papers <= 0) continue;
                    int dist = dx * dx + dy * dy + dz * dz;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = m.toImmutable();
                    }
                }
            }
        }
        if (best == null) return false;
        BlockState state = this.world.getBlockState(best);
        if (!(state.getBlock() instanceof ShelfBlock)) return false;
        int papers = state.get(ShelfBlock.PAPERS);
        if (papers <= 0) return false;
        this.world.setBlockState(best, state.with(ShelfBlock.PAPERS, papers - 1), Block.NOTIFY_ALL);
        return true;
    }

    private boolean tryConsumeOnePlate() {
        if (this.world == null || this.world.isClient) return false;
        BlockPos best = null;
        int bestDist = Integer.MAX_VALUE;
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dy = -5; dy <= 5; dy++) {
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    m.set(this.pos.getX() + dx, this.pos.getY() + dy, this.pos.getZ() + dz);
                    BlockState state = this.world.getBlockState(m);
                    if (!(state.getBlock() instanceof PlateShelfBlock)) continue;
                    int plates = state.get(PlateShelfBlock.PLATES);
                    if (plates <= 0) continue;
                    int dist = dx * dx + dy * dy + dz * dz;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = m.toImmutable();
                    }
                }
            }
        }
        if (best == null) return false;
        BlockState state = this.world.getBlockState(best);
        if (!(state.getBlock() instanceof PlateShelfBlock)) return false;
        int plates = state.get(PlateShelfBlock.PLATES);
        if (plates <= 0) return false;
        this.world.setBlockState(best, state.with(PlateShelfBlock.PLATES, plates - 1), Block.NOTIFY_ALL);
        return true;
    }

    public int countAvailablePapers() {
        if (this.world == null) return 0;
        int total = 0;
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dy = -5; dy <= 5; dy++) {
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    m.set(this.pos.getX() + dx, this.pos.getY() + dy, this.pos.getZ() + dz);
                    BlockState state = this.world.getBlockState(m);
                    if (!(state.getBlock() instanceof ShelfBlock)) continue;
                    total += state.get(ShelfBlock.PAPERS);
                }
            }
        }
        return total;
    }

    public int countAvailablePlates() {
        if (this.world == null) return 0;
        int total = 0;
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dy = -5; dy <= 5; dy++) {
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    m.set(this.pos.getX() + dx, this.pos.getY() + dy, this.pos.getZ() + dz);
                    BlockState state = this.world.getBlockState(m);
                    if (!(state.getBlock() instanceof PlateShelfBlock)) continue;
                    total += state.get(PlateShelfBlock.PLATES);
                }
            }
        }
        return total;
    }

    public void refreshAvailableCounts() {
        if (this.world == null || this.world.isClient) return;
        this.propertyDelegate.set(0, countAvailablePapers());
        this.propertyDelegate.set(1, countAvailablePlates());
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.ordertocook.countertop");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        if (!playerInventory.player.getWorld().isClient) {
            refreshAvailableCounts();
        }
        return new TakeoutBoxScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
    }
}
