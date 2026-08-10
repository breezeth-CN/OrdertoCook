package cn.breezeth.ordertocook.core;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.ai.attributes.Attributes;


import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.compat.TouhouLittleMaidCustomerCompat;
import cn.breezeth.ordertocook.registry.ModBlocks;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.registry.ModEntities;
import net.minecraft.world.entity.decoration.ArmorStand;




public final class OrderNpcManager {
    private static final ConcurrentHashMap<String, UUID> orderToNpc = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, PendingCompatCompletion> pendingCompatCompletions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, PendingTouhouSpawn> pendingTouhouSpawns = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Integer> pendingTouhouSwingStages = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, PendingBoomBonusData> pendingTouhouBoomBonuses = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> deliverySpawnSearchTicks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> pendingPassengerSyncs = new ConcurrentHashMap<>();
    private static final String TAG_NPC = "otc_npc";
    private static final String TAG_ORDER_PREFIX = "otc_order:";
    private static final String TAG_ORDER_EXPIRY_TICK_PREFIX = "otc_order_expiry_tick:";
    private static final String TAG_ORDER_EXPIRY_SYS_PREFIX = "otc_order_expiry_sys:";
    private static final String TAG_TOUHOU_SPAWN = "otc_touhou_spawn";
    private static final String TAG_TOUHOU_SPAWN_START_PREFIX = "otc_touhou_spawn_start:";
    private static final String TAG_TOUHOU_SPAWN_FINISH_PREFIX = "otc_touhou_spawn_finish:";
    private static final String TAG_TOUHOU_SPAWN_SEATED_PREFIX = "otc_touhou_spawn_seated:";
    private static final String TAG_TOUHOU_COMPLETION = "otc_touhou_completion";
    private static final String TAG_TOUHOU_COMPLETION_ORDER_PREFIX = "otc_touhou_completion_order:";
    private static final String TAG_TOUHOU_COMPLETION_START_PREFIX = "otc_touhou_completion_start:";
    private static final String TAG_TOUHOU_COMPLETION_FINISH_PREFIX = "otc_touhou_completion_finish:";
    private static final String TAG_TOUHOU_COMPLETION_EATING_PREFIX = "otc_touhou_completion_eating:";
    private static final String TAG_TOUHOU_COMPLETION_ANIMATION_PREFIX = "otc_touhou_completion_animation:";

    public static final String TAG_WALKIN = "otc_walkin";
    public static final String TAG_WALKIN_SPAWN_TIME = "otc_walkin_spawn_time:"; // Stores world time ticks
    public static final String TAG_WALKIN_SPAWN_SYSTEM_TIME = "otc_walkin_spawn_sys:"; // Stores system millis
    public static final String TAG_WALKIN_MACHINE_POS_PREFIX = "otc_walkin_machine_pos:";
    public static final String TAG_WALKIN_BOARD_POS_PREFIX = "otc_walkin_board_pos:";
    public static final String TAG_CHAIR_SEAT = "otc_chair_seat";
    public static final String TAG_CHAIR_SEAT_POS_PREFIX = "otc_chair_seat_pos:";
    public static final String TAG_CHAIR_CUSTOMER = "otc_chair_customer";
    public static final String TAG_CHAIR_CUSTOMER_POS_PREFIX = "otc_chair_customer_pos:";
    private static final double SEAT_Y_OFFSET = 1.0 - (9.0 / 16.0) + 0.6;
    // Same absolute seat height as the two Fabric builds: about 8/16 above the chair base.
    private static final double TOUHOU_MAID_SEAT_Y_OFFSET = SEAT_Y_OFFSET - (9.0 / 16.0);
    public enum CompletionAnimation {
        EAT_BOOM,
        EAT_SCALE,
        SIT_TAKE,
        STAND_TAKE
    }

    public record PendingBoomBonusData(
            String playerUuid,
            int machineId,
            boolean delivery,
            boolean longDistance,
            int deliveryDistance,
            boolean walkIn,
            String restaurantName,
            String customerName,
            int bonusCoin
    ) {
    }

    public static void setPendingTouhouBoomBonus(UUID npcUuid, PendingBoomBonusData data) {
        if (npcUuid == null || data == null || data.bonusCoin() <= 0) {
            return;
        }
        pendingTouhouBoomBonuses.put(npcUuid, data);
    }

    public static void clearPendingTouhouBoomBonus(UUID npcUuid) {
        if (npcUuid != null) {
            pendingTouhouBoomBonuses.remove(npcUuid);
        }
    }

    public static boolean spawnWalkIn(ServerLevel world, BlockPos machinePos, int level) {
        return WalkInNpcManager.spawn(world, machinePos, level);
    }

    public static void checkWalkInDespawn(ServerLevel world) {
        WalkInNpcManager.checkDespawn(world);
    }

    public static boolean acquireDeliverySpawnSearchBudget(ServerLevel world) {
        String dimension = world.dimension().location().toString();
        long tick = world.getGameTime();
        Long previous = deliverySpawnSearchTicks.put(dimension, tick);
        return previous == null || previous.longValue() != tick;
    }

    public static void tickCompletionAnimations(ServerLevel world) {
        boolean recoveryScan = world.getGameTime() % 20L == 0L;
        tickPendingPassengerSyncs(world);
        tickPendingTouhouSpawns(world, recoveryScan);
        tickPendingCompatCompletions(world, recoveryScan);
        if (recoveryScan) tickTouhouIdleAnimations(world);
    }

