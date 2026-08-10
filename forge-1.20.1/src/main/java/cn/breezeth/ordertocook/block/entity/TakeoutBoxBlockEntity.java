package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.screen.TakeoutBoxScreenHandler;
import cn.breezeth.ordertocook.util.ImplementedInventory;
import org.jetbrains.annotations.Nullable;
import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.registry.ModBlocks;
import cn.breezeth.ordertocook.util.DataCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
        this.propertyDelegate = new SimpleContainerData(22);
    }

    public cn.breezeth.ordertocook.api.CountertopAutomationApi.Status getAutomationStatus(cn.breezeth.ordertocook.api.CountertopAutomationApi.Action action) {
        ItemStack orderStack=inventory.get(0); if(orderStack.isEmpty()||!orderStack.is(ModItems.ORDER.get()))return cn.breezeth.ordertocook.api.CountertopAutomationApi.Status.NO_ORDER; CompoundTag nbt=DataCompat.copy(orderStack); if(nbt==null||isExpired(nbt))return cn.breezeth.ordertocook.api.CountertopAutomationApi.Status.EXPIRED; if(action==cn.breezeth.ordertocook.api.CountertopAutomationApi.Action.PLATE&&isOutputBlocked())return cn.breezeth.ordertocook.api.CountertopAutomationApi.Status.OUTPUT_BLOCKED; if(action==cn.breezeth.ordertocook.api.CountertopAutomationApi.Action.PLATE&&nbt.getBoolean(ModConstants.NBT_DELIVERY))return cn.breezeth.ordertocook.api.CountertopAutomationApi.Status.DELIVERY_REQUIRES_PACKAGING; if(getFoodListIfSatisfied(nbt)==null)return cn.breezeth.ordertocook.api.CountertopAutomationApi.Status.MISSING_FOOD; if(action==cn.breezeth.ordertocook.api.CountertopAutomationApi.Action.PACK&&ConfigManager.get().countertopConsumeLeather&&countAvailablePapers()<=0)return cn.breezeth.ordertocook.api.CountertopAutomationApi.Status.MISSING_PACKAGING; if(action==cn.breezeth.ordertocook.api.CountertopAutomationApi.Action.PLATE&&countAvailablePlates()<=0)return cn.breezeth.ordertocook.api.CountertopAutomationApi.Status.MISSING_PLATE; return cn.breezeth.ordertocook.api.CountertopAutomationApi.Status.SUCCESS;
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
        if (foodList == null) {
            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.translatable("message.ordertocook.countertop.missing_food").withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        if (ConfigManager.get().countertopConsumeLeather) {
            if (!tryConsumeOnePaper()) {
                if (player instanceof ServerPlayer sp) {
                    sp.closeContainer();
                    sp.displayClientMessage(Component.translatable("message.ordertocook.countertop.no_packaging").withStyle(ChatFormatting.RED), true);
                }
                return;
            }
        }

        consumeFoodList(foodList);

        // Create Takeout Bag
        ItemStack bag = new ItemStack(ModItems.TAKEOUT_BAG.get());
        CompoundTag bagNbt = new CompoundTag();

        var customName = orderStack.getHoverName();
        if (customName != null) {
            bag.setHoverName(customName);
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
        
        if (level instanceof net.minecraft.server.level.ServerLevel lifecycleWorld) {
            cn.breezeth.ordertocook.api.OrderLifecycleApi.emit(
                    cn.breezeth.ordertocook.api.OrderLifecycleApi.Type.PACKED,
                    lifecycleWorld, player, orderStack);
        }
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
                spawnFreshSteam(bagPos);
            } else {
                ItemEntity entity = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, bag);
                entity.setDeltaMovement(0, 0.2, 0);
                level.addFreshEntity(entity);
                spawnFreshSteam(worldPosition.above());
            }
        }
        
        closeCountertopScreen(player);
        refreshAvailableCounts();
        setChanged();
    }

    public void refreshIngredientStatus(Player player, boolean sendDetails) {
        if (!(player instanceof ServerPlayer sp) || level == null || level.isClientSide) return;
        ItemStack orderStack = inventory.get(0);
        if (orderStack.isEmpty() || !orderStack.is(ModItems.ORDER.get())) {
            clearIngredientQueryData();
            propertyDelegate.set(2, 0);
            return;
        }
        CompoundTag nbt = DataCompat.copy(orderStack);
        if (nbt == null || !nbt.contains("FoodList")) {
            clearIngredientQueryData();
            propertyDelegate.set(2, 2);
            if (sendDetails) sp.sendSystemMessage(Component.translatable("message.ordertocook.countertop.query_invalid").withStyle(ChatFormatting.RED));
            return;
        }
        if (isExpired(nbt)) {
            clearIngredientQueryData();
            propertyDelegate.set(2, 0);
            return;
        }
        CompoundTag foodList = nbt.getCompound("FoodList");
        Map<String, Integer> availableItems = collectAvailableItems(foodList);
        clearIngredientQueryData();
        boolean missing = false;
        int missingIndex = 0;
        for (String key : foodList.getAllKeys()) {
            int required = foodList.getInt(key);
            int available = availableItems.getOrDefault(key, 0);
            if (available >= required) continue;
            if (!missing && sendDetails) {
                sp.sendSystemMessage(Component.translatable("message.ordertocook.countertop.query_missing_header").withStyle(ChatFormatting.YELLOW));
            }
            missing = true;
            ResourceLocation id = ResourceLocation.tryParse(key);
            Component foodName = id == null ? Component.literal(key) : new ItemStack(BuiltInRegistries.ITEM.get(id)).getHoverName();
            if (id != null && missingIndex < 6) {
                int base = 4 + missingIndex * 3;
                propertyDelegate.set(base, BuiltInRegistries.ITEM.getId(BuiltInRegistries.ITEM.get(id)));
                propertyDelegate.set(base + 1, required);
                propertyDelegate.set(base + 2, available);
                missingIndex++;
            }
            if (sendDetails) {
                sp.sendSystemMessage(Component.translatable("message.ordertocook.countertop.query_missing_detail",
                        foodName, required, available, required - available).withStyle(ChatFormatting.YELLOW));
            }
        }
        propertyDelegate.set(3, missingIndex);
        propertyDelegate.set(2, missing ? 2 : 1);
        if (!missing && sendDetails) {
            sp.sendSystemMessage(Component.translatable("message.ordertocook.countertop.query_ready").withStyle(ChatFormatting.GREEN));
        }
    }

    private void clearIngredientQueryData() {
        for (int i = 3; i < propertyDelegate.getCount(); i++) propertyDelegate.set(i, 0);
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

        var customName = orderStack.getHoverName();
        if (customName != null) {
            plated.setHoverName(customName);
        }

        if (level instanceof net.minecraft.server.level.ServerLevel lifecycleWorld) {
            cn.breezeth.ordertocook.api.OrderLifecycleApi.emit(
                    cn.breezeth.ordertocook.api.OrderLifecycleApi.Type.PACKED,
                    lifecycleWorld, player, orderStack);
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
        spawnFreshSteam(platePos);

        closeCountertopScreen(player);
        refreshAvailableCounts();
        setChanged();
    }
    private void spawnFreshSteam(BlockPos outputPos) {
        if (!(level instanceof ServerLevel serverWorld)) return;
        serverWorld.sendParticles(
                ParticleTypes.CLOUD,
                outputPos.getX() + 0.5,
                outputPos.getY() + 0.35,
                outputPos.getZ() + 0.5,
                6,
                0.18,
                0.05,
                0.18,
                0.01
        );
    }

    private void closeCountertopScreen(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.closeContainer();
        }
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
        Map<String, Integer> availableItems = collectAvailableItems(foodList);
        for (String key : foodList.getAllKeys()) {
            if (availableItems.getOrDefault(key, 0) < foodList.getInt(key)) {
                return null;
            }
        }
        return foodList;
    }

    private Map<String, Integer> collectAvailableItems(CompoundTag foodList) {
        Map<String, Integer> availableItems = new HashMap<>(
                cn.breezeth.ordertocook.api.IngredientSourceCompatApi.countAllNearby(level, worldPosition, 24, foodList.getAllKeys()));
        for (int i = 1; i < 13; i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                availableItems.put(id, availableItems.getOrDefault(id, 0) + stack.getCount());
            }
        }
        return availableItems;
    }

    private void consumeFoodList(CompoundTag foodList) {
        for (String key : foodList.getAllKeys()) {
            int required = foodList.getInt(key);
            required -= cn.breezeth.ordertocook.api.IngredientSourceCompatApi.extractNearby(level, worldPosition, 24, key, required, false);
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
        return this.level != null && !this.level.isClientSide
                && cn.breezeth.ordertocook.api.RestaurantSupplyCompatApi.extractNearby(level, worldPosition, 5,
                cn.breezeth.ordertocook.api.RestaurantSupplyCompatApi.SupplyType.PACKAGING, 1, false) == 1;
    }

    private boolean tryConsumeOnePlate() {
        return this.level != null && !this.level.isClientSide
                && cn.breezeth.ordertocook.api.RestaurantSupplyCompatApi.extractNearby(level, worldPosition, 5,
                cn.breezeth.ordertocook.api.RestaurantSupplyCompatApi.SupplyType.CLEAN_PLATE, 1, false) == 1;
    }

    public int countAvailablePapers() {
        return cn.breezeth.ordertocook.api.RestaurantSupplyCompatApi.countNearby(level, worldPosition, 5,
                cn.breezeth.ordertocook.api.RestaurantSupplyCompatApi.SupplyType.PACKAGING);
    }

    public int countAvailablePlates() {
        return cn.breezeth.ordertocook.api.RestaurantSupplyCompatApi.countNearby(level, worldPosition, 5,
                cn.breezeth.ordertocook.api.RestaurantSupplyCompatApi.SupplyType.CLEAN_PLATE);
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
            refreshIngredientStatus(player, false);
        }
        return new TakeoutBoxScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        ContainerHelper.saveAllItems(nbt, inventory);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        ContainerHelper.loadAllItems(nbt, inventory);
    }
}
