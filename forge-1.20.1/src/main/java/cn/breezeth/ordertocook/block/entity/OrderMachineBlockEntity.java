package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.core.OrderGenerator;
import cn.breezeth.ordertocook.util.CoinUtils;
import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.registry.ModSounds;
import cn.breezeth.ordertocook.screen.OrderMachineScreenHandler;
import cn.breezeth.ordertocook.util.ImplementedInventory;
import org.jetbrains.annotations.Nullable;
import cn.breezeth.ordertocook.block.OrderMachineBlock;
import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.util.DataCompat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;


public class OrderMachineBlockEntity extends BlockEntity implements MenuProvider, ImplementedInventory {
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(5, ItemStack.EMPTY);
    
    private int progress = 0;
    // 闁告帗婢樼花鐢碘偓骞垮灪缁侊箑螞閳ь剟寮婚妷銉﹀櫙闁哄牏鍠曢鎼佸籍鐠佸湱绀勯柛濠冨笩椤撴悂寮拋鍦鐟滅増甯″ù鍌滄喆閿曗偓瑜板倹绋夐埀顒€鈻庨垾宕囨閻犲洦娲╃槐?
    private int walkInTimer = ModConstants.WALK_IN_INTERVAL_TICKS;
    private int restaurantLevel = 0;
    private boolean active = false;
    private boolean initialized = false;
    private int initDelay = -1; // -1 means no delay pending
    private long boundBoardPos = Long.MIN_VALUE;
    private long lastBoundBoardScanTick = Long.MIN_VALUE;
    private long lastNearbyBoardScanTick = Long.MIN_VALUE;
    private long lastChairScanTick = Long.MIN_VALUE;
    private int cachedNearbyBoardMaxHunger = 0;
    private boolean cachedHasChairAround = false;
    private String restaurantName = "";
    private String restaurantOwner = "";
    private int restaurantAccepted = 0;
    private int restaurantDelivery = 0;
    private int restaurantLongDistance = 0;
    private int restaurantTotalProfit = 0;
    private int restaurantDeliveryProfit = 0;
    private int restaurantMaxDeliveryDist = 0;
    private int restaurantWalkIn = 0;
    private static final int MAX_LEVEL = 8;
    private int machineId = 0;
    private boolean isPlaced = true;
    private transient boolean oc$registeredPlacedSynced = false;

    protected final ContainerData propertyDelegate;
    private int effectiveBenefitLevel = 0;