    private static void tickTouhouIdleAnimations(ServerLevel world) {
        for (net.minecraft.world.entity.Entity entity : world.getAllEntities()) {
            if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) {
                continue;
            }
            if (!TouhouLittleMaidCustomerCompat.isTouhouMaid(living)) {
                continue;
            }
            if (living.getTags().contains(TAG_TOUHOU_COMPLETION)) {
                continue;
            }
            if (isChairCustomer(living)) {
                TouhouLittleMaidCustomerCompat.tickIdleSitAnimation(living);
            } else {
                TouhouLittleMaidCustomerCompat.stopIdleAnimation(living);
            }
        }
    }

    public static void checkOrderNpcDespawn(ServerLevel world) {
        long nowTick = world.getGameTime();
        long nowSys = System.currentTimeMillis();
        for (net.minecraft.world.entity.Entity entity : world.getAllEntities()) {
            if (entity instanceof net.minecraft.world.entity.LivingEntity living
                    && living.getTags().contains(TAG_NPC)) {
                OrderNpcRegistry.register(living.getUUID());
            }
        }
        java.util.List<net.minecraft.world.entity.LivingEntity> toDespawn = new java.util.ArrayList<>();
        for (java.util.UUID id : OrderNpcRegistry.ids()) {
            net.minecraft.world.entity.Entity entity = world.getEntity(id);
            if (!(entity instanceof net.minecraft.world.entity.LivingEntity le)) continue;
            if (!le.getTags().contains(TAG_NPC)) continue;
            if (le.getTags().contains(TAG_WALKIN)) continue;
            String orderId = null;
            long expiryTick = -1L;
            long expirySys = -1L;
            String expirySysTag = null;
            for (String tag : le.getTags()) {
                if (orderId == null && tag.startsWith(TAG_ORDER_PREFIX)) {
                    orderId = tag.substring(TAG_ORDER_PREFIX.length());
                    continue;
                }
                if (expiryTick == -1L && tag.startsWith(TAG_ORDER_EXPIRY_TICK_PREFIX)) {
                    try {
                        expiryTick = Long.parseLong(tag.substring(TAG_ORDER_EXPIRY_TICK_PREFIX.length()));
                    } catch (NumberFormatException ignored) {
                        expiryTick = -1L;
                    }
                    continue;
                }
                if (expirySys == -1L && tag.startsWith(TAG_ORDER_EXPIRY_SYS_PREFIX)) {
                    try {
                        expirySys = Long.parseLong(tag.substring(TAG_ORDER_EXPIRY_SYS_PREFIX.length()));
                    } catch (NumberFormatException ignored) {
                        expirySys = -1L;
                    }
                    expirySysTag = tag;
                }
            }
            if (orderId == null || orderId.isBlank()) continue;
            if (expiryTick < 0L && expirySys >= 0L) {
                long remainingMs = expirySys - nowSys;
                long convertedTick;
                if (remainingMs <= 0L) {
                    convertedTick = nowTick - 1L;
                } else {
                    long remainingTicks = (remainingMs + 49L) / 50L;
                    convertedTick = nowTick + remainingTicks;
                }
                stripExpiryTags(le);
                le.addTag(TAG_ORDER_EXPIRY_TICK_PREFIX + convertedTick);
                expiryTick = convertedTick;
                if (expirySysTag != null) le.removeTag(expirySysTag);
            }
            boolean expired = false;
            if (expiryTick >= 0L) {
                expired = nowTick >= expiryTick;
            }
            if (expired) {
                toDespawn.add(le);
            }
        }
        for (net.minecraft.world.entity.LivingEntity le : toDespawn) {
            if (pendingCompatCompletions.containsKey(le.getUUID())) {
                continue;
            }
            String orderId = null;
            for (String tag : le.getTags()) {
                if (tag.startsWith(TAG_ORDER_PREFIX)) {
                    orderId = tag.substring(TAG_ORDER_PREFIX.length());
                    break;
                }
            }
            if (orderId == null || orderId.isBlank()) continue;
            orderToNpc.remove(orderId);
            despawnEntity(world, null, orderId, le, DespawnReason.EXPIRED);
        }
    }

    static BlockPos findEmptyChair(ServerLevel world, BlockPos machinePos) {
        int radius = 24;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int minY = machinePos.getY() - 8;
        int maxY = machinePos.getY() + 8;
        int worldMinY = world.getMinBuildHeight();
        int worldMaxY = world.getMaxBuildHeight();
        if (minY < worldMinY) minY = worldMinY;
        if (maxY > worldMaxY) maxY = worldMaxY;
        
        java.util.List<BlockPos> candidates = new java.util.ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                int x = machinePos.getX() + dx;
                int z = machinePos.getZ() + dz;
                for (int y = maxY; y >= minY; y--) {
                    mutable.set(x, y, z);
                    if (world.getBlockState(mutable).getBlock() != ModBlocks.CHAIR.get()) {
                        continue;
                    }
                    BlockPos spawn = mutable.above();
                    if (!isHeadClear(world, spawn)) continue;
                    
                    if (isChairOccupied(world, mutable)) continue;
                    
                    candidates.add(spawn);
                }
            }
        }
        
        if (candidates.isEmpty()) return null;
        return candidates.get(world.random.nextInt(candidates.size()));
    }

    private static boolean isChairOccupied(ServerLevel world, BlockPos chairPos) {
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(chairPos).inflate(0.0, 1.0, 0.0);
        
        java.util.List<SeatEntity> seatEntities = world.getEntitiesOfClass(
                SeatEntity.class,
                box,
                e -> true
        );
        for (var s : seatEntities) {
            if (s.isVehicle()) return true;
        }

        java.util.List<net.minecraft.world.entity.decoration.ArmorStand> seats = world.getEntitiesOfClass(
                net.minecraft.world.entity.decoration.ArmorStand.class,
                box,
                e -> true
        );
        for (var s : seats) {
            if (s.isVehicle()) return true;
        }
        
        java.util.List<net.minecraft.world.entity.LivingEntity> others = world.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                box,
                e -> !(e instanceof net.minecraft.world.entity.decoration.ArmorStand)
        );
        if (!others.isEmpty()) return true;

        return false;
    }

    static void setupWalkInTeam(ServerLevel world, net.minecraft.world.entity.LivingEntity entity) {
        net.minecraft.world.scores.Scoreboard scoreboard = world.getScoreboard();
        String teamName = "otc_npc_walkin";
        net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setColor(ChatFormatting.YELLOW);
            team.setCollisionRule(net.minecraft.world.scores.Team.CollisionRule.NEVER);
        }
        scoreboard.addPlayerToTeam(entity.getScoreboardName(), team);
    }

    static void setupOrderTeam(ServerLevel world, net.minecraft.world.entity.LivingEntity entity) {
        net.minecraft.world.scores.Scoreboard scoreboard = world.getScoreboard();
        String teamName = "otc_npc_order";
        net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setColor(ChatFormatting.WHITE);
            team.setCollisionRule(net.minecraft.world.scores.Team.CollisionRule.NEVER);
        }
        scoreboard.addPlayerToTeam(entity.getScoreboardName(), team);
    }
    
    public static void changeToNormalTeam(ServerLevel world, net.minecraft.world.entity.LivingEntity entity) {
        // Just remove from walkin team, or add to normal glowing team
        // If we want white, we can remove from all teams or add to a white team.
        // The original code adds to "otc_npc_glowing" (Gold).
        // Requirement says "outline color changes back to white".
        // Default glowing color is white if not on a team with color.
        // So we can just remove it from team, or add to a white team.
        // Let's add to a white team to be explicit, or just remove team.
        // If we remove team, collision rule might be lost. So better add to a White/NoColor team with CollisionRule.NEVER.
        
        net.minecraft.world.scores.Scoreboard scoreboard = world.getScoreboard();
        String teamName = "otc_npc_normal";
        net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setColor(ChatFormatting.WHITE);
            team.setCollisionRule(net.minecraft.world.scores.Team.CollisionRule.NEVER);
        }
        scoreboard.addPlayerToTeam(entity.getScoreboardName(), team);
    }

    public enum DespawnReason {
        COMPLETED,
        EXPIRED
    }

    public static void spawnForNormal(ServerLevel world, Player player, BlockPos machinePos, String orderId, String customerName) {
        NormalOrderNpcManager.spawn(world, player, machinePos, orderId, customerName, -1L, -1L, null);
    }

    public static boolean spawnForDelivery(ServerLevel world, Player player, BlockPos targetPos, String orderId, String customerName, long expiryTick, long expirySys) {
        targetPos = cn.breezeth.ordertocook.api.PhysicalRestaurantCompatApi.toWorldPosition(world, targetPos);
        FailureCounters counters = new FailureCounters();
        BlockPos pos = findSpawnPosDelivery(world, targetPos, counters);
        if (pos == null) {
            player.displayClientMessage(Component.translatable("message.ordertocook.npc_respawn_try").withStyle(ChatFormatting.AQUA), false);
            // retry within 10 radius (approx 20x20)
            pos = tryFindDrySpot(world, targetPos, 10, 64, counters);
            if (pos == null) {
                // retry within 50 radius (approx 100x100)
                pos = tryFindDrySpot(world, targetPos, 50, 256, counters);
            }
            if (pos == null) {
                if (ConfigManager.isDevModeEnabled()) {
                    player.displayClientMessage(Component.translatable("message.ordertocook.npc_spawn_fail_delivery", counters.describeForDelivery()).withStyle(ChatFormatting.RED), false);
                }
                return false;
            }
        }
        net.minecraft.world.entity.LivingEntity npc = createNpc(world, pos, customerName);
        if (npc == null) return false;
        setupOrderTeam(world, npc);
        tagNpc(player, orderId, expiryTick, expirySys, npc);
        addMapping(player, orderId, npc);
        if (pos.getX() != targetPos.getX() || pos.getZ() != targetPos.getZ()) {
            player.displayClientMessage(Component.translatable("message.ordertocook.npc_respawn_success", customerName, String.valueOf(pos.getX()), String.valueOf(pos.getZ())).withStyle(ChatFormatting.AQUA), false);
        }
        if (ConfigManager.isDevModeEnabled()) {
            Component typeName = Component.translatable(npc.getType().getDescriptionId());
            player.displayClientMessage(Component.translatable("message.ordertocook.npc_spawn_delivery", orderId, typeName, pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.AQUA), false);
        }
        return true;
    }

    public static boolean spawnForDelivery(
            ServerLevel world,
            Player player,
            BlockPos targetPos,
            String orderId,
            String customerName,
            long expiryTick,
            long expirySys,
            net.minecraft.nbt.CompoundTag customerData
    ) {
        return spawnForDelivery(world, player, targetPos, orderId, customerName, expiryTick, expirySys);
    }

    public static void despawnFor(ServerLevel world, Player player, String orderId, boolean delivered) {
        despawnFor(world, player, orderId, delivered ? DespawnReason.COMPLETED : DespawnReason.EXPIRED);
    }

    public static void despawnFor(ServerLevel world, Player player, String orderId, DespawnReason reason) {
        UUID id = orderToNpc.remove(orderId);
        net.minecraft.world.entity.LivingEntity npc = null;
        if (id != null) {
            net.minecraft.world.entity.Entity e = world.getEntity(id);
            if (e instanceof net.minecraft.world.entity.LivingEntity le) {
                npc = le;
            }
        }
        if (npc == null) {
            BlockPos center = player != null ? player.blockPosition() : null;
            if (center != null) {
                npc = findNpcByTags(world, center, 64, orderId);
            }
        }
        if (npc == null) {
            if (ConfigManager.isDevModeEnabled() && player != null) {
                player.displayClientMessage(Component.translatable("message.ordertocook.npc_despawn_missing", orderId).withStyle(ChatFormatting.RED), false);
            }
            return;
        }
        despawnEntity(world, player, orderId, npc, reason);
    }

    public static void beginCompletedAnimation(
            ServerLevel world,
            Player player,
            String orderId,
            net.minecraft.world.entity.Entity npcEntity,
            CompletionAnimation animation
    ) {
        net.minecraft.world.entity.LivingEntity npc = null;
        if (npcEntity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
            npc = livingEntity;
        } else if (npcEntity instanceof ArmorStand armorStandEntity
                && armorStandEntity.getFirstPassenger() instanceof net.minecraft.world.entity.LivingEntity livingPassenger) {
            npc = livingPassenger;
        }
        if (npc == null) {
            despawnFor(world, player, orderId, DespawnReason.COMPLETED);
            return;
        }
        orderToNpc.remove(orderId);
        clearCompletionTags(npc);
        net.minecraft.world.entity.Entity vehicle = npc.getVehicle();
        if (vehicle != null) {
            clearCompletionTags(vehicle);
        }
        if (ConfigManager.isDevModeEnabled() && player != null) {
            player.displayClientMessage(Component.translatable("message.ordertocook.npc_despawn", orderId, reasonText(DespawnReason.COMPLETED)).withStyle(ChatFormatting.GRAY), false);
        }
        if (TouhouLittleMaidCustomerCompat.isTouhouMaid(npc)) {
            beginTouhouMaidCompletion(world, orderId, npc, animation);
            return;
        }
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, npc.getX(), npc.getY() + 0.5, npc.getZ(), 30, 0.3, 0.5, 0.3, 0.1);
        world.playSound(
                null,
                npc.blockPosition(),
                getRandomPositiveVillagerSound(world),
                net.minecraft.sounds.SoundSource.NEUTRAL,
                0.9f,
                0.95f + world.random.nextFloat() * 0.1f
        );
        if (npc instanceof cn.breezeth.ordertocook.entity.CustomerEntity customerEntity) {
            switch (animation) {
                case EAT_BOOM -> customerEntity.startEatBoomAnimation();
                case EAT_SCALE -> customerEntity.startEatScaleAnimation();
                case SIT_TAKE -> customerEntity.startSitTakeAnimation();
                case STAND_TAKE -> customerEntity.startStandTakeAnimation();
            }
            return;
        }
        despawnEntity(world, player, orderId, npc, DespawnReason.COMPLETED);
    }

    private static void beginTouhouMaidCompletion(ServerLevel world, String orderId, net.minecraft.world.entity.LivingEntity npc, CompletionAnimation animation) {
        boolean eating = animation == CompletionAnimation.EAT_BOOM || animation == CompletionAnimation.EAT_SCALE;
        boolean seated = isChairCustomer(npc);
        if (eating && seated) {
            standMaidOnChairForEating(npc);
            seated = false;
        }
        int durationTicks = switch (animation) {
            case EAT_BOOM -> 120;
            case EAT_SCALE -> 110;
            case SIT_TAKE -> 40;
            case STAND_TAKE -> 60;
        };
        TouhouLittleMaidCustomerCompat.startCompletionAnimation(npc, eating, seated);
        long startTick = world.getGameTime();
        long finishTick = startTick + durationTicks;
        PendingCompatCompletion pending = new PendingCompatCompletion(orderId, startTick, finishTick, eating, animation);
        pendingCompatCompletions.put(npc.getUUID(), pending);
        pendingTouhouSwingStages.put(npc.getUUID(), 0);
        writeTouhouCompletionTags(npc, pending);
        if (ConfigManager.isDevModeEnabled()) {
            cn.breezeth.ordertocook.OrderToCookMod.LOGGER.info("[OTC Dev] Touhou maid completion start orderId={} uuid={} animation={} duration={}", orderId, npc.getUUID(), animation, durationTicks);
        }
    }

    public static Component interruptTouhouMaidEatingNear(ServerLevel world, BlockPos platePos) {
        java.util.List<net.minecraft.world.entity.LivingEntity> maids = world.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                new net.minecraft.world.phys.AABB(platePos).inflate(3.0),
                entity -> TouhouLittleMaidCustomerCompat.isTouhouMaid(entity) && isTouhouMaidEating(entity)
        );
        if (maids.isEmpty()) {
            return null;
        }
        net.minecraft.world.entity.LivingEntity maid = maids.get(0);
        String orderId = "touhou";
        PendingCompatCompletion previous = pendingCompatCompletions.get(maid.getUUID());
        if (previous == null) {
            previous = readTouhouCompletionTags(maid);
        }
        if (previous != null) {
            orderId = previous.orderId();
        }

        pendingTouhouBoomBonuses.remove(maid.getUUID());
        clearTouhouCompletionTags(maid);
        long startTick = world.getGameTime();
        PendingCompatCompletion pending = new PendingCompatCompletion(orderId, startTick, startTick + 10L, true, CompletionAnimation.EAT_SCALE);
        pendingCompatCompletions.put(maid.getUUID(), pending);
        writeTouhouCompletionTags(maid, pending);
        TouhouLittleMaidCustomerCompat.startCompletionAnimation(maid, true, isChairCustomer(maid));
        return maid.getCustomName() != null
                ? maid.getCustomName()
                : Component.translatable("keyword.ordertocook.customer");
    }

    private static boolean isTouhouMaidEating(net.minecraft.world.entity.LivingEntity entity) {
        PendingCompatCompletion pending = pendingCompatCompletions.get(entity.getUUID());
        if (pending == null) {
            pending = readTouhouCompletionTags(entity);
        }
        return pending != null && pending.eating();
    }

    private static void beginTouhouSpawn(ServerLevel world, net.minecraft.world.entity.LivingEntity npc, boolean seated) {
        long startTick = world.getGameTime();
        PendingTouhouSpawn pending = new PendingTouhouSpawn(startTick, startTick + 12L, seated);
        pendingTouhouSpawns.put(npc.getUUID(), pending);
        writeTouhouSpawnTags(npc, pending);
        TouhouLittleMaidCustomerCompat.stopIdleAnimation(npc);
        TouhouLittleMaidCustomerCompat.setSitting(npc, false);
        world.playSound(null, npc.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.55f, 1.35f);
        world.sendParticles(ParticleTypes.PORTAL, npc.getX(), npc.getY() + 0.65, npc.getZ(), 24, 0.24, 0.35, 0.24, 0.09);
    }

    private static void tickPendingTouhouSpawns(ServerLevel world, boolean recoveryScan) {
        java.util.HashSet<UUID> ids = new java.util.HashSet<>(pendingTouhouSpawns.keySet());
        if (recoveryScan) {
            for (net.minecraft.world.entity.Entity entity : world.getAllEntities()) {
                if (entity.getTags().contains(TAG_TOUHOU_SPAWN)) ids.add(entity.getUUID());
            }
        }
        long now = world.getGameTime();
        for (UUID uuid : ids) {
            net.minecraft.world.entity.Entity entity = world.getEntity(uuid);
            if (!(entity instanceof net.minecraft.world.entity.LivingEntity npc) || npc.isRemoved()) {
                pendingTouhouSpawns.remove(uuid);
                continue;
            }
            PendingTouhouSpawn pending = pendingTouhouSpawns.get(uuid);
            if (pending == null) {
                pending = readTouhouSpawnTags(npc);
                if (pending == null) {
                    continue;
                }
                pendingTouhouSpawns.put(uuid, pending);
            }
            long elapsed = now - pending.startTick();
            TouhouLittleMaidCustomerCompat.setSitting(npc, false);
            if (elapsed >= 0L && elapsed < 12L && elapsed % 3L == 0L) {
                world.sendParticles(ParticleTypes.CLOUD, npc.getX(), npc.getY() + 0.25 + elapsed * 0.035, npc.getZ(), 5, 0.18, 0.14, 0.18, 0.025);
            }
            if (now < pending.finishTick()) {
                continue;
            }
            pendingTouhouSpawns.remove(uuid);
            clearTouhouSpawnTags(npc);
            if (pending.seated() && isChairCustomer(npc)) {
                TouhouLittleMaidCustomerCompat.startIdleSitAnimation(npc);
            } else {
                TouhouLittleMaidCustomerCompat.setSitting(npc, false);
            }
        }
    }

    private static void tickPendingCompatCompletions(ServerLevel world, boolean recoveryScan) {
        java.util.HashSet<UUID> ids = new java.util.HashSet<>(pendingCompatCompletions.keySet());
        if (recoveryScan) {
            for (UUID id : OrderNpcRegistry.ids()) {
                net.minecraft.world.entity.Entity entity = world.getEntity(id);
                if (entity != null && entity.getTags().contains(TAG_TOUHOU_COMPLETION)) ids.add(id);
            }
            for (net.minecraft.world.entity.Entity entity : world.getAllEntities()) {
                if (entity.getTags().contains(TAG_TOUHOU_COMPLETION)) ids.add(entity.getUUID());
            }
        }
        long now = world.getGameTime();
        for (UUID uuid : ids) {
            net.minecraft.world.entity.Entity entity = world.getEntity(uuid);
            if (!(entity instanceof net.minecraft.world.entity.LivingEntity npc) || npc.isRemoved()) {
                pendingCompatCompletions.remove(uuid);
                continue;
            }
            PendingCompatCompletion pending = pendingCompatCompletions.get(uuid);
            if (pending == null) {
                pending = readTouhouCompletionTags(npc);
                if (pending == null) {
                    clearTouhouCompletionTags(npc);
                    despawnEntity(world, null, "touhou_recovery", npc, DespawnReason.COMPLETED);
                    continue;
                }
                pendingCompatCompletions.put(uuid, pending);
            }
            long elapsed = now - pending.startTick();
            boolean seated = npc.isPassenger() && isChairCustomer(npc);
            TouhouLittleMaidCustomerCompat.maintainCompletionPose(npc, seated);
            if (pending.eating()) {
                TouhouLittleMaidCustomerCompat.faceFood(npc, getCustomerPlateDisplayPos(npc));
            }
            int swingStage = elapsed >= 100 ? 4 : elapsed >= 80 ? 3 : elapsed >= 60 ? 2 : elapsed >= 15 ? 1 : 0;
            int previousSwingStage = pendingTouhouSwingStages.getOrDefault(uuid, swingStage);
            pendingTouhouSwingStages.putIfAbsent(uuid, previousSwingStage);
            if (pending.eating() && swingStage > previousSwingStage) {
                boolean leftHand = (swingStage & 1) == 0;
                pendingTouhouSwingStages.put(uuid, swingStage);
                TouhouLittleMaidCustomerCompat.tickCompletionAnimation(npc, true, seated, leftHand);
            }
            if (pending.eating() && elapsed >= 45 && elapsed < 95 && elapsed % 7 == 0) {
                world.playSound(null, npc.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.7f, 0.9f + world.random.nextFloat() * 0.2f);
                world.sendParticles(ParticleTypes.HAPPY_VILLAGER, npc.getX(), npc.getY() + 0.75, npc.getZ(), 2, 0.18, 0.22, 0.18, 0.02);
            } else if (!pending.eating() && elapsed % 12 == 0) {
                TouhouLittleMaidCustomerCompat.tickCompletionAnimation(npc, false, seated);
            }
            if (pending.animation() == CompletionAnimation.SIT_TAKE && elapsed == 35) {
                world.playSound(null, npc.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0f, 1.0f);
                world.sendParticles(ParticleTypes.PORTAL, npc.getX(), npc.getY() + 0.8, npc.getZ(), 40, 0.35, 0.5, 0.35, 0.2);
            } else if (pending.animation() == CompletionAnimation.STAND_TAKE && elapsed == 55) {
                world.playSound(null, npc.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0f, 1.0f);
                world.sendParticles(ParticleTypes.PORTAL, npc.getX(), npc.getY() + 0.8, npc.getZ(), 40, 0.35, 0.5, 0.35, 0.2);
            }
            if (pending.animation() == CompletionAnimation.EAT_BOOM && elapsed == 97) {
                world.playSound(null, npc.blockPosition(), SoundEvents.CREEPER_PRIMED, SoundSource.NEUTRAL, 0.9f, 1.0f);
            }
            if (pending.animation() == CompletionAnimation.EAT_BOOM && elapsed == 118) {
                world.playSound(null, npc.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, npc.getX(), npc.getY() + 0.8, npc.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                world.sendParticles(ParticleTypes.CLOUD, npc.getX(), npc.getY() + 0.8, npc.getZ(), 18, 0.3, 0.4, 0.3, 0.05);
                grantPendingTouhouBoomBonus(world, uuid);
            } else if (pending.animation() == CompletionAnimation.EAT_SCALE && elapsed == getEatScaleSignalTick(pending)) {
                world.playSound(null, npc.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0f, 1.0f);
                world.sendParticles(ParticleTypes.PORTAL, npc.getX(), npc.getY() + 0.8, npc.getZ(), 40, 0.35, 0.5, 0.35, 0.2);
            }
            if (now < pending.finishTick()) {
                continue;
            }
            pendingCompatCompletions.remove(uuid);
            pendingTouhouSwingStages.remove(uuid);
            pendingTouhouBoomBonuses.remove(uuid);
            clearTouhouCompletionTags(npc);
            if (ConfigManager.isDevModeEnabled()) {
                cn.breezeth.ordertocook.OrderToCookMod.LOGGER.info("[OTC Dev] Touhou maid completion finish orderId={} uuid={}", pending.orderId(), uuid);
            }
            TouhouLittleMaidCustomerCompat.stopCompletionAnimation(npc);
            if (pending.animation() == CompletionAnimation.EAT_BOOM) {
                world.sendParticles(ParticleTypes.EXPLOSION, npc.getX(), npc.getY() + 0.8, npc.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            } else if (pending.animation() == CompletionAnimation.EAT_SCALE) {
                world.sendParticles(ParticleTypes.PORTAL, npc.getX(), npc.getY() + 0.8, npc.getZ(), 35, 0.25, 0.45, 0.25, 0.16);
            }
            despawnEntity(world, null, pending.orderId(), npc, DespawnReason.COMPLETED);
        }
    }

    private static long getEatScaleSignalTick(PendingCompatCompletion pending) {
        return pending.finishTick() - pending.startTick() <= 10L ? 5L : 105L;
    }

    private static void grantPendingTouhouBoomBonus(ServerLevel world, UUID uuid) {
        PendingBoomBonusData data = pendingTouhouBoomBonuses.remove(uuid);
        if (data == null || data.bonusCoin() <= 0 || world.getServer() == null) {
            return;
        }
        try {
            Player player = data.playerUuid().isBlank() ? null : world.getServer().getPlayerList().getPlayer(java.util.UUID.fromString(data.playerUuid()));
            if (player != null) {
                cn.breezeth.ordertocook.util.CoinUtils.giveCoins(player, data.bonusCoin());
                cn.breezeth.ordertocook.core.PrestigeManager.addPlayerPrestige(player, data.bonusCoin());
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    int totalEarned = cn.breezeth.ordertocook.core.PrestigeManager.getPlayerPrestige(player);
                    cn.breezeth.ordertocook.registry.ModCriteria.TOTAL_COIN.trigger(serverPlayer, totalEarned);
                }
            }
            if (data.machineId() > 0) {
                cn.breezeth.ordertocook.core.RestaurantRegistry.applyProfitBonusById(world, data.machineId(), data.bonusCoin(), data.delivery(), data.longDistance(), data.deliveryDistance(), data.walkIn());
            }
            String restaurantName = data.restaurantName().isBlank()
                    ? Component.translatable("keyword.ordertocook.unknown").getString()
                    : data.restaurantName();
            String customerName = data.customerName().isBlank()
                    ? Component.translatable("keyword.ordertocook.customer").getString()
                    : data.customerName();
            Component message = Component.translatable(
                    "message.ordertocook.order_boom_bonus",
                    restaurantName,
                    customerName,
                    data.bonusCoin()
            ).withStyle(net.minecraft.ChatFormatting.GOLD);
            for (net.minecraft.server.level.ServerPlayer online : world.getServer().getPlayerList().getPlayers()) {
                online.displayClientMessage(message, false);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static void writeTouhouSpawnTags(net.minecraft.world.entity.Entity entity, PendingTouhouSpawn pending) {
        clearTouhouSpawnTags(entity);
        entity.addTag(TAG_TOUHOU_SPAWN);
        entity.addTag(TAG_TOUHOU_SPAWN_START_PREFIX + pending.startTick());
        entity.addTag(TAG_TOUHOU_SPAWN_FINISH_PREFIX + pending.finishTick());
        entity.addTag(TAG_TOUHOU_SPAWN_SEATED_PREFIX + pending.seated());
    }

    private static PendingTouhouSpawn readTouhouSpawnTags(net.minecraft.world.entity.Entity entity) {
        if (!entity.getTags().contains(TAG_TOUHOU_SPAWN)) {
            return null;
        }
        long startTick = -1L;
        long finishTick = -1L;
        boolean seated = false;
        for (String tag : entity.getTags()) {
            if (tag.startsWith(TAG_TOUHOU_SPAWN_START_PREFIX)) {
                startTick = parseLongTag(tag, TAG_TOUHOU_SPAWN_START_PREFIX, -1L);
            } else if (tag.startsWith(TAG_TOUHOU_SPAWN_FINISH_PREFIX)) {
                finishTick = parseLongTag(tag, TAG_TOUHOU_SPAWN_FINISH_PREFIX, -1L);
            } else if (tag.startsWith(TAG_TOUHOU_SPAWN_SEATED_PREFIX)) {
                seated = Boolean.parseBoolean(tag.substring(TAG_TOUHOU_SPAWN_SEATED_PREFIX.length()));
            }
        }
        if (startTick < 0L || finishTick < 0L) {
            return null;
        }
        return new PendingTouhouSpawn(startTick, finishTick, seated);
    }

    private static void clearTouhouSpawnTags(net.minecraft.world.entity.Entity entity) {
        java.util.List<String> tags = java.util.List.copyOf(entity.getTags());
        for (String tag : tags) {
            if (tag.equals(TAG_TOUHOU_SPAWN)
                    || tag.startsWith(TAG_TOUHOU_SPAWN_START_PREFIX)
                    || tag.startsWith(TAG_TOUHOU_SPAWN_FINISH_PREFIX)
                    || tag.startsWith(TAG_TOUHOU_SPAWN_SEATED_PREFIX)) {
                entity.removeTag(tag);
            }
        }
    }

    private static void writeTouhouCompletionTags(net.minecraft.world.entity.Entity entity, PendingCompatCompletion pending) {
        clearTouhouCompletionTags(entity);
        entity.addTag(TAG_NPC);
        entity.addTag(TAG_TOUHOU_COMPLETION);
        entity.addTag(TAG_TOUHOU_COMPLETION_ORDER_PREFIX + pending.orderId());
        entity.addTag(TAG_TOUHOU_COMPLETION_START_PREFIX + pending.startTick());
        entity.addTag(TAG_TOUHOU_COMPLETION_FINISH_PREFIX + pending.finishTick());
        entity.addTag(TAG_TOUHOU_COMPLETION_EATING_PREFIX + pending.eating());
        entity.addTag(TAG_TOUHOU_COMPLETION_ANIMATION_PREFIX + pending.animation().name());
        OrderNpcRegistry.register(entity.getUUID());
    }

    private static PendingCompatCompletion readTouhouCompletionTags(net.minecraft.world.entity.Entity entity) {
        if (!entity.getTags().contains(TAG_TOUHOU_COMPLETION)) {
            return null;
        }
        String orderId = "touhou";
        long startTick = -1L;
        long finishTick = -1L;
        boolean eating = false;
        CompletionAnimation animation = CompletionAnimation.STAND_TAKE;
        for (String tag : entity.getTags()) {
            if (tag.startsWith(TAG_TOUHOU_COMPLETION_ORDER_PREFIX)) {
                orderId = tag.substring(TAG_TOUHOU_COMPLETION_ORDER_PREFIX.length());
            } else if (tag.startsWith(TAG_TOUHOU_COMPLETION_START_PREFIX)) {
                startTick = parseLongTag(tag, TAG_TOUHOU_COMPLETION_START_PREFIX, -1L);
            } else if (tag.startsWith(TAG_TOUHOU_COMPLETION_FINISH_PREFIX)) {
                finishTick = parseLongTag(tag, TAG_TOUHOU_COMPLETION_FINISH_PREFIX, -1L);
            } else if (tag.startsWith(TAG_TOUHOU_COMPLETION_EATING_PREFIX)) {
                eating = Boolean.parseBoolean(tag.substring(TAG_TOUHOU_COMPLETION_EATING_PREFIX.length()));
            } else if (tag.startsWith(TAG_TOUHOU_COMPLETION_ANIMATION_PREFIX)) {
                try {
                    animation = CompletionAnimation.valueOf(tag.substring(TAG_TOUHOU_COMPLETION_ANIMATION_PREFIX.length()));
                } catch (IllegalArgumentException ignored) {
                    animation = CompletionAnimation.STAND_TAKE;
                }
            }
        }
        if (startTick < 0L || finishTick < 0L) {
            return null;
        }
        return new PendingCompatCompletion(orderId, startTick, finishTick, eating, animation);
    }

    private static long parseLongTag(String tag, String prefix, long fallback) {
        try {
            return Long.parseLong(tag.substring(prefix.length()));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void clearTouhouCompletionTags(net.minecraft.world.entity.Entity entity) {
        java.util.List<String> tags = java.util.List.copyOf(entity.getTags());
        for (String tag : tags) {
            if (tag.equals(TAG_TOUHOU_COMPLETION)
                    || tag.startsWith(TAG_TOUHOU_COMPLETION_ORDER_PREFIX)
                    || tag.startsWith(TAG_TOUHOU_COMPLETION_START_PREFIX)
                    || tag.startsWith(TAG_TOUHOU_COMPLETION_FINISH_PREFIX)
                    || tag.startsWith(TAG_TOUHOU_COMPLETION_EATING_PREFIX)
                    || tag.startsWith(TAG_TOUHOU_COMPLETION_ANIMATION_PREFIX)) {
                entity.removeTag(tag);
            }
        }
    }

    private record PendingCompatCompletion(String orderId, long startTick, long finishTick, boolean eating, CompletionAnimation animation) {
    }

    private record PendingTouhouSpawn(long startTick, long finishTick, boolean seated) {
    }

    public static void finishAnimatedDeparture(ServerLevel world, cn.breezeth.ordertocook.entity.CustomerEntity npc) {
        if (npc == null || npc.isRemoved()) {
            return;
        }
        net.minecraft.world.entity.Entity seat = npc.getVehicle();
        if (seat != null) {
            npc.stopRiding();
        }
        if (!npc.isRemoved()) {
            npc.discard();
        }
        OrderNpcRegistry.unregister(npc.getUUID());
        if (seat != null && !seat.isRemoved()) {
            seat.discard();
        }
    }

    private static net.minecraft.sounds.SoundEvent getRandomPositiveVillagerSound(ServerLevel world) {
        return switch (world.random.nextInt(3)) {
            case 0 -> net.minecraft.sounds.SoundEvents.VILLAGER_YES;
            case 1 -> net.minecraft.sounds.SoundEvents.VILLAGER_AMBIENT;
            default -> net.minecraft.sounds.SoundEvents.VILLAGER_CELEBRATE;
        };
    }

    public static boolean despawnOwnedNpcNear(ServerLevel world, Player player, String preferredOrderId, BlockPos center, int radius, DespawnReason reason) {
        if (player == null) return false;
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(center).inflate(radius);

        net.minecraft.world.entity.LivingEntity target = null;
        String orderId = null;

        if (preferredOrderId != null && !preferredOrderId.isBlank()) {
            UUID id = orderToNpc.get(preferredOrderId);
            if (id != null) {
                net.minecraft.world.entity.Entity e = world.getEntity(id);
                if (e instanceof net.minecraft.world.entity.LivingEntity le) {
                    double distSq = le.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
                    if (distSq <= (double) radius * (double) radius) {
                        target = le;
                        orderId = preferredOrderId;
                    }
                }
            }
            if (target == null) {
                String preferredOrderTag = TAG_ORDER_PREFIX + preferredOrderId;
                java.util.List<net.minecraft.world.entity.LivingEntity> list = world.getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class,
                        box,
                        ent -> ent.getTags().contains(TAG_NPC) && hasOrderTag(ent, preferredOrderTag)
                );
                if (!list.isEmpty()) {
                    target = list.get(0);
                    orderId = preferredOrderId;
                }
            }
        } else {
            java.util.List<net.minecraft.world.entity.LivingEntity> list = world.getEntitiesOfClass(
                    net.minecraft.world.entity.LivingEntity.class,
                    box,
                    ent -> ent.getTags().contains(TAG_NPC)
            );
            if (!list.isEmpty()) {
                double best = Double.MAX_VALUE;
                for (net.minecraft.world.entity.LivingEntity ent : list) {
                    double d = ent.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
                    if (d < best) {
                        best = d;
                        target = ent;
                    }
                }
                if (target != null) {
                    for (String tag : target.getTags()) {
                        if (tag.startsWith(TAG_ORDER_PREFIX)) {
                            orderId = tag.substring(TAG_ORDER_PREFIX.length());
                            break;
                        }
                    }
                }
            }
        }

        if (target == null || orderId == null) return false;

        orderToNpc.remove(orderId);
        despawnEntity(world, player, orderId, target, reason);
        return true;
    }

    public static int despawnAllNpcNear(ServerLevel world, BlockPos center, int radius) {
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(center).inflate(radius);
        java.util.List<net.minecraft.world.entity.LivingEntity> list = world.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                box,
                ent -> ent.getTags().contains(TAG_NPC) || ent.getTags().contains(TAG_WALKIN)
        );
        if (list.isEmpty()) return 0;

        java.util.HashSet<java.util.UUID> handled = new java.util.HashSet<>();
        int removed = 0;

        for (net.minecraft.world.entity.LivingEntity ent : list) {
            net.minecraft.world.entity.LivingEntity handle = ent;
            net.minecraft.world.entity.Entity veh = ent.getVehicle();
            if (veh instanceof net.minecraft.world.entity.decoration.ArmorStand as && (as.getTags().contains(TAG_NPC) || as.getTags().contains(TAG_WALKIN))) {
                handle = as;
            }

            if (!handled.add(handle.getUUID())) continue;

            String orderId = null;
            for (String tag : handle.getTags()) {
                if (tag.startsWith(TAG_ORDER_PREFIX)) {
                    orderId = tag.substring(TAG_ORDER_PREFIX.length());
                    break;
                }
            }
            if (orderId == null) {
                for (String tag : ent.getTags()) {
                    if (tag.startsWith(TAG_ORDER_PREFIX)) {
                        orderId = tag.substring(TAG_ORDER_PREFIX.length());
                        break;
                    }
                }
            }

            if (orderId != null && !orderId.isBlank()) {
                orderToNpc.remove(orderId);
                despawnEntity(world, null, orderId, handle, DespawnReason.EXPIRED);
            } else {
                despawnEntity(world, null, "dev", handle, DespawnReason.EXPIRED);
            }
            removed++;
        }

        return removed;
    }

    private static boolean hasOrderTag(net.minecraft.world.entity.Entity entity, String orderTag) {
        if (entity.getTags().contains(orderTag)) return true;
        net.minecraft.world.entity.Entity vehicle = entity.getVehicle();
        if (vehicle != null && vehicle.getTags().contains(orderTag)) return true;
        if (entity.isVehicle()) {
            for (net.minecraft.world.entity.Entity passenger : entity.getPassengers()) {
                if (passenger.getTags().contains(orderTag)) return true;
            }
        }
        return false;
    }

    public static boolean isNpcForOrder(Player player, String orderId, net.minecraft.world.entity.Entity entity) {
        String orderTag = TAG_ORDER_PREFIX + orderId;
        
        boolean hasOrderTag = entity.getTags().contains(orderTag);
        if (!hasOrderTag) {
            net.minecraft.world.entity.Entity vehicle = entity.getVehicle();
            if (vehicle != null && vehicle.getTags().contains(orderTag)) {
                hasOrderTag = true;
            }
        }
        if (!hasOrderTag && entity.isVehicle()) {
            for (net.minecraft.world.entity.Entity passenger : entity.getPassengers()) {
                if (passenger.getTags().contains(orderTag)) {
                    hasOrderTag = true;
                    break;
                }
            }
        }
        
        if (ConfigManager.isDevModeEnabled() && player != null && !player.level().isClientSide) {
            player.displayClientMessage(Component.literal("[OTC Dev] Check Order:" + orderId + " Match:" + hasOrderTag).withStyle(ChatFormatting.GRAY), false);
        }

        if (!hasOrderTag) return false;
        
        if (!OrderNpcRegistry.isNpc(entity.getUUID())) {
            OrderNpcRegistry.register(entity.getUUID());
        }
        return true;
    }

    public static void despawnNpcForOrder(ServerLevel world, Player player, String orderId, net.minecraft.world.entity.LivingEntity entity, boolean delivered) {
        despawnNpcForOrder(world, player, orderId, entity, delivered ? DespawnReason.COMPLETED : DespawnReason.EXPIRED);
    }

    public static void despawnNpcForOrder(ServerLevel world, Player player, String orderId, net.minecraft.world.entity.LivingEntity entity, DespawnReason reason) {
        if (!isNpcForOrder(player, orderId, entity)) return;
        orderToNpc.remove(orderId);
        despawnEntity(world, player, orderId, entity, reason);
    }

    static void despawnNpcForWalkIn(ServerLevel world, net.minecraft.world.entity.LivingEntity entity) {
        despawnEntity(world, null, "walkin", entity, DespawnReason.EXPIRED);
    }

    private static void despawnEntity(ServerLevel world, Player player, String orderId, net.minecraft.world.entity.LivingEntity npc, DespawnReason reason) {
        net.minecraft.world.entity.Entity seat;
        net.minecraft.world.entity.LivingEntity primary;
        if (npc instanceof net.minecraft.world.entity.decoration.ArmorStand as) {
            seat = as;
            primary = null;
            for (net.minecraft.world.entity.Entity p : seat.getPassengers()) {
                if (p instanceof net.minecraft.world.entity.LivingEntity le) {
                    primary = le;
                    break;
                }
            }
            if (primary == null) {
                primary = npc;
            }
        } else {
            net.minecraft.world.entity.Entity veh = npc.getVehicle();
            if (veh instanceof net.minecraft.world.entity.decoration.ArmorStand || veh instanceof SeatEntity) {
                seat = veh;
            } else {
                seat = null;
            }
            primary = npc;
        }
        if (ConfigManager.isDevModeEnabled() && player != null) {
            player.displayClientMessage(Component.translatable("message.ordertocook.npc_despawn", orderId, reasonText(reason)).withStyle(ChatFormatting.GRAY), false);
        }
        if (reason == DespawnReason.COMPLETED) {
            world.sendParticles(ParticleTypes.HAPPY_VILLAGER, primary.getX(), primary.getY() + 0.5, primary.getZ(), 30, 0.3, 0.5, 0.3, 0.1);
        } else {
            world.sendParticles(ParticleTypes.LARGE_SMOKE, primary.getX(), primary.getY() + 0.5, primary.getZ(), 20, 0.3, 0.5, 0.3, 0.0);
            world.playSound(null, primary.blockPosition(), net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        if (seat != null) {
            for (net.minecraft.world.entity.Entity p : seat.getPassengers()) {
                if (p instanceof net.minecraft.world.entity.LivingEntity le) {
                    le.discard();
                    OrderNpcRegistry.unregister(le.getUUID());
                }
            }
            seat.discard();
        }
        if (!primary.isRemoved()) {
            primary.discard();
        }
        OrderNpcRegistry.unregister(primary.getUUID());
    }

    private static Component reasonText(DespawnReason reason) {
        return switch (reason) {
            case COMPLETED -> Component.translatable("message.ordertocook.npc_despawn_reason.completed");
            case EXPIRED -> Component.translatable("message.ordertocook.npc_despawn_reason.expired");
        };
    }

    public static void tagNpc(Player player, String orderId, net.minecraft.world.entity.LivingEntity npc) {
        tagNpc(player, orderId, -1L, -1L, npc);
    }

    public static void tagNpc(Player player, String orderId, long expiryTick, long expirySys, net.minecraft.world.entity.LivingEntity npc) {
        stripWalkInTags(npc);
        npc.addTag(TAG_NPC);
        npc.addTag(TAG_ORDER_PREFIX + orderId);

        stripExpiryTags(npc);
        if (expiryTick >= 0L) {
            npc.addTag(TAG_ORDER_EXPIRY_TICK_PREFIX + expiryTick);
        } else if (expirySys >= 0L) {
            npc.addTag(TAG_ORDER_EXPIRY_SYS_PREFIX + expirySys);
        }

        net.minecraft.world.entity.Entity veh = npc.getVehicle();
        if (veh instanceof SeatEntity seat) {
            stripWalkInTags(seat);
            seat.addTag(TAG_NPC);
            seat.addTag(TAG_ORDER_PREFIX + orderId);
            stripExpiryTags(seat);
            if (expiryTick >= 0L) {
                seat.addTag(TAG_ORDER_EXPIRY_TICK_PREFIX + expiryTick);
            } else if (expirySys >= 0L) {
                seat.addTag(TAG_ORDER_EXPIRY_SYS_PREFIX + expirySys);
            }
        } else if (veh instanceof ArmorStand seat) {
            stripWalkInTags(seat);
            seat.addTag(TAG_NPC);
            seat.addTag(TAG_ORDER_PREFIX + orderId);
            stripExpiryTags(seat);
            if (expiryTick >= 0L) {
                seat.addTag(TAG_ORDER_EXPIRY_TICK_PREFIX + expiryTick);
            } else if (expirySys >= 0L) {
                seat.addTag(TAG_ORDER_EXPIRY_SYS_PREFIX + expirySys);
            }
        }
        if (ConfigManager.isDevModeEnabled() && player != null && !player.level().isClientSide) {
            player.displayClientMessage(Component.literal("[OTC Dev] tagNpc orderId=" + orderId + " npc=" + npc.getUUID() + " tags=" + npc.getTags()).withStyle(ChatFormatting.GRAY), false);
        }
    }

    private static void stripExpiryTags(net.minecraft.world.entity.Entity entity) {
        java.util.List<String> toRemove = new java.util.ArrayList<>();
        for (String tag : entity.getTags()) {
            if (tag.startsWith(TAG_ORDER_EXPIRY_TICK_PREFIX) || tag.startsWith(TAG_ORDER_EXPIRY_SYS_PREFIX)) {
                toRemove.add(tag);
            }
        }
        for (String tag : toRemove) {
            entity.removeTag(tag);
        }
    }

    private static void clearCompletionTags(net.minecraft.world.entity.Entity entity) {
        entity.removeTag(TAG_NPC);
        entity.removeTag(TAG_WALKIN);
        entity.removeTag(TAG_CHAIR_CUSTOMER);
        java.util.List<String> tags = java.util.List.copyOf(entity.getTags());
        for (String tag : tags) {
            if (tag.startsWith(TAG_ORDER_PREFIX)
                    || tag.startsWith(TAG_CHAIR_CUSTOMER_POS_PREFIX)
                    || tag.startsWith(TAG_ORDER_EXPIRY_TICK_PREFIX)
                    || tag.startsWith(TAG_ORDER_EXPIRY_SYS_PREFIX)) {
                entity.removeTag(tag);
            }
        }
    }

    private static net.minecraft.world.entity.LivingEntity findNpcByTags(ServerLevel world, BlockPos center, int radius, String orderId) {
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(center).inflate(radius);
        String orderTag = TAG_ORDER_PREFIX + orderId;
        java.util.List<net.minecraft.world.entity.LivingEntity> list = world.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                box,
                ent -> hasOrderTag(ent, orderTag)
        );
        return list.isEmpty() ? null : list.get(0);
    }

    static net.minecraft.world.entity.LivingEntity createNpc(ServerLevel world, BlockPos pos, String name, net.minecraft.nbt.CompoundTag customerData) {
        CustomerProfileLibrary.CustomerProfile profile = customerData == null
                ? CustomerProfileLibrary.createWalkInProfile(world, name)
                : CustomerProfileLibrary.fromNbt(customerData, world);
        String customerId = customerData != null && customerData.contains(ModConstants.NBT_CUSTOMER_ID)
                ? customerData.getString(ModConstants.NBT_CUSTOMER_ID)
                : OtcRuntimeIdState.get(world).allocateCustomerId();
        return createNpc(world, pos, profile, customerId);
    }

    static net.minecraft.world.entity.LivingEntity createNpc(ServerLevel world, BlockPos pos, CustomerProfileLibrary.CustomerProfile profile, String customerId) {
        CustomerProfileLibrary.CustomerProfile finalProfile = profile == null
                ? CustomerProfileLibrary.createWalkInProfile(world, Component.translatable("keyword.ordertocook.customer").getString())
                : profile;
        float yaw = world.random.nextFloat() * 360.0f;

        if (TouhouLittleMaidCustomerCompat.canUseFor(finalProfile)) {
            net.minecraft.world.entity.LivingEntity maid = TouhouLittleMaidCustomerCompat.createMaid(world, finalProfile.toNameText(), yaw);
            if (maid != null) {
                prepareCommonNpc(maid);
                if (spawnPreparedNpc(world, pos, maid, yaw, true)) {
                    return maid;
                }
                maid.discard();
            }
        }

        cn.breezeth.ordertocook.entity.CustomerEntity entity = ModEntities.CUSTOMER.get().create(world);
        if (entity == null) return null;
        entity.setTextureVariant(finalProfile.textureVariant());
        entity.setSkinAccount(finalProfile.skinAccount());
        entity.setSkinUuid(finalProfile.skinUuid());
        entity.setEasterEgg(finalProfile.easterEgg());
        entity.assignRandomAnimationVariant(world.random);
        entity.setCustomerId(customerId == null ? "" : customerId);

        entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        entity.setNoAi(false);
        entity.setCustomName(finalProfile.toNameText());
        prepareCommonNpc(entity);
        if (spawnPreparedNpc(world, pos, entity, yaw, false)) {
            return entity;
        }
        return null;
    }

    private static void prepareCommonNpc(net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            mob.setPersistenceRequired();
        }
        entity.setInvulnerable(true);
        entity.setSilent(true);
        if (entity.getCustomName() == null) {
            entity.setCustomName(Component.translatable("keyword.ordertocook.customer"));
        }
        entity.setCustomNameVisible(true);
        entity.setGlowingTag(true);
        entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 20 * 60 * 30, 0, false, false, true));
        entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER, 20 * 60 * 30, 0, false, false, true));
        if (entity.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
        }
        if (entity.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
        }
    }

    private static boolean spawnPreparedNpc(ServerLevel world, BlockPos pos, net.minecraft.world.entity.LivingEntity entity, float yaw, boolean touhouMaid) {
        if (world.getBlockState(pos.below()).getBlock() == ModBlocks.CHAIR.get()) {
            float chairYaw = world.getBlockState(pos.below()).getValue(cn.breezeth.ordertocook.block.ChairBlock.FACING).toYRot();
            entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, chairYaw, 0.0f);
            entity.setYRot(chairYaw);
            entity.setYHeadRot(chairYaw);
            entity.setYBodyRot(chairYaw);
            entity.yRotO = chairYaw;

            SeatEntity seat = new SeatEntity(ModEntities.CUSTOMER_SEAT.get(), world);
            seat.setPos(pos.getX() + 0.5, pos.getY() - (touhouMaid ? TOUHOU_MAID_SEAT_Y_OFFSET : SEAT_Y_OFFSET), pos.getZ() + 0.5);
            seat.setYRot(chairYaw);
            seat.setInvisible(true);
            seat.setNoGravity(true);
            seat.setSilent(true);
            seat.setInvulnerable(true);
            seat.addTag(TAG_CHAIR_SEAT);
            seat.addTag(TAG_CHAIR_SEAT_POS_PREFIX + pos.below().asLong());
            if (!world.addFreshEntity(seat)) return false;
            entity.addTag(TAG_CHAIR_CUSTOMER);
            entity.addTag(TAG_CHAIR_CUSTOMER_POS_PREFIX + pos.below().asLong());

            BlockPos spawnPos = pos;
            if (!isHeadClear(world, spawnPos)) {
                spawnPos = findTempSpawnPosNear(world, pos, 2);
                if (spawnPos == null) {
                    seat.discard();
                    return false;
                }
                entity.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, chairYaw, 0.0f);
            }
            if (touhouMaid) {
                TouhouLittleMaidCustomerCompat.initializeSpawnData(world, entity);
                entity.setNoGravity(true);
            }

            if (!world.addFreshEntity(entity)) {
                seat.discard();
                return false;
            }
            if (!startManagedCustomerRiding(entity, seat)) {
                entity.discard();
                seat.discard();
                return false;
            }
            if (entity instanceof cn.breezeth.ordertocook.entity.CustomerEntity customer) {
                customer.startSitSpawnAnimation();
            } else if (touhouMaid) {
                beginTouhouSpawn(world, entity, true);
            }
            OrderNpcRegistry.register(entity.getUUID());
            return true;
        }

        entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        if (touhouMaid) {
            TouhouLittleMaidCustomerCompat.initializeSpawnData(world, entity);
        }
        if (!world.addFreshEntity(entity)) return false;
        if (entity instanceof cn.breezeth.ordertocook.entity.CustomerEntity customer) {
            customer.startStandSpawnAnimation();
        } else if (touhouMaid) {
            beginTouhouSpawn(world, entity, false);
        }
        OrderNpcRegistry.register(entity.getUUID());
        return true;
    }

    public static BlockPos getCustomerPlateDisplayPos(net.minecraft.world.entity.LivingEntity npc) {
        if (npc == null) {
            return null;
        }
        BlockPos chairPos = getCustomerChairPos(npc);
        if (chairPos != null) {
            net.minecraft.world.level.block.state.BlockState chairState = npc.level().getBlockState(chairPos);
            if (chairState.getBlock() == ModBlocks.CHAIR.get() && chairState.hasProperty(cn.breezeth.ordertocook.block.ChairBlock.FACING)) {
                net.minecraft.core.Direction facing = chairState.getValue(cn.breezeth.ordertocook.block.ChairBlock.FACING);
                return chairPos.relative(facing).above();
            }
        }
        return npc.blockPosition().relative(npc.getDirection()).above();
    }

    public static boolean isChairCustomer(net.minecraft.world.entity.LivingEntity npc) {
        return getCustomerChairPos(npc) != null;
    }

    private static void tickPendingPassengerSyncs(ServerLevel world) {
        long now = world.getGameTime();
        for (var entry : pendingPassengerSyncs.entrySet()) {
            if (entry.getValue() > now) continue;
            net.minecraft.world.entity.Entity seat = world.getEntity(entry.getKey());
            if (seat != null && !seat.getPassengers().isEmpty()) {
                world.getChunkSource().broadcast(seat, new ClientboundSetPassengersPacket(seat));
            }
            pendingPassengerSyncs.remove(entry.getKey(), entry.getValue());
        }
    }

    private static void stripWalkInTags(net.minecraft.world.entity.Entity entity) {
        for (String tag : java.util.List.copyOf(entity.getTags())) {
            if (tag.equals(TAG_WALKIN) || tag.equals("otc_walkin_interacted") || tag.startsWith("otc_level:")
                    || tag.startsWith(TAG_WALKIN_SPAWN_TIME) || tag.startsWith(TAG_WALKIN_SPAWN_SYSTEM_TIME)
                    || tag.startsWith(TAG_WALKIN_MACHINE_POS_PREFIX) || tag.startsWith(TAG_WALKIN_BOARD_POS_PREFIX)) {
                entity.removeTag(tag);
            }
        }
    }

    public static boolean isManagedTouhouMaid(net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.world.entity.LivingEntity living
                && TouhouLittleMaidCustomerCompat.isTouhouMaid(living)
                && (living.getTags().contains(TAG_NPC)
                || living.getTags().contains(TAG_WALKIN)
                || living.getTags().contains(TAG_TOUHOU_SPAWN)
                || living.getTags().contains(TAG_TOUHOU_COMPLETION));
    }

    public static void migrateLegacyCustomerSeats(ServerLevel world) {
        java.util.List<SeatEntity> legacySeats = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.Entity entity : world.getAllEntities()) {
            if (entity instanceof SeatEntity seat
                    && seat.getType() == ModEntities.SEAT.get()
                    && seat.getTags().contains(TAG_CHAIR_SEAT)
                    && seat.getFirstPassenger() instanceof net.minecraft.world.entity.LivingEntity living
                    && living.getTags().contains(TAG_CHAIR_CUSTOMER)) {
                legacySeats.add(seat);
            }
        }
        for (SeatEntity oldSeat : legacySeats) {
            net.minecraft.world.entity.Entity passenger = oldSeat.getFirstPassenger();
            if (!(passenger instanceof net.minecraft.world.entity.LivingEntity living) || passenger.isRemoved()) continue;
            SeatEntity newSeat = new SeatEntity(ModEntities.CUSTOMER_SEAT.get(), world);
            newSeat.moveTo(oldSeat.getX(), oldSeat.getY(), oldSeat.getZ(), oldSeat.getYRot(), oldSeat.getXRot());
            newSeat.setInvisible(true);
            newSeat.setNoGravity(true);
            newSeat.setSilent(true);
            newSeat.setInvulnerable(true);
            for (String tag : oldSeat.getTags()) newSeat.addTag(tag);
            passenger.stopRiding();
            if (world.addFreshEntity(newSeat) && startManagedCustomerRiding(living, newSeat)) {
                oldSeat.discard();
            } else {
                newSeat.discard();
                startManagedCustomerRiding(living, oldSeat);
            }
        }
        normalizePersistedCustomerSeatHeights(world);
        recoverOrphanedChairCustomers(world);
    }

    private static void normalizePersistedCustomerSeatHeights(ServerLevel world) {
        for (net.minecraft.world.entity.Entity entity : world.getAllEntities()) {
            if (!(entity instanceof SeatEntity seat)
                    || seat.getType() != ModEntities.CUSTOMER_SEAT.get()
                    || !seat.getTags().contains(TAG_CHAIR_SEAT)
                    || !(seat.getFirstPassenger() instanceof net.minecraft.world.entity.LivingEntity passenger)) {
                continue;
            }
            BlockPos chairPos = null;
            for (String tag : seat.getTags()) {
                if (!tag.startsWith(TAG_CHAIR_SEAT_POS_PREFIX)) continue;
                try {
                    chairPos = BlockPos.of(Long.parseLong(tag.substring(TAG_CHAIR_SEAT_POS_PREFIX.length())));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed legacy tags.
                }
                break;
            }
            if (chairPos == null || world.getBlockState(chairPos).getBlock() != ModBlocks.CHAIR.get()) continue;
            boolean touhouMaid = TouhouLittleMaidCustomerCompat.isTouhouMaid(passenger);
            if (touhouMaid) {
                passenger.setNoGravity(true);
            }
            double expectedY = chairPos.getY() + 1.0D
                    - (touhouMaid ? TOUHOU_MAID_SEAT_Y_OFFSET : SEAT_Y_OFFSET);
            if (Math.abs(seat.getY() - expectedY) > 1.0E-4D) {
                seat.setPos(seat.getX(), expectedY, seat.getZ());
            }
        }
    }

    private static void recoverOrphanedChairCustomers(ServerLevel world) {
        java.util.List<net.minecraft.world.entity.LivingEntity> orphans = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.Entity entity : world.getAllEntities()) {
            if (entity instanceof net.minecraft.world.entity.LivingEntity living
                    && living.getTags().contains(TAG_CHAIR_CUSTOMER)
                    && !living.isPassenger()
                    && !isTouhouMaidEating(living)) {
                orphans.add(living);
            }
        }
        for (net.minecraft.world.entity.LivingEntity customer : orphans) {
            BlockPos chairPos = getCustomerChairPos(customer);
            if (chairPos == null || world.getBlockState(chairPos).getBlock() != ModBlocks.CHAIR.get()) continue;
            boolean touhouMaid = TouhouLittleMaidCustomerCompat.isTouhouMaid(customer);
            double seatY = chairPos.getY() + 1.0D
                    - (touhouMaid ? TOUHOU_MAID_SEAT_Y_OFFSET : SEAT_Y_OFFSET);
            net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(chairPos).inflate(0.75D, 1.0D, 0.75D);
            SeatEntity seat = world.getEntitiesOfClass(SeatEntity.class, searchBox,
                            candidate -> candidate.getType() == ModEntities.CUSTOMER_SEAT.get()
                                    && candidate.getPassengers().isEmpty())
                    .stream().findFirst().orElse(null);
            boolean created = false;
            if (seat == null) {
                seat = new SeatEntity(ModEntities.CUSTOMER_SEAT.get(), world);
                seat.setPos(chairPos.getX() + 0.5D, seatY, chairPos.getZ() + 0.5D);
                seat.setYRot(customer.getYRot());
                seat.setInvisible(true);
                seat.setNoGravity(true);
                seat.setSilent(true);
                seat.setInvulnerable(true);
                seat.addTag(TAG_CHAIR_SEAT);
                seat.addTag(TAG_CHAIR_SEAT_POS_PREFIX + chairPos.asLong());
                created = world.addFreshEntity(seat);
                if (!created) continue;
            } else {
                seat.setPos(chairPos.getX() + 0.5D, seatY, chairPos.getZ() + 0.5D);
            }
            if (!startManagedCustomerRiding(customer, seat) && created) {
                seat.discard();
            }
        }
    }

    private static void standMaidOnChairForEating(net.minecraft.world.entity.LivingEntity maid) {
        BlockPos chairPos = getCustomerChairPos(maid);
        net.minecraft.world.entity.Entity seat = maid.getVehicle();
        maid.stopRiding();
        if (seat instanceof SeatEntity) seat.discard();
        if (chairPos != null) {
            maid.setPos(chairPos.getX() + 0.5D, chairPos.getY() + 0.5D, chairPos.getZ() + 0.5D);
        }
        maid.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        maid.setNoGravity(true);
        TouhouLittleMaidCustomerCompat.setSitting(maid, false);
    }

    private static boolean startManagedCustomerRiding(net.minecraft.world.entity.LivingEntity customer, net.minecraft.world.entity.Entity seat) {
        boolean touhouMaid = TouhouLittleMaidCustomerCompat.isTouhouMaid(customer);
        if (touhouMaid) {
            TouhouLittleMaidCustomerCompat.setRideableForManagedMount(customer, true);
        }
        try {
            boolean mounted = customer.startRiding(seat, true);
            if (mounted && seat.level() instanceof ServerLevel world) {
                pendingPassengerSyncs.put(seat.getUUID(), world.getGameTime() + 5L);
            }
            return mounted;
        } finally {
            if (touhouMaid) {
                TouhouLittleMaidCustomerCompat.setRideableForManagedMount(customer, false);
            }
        }
    }

    private static BlockPos getCustomerChairPos(net.minecraft.world.entity.LivingEntity npc) {
        net.minecraft.world.entity.Entity vehicle = npc.getVehicle();
        if (vehicle != null) {
            for (String tag : vehicle.getTags()) {
                if (tag.startsWith(TAG_CHAIR_SEAT_POS_PREFIX)) {
                    try {
                        return BlockPos.of(Long.parseLong(tag.substring(TAG_CHAIR_SEAT_POS_PREFIX.length())));
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                }
            }
        }
        for (String tag : npc.getTags()) {
            if (tag.startsWith(TAG_CHAIR_CUSTOMER_POS_PREFIX)) {
                try {
                    return BlockPos.of(Long.parseLong(tag.substring(TAG_CHAIR_CUSTOMER_POS_PREFIX.length())));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    static net.minecraft.world.entity.LivingEntity createNpc(ServerLevel world, BlockPos pos, String name) {
        CustomerProfileLibrary.CustomerProfile profile = CustomerProfileLibrary.createWalkInProfile(world, name);
        String customerId = OtcRuntimeIdState.get(world).allocateCustomerId();
        return createNpc(world, pos, profile, customerId);
    }

    private static BlockPos findTempSpawnPosNear(ServerLevel world, BlockPos origin, int r) {
        java.util.List<BlockPos> candidates = new java.util.ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx == 0 && dz == 0) continue;
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int topY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                int spawnY = topY;
                BlockPos spawn = new BlockPos(x, spawnY, z);
                if (!isHeadClear(world, spawn)) continue;
                if (!isDry(world, spawn)) continue;
                candidates.add(spawn);
            }
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(world.random.nextInt(candidates.size()));
    }

    static BlockPos findSpawnPosNormal(ServerLevel world, BlockPos machinePos, FailureCounters counters) {
        int radius = 24;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean anyChair = false;
        int minY = machinePos.getY() - 8;
        int maxY = machinePos.getY() + 8;
        int worldMinY = world.getMinBuildHeight();
        int worldMaxY = world.getMaxBuildHeight();
        if (minY < worldMinY) minY = worldMinY;
        if (maxY > worldMaxY) maxY = worldMaxY;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                int x = machinePos.getX() + dx;
                int z = machinePos.getZ() + dz;
                for (int y = maxY; y >= minY; y--) {
                    mutable.set(x, y, z);
                    if (world.getBlockState(mutable).getBlock() != ModBlocks.CHAIR.get()) {
                        continue;
                    }
                    anyChair = true;
                    BlockPos spawn = mutable.above();
                    if (!isHeadClear(world, spawn)) {
                        continue;
                    }
                    if (!isDry(world, spawn)) {
                        continue;
                    }
                    
                    if (isChairOccupied(world, mutable)) continue;

                    return spawn;
                }
            }
        }
        if (!anyChair) counters.space++;
        BlockPos fallback = findFallbackPosNearMachine(world, machinePos, 5);
        return fallback;
    }

    static BlockPos findSpawnPosDelivery(ServerLevel world, BlockPos targetPos, FailureCounters counters) {
        targetPos = cn.breezeth.ordertocook.api.PhysicalRestaurantCompatApi.toWorldPosition(world, targetPos);
        int x = targetPos.getX();
        int z = targetPos.getZ();
        int topY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int spawnY = topY;
        if (topY <= world.getMinBuildHeight()) {
            counters.lowY++;
        }
        BlockPos base = new BlockPos(x, spawnY, z);
        if (isHeadClear(world, base) && isDry(world, base)) return base;
        if (!isHeadClear(world, base)) counters.space++;
        if (!isDry(world, base)) counters.water++;
        BlockPos pos = tryFindDrySpot(world, targetPos, 8, 64, counters);
        if (pos != null) return pos;
        return null;
    }

    private static BlockPos tryFindDrySpot(ServerLevel world, BlockPos targetPos, int radius, int attempts, FailureCounters counters) {
        int x = targetPos.getX();
        int z = targetPos.getZ();
        int checked = 0;
        for (int ring = 1; ring <= radius && checked < attempts; ring++) {
            for (int dx = -ring; dx <= ring && checked < attempts; dx++) {
                for (int dz : new int[]{-ring, ring}) {
                    if (checked++ >= attempts) break;
                    BlockPos found = checkLoadedDeliveryColumn(world, x + dx, z + dz, counters);
                    if (found != null) return found;
                }
            }
            for (int dz = -ring + 1; dz < ring && checked < attempts; dz++) {
                for (int dx : new int[]{-ring, ring}) {
                    if (checked++ >= attempts) break;
                    BlockPos found = checkLoadedDeliveryColumn(world, x + dx, z + dz, counters);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private static BlockPos checkLoadedDeliveryColumn(ServerLevel world, int x, int z, FailureCounters counters) {
        if (!world.hasChunk(x >> 4, z >> 4)) return null;
        int topY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (topY <= world.getMinBuildHeight() + 1) counters.lowY++;
        BlockPos pos = new BlockPos(x, topY, z);
        if (!isHeadClear(world, pos)) { counters.space++; return null; }
        if (!isDry(world, pos)) { counters.water++; return null; }
        return pos;
    }

    private static boolean isHeadClear(Level world, BlockPos pos) {
        if (pos.getY() >= world.getMaxBuildHeight()) return false;
        // 上方必须是空气；脚下允许完整实体方块，或模组椅子这种可坐但非完整立方体的承托面。
        BlockPos below = pos.below();
        var state = world.getBlockState(below);
        return world.isEmptyBlock(pos) 
            && world.isEmptyBlock(pos.above()) 
            && !state.isAir()
            && isSpawnBaseValid(world, below, state);
    }

    private static boolean isSpawnBaseValid(Level world, BlockPos below, net.minecraft.world.level.block.state.BlockState state) {
        if (state.getBlock() == ModBlocks.CHAIR.get()) {
            return true;
        }
        return state.isRedstoneConductor(world, below);
    }

    private static boolean isDry(Level world, BlockPos pos) {
        // 检查当前位置和下方均不在水里
        if (!world.getBlockState(pos).getFluidState().isEmpty()) {
            return false;
        }
        BlockPos below = pos.below();
        if (below.getY() < world.getMinBuildHeight()) {
            return true; // 世界边界下方视为无水
        }
        return world.getBlockState(below).getFluidState().isEmpty();
    }
    private static BlockPos findFallbackPosNearMachine(ServerLevel world, BlockPos machinePos, int r) {
        java.util.List<BlockPos> candidates = new java.util.ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                int x = machinePos.getX() + dx;
                int z = machinePos.getZ() + dz;
                int topY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                int spawnY = topY;
                BlockPos spawn = new BlockPos(x, spawnY, z);
                if (!isHeadClear(world, spawn)) continue;
                if (!isDry(world, spawn)) continue;
                candidates.add(spawn);
            }
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(world.random.nextInt(candidates.size()));
    }

    static final class FailureCounters {
        int space;
        int water;
        int lowY;
        String describe() {
            String s = "";
            if (space > 0) s += Component.translatable("keyword.ordertocook.no_space").getString() + "(" + space + ")";
            if (water > 0) s += (s.isEmpty() ? "" : ", ") + Component.translatable("keyword.ordertocook.water").getString() + "(" + water + ")";
            if (lowY > 0) s += (s.isEmpty() ? "" : ", ") + Component.translatable("keyword.ordertocook.too_low").getString() + "(" + lowY + ")";
            return s.isEmpty() ? Component.translatable("keyword.ordertocook.unknown").getString() : s;
        }
        String describeForDelivery() {
            String s = "";
            if (space > 0) s += Component.translatable("keyword.ordertocook.no_space").getString() + "(" + space + ")";
            if (water > 0) s += (s.isEmpty() ? "" : ", ") + Component.translatable("keyword.ordertocook.water").getString() + "(" + water + ")";
            if (lowY > 0) s += (s.isEmpty() ? "" : ", ") + Component.translatable("keyword.ordertocook.too_low").getString() + "(" + lowY + ")";
            return s.isEmpty() ? Component.translatable("keyword.ordertocook.unknown").getString() : s;
        }
    }

    static void addMapping(Player player, String orderId, net.minecraft.world.entity.LivingEntity npc) {
        if (orderId == null || orderId.isBlank()) return;
        orderToNpc.put(orderId, npc.getUUID());
    }

    public static void cleanupChairSeats(ServerLevel world) {
        java.util.List<net.minecraft.world.entity.Entity> toDiscard = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.Entity entity : world.getAllEntities()) {
            if (!(entity instanceof net.minecraft.world.entity.decoration.ArmorStand) && !(entity instanceof SeatEntity)) continue;
            if (!entity.getTags().contains(TAG_CHAIR_SEAT)) continue;
            if (entity.isVehicle()) continue;
            toDiscard.add(entity);
        }
        for (var seat : toDiscard) {
            seat.discard();
        }
    }
}

