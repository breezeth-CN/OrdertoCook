package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.core.OrderGenerator;
import cn.breezeth.ordertocook.util.CoinUtils;
import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.registry.ModSounds;
import cn.breezeth.ordertocook.screen.OrderMachineScreenHandler;
import cn.breezeth.ordertocook.util.ImplementedInventory;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.world.ServerWorld;
import cn.breezeth.ordertocook.block.OrderMachineBlock;
import net.minecraft.block.Block;
import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.util.DataCompat;
import net.minecraft.item.FoodComponent;

import java.util.ArrayList;
import java.util.List;


public class OrderMachineBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(5, ItemStack.EMPTY);
    
    private int progress = 0;
    // 到店客流检查周期计时（倒计时，归零触发一次尝试）
    private int walkInTimer = ModConstants.WALK_IN_INTERVAL_TICKS;
    private int level = 0;
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

    protected final PropertyDelegate propertyDelegate;
    private int effectiveBenefitLevel = 0;

    public OrderMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORDER_MACHINE, pos, state);
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> level;
                    case 2 -> active ? 1 : 0;
                    case 3 -> effectiveBenefitLevel;
                    case 4 -> Math.max(1, ConfigManager.get().orderMachineRefreshSeconds);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) progress = value;
                if (index == 1) level = value;
                if (index == 2) active = value != 0;
            }

            @Override
            public int size() {
                return 5;
            }
        };
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.ordertocook.order_machine");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        if (this.world instanceof ServerWorld sw) {
            int eff = computeEffectiveBenefitLevel(sw);
            effectiveBenefitLevel = eff;
            if (eff < level) {
                applyBenefitLevelToOrders(eff);
            }
        }
        return new OrderMachineScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putInt(ModConstants.NBT_PROGRESS, progress);
        nbt.putInt(ModConstants.NBT_LEVEL, level);
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
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, inventory);
        progress = nbt.getInt(ModConstants.NBT_PROGRESS);
        level = nbt.contains(ModConstants.NBT_LEVEL) ? nbt.getInt(ModConstants.NBT_LEVEL) : 0;
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
    public void setWorld(World world) {
        super.setWorld(world);
        if (world != null && !world.isClient && world instanceof ServerWorld sw) {
            if (!oc$registeredPlacedSynced && this.machineId > 0) {
                cn.breezeth.ordertocook.core.RestaurantRegistry.registerById(sw, this.machineId, this);
                oc$registeredPlacedSynced = true;
            }
        }
    }

    public void scheduleInitialization(int ticks) {
        this.initDelay = ticks;
    }

    public void applyRestaurantDataFromItemNbt(NbtCompound nbt) {
        if (nbt == null) return;
        if (nbt.contains(ModConstants.NBT_LEVEL)) level = nbt.getInt(ModConstants.NBT_LEVEL);
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
        markDirty();
    }

    public void tick(World world, BlockPos pos, BlockState state) {
        if (world.isClient) return;

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
            // 重置到店客流检查周期
            walkInTimer = getWalkInAttemptIntervalTicks();
            if (this.world instanceof ServerWorld sw) {
                trySpawnWalkIn(sw);
            }
        }
    }

    private int getRefreshTimeTicks() {
        int seconds = Math.max(1, ConfigManager.get().orderMachineRefreshSeconds);
        long ticks = seconds * 20L;
        if (ticks > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) ticks;
    }

    private int getWalkInAttemptIntervalTicks() {
        int seconds = Math.max(1, ConfigManager.get().walkInAttemptIntervalSeconds);
        long ticks = seconds * 20L;
        if (ticks > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) ticks;
    }

    private void trySpawnWalkIn(ServerWorld sw) {
        double chance = walkInChance(level);
        if (sw.random.nextDouble() < chance) {
            long boardPos = ensureBoundBoard(sw);
            cn.breezeth.ordertocook.core.WalkInNpcManager.spawn(sw, this.pos, level, boardPos);
        }
    }

    private void refreshOrders() {
        inventory.clear();
        int slots = getUnlockedSlots(level);
        if (this.world instanceof ServerWorld sw) {
            List<Item> menuFoods = getMenuFoodsForMachine(sw, this.pos);
            for (int i = 0; i < slots; i++) {
                inventory.set(i, OrderGenerator.generateRandomOrder(sw, this.pos, level, menuFoods));
            }
        }
        markDirty();
        if (this.world != null) {
            this.world.playSound(null, this.pos, ModSounds.ORDER_REFRESH, SoundCategory.BLOCKS, 1.0f, 1.0f);
            BlockState state = this.world.getBlockState(this.pos);
            if (state.getBlock() instanceof OrderMachineBlock block) {
                if (!this.world.isClient) {
                    ServerWorld sw = (ServerWorld) this.world;
                    BlockState s1 = state.with(OrderMachineBlock.MODE, OrderMachineBlock.Mode.PRINTING).with(OrderMachineBlock.FRAME, 0);
                    sw.setBlockState(this.pos, s1, Block.NOTIFY_ALL);
                    sw.scheduleBlockTick(this.pos, block, OrderMachineBlock.BEAT_TICKS);
                }
            }
        }
    }

    public static List<Item> getBoundBoardMenuFoods(ServerWorld sw, BlockPos machinePos) {
        return getMenuFoodsForMachine(sw, machinePos);
    }

    public static List<Item> getMenuFoodsForMachine(ServerWorld sw, BlockPos machinePos) {
        if (sw.getBlockState(machinePos).getBlock() == cn.breezeth.ordertocook.registry.ModBlocks.ORDER_MACHINE
                && sw.getBlockEntity(machinePos) instanceof OrderMachineBlockEntity be) {
            return be.getMenuFoods(sw);
        }
        long boardPos = findBestBoardPos(sw, machinePos);
        if (boardPos == Long.MIN_VALUE) return List.of();
        return getMenuFoodsForBoardPos(sw, boardPos);
    }

    private List<Item> getMenuFoods(ServerWorld sw) {
        long boardPos = ensureBoundBoard(sw);
        if (boardPos == Long.MIN_VALUE) return List.of();
        return getMenuFoodsForBoardPos(sw, boardPos);
    }

    private long ensureBoundBoard(ServerWorld sw) {
        if (boundBoardPos != Long.MIN_VALUE) {
            BlockPos p = BlockPos.fromLong(boundBoardPos);
            if (sw.getBlockState(p).getBlock() == cn.breezeth.ordertocook.registry.ModBlocks.BOARD
                    && sw.getBlockEntity(p) instanceof BoardBlockEntity) {
                return boundBoardPos;
            }
        }
        long now = sw.getTime();
        if (lastBoundBoardScanTick != Long.MIN_VALUE
                && now - lastBoundBoardScanTick < ModConstants.SCAN_CACHE_INTERVAL_TICKS) {
            return boundBoardPos;
        }
        long found = findBestBoardPos(sw, this.pos);
        boundBoardPos = found;
        lastBoundBoardScanTick = now;
        markDirty();
        return found;
    }

    private static long findBestBoardPos(ServerWorld sw, BlockPos machinePos) {
        int radius = ModConstants.BOARD_SCAN_RADIUS;
        BlockPos center = machinePos;
        BlockPos.Mutable m = new BlockPos.Mutable();
        int bestNutrition = -1;
        double bestDist = Double.MAX_VALUE;
        long bestPos = Long.MIN_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int dy = -ModConstants.SCAN_VERTICAL; dy <= ModConstants.SCAN_VERTICAL; dy++) {
                    int y = center.getY() + dy;
                    if (y < sw.getBottomY() || y >= sw.getTopY()) continue;
                    m.set(center.getX() + dx, y, center.getZ() + dz);
                    if (sw.getBlockState(m).getBlock() != cn.breezeth.ordertocook.registry.ModBlocks.BOARD) continue;
                    if (!(sw.getBlockEntity(m) instanceof BoardBlockEntity be)) continue;
                    int nutrition = be.getTotalNutrition();
                    double dist = m.getSquaredDistance(center);
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

    private static List<Item> getMenuFoodsForBoardPos(ServerWorld sw, long boardPos) {
        if (boardPos == Long.MIN_VALUE) return List.of();
        BlockPos p = BlockPos.fromLong(boardPos);
        if (sw.getBlockState(p).getBlock() != cn.breezeth.ordertocook.registry.ModBlocks.BOARD) return List.of();
        if (!(sw.getBlockEntity(p) instanceof BoardBlockEntity be)) return List.of();

        ArrayList<Item> list = new ArrayList<>();
        for (ItemStack s : be.getItems()) {
            if (s.isEmpty()) continue;
            int nutrition = ConfigManager.getCustomMenuNutrition(s);
            if (nutrition <= 0) {
                FoodComponent fc = s.getItem().getFoodComponent();
                nutrition = (fc != null) ? fc.getHunger() : 0;
            }
            if (nutrition <= 0) continue;
            list.add(s.getItem());
        }
        return list;
    }

    public int getLevel() {
        return level;
    }

    public boolean isActive() {
        return active;
    }

    private static int getUnlockedSlots(int level) {
        return switch (level) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 3;
            case 4 -> 4;
            default -> 5;
        };
    }

    public boolean tryUpgrade(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity sp)) return false;
        if (level >= MAX_LEVEL) return false;

        if (this.world == null || !(this.world instanceof ServerWorld sw)) return false;

        int nextLevel = level + 1;
        int requiredHunger = requiredBoardHunger(nextLevel);
        if (requiredHunger > 0) {
            int maxHunger = nearbyBoardMaxHunger(sw);
            if (maxHunger < requiredHunger) {
                sp.closeHandledScreen();
                sp.sendMessage(Text.translatable("message.ordertocook.upgrade_not_enough_board_hunger", maxHunger, requiredHunger), true);
                return false;
            }
        }

        int cost = upgradeCost(nextLevel);
        try {
            if (!CoinUtils.tryConsumeWithChange(sp, cost)) {
                sp.closeHandledScreen();
                sp.sendMessage(Text.translatable("message.ordertocook.upgrade_not_enough"), true);
                return false;
            }
        } catch (Throwable t) {
            cn.breezeth.ordertocook.OrderToCookMod.LOGGER.error("Failed to consume coins while upgrading order machine", t);
            sp.closeHandledScreen();
            sp.sendMessage(Text.translatable("message.ordertocook.upgrade_failed_currency").formatted(Formatting.RED), false);
            return false;
        }
        level = nextLevel;
        // 升级成功后刷新受益等级，避免打开界面未关闭时出现误提示
        int eff = computeEffectiveBenefitLevel(sw);
        effectiveBenefitLevel = eff;
        markDirty();
        if (this.world != null) {
            this.world.playSound(null, this.pos, ModSounds.ORDER_REFRESH, SoundCategory.BLOCKS, 1.0f, 1.2f);
        }
        if (level >= MAX_LEVEL) {
            grantAdvancement(sp, new Identifier(ModConstants.MOD_ID, "ordertocook/eight_star_restaurant"));
        }
        // Keep timer progress and wait for next refresh
        if (this.world instanceof ServerWorld) {
            cn.breezeth.ordertocook.core.RestaurantRegistry.update(this);
        }
        return true;
    }

    private static void grantAdvancement(ServerPlayerEntity player, Identifier advancementId) {
        if (player.getServer() == null) return;
        var advancement = player.getServer().getAdvancementLoader().get(advancementId);
        if (advancement == null) return;
        AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancement);
        if (progress.isDone()) return;
        for (String criterion : progress.getUnobtainedCriteria()) {
            player.getAdvancementTracker().grantCriterion(advancement, criterion);
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

    private static double walkInChance(int level) {
        var cfg = ConfigManager.get();
        return switch (level) {
            case 1 -> cfg.walkInChanceLevel1;
            case 2 -> cfg.walkInChanceLevel2;
            case 3 -> cfg.walkInChanceLevel3;
            case 4 -> cfg.walkInChanceLevel4;
            case 5 -> cfg.walkInChanceLevel5;
            case 6 -> cfg.walkInChanceLevel6;
            case 7 -> cfg.walkInChanceLevel7;
            case 8 -> cfg.walkInChanceLevel8;
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

    private int nearbyBoardMaxHunger(ServerWorld sw) {
        long now = sw.getTime();
        if (lastNearbyBoardScanTick != Long.MIN_VALUE
                && now - lastNearbyBoardScanTick < ModConstants.SCAN_CACHE_INTERVAL_TICKS) {
            return cachedNearbyBoardMaxHunger;
        }
        int radius = ModConstants.BOARD_SCAN_RADIUS;
        BlockPos center = this.pos;
        BlockPos.Mutable m = new BlockPos.Mutable();
        int max = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int dy = -ModConstants.SCAN_VERTICAL; dy <= ModConstants.SCAN_VERTICAL; dy++) {
                    int y = center.getY() + dy;
                    if (y < sw.getBottomY() || y >= sw.getTopY()) continue;
                    m.set(center.getX() + dx, y, center.getZ() + dz);
                    if (sw.getBlockState(m).getBlock() != cn.breezeth.ordertocook.registry.ModBlocks.BOARD) continue;
                    if (!(sw.getBlockEntity(m) instanceof BoardBlockEntity be)) continue;
                    max = Math.max(max, be.getTotalNutrition());
                }
            }
        }
        cachedNearbyBoardMaxHunger = max;
        lastNearbyBoardScanTick = now;
        return max;
    }

    public boolean tryToggleActive(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity)) return false;
        boolean target = !active;
        if (target) {
            ServerPlayerEntity sp = (ServerPlayerEntity) player;
            if (!hasChairAround(sp)) {
                sp.closeHandledScreen();
                player.sendMessage(Text.translatable("message.ordertocook.no_chair").formatted(Formatting.RED), true);
                return false;
            }
        }
        active = target;
        if (!active) {
            inventory.clear();
        }
        markDirty();
        return true;
    }

    private boolean hasChairAround(ServerPlayerEntity player) {
        if (this.world == null) return false;
        if (!(this.world instanceof ServerWorld sw)) return false;
        long now = sw.getTime();
        if (lastChairScanTick != Long.MIN_VALUE
                && now - lastChairScanTick < ModConstants.SCAN_CACHE_INTERVAL_TICKS) {
            return cachedHasChairAround;
        }
        int radius = ModConstants.CHAIR_SCAN_RADIUS;
        BlockPos center = this.pos;
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int dy = -ModConstants.SCAN_VERTICAL; dy <= ModConstants.SCAN_VERTICAL; dy++) {
                    int y = center.getY() + dy;
                    if (y < sw.getBottomY() || y >= sw.getTopY()) continue;
                    m.set(center.getX() + dx, y, center.getZ() + dz);
                    if (sw.getBlockState(m).getBlock() == cn.breezeth.ordertocook.registry.ModBlocks.CHAIR) {
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
        if (this.world == null || this.world.isClient) return;
        ServerWorld sw = (ServerWorld) this.world;
        BlockState state = sw.getBlockState(this.pos);
        if (state.getBlock() instanceof OrderMachineBlock) {
            BlockState idle = state.with(OrderMachineBlock.MODE, OrderMachineBlock.Mode.IDLE)
                                   .with(OrderMachineBlock.FRAME, 0);
            sw.setBlockState(this.pos, idle, Block.NOTIFY_ALL);
        }
    }

    public void recordAcceptedOrder(ServerPlayerEntity player, NbtCompound nbt) {
        markDirty();
    }

    public void recordCompletedProfit(ServerPlayerEntity player, NbtCompound nbt, int earnedCoin) {
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
        markDirty();
    }

    public void setOwnerIfEmpty(ServerPlayerEntity player) {
        if (restaurantOwner == null || restaurantOwner.isEmpty()) {
            restaurantOwner = player.getGameProfile().getName();
            markDirty();
        }
    }

    public boolean tryRename(ServerPlayerEntity player, String name) {
        String owner = restaurantOwner == null ? "" : restaurantOwner;
        String pn = player.getGameProfile().getName();
        if (!owner.isEmpty() && !owner.equals(pn)) {
            player.closeHandledScreen();
            player.sendMessage(Text.translatable("message.ordertocook.rename_not_owner").formatted(Formatting.RED), true);
            return false;
        }
        String cur = restaurantName == null ? "" : restaurantName;
        boolean first = cur.isEmpty();
        if (!first) {
            try {
                if (!CoinUtils.tryConsumeWithChange(player, 1)) {
                    player.closeHandledScreen();
                    player.sendMessage(Text.translatable("message.ordertocook.rename_not_enough"), true);
                    return false;
                }
            } catch (Throwable t) {
                cn.breezeth.ordertocook.OrderToCookMod.LOGGER.error("Failed to consume coins while renaming order machine", t);
                player.closeHandledScreen();
                player.sendMessage(Text.translatable("message.ordertocook.rename_failed_currency").formatted(Formatting.RED), false);
                return false;
            }
        }
        restaurantName = name == null ? "" : name;
        markDirty();
        return true;
    }

    public RestaurantStats snapshotStats() {
        return new RestaurantStats(
                this.world instanceof ServerWorld sw ? sw.getRegistryKey().getValue().toString() : "",
                this.pos.asLong(),
                restaurantName == null ? "" : restaurantName,
                restaurantOwner == null ? "" : restaurantOwner,
                level,
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
        markDirty();
    }

    public void setPlaced(boolean placed) {
        this.isPlaced = placed;
        markDirty();
    }

    public boolean isPlaced() {
        return isPlaced;
    }

    public int ensureMachineId(ServerWorld sw) {
        if (machineId > 0) return machineId;
        int id = cn.breezeth.ordertocook.core.MachineRankingState.get(sw).allocateId();
        this.machineId = id;
        markDirty();
        cn.breezeth.ordertocook.core.RestaurantRegistry.registerById(sw, id, this);
        return id;
    }

    public void forceRefresh() {
        refreshOrders();
        progress = 0;
    }

    private int computeEffectiveBenefitLevel(ServerWorld sw) {
        int maxHunger = nearbyBoardMaxHunger(sw);
        int l = this.level;
        while (l > 0 && requiredBoardHunger(l) > maxHunger) {
            l--;
        }
        return l;
    }

    private void applyBenefitLevelToOrders(int eff) {
        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack s = this.inventory.get(i);
            if (s.isEmpty()) continue;
            NbtCompound nbt = DataCompat.copy(s);
            if (nbt == null) continue;
            int coin = cn.breezeth.ordertocook.core.OrderGenerator.recalcPrestigeFromNbt(nbt, eff);
            nbt.putInt(ModConstants.NBT_PRESTIGE, coin);
            DataCompat.set(s, nbt);
            this.inventory.set(i, s);
        }
        markDirty();
    }
}
