package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.screen.TakeoutBoxScreenHandler;
import cn.breezeth.ordertocook.util.ImplementedInventory;
import org.jetbrains.annotations.Nullable;
import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.registry.ModBlocks;
import cn.breezeth.ordertocook.util.DataCompat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.block.PlateShelfBlock;
import cn.breezeth.ordertocook.block.ShelfBlock;
import cn.breezeth.ordertocook.block.FoodPlateBlock;
import cn.breezeth.ordertocook.block.TakeoutBagBlock;
import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.registry.ModSounds;

public class TakeoutBoxBlockEntity extends BlockEntity implements MenuProvider, ImplementedInventory {
    // Slot 0: Input Order
    // Slot 1-12: Input Items
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(13, ItemStack.EMPTY);

    protected final ContainerData propertyDelegate;

    public TakeoutBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COUNTERTOP.get(), pos, state);
        this.propertyDelegate = new SimpleContainerData(2);
    }

    public void tryPackOrder(Player player) {
        ItemStack orderStack = inventory.get(0);
        if (orderStack.isEmpty() || !orderStack.is(ModItems.ORDER.get())) return;

        CompoundTag nbt = DataCompat.copy(orderStack);
        if (nbt == null) return;

        // Check expiry
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
            long expiryTick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
            if (level == null || level.getGameTime() >= expiryTick) return;
        } else if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
            long expiryTime = nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
            if (System.currentTimeMillis() >= expiryTime) return;
        }

        CompoundTag foodList = getFoodListIfSatisfied(nbt);
        if (foodList == null) return;

        if (ConfigManager.get().countertopConsumeLeather) {
            if (!tryConsumeOnePaper()) {
                if (player instanceof ServerPlayer sp) {
                    sp.closeContainer();
                    sp.displayClientMessage(Component.translatable("message.ordertocook.countertop.no_packaging").withStyle(ChatFormatting.RED), true);
                }
                return;
            }
        }

        // Consume items
        for (String key : foodList.getAllKeys()) {
            int required = foodList.getInt(key);
            for (int i = 1; i < 13; i++) {
                if (required <= 0) break;
                ItemStack stack = inventory.get(i);
                if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(key)) {
                    int consume = Math.min(stack.getCount(), required);
                    stack.shrink(consume);
                    required -= consume;
                }
            }
        }

        // Create Takeout Bag
        ItemStack bag = new ItemStack(ModItems.TAKEOUT_BAG.get());
        CompoundTag bagNbt = new CompoundTag();

        var customName = orderStack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (customName != null) {
            bag.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, customName);
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
        if (level != null && !level.isClientSide) {
            BlockPos bagPos = worldPosition.above();
            if (level.getBlockState(bagPos).isAir()) {
                Direction facing = Direction.NORTH;
                BlockState selfState = getBlockState();
                if (selfState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                    facing = selfState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                }
                level.setBlock(bagPos, ModBlocks.TAKEOUT_BAG.get().defaultBlockState().setValue(TakeoutBagBlock.FACING, facing), Block.UPDATE_ALL);
                if (level.getBlockEntity(bagPos) instanceof TakeoutBagBlockEntity bagBe) {
                    bagBe.setBagStack(bag.copy());
                }
            } else {
                ItemEntity entity = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, bag);
                entity.setDeltaMovement(0, 0.2, 0);
                level.addFreshEntity(entity);
            }
        }
        
        refreshAvailableCounts();
        setChanged();
    }

    public void tryPlateOrder(Player player) {
        ItemStack orderStack = inventory.get(0);
        if (orderStack.isEmpty() || !orderStack.is(ModItems.ORDER.get()) || level == null || level.isClientSide) return;

        if (isOutputBlocked()) {
            if (player instanceof ServerPlayer sp) {
                sp.closeContainer();
                sp.displayClientMessage(Component.translatable("message.ordertocook.countertop_blocked").withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        CompoundTag nbt = DataCompat.copy(orderStack);
        if (nbt == null) return;
        if (isExpired(nbt)) return;

        if (nbt.getBoolean(ModConstants.NBT_DELIVERY)) {
            if (player instanceof ServerPlayer sp) {
                sp.closeContainer();
                sp.displayClientMessage(Component.translatable("message.ordertocook.countertop.plating_delivery_forbidden").withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        CompoundTag foodList = getFoodListIfSatisfied(nbt);
        if (foodList == null) {
            if (player instanceof ServerPlayer sp) {
                sp.closeContainer();
                sp.displayClientMessage(Component.translatable("message.ordertocook.countertop.missing_food").withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        if (!tryConsumeOnePlate()) {
            if (player instanceof ServerPlayer sp) {
                sp.closeContainer();
                sp.displayClientMessage(Component.translatable("message.ordertocook.countertop.no_clean_plates").withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        consumeFoodList(foodList);

        ItemStack plated = new ItemStack(ModItems.FOOD_PLATE.get());
        CompoundTag platedNbt = nbt.copy();
        platedNbt.putBoolean("Plated", true);
        platedNbt.putString("Carrier", "plate");
        DataCompat.set(plated, platedNbt);

        var customName = orderStack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (customName != null) {
            plated.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, customName);
        }

        inventory.set(0, ItemStack.EMPTY);
        BlockPos platePos = worldPosition.above();
        Direction facing = Direction.NORTH;
        BlockState selfState = getBlockState();
        if (selfState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = selfState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        level.setBlock(platePos, ModBlocks.FOOD_PLATE_DISPLAY.get().defaultBlockState().setValue(FoodPlateBlock.FACING, facing), Block.UPDATE_ALL);
        if (level.getBlockEntity(platePos) instanceof FoodPlateBlockEntity foodPlateBe) {
            foodPlateBe.setPlateStack(plated.copy());
        }
        level.playSound(null, worldPosition, ModSounds.FOOD_PLATE_PLACE.get(), SoundSource.BLOCKS, 0.9f, 1.0f);

        refreshAvailableCounts();
        setChanged();
    }

    private boolean isOutputBlocked() {
        if (this.level == null) return true;
        BlockPos outputPos = this.worldPosition.above();
        if (!this.level.getBlockState(outputPos).isAir()) {
            return true;
        }
        AABB itemBox = new AABB(
                outputPos.getX() + 0.2, outputPos.getY(), outputPos.getZ() + 0.2,
                outputPos.getX() + 0.8, outputPos.getY() + 1.0, outputPos.getZ() + 0.8
        );
        return !this.level.getEntitiesOfClass(ItemEntity.class, itemBox, entity -> entity.isAlive() && !entity.getItem().isEmpty()).isEmpty();
    }

    private boolean isExpired(CompoundTag nbt) {
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
            long expiryTick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
            return level == null || level.getGameTime() >= expiryTick;
        }
        if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
            return System.currentTimeMillis() >= nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
        }
        return false;
    }

    private CompoundTag getFoodListIfSatisfied(CompoundTag nbt) {
        if (!nbt.contains("FoodList")) return null;
        CompoundTag foodList = nbt.getCompound("FoodList");
        Map<String, Integer> availableItems = new HashMap<>();
        for (int i = 1; i < 13; i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                availableItems.put(id, availableItems.getOrDefault(id, 0) + stack.getCount());
            }
        }
        for (String key : foodList.getAllKeys()) {
            if (availableItems.getOrDefault(key, 0) < foodList.getInt(key)) {
                return null;
            }
        }
        return foodList;
    }

    private void consumeFoodList(CompoundTag foodList) {
        for (String key : foodList.getAllKeys()) {
            int required = foodList.getInt(key);
            for (int i = 1; i < 13 && required > 0; i++) {
                ItemStack stack = inventory.get(i);
                if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(key)) {
                    int consume = Math.min(stack.getCount(), required);
                    stack.shrink(consume);
                    required -= consume;
                }
            }
        }
    }

    private boolean tryConsumeOnePaper() {
        if (this.level == null || this.level.isClientSide) return false;
        BlockPos best = null;
        int bestDist = Integer.MAX_VALUE;

        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = -5; dy <= 5; dy++) {
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    m.set(this.worldPosition.getX() + dx, this.worldPosition.getY() + dy, this.worldPosition.getZ() + dz);
                    BlockState state = this.level.getBlockState(m);
                    if (!(state.getBlock() instanceof ShelfBlock)) continue;
                    int papers = state.getValue(ShelfBlock.PAPERS);
                    if (papers <= 0) continue;
                    int dist = dx * dx + dy * dy + dz * dz;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = m.immutable();
                    }
                }
            }
        }
        if (best == null) return false;
        BlockState state = this.level.getBlockState(best);
        if (!(state.getBlock() instanceof ShelfBlock)) return false;
        int papers = state.getValue(ShelfBlock.PAPERS);
        if (papers <= 0) return false;
        this.level.setBlock(best, state.setValue(ShelfBlock.PAPERS, papers - 1), Block.UPDATE_ALL);
        return true;
    }

    private boolean tryConsumeOnePlate() {
        if (this.level == null || this.level.isClientSide) return false;
        BlockPos best = null;
        int bestDist = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = -5; dy <= 5; dy++) {
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    m.set(this.worldPosition.getX() + dx, this.worldPosition.getY() + dy, this.worldPosition.getZ() + dz);
                    BlockState state = this.level.getBlockState(m);
                    if (!(state.getBlock() instanceof PlateShelfBlock)) continue;
                    int plates = state.getValue(PlateShelfBlock.PLATES);
                    if (plates <= 0) continue;
                    int dist = dx * dx + dy * dy + dz * dz;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = m.immutable();
                    }
                }
            }
        }
        if (best == null) return false;
        BlockState state = this.level.getBlockState(best);
        if (!(state.getBlock() instanceof PlateShelfBlock)) return false;
        int plates = state.getValue(PlateShelfBlock.PLATES);
        if (plates <= 0) return false;
        this.level.setBlock(best, state.setValue(PlateShelfBlock.PLATES, plates - 1), Block.UPDATE_ALL);
        return true;
    }

    public int countAvailablePapers() {
        if (this.level == null) return 0;
        int total = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = -5; dy <= 5; dy++) {
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    m.set(this.worldPosition.getX() + dx, this.worldPosition.getY() + dy, this.worldPosition.getZ() + dz);
                    BlockState state = this.level.getBlockState(m);
                    if (!(state.getBlock() instanceof ShelfBlock)) continue;
                    total += state.getValue(ShelfBlock.PAPERS);
                }
            }
        }
        return total;
    }

    public int countAvailablePlates() {
        if (this.level == null) return 0;
        int total = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = -5; dy <= 5; dy++) {
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    m.set(this.worldPosition.getX() + dx, this.worldPosition.getY() + dy, this.worldPosition.getZ() + dz);
                    BlockState state = this.level.getBlockState(m);
                    if (!(state.getBlock() instanceof PlateShelfBlock)) continue;
                    total += state.getValue(PlateShelfBlock.PLATES);
                }
            }
        }
        return total;
    }

    public void refreshAvailableCounts() {
        if (this.level == null || this.level.isClientSide) return;
        this.propertyDelegate.set(0, countAvailablePapers());
        this.propertyDelegate.set(1, countAvailablePlates());
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ordertocook.countertop");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        if (!playerInventory.player.level().isClientSide) {
            refreshAvailableCounts();
        }
        return new TakeoutBoxScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        ContainerHelper.saveAllItems(nbt, inventory, registryLookup);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        ContainerHelper.loadAllItems(nbt, inventory, registryLookup);
    }
}