    public OrderMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORDER_MACHINE.get(), pos, state);
        this.propertyDelegate = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> restaurantLevel;
                    case 2 -> active ? 1 : 0;
                    case 3 -> effectiveBenefitLevel;
                    case 4 -> Math.max(1, ConfigManager.get().orderMachineCdMinutes);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) progress = value;
                if (index == 1) restaurantLevel = value;
                if (index == 2) active = value != 0;
            }

            @Override
            public int getCount() {
                return 5;
            }
        };
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ordertocook.order_machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        if (this.level instanceof ServerLevel sw) {
            int eff = computeEffectiveBenefitLevel(sw);
            effectiveBenefitLevel = eff;
            if (eff < restaurantLevel) {
                applyBenefitLevelToOrders(eff);
            }
        }
        return new OrderMachineScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        ContainerHelper.saveAllItems(nbt, inventory);
        nbt.putInt(ModConstants.NBT_PROGRESS, progress);
        nbt.putInt(ModConstants.NBT_LEVEL, restaurantLevel);
        nbt.putBoolean(ModConstants.NBT_ACTIVE, active);
        nbt.putBoolean(ModConstants.NBT_INITIALIZED, initialized);
        nbt.putInt(ModConstants.NBT_INIT_DELAY, initDelay);
        nbt.putLong(ModConstants.NBT_BOUND_BOARD_POS, boundBoardPos);
        if (!restaurantName.isEmpty()) nbt.putString(ModConstants.NBT_RESTAURANT_NAME, restaurantName);
        if (!restaurantOwner.isEmpty()) nbt.putString(ModConstants.NBT_RESTAURANT_OWNER, restaurantOwner);
        nbt.putInt(ModConstants.NBT_RESTAURANT_ACCEPTED, restaurantAccepted);
        nbt.putInt(ModConstants.NBT_RESTAURANT_DELIVERY, restaurantDelivery);
        nbt.putInt(ModConstants.NBT_RESTAURANT_LONG_DISTANCE, restaurantLongDistance);
        nbt.putInt(ModConstants.NBT_RESTAURANT_TOTAL_PROFIT, restaurantTotalProfit);
        nbt.putInt(ModConstants.NBT_RESTAURANT_DELIVERY_PROFIT, restaurantDeliveryProfit);
        nbt.putInt(ModConstants.NBT_RESTAURANT_MAX_DELIVERY_DIST, restaurantMaxDeliveryDist);
        nbt.putInt(ModConstants.NBT_RESTAURANT_WALKIN, restaurantWalkIn);
        if (machineId > 0) nbt.putInt(ModConstants.NBT_MACHINE_ID, machineId);
        nbt.putBoolean("isPlaced", isPlaced);
        // cache fields are transient; do not persist
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        ContainerHelper.loadAllItems(nbt, inventory);
        progress = nbt.getInt(ModConstants.NBT_PROGRESS);
        restaurantLevel = nbt.contains(ModConstants.NBT_LEVEL) ? nbt.getInt(ModConstants.NBT_LEVEL) : 0;
        active = nbt.contains(ModConstants.NBT_ACTIVE) ? nbt.getBoolean(ModConstants.NBT_ACTIVE) : false;
        initialized = nbt.getBoolean(ModConstants.NBT_INITIALIZED);
        if (nbt.contains(ModConstants.NBT_INIT_DELAY)) {
            initDelay = nbt.getInt(ModConstants.NBT_INIT_DELAY);
        }
        if (nbt.contains(ModConstants.NBT_BOUND_BOARD_POS)) {
            boundBoardPos = nbt.getLong(ModConstants.NBT_BOUND_BOARD_POS);
        } else {
            boundBoardPos = Long.MIN_VALUE;
        }
        restaurantName = nbt.contains(ModConstants.NBT_RESTAURANT_NAME) ? nbt.getString(ModConstants.NBT_RESTAURANT_NAME) : "";
        restaurantOwner = nbt.contains(ModConstants.NBT_RESTAURANT_OWNER) ? nbt.getString(ModConstants.NBT_RESTAURANT_OWNER) : "";
        restaurantAccepted = nbt.getInt(ModConstants.NBT_RESTAURANT_ACCEPTED);
        restaurantDelivery = nbt.getInt(ModConstants.NBT_RESTAURANT_DELIVERY);
        restaurantLongDistance = nbt.getInt(ModConstants.NBT_RESTAURANT_LONG_DISTANCE);
        restaurantTotalProfit = nbt.getInt(ModConstants.NBT_RESTAURANT_TOTAL_PROFIT);
        restaurantDeliveryProfit = nbt.getInt(ModConstants.NBT_RESTAURANT_DELIVERY_PROFIT);
        restaurantMaxDeliveryDist = nbt.getInt(ModConstants.NBT_RESTAURANT_MAX_DELIVERY_DIST);
        restaurantWalkIn = nbt.getInt(ModConstants.NBT_RESTAURANT_WALKIN);
        machineId = nbt.contains(ModConstants.NBT_MACHINE_ID) ? nbt.getInt(ModConstants.NBT_MACHINE_ID) : 0;
        isPlaced = !nbt.contains("isPlaced") || nbt.getBoolean("isPlaced");
        // reset caches on load
        lastBoundBoardScanTick = Long.MIN_VALUE;
        lastNearbyBoardScanTick = Long.MIN_VALUE;
        lastChairScanTick = Long.MIN_VALUE;
        cachedNearbyBoardMaxHunger = 0;
        cachedHasChairAround = false;
    }

    @Override
    public void setLevel(Level world) {
        super.setLevel(world);
        if (world != null && !world.isClientSide && world instanceof ServerLevel sw) {
            if (!oc$registeredPlacedSynced && this.machineId > 0) {
                cn.breezeth.ordertocook.core.RestaurantRegistry.registerById(sw, this.machineId, this);
                oc$registeredPlacedSynced = true;
            }
        }
    }

    public void scheduleInitialization(int ticks) {
        this.initDelay = ticks;
    }

    public void applyRestaurantDataFromItemNbt(CompoundTag nbt) {
        if (nbt == null) return;
        if (nbt.contains(ModConstants.NBT_LEVEL)) restaurantLevel = nbt.getInt(ModConstants.NBT_LEVEL);
        if (nbt.contains(ModConstants.NBT_RESTAURANT_NAME)) restaurantName = nbt.getString(ModConstants.NBT_RESTAURANT_NAME);
        if (nbt.contains(ModConstants.NBT_RESTAURANT_OWNER)) restaurantOwner = nbt.getString(ModConstants.NBT_RESTAURANT_OWNER);
        if (nbt.contains(ModConstants.NBT_RESTAURANT_ACCEPTED)) restaurantAccepted = nbt.getInt(ModConstants.NBT_RESTAURANT_ACCEPTED);
        if (nbt.contains(ModConstants.NBT_RESTAURANT_DELIVERY)) restaurantDelivery = nbt.getInt(ModConstants.NBT_RESTAURANT_DELIVERY);
        if (nbt.contains(ModConstants.NBT_RESTAURANT_LONG_DISTANCE)) restaurantLongDistance = nbt.getInt(ModConstants.NBT_RESTAURANT_LONG_DISTANCE);
        if (nbt.contains(ModConstants.NBT_RESTAURANT_TOTAL_PROFIT)) restaurantTotalProfit = nbt.getInt(ModConstants.NBT_RESTAURANT_TOTAL_PROFIT);
        if (nbt.contains(ModConstants.NBT_RESTAURANT_DELIVERY_PROFIT)) restaurantDeliveryProfit = nbt.getInt(ModConstants.NBT_RESTAURANT_DELIVERY_PROFIT);
        if (nbt.contains(ModConstants.NBT_RESTAURANT_MAX_DELIVERY_DIST)) restaurantMaxDeliveryDist = nbt.getInt(ModConstants.NBT_RESTAURANT_MAX_DELIVERY_DIST);
        if (nbt.contains(ModConstants.NBT_RESTAURANT_WALKIN)) restaurantWalkIn = nbt.getInt(ModConstants.NBT_RESTAURANT_WALKIN);
        if (nbt.contains(ModConstants.NBT_MACHINE_ID)) {
            int id = nbt.getInt(ModConstants.NBT_MACHINE_ID);
            if (id > 0) machineId = id;
        }
        setChanged();
    }

    public void tick(Level world, BlockPos pos, BlockState state) {
        if (world.isClientSide) return;

        // Handle delayed initialization
        if (initDelay > 0) {
            initDelay--;
            if (initDelay == 0) {
                if (!initialized) {
                    initialized = true;
                }
                initDelay = -1;
            }
            return; // Skip normal tick while waiting for initialization
        }

        // Fallback initialization for legacy blocks or direct placements
        if (!initialized && initDelay == -1) {
             initialized = true;
        }

        if (!active) return;
        progress++;
        if (progress >= getRefreshTimeTicks()) {
            refreshOrders();
            progress = 0;
        }

        if (walkInTimer > 0) {
            walkInTimer--;
        } else {
            // 闂佹彃绉堕悿鍡涘礆閺夎法鏆楅悗骞垮灪缁侊箑螞閳ь剟寮婚妷銉﹀櫙闁?            walkInTimer = ModConstants.WALK_IN_INTERVAL_TICKS;
            if (this.level instanceof ServerLevel sw) {
                trySpawnWalkIn(sw);
            }
        }
    }

    private int getRefreshTimeTicks() {
        int minutes = Math.max(1, ConfigManager.get().orderMachineCdMinutes);
        long ticks = minutes * 60L * 20L;
        if (ticks > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) ticks;
    }

    private void trySpawnWalkIn(ServerLevel sw) {
        double chance = walkInChance(restaurantLevel);
        if (sw.random.nextDouble() < chance) {
            long boardPos = ensureBoundBoard(sw);
            cn.breezeth.ordertocook.core.WalkInNpcManager.spawn(sw, this.worldPosition, restaurantLevel, boardPos);
        }
    }

    private void refreshOrders() {
        inventory.clear();
        int slots = getUnlockedSlots(restaurantLevel);
        if (this.level instanceof ServerLevel sw) {
            List<Item> menuFoods = getMenuFoodsForMachine(sw, this.worldPosition);
            for (int i = 0; i < slots; i++) {
                inventory.set(i, OrderGenerator.generateRandomOrder(sw, this.worldPosition, restaurantLevel, menuFoods));
            }
        }
        setChanged();
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, ModSounds.ORDER_REFRESH.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            BlockState state = this.level.getBlockState(this.worldPosition);
            if (state.getBlock() instanceof OrderMachineBlock block) {
                if (!this.level.isClientSide) {
                    ServerLevel sw = (ServerLevel) this.level;
                    BlockState s1 = state.setValue(OrderMachineBlock.MODE, OrderMachineBlock.Mode.PRINTING).setValue(OrderMachineBlock.FRAME, 0);
                    sw.setBlock(this.worldPosition, s1, Block.UPDATE_ALL);
                    sw.scheduleTick(this.worldPosition, block, OrderMachineBlock.BEAT_TICKS);
                }
            }
        }
    }

    public static List<Item> getBoundBoardMenuFoods(ServerLevel sw, BlockPos machinePos) {
        return getMenuFoodsForMachine(sw, machinePos);
    }

    public static List<Item> getMenuFoodsForMachine(ServerLevel sw, BlockPos machinePos) {
        if (sw.getBlockState(machinePos).getBlock() == cn.breezeth.ordertocook.registry.ModBlocks.ORDER_MACHINE.get()
                && sw.getBlockEntity(machinePos) instanceof OrderMachineBlockEntity be) {
            return be.getMenuFoods(sw);
        }
        long boardPos = findBestBoardPos(sw, machinePos);
        if (boardPos == Long.MIN_VALUE) return List.of();
        return getMenuFoodsForBoardPos(sw, boardPos);
    }

    private List<Item> getMenuFoods(ServerLevel sw) {
        long boardPos = ensureBoundBoard(sw);
        if (boardPos == Long.MIN_VALUE) return List.of();
        return getMenuFoodsForBoardPos(sw, boardPos);
    }

    private long ensureBoundBoard(ServerLevel sw) {
        if (boundBoardPos != Long.MIN_VALUE) {
            BlockPos p = BlockPos.of(boundBoardPos);
            if (sw.getBlockState(p).getBlock() == cn.breezeth.ordertocook.registry.ModBlocks.BOARD.get()
                    && sw.getBlockEntity(p) instanceof BoardBlockEntity) {
                return boundBoardPos;
            }
        }
        long now = sw.getGameTime();
        if (lastBoundBoardScanTick != Long.MIN_VALUE
                && now - lastBoundBoardScanTick < ModConstants.SCAN_CACHE_INTERVAL_TICKS) {
            return boundBoardPos;
        }
        long found = findBestBoardPos(sw, this.worldPosition);
        boundBoardPos = found;
        lastBoundBoardScanTick = now;
        setChanged();
        return found;
    }

    private static long findBestBoardPos(ServerLevel sw, BlockPos machinePos) {
        int radius = ModConstants.BOARD_SCAN_RADIUS;
        BlockPos center = machinePos;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int bestNutrition = -1;
        double bestDist = Double.MAX_VALUE;
        long bestPos = Long.MIN_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int dy = -ModConstants.SCAN_VERTICAL; dy <= ModConstants.SCAN_VERTICAL; dy++) {
                    int y = center.getY() + dy;
                    if (y < sw.getMinBuildHeight() || y >= sw.getMaxBuildHeight()) continue;
                    m.set(center.getX() + dx, y, center.getZ() + dz);
                    if (sw.getBlockState(m).getBlock() != cn.breezeth.ordertocook.registry.ModBlocks.BOARD.get()) continue;
                    if (!(sw.getBlockEntity(m) instanceof BoardBlockEntity be)) continue;
                    int nutrition = be.getTotalNutrition();
                    double dist = m.distSqr(center);
                    if (nutrition > bestNutrition || (nutrition == bestNutrition && dist < bestDist)) {
                        bestNutrition = nutrition;
                        bestDist = dist;
                        bestPos = m.asLong();
                    }
                }
            }
        }

        return bestPos;
    }

    private static List<Item> getMenuFoodsForBoardPos(ServerLevel sw, long boardPos) {
        if (boardPos == Long.MIN_VALUE) return List.of();
        BlockPos p = BlockPos.of(boardPos);
        if (sw.getBlockState(p).getBlock() != cn.breezeth.ordertocook.registry.ModBlocks.BOARD.get()) return List.of();
        if (!(sw.getBlockEntity(p) instanceof BoardBlockEntity be)) return List.of();

        ArrayList<Item> list = new ArrayList<>();
        for (ItemStack s : be.getItems()) {
            if (s.isEmpty()) continue;
            FoodProperties fc = s.getItem().getFoodProperties();
            int nutrition = (fc != null) ? fc.getNutrition() : 0;
            if (nutrition <= 0) {
                nutrition = ConfigManager.getCustomMenuNutrition(s);
            }
            if (nutrition <= 0) continue;
            list.add(s.getItem());
        }
        return list;
    }

    public int getRestaurantLevel() {
        return restaurantLevel;
    }

    public boolean isActive() {
        return active;
    }

    private static int getUnlockedSlots(int restaurantLevel) {
        return switch (restaurantLevel) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 3;
            case 4 -> 4;
            default -> 5;
        };
    }

    public boolean tryUpgrade(Player player) {
        if (!(player instanceof ServerPlayer sp)) return false;
        if (restaurantLevel >= MAX_LEVEL) return false;

        if (this.level == null || !(this.level instanceof ServerLevel sw)) return false;

        int nextLevel = restaurantLevel + 1;
        int requiredHunger = requiredBoardHunger(nextLevel);
        if (requiredHunger > 0) {
            int maxHunger = nearbyBoardMaxHunger(sw);
            if (maxHunger < requiredHunger) {
                sp.closeContainer();
                sp.displayClientMessage(Component.translatable("message.ordertocook.upgrade_not_enough_board_hunger", maxHunger, requiredHunger), true);
                return false;
            }
        }

        int cost = upgradeCost(nextLevel);
        try {
            if (!CoinUtils.tryConsumeWithChange(sp, cost)) {
                sp.closeContainer();
                sp.displayClientMessage(Component.translatable("message.ordertocook.upgrade_not_enough"), true);
                return false;
            }
        } catch (Throwable t) {
            cn.breezeth.ordertocook.OrderToCookMod.LOGGER.error("Failed to consume coins while upgrading order machine", t);
            sp.closeContainer();
            sp.displayClientMessage(Component.translatable("message.ordertocook.upgrade_failed_currency").withStyle(ChatFormatting.RED), false);
            return false;
        }
        restaurantLevel = nextLevel;
        // Recompute board benefit after upgrade.
        int eff = computeEffectiveBenefitLevel(sw);
        effectiveBenefitLevel = eff;
        setChanged();
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, ModSounds.ORDER_REFRESH.get(), SoundSource.BLOCKS, 1.0f, 1.2f);
        }
        if (restaurantLevel >= MAX_LEVEL) {
            grantAdvancement(sp, new ResourceLocation(ModConstants.MOD_ID, "ordertocook/eight_star_restaurant"));
        }
        // Keep timer progress and wait for next refresh
        if (this.level instanceof ServerLevel) {
            cn.breezeth.ordertocook.core.RestaurantRegistry.update(this);
        }
        return true;
    }

    private static void grantAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        if (player.getServer() == null) return;
        var advancement = player.getServer().getAdvancements().getAdvancement(advancementId);
        if (advancement == null) return;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private static int upgradeCost(int nextLevel) {
        return switch (nextLevel) {
            case 1 -> 2;
            case 2 -> 20;
            case 3 -> 40;
            case 4 -> 100;
            case 5 -> 300;
            case 6 -> 500;
            case 7 -> 500;
            case 8 -> 800;
            default -> 0;
        };
    }

    private static double walkInChance(int restaurantLevel) {
        return switch (restaurantLevel) {
            case 1 -> 0.05;
            case 2 -> 0.10;
            case 3 -> 0.15;
            case 4 -> 0.20;
            case 5 -> 0.30;
            case 6 -> 0.40;
            case 7 -> 0.50;
            case 8 -> 0.70;
            default -> 0.0;
        };
    }

    private static int requiredBoardHunger(int nextLevel) {
        var list = ConfigManager.get().orderMachineUpgradeBoardHunger;
        if (list == null || list.isEmpty()) return 0;
        if (nextLevel < 0) return 0;
        if (nextLevel >= list.size()) {
            return list.get(list.size() - 1);
        }
        return list.get(nextLevel);
    }

    private int nearbyBoardMaxHunger(ServerLevel sw) {
        long now = sw.getGameTime();
        if (lastNearbyBoardScanTick != Long.MIN_VALUE
                && now - lastNearbyBoardScanTick < ModConstants.SCAN_CACHE_INTERVAL_TICKS) {
            return cachedNearbyBoardMaxHunger;
        }
        int radius = ModConstants.BOARD_SCAN_RADIUS;
        BlockPos center = this.worldPosition;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int max = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int dy = -ModConstants.SCAN_VERTICAL; dy <= ModConstants.SCAN_VERTICAL; dy++) {
                    int y = center.getY() + dy;
                    if (y < sw.getMinBuildHeight() || y >= sw.getMaxBuildHeight()) continue;
                    m.set(center.getX() + dx, y, center.getZ() + dz);
                    if (sw.getBlockState(m).getBlock() != cn.breezeth.ordertocook.registry.ModBlocks.BOARD.get()) continue;
                    if (!(sw.getBlockEntity(m) instanceof BoardBlockEntity be)) continue;
                    max = Math.max(max, be.getTotalNutrition());
                }
            }
        }
        cachedNearbyBoardMaxHunger = max;
        lastNearbyBoardScanTick = now;
        return max;
    }

    public boolean tryToggleActive(Player player) {
        if (!(player instanceof ServerPlayer)) return false;
        boolean target = !active;
        if (target) {
            ServerPlayer sp = (ServerPlayer) player;
            if (!hasChairAround(sp)) {
                sp.closeContainer();
                player.displayClientMessage(Component.translatable("message.ordertocook.no_chair").withStyle(ChatFormatting.RED), true);
                return false;
            }
        }
        active = target;
        if (!active) {
            inventory.clear();
        }
        setChanged();
        return true;
    }

    private boolean hasChairAround(ServerPlayer player) {
        if (this.level == null) return false;
        if (!(this.level instanceof ServerLevel sw)) return false;
        long now = sw.getGameTime();
        if (lastChairScanTick != Long.MIN_VALUE
                && now - lastChairScanTick < ModConstants.SCAN_CACHE_INTERVAL_TICKS) {
            return cachedHasChairAround;
        }
        int radius = ModConstants.CHAIR_SCAN_RADIUS;
        BlockPos center = this.worldPosition;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int dy = -ModConstants.SCAN_VERTICAL; dy <= ModConstants.SCAN_VERTICAL; dy++) {
                    int y = center.getY() + dy;
                    if (y < sw.getMinBuildHeight() || y >= sw.getMaxBuildHeight()) continue;
                    m.set(center.getX() + dx, y, center.getZ() + dz);
                    if (sw.getBlockState(m).getBlock() == cn.breezeth.ordertocook.registry.ModBlocks.CHAIR.get()) {
                        cachedHasChairAround = true;
                        lastChairScanTick = now;
                        return true;
                    }
                }
            }
        }
        cachedHasChairAround = false;
        lastChairScanTick = now;
        return false;
    }

    public void onOrderAccepted() {
        if (this.level == null || this.level.isClientSide) return;
        ServerLevel sw = (ServerLevel) this.level;
        BlockState state = sw.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof OrderMachineBlock) {
            BlockState idle = state.setValue(OrderMachineBlock.MODE, OrderMachineBlock.Mode.IDLE)
                                   .setValue(OrderMachineBlock.FRAME, 0);
            sw.setBlock(this.worldPosition, idle, Block.UPDATE_ALL);
        }
    }

    public void recordAcceptedOrder(ServerPlayer player, CompoundTag nbt) {
        setChanged();
    }

    public void recordCompletedProfit(ServerPlayer player, CompoundTag nbt, int earnedCoin) {
        int orderType = nbt.contains(ModConstants.NBT_ORDER_TYPE) ? nbt.getInt(ModConstants.NBT_ORDER_TYPE) : 0;
        boolean isLong = nbt.getBoolean(ModConstants.NBT_IS_LONG_DISTANCE);
        int deliveryDist = orderType > 1 ? orderType : (nbt.contains(ModConstants.NBT_DELIVERY_DIST) ? nbt.getInt(ModConstants.NBT_DELIVERY_DIST) : 0);
        boolean delivery = deliveryDist > 1;
        int coin = Math.max(0, earnedCoin);
        restaurantTotalProfit += coin;
        restaurantAccepted++;
        if (delivery) {
            restaurantDelivery++;
            if (isLong) restaurantLongDistance++;
            if (deliveryDist > restaurantMaxDeliveryDist) restaurantMaxDeliveryDist = deliveryDist;
            restaurantDeliveryProfit += coin;
        } else if (orderType == 1) {
            restaurantWalkIn++;
        }
        setChanged();
    }

    public void setOwnerIfEmpty(ServerPlayer player) {
        if (restaurantOwner == null || restaurantOwner.isEmpty()) {
            restaurantOwner = player.getGameProfile().getName();
            setChanged();
        }
    }

    public boolean tryRename(ServerPlayer player, String name) {
        String owner = restaurantOwner == null ? "" : restaurantOwner;
        String pn = player.getGameProfile().getName();
        if (!owner.isEmpty() && !owner.equals(pn)) {
            player.closeContainer();
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.ordertocook.rename_not_owner").withStyle(net.minecraft.ChatFormatting.RED), true);
            return false;
        }
        String cur = restaurantName == null ? "" : restaurantName;
        boolean first = cur.isEmpty();
        if (!first) {
            try {
                if (!CoinUtils.tryConsumeWithChange(player, 1)) {
                    player.closeContainer();
                    player.displayClientMessage(Component.translatable("message.ordertocook.rename_not_enough"), true);
                    return false;
                }
            } catch (Throwable t) {
                cn.breezeth.ordertocook.OrderToCookMod.LOGGER.error("Failed to consume coins while renaming order machine", t);
                player.closeContainer();
                player.displayClientMessage(Component.translatable("message.ordertocook.rename_failed_currency").withStyle(ChatFormatting.RED), false);
                return false;
            }
        }
        restaurantName = name == null ? "" : name;
        setChanged();
        return true;
    }

    public RestaurantStats snapshotStats() {
        return new RestaurantStats(
                this.level instanceof ServerLevel sw ? sw.dimension().location().toString() : "",
                this.worldPosition.asLong(),
                restaurantName == null ? "" : restaurantName,
                restaurantOwner == null ? "" : restaurantOwner,
                restaurantLevel,
                restaurantAccepted,
                restaurantDelivery,
                restaurantLongDistance,
                restaurantTotalProfit,
                restaurantDeliveryProfit,
                restaurantMaxDeliveryDist,
                restaurantWalkIn
        );
    }

    public record RestaurantStats(String dimension, long posLong, String name, String owner, int level,
                                  int accepted, int delivery, int longDistance, int totalProfit, int deliveryProfit,
                                  int maxDeliveryDist, int walkIn) {}

    public int getMachineId() {
        return machineId;
    }

    public void setMachineId(int id) {
        this.machineId = id;
        setChanged();
    }

    public void setPlaced(boolean placed) {
        this.isPlaced = placed;
        setChanged();
    }

    public boolean isPlaced() {
        return isPlaced;
    }

    public int ensureMachineId(ServerLevel sw) {
        if (machineId > 0) return machineId;
        int id = cn.breezeth.ordertocook.core.MachineRankingState.get(sw).allocateId();
        this.machineId = id;
        setChanged();
        cn.breezeth.ordertocook.core.RestaurantRegistry.registerById(sw, id, this);
        return id;
    }

    public void forceRefresh() {
        refreshOrders();
        progress = 0;
    }

    private int computeEffectiveBenefitLevel(ServerLevel sw) {
        int maxHunger = nearbyBoardMaxHunger(sw);
        int l = this.restaurantLevel;
        while (l > 0 && requiredBoardHunger(l) > maxHunger) {
            l--;
        }
        return l;
    }

    private void applyBenefitLevelToOrders(int eff) {
        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack s = this.inventory.get(i);
            if (s.isEmpty()) continue;
            CompoundTag nbt = DataCompat.copy(s);
            if (nbt == null) continue;
            int coin = cn.breezeth.ordertocook.core.OrderGenerator.recalcPrestigeFromNbt(nbt, eff);
            nbt.putInt(ModConstants.NBT_PRESTIGE, coin);
            DataCompat.set(s, nbt);
            this.inventory.set(i, s);
        }
        setChanged();
    }
}
