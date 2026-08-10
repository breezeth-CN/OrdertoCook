package cn.breezeth.ordertocook.core;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.entity.attribute.EntityAttributes;
 

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.compat.TouhouLittleMaidCustomerCompat;
import cn.breezeth.ordertocook.registry.ModBlocks;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.registry.ModEntities;
import net.minecraft.entity.decoration.ArmorStandEntity;




public final class OrderNpcManager {
    private static final ConcurrentHashMap<String, UUID> orderToNpc = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, PendingCompatCompletion> pendingCompatCompletions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, PendingTouhouSpawn> pendingTouhouSpawns = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Integer> pendingTouhouSwingStages = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, PendingBoomBonusData> pendingTouhouBoomBonuses = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> deliverySpawnSearchTicks = new ConcurrentHashMap<>();
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
    // Maid customers sit eight pixels above the unified baseline.
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

    public static boolean spawnWalkIn(ServerWorld world, BlockPos machinePos, int level) {
        return WalkInNpcManager.spawn(world, machinePos, level);
    }

    public static void checkWalkInDespawn(ServerWorld world) {
        WalkInNpcManager.checkDespawn(world);
    }

    public static boolean acquireDeliverySpawnSearchBudget(ServerWorld world) {
        String dimension = world.getRegistryKey().getValue().toString();
        long tick = world.getTime();
        Long previous = deliverySpawnSearchTicks.put(dimension, tick);
        return previous == null || previous.longValue() != tick;
    }

    public static void tickCompletionAnimations(ServerWorld world) {
        boolean recoveryScan = world.getTime() % 20L == 0L;
        tickPendingTouhouSpawns(world, recoveryScan);
        tickPendingCompatCompletions(world, recoveryScan);
        if (recoveryScan) tickTouhouIdleAnimations(world);
    }

    private static void tickTouhouIdleAnimations(ServerWorld world) {
        for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
            if (!(entity instanceof net.minecraft.entity.LivingEntity living)) {
                continue;
            }
            if (!TouhouLittleMaidCustomerCompat.isTouhouMaid(living)) {
                continue;
            }
            if (living.getCommandTags().contains(TAG_TOUHOU_COMPLETION)) {
                continue;
            }
            if (isChairCustomer(living)) {
                TouhouLittleMaidCustomerCompat.tickIdleSitAnimation(living);
            } else {
                TouhouLittleMaidCustomerCompat.stopIdleAnimation(living);
            }
        }
    }

    public static void checkOrderNpcDespawn(ServerWorld world) {
        long nowTick = world.getTime();
        long nowSys = System.currentTimeMillis();
        for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
            if (entity instanceof net.minecraft.entity.LivingEntity living
                    && living.getCommandTags().contains(TAG_NPC)) {
                OrderNpcRegistry.register(living.getUuid());
            }
        }
        java.util.List<net.minecraft.entity.LivingEntity> toDespawn = new java.util.ArrayList<>();
        for (java.util.UUID id : OrderNpcRegistry.ids()) {
            net.minecraft.entity.Entity entity = world.getEntity(id);
            if (!(entity instanceof net.minecraft.entity.LivingEntity le)) continue;
            if (!le.getCommandTags().contains(TAG_NPC)) continue;
            if (le.getCommandTags().contains(TAG_WALKIN)) continue;
            String orderId = null;
            long expiryTick = -1L;
            long expirySys = -1L;
            String expirySysTag = null;
            for (String tag : le.getCommandTags()) {
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
                le.addCommandTag(TAG_ORDER_EXPIRY_TICK_PREFIX + convertedTick);
                expiryTick = convertedTick;
                if (expirySysTag != null) le.removeCommandTag(expirySysTag);
            }
            boolean expired = false;
            if (expiryTick >= 0L) {
                expired = nowTick >= expiryTick;
            }
            if (expired) {
                toDespawn.add(le);
            }
        }
        for (net.minecraft.entity.LivingEntity le : toDespawn) {
            if (pendingCompatCompletions.containsKey(le.getUuid())) {
                continue;
            }
            String orderId = null;
            for (String tag : le.getCommandTags()) {
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

    static BlockPos findEmptyChair(ServerWorld world, BlockPos machinePos) {
        int radius = 24;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int minY = machinePos.getY() - 8;
        int maxY = machinePos.getY() + 8;
        int worldMinY = world.getBottomY();
        int worldMaxY = world.getTopY();
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
                    if (world.getBlockState(mutable).getBlock() != ModBlocks.CHAIR) {
                        continue;
                    }
                    BlockPos spawn = mutable.up();
                    if (!isHeadClear(world, spawn)) continue;
                    
                    if (isChairOccupied(world, mutable)) continue;
                    
                    candidates.add(spawn);
                }
            }
        }
        
        if (candidates.isEmpty()) return null;
        return candidates.get(world.random.nextInt(candidates.size()));
    }

    private static boolean isChairOccupied(ServerWorld world, BlockPos chairPos) {
        net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(chairPos).expand(0.0, 1.0, 0.0);
        
        java.util.List<SeatEntity> seatEntities = world.getEntitiesByClass(
                SeatEntity.class,
                box,
                e -> true
        );
        for (var s : seatEntities) {
            if (s.hasPassengers()) return true;
        }

        java.util.List<net.minecraft.entity.decoration.ArmorStandEntity> seats = world.getEntitiesByClass(
                net.minecraft.entity.decoration.ArmorStandEntity.class,
                box,
                e -> true
        );
        for (var s : seats) {
            if (s.hasPassengers()) return true;
        }
        
        java.util.List<net.minecraft.entity.LivingEntity> others = world.getEntitiesByClass(
                net.minecraft.entity.LivingEntity.class,
                box,
                e -> !(e instanceof net.minecraft.entity.decoration.ArmorStandEntity)
        );
        if (!others.isEmpty()) return true;

        return false;
    }

    static void setupWalkInTeam(ServerWorld world, net.minecraft.entity.LivingEntity entity) {
        net.minecraft.scoreboard.Scoreboard scoreboard = world.getScoreboard();
        String teamName = "otc_npc_walkin";
        net.minecraft.scoreboard.Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.addTeam(teamName);
            team.setColor(Formatting.YELLOW);
            team.setCollisionRule(net.minecraft.scoreboard.AbstractTeam.CollisionRule.NEVER);
        }
        scoreboard.addScoreHolderToTeam(entity.getNameForScoreboard(), team);
    }

    static void setupOrderTeam(ServerWorld world, net.minecraft.entity.LivingEntity entity) {
        net.minecraft.scoreboard.Scoreboard scoreboard = world.getScoreboard();
        String teamName = "otc_npc_order";
        net.minecraft.scoreboard.Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.addTeam(teamName);
            team.setColor(Formatting.WHITE);
            team.setCollisionRule(net.minecraft.scoreboard.AbstractTeam.CollisionRule.NEVER);
        }
        scoreboard.addScoreHolderToTeam(entity.getNameForScoreboard(), team);
    }
    
    public static void changeToNormalTeam(ServerWorld world, net.minecraft.entity.LivingEntity entity) {
        // Just remove from walkin team, or add to normal glowing team
        // If we want white, we can remove from all teams or add to a white team.
        // The original code adds to "otc_npc_glowing" (Gold).
        // Requirement says "outline color changes back to white".
        // Default glowing color is white if not on a team with color.
        // So we can just remove it from team, or add to a white team.
        // Let's add to a white team to be explicit, or just remove team.
        // If we remove team, collision rule might be lost. So better add to a White/NoColor team with CollisionRule.NEVER.
        
        net.minecraft.scoreboard.Scoreboard scoreboard = world.getScoreboard();
        String teamName = "otc_npc_normal";
        net.minecraft.scoreboard.Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.addTeam(teamName);
            team.setColor(Formatting.WHITE);
            team.setCollisionRule(net.minecraft.scoreboard.AbstractTeam.CollisionRule.NEVER);
        }
        scoreboard.addScoreHolderToTeam(entity.getNameForScoreboard(), team);
    }

    public enum DespawnReason {
        COMPLETED,
        EXPIRED
    }

    public static void spawnForNormal(ServerWorld world, PlayerEntity player, BlockPos machinePos, String orderId, String customerName) {
        NormalOrderNpcManager.spawn(world, player, machinePos, orderId, customerName, -1L, -1L, null);
    }

    public static boolean spawnForDelivery(ServerWorld world, PlayerEntity player, BlockPos targetPos, String orderId, String customerName, long expiryTick, long expirySys) {
        FailureCounters counters = new FailureCounters();
        BlockPos pos = findSpawnPosDelivery(world, targetPos, counters);
        if (pos == null) {
            player.sendMessage(Text.translatable("message.ordertocook.npc_respawn_try").formatted(Formatting.AQUA), false);
            // retry within 10 radius (approx 20x20)
            pos = tryFindDrySpot(world, targetPos, 10, 64, counters);
            if (pos == null) {
                // retry within 50 radius (approx 100x100)
                pos = tryFindDrySpot(world, targetPos, 50, 256, counters);
            }
            if (pos == null) {
                if (ConfigManager.isDevModeEnabled()) {
                    player.sendMessage(Text.translatable("message.ordertocook.npc_spawn_fail_delivery", counters.describeForDelivery()).formatted(Formatting.RED), false);
                }
                return false;
            }
        }
        net.minecraft.entity.LivingEntity npc = createNpc(world, pos, customerName);
        if (npc == null) return false;
        setupOrderTeam(world, npc);
        tagNpc(player, orderId, expiryTick, expirySys, npc);
        addMapping(player, orderId, npc);
        if (pos.getX() != targetPos.getX() || pos.getZ() != targetPos.getZ()) {
            player.sendMessage(Text.translatable("message.ordertocook.npc_respawn_success", customerName, String.valueOf(pos.getX()), String.valueOf(pos.getZ())).formatted(Formatting.AQUA), false);
        }
        if (ConfigManager.isDevModeEnabled()) {
            Text typeName = Text.translatable(npc.getType().getTranslationKey());
            player.sendMessage(Text.translatable("message.ordertocook.npc_spawn_delivery", orderId, typeName, pos.getX(), pos.getY(), pos.getZ()).formatted(Formatting.AQUA), false);
        }
        return true;
    }

    public static boolean spawnForDelivery(
            ServerWorld world,
            PlayerEntity player,
            BlockPos targetPos,
            String orderId,
            String customerName,
            long expiryTick,
            long expirySys,
            net.minecraft.nbt.NbtCompound customerData
    ) {
        return spawnForDelivery(world, player, targetPos, orderId, customerName, expiryTick, expirySys);
    }

    public static void despawnFor(ServerWorld world, PlayerEntity player, String orderId, boolean delivered) {
        despawnFor(world, player, orderId, delivered ? DespawnReason.COMPLETED : DespawnReason.EXPIRED);
    }

    public static void despawnFor(ServerWorld world, PlayerEntity player, String orderId, DespawnReason reason) {
        UUID id = orderToNpc.remove(orderId);
        net.minecraft.entity.LivingEntity npc = null;
        if (id != null) {
            net.minecraft.entity.Entity e = world.getEntity(id);
            if (e instanceof net.minecraft.entity.LivingEntity le) {
                npc = le;
            }
        }
        if (npc == null) {
            BlockPos center = player != null ? player.getBlockPos() : null;
            if (center != null) {
                npc = findNpcByTags(world, center, 64, orderId);
            }
        }
        if (npc == null) {
            if (ConfigManager.isDevModeEnabled() && player != null) {
                player.sendMessage(Text.translatable("message.ordertocook.npc_despawn_missing", orderId).formatted(Formatting.RED), false);
            }
            return;
        }
        despawnEntity(world, player, orderId, npc, reason);
    }

    public static void beginCompletedAnimation(
            ServerWorld world,
            PlayerEntity player,
            String orderId,
            net.minecraft.entity.Entity npcEntity,
            CompletionAnimation animation
    ) {
        net.minecraft.entity.LivingEntity npc = null;
        if (npcEntity instanceof net.minecraft.entity.LivingEntity livingEntity) {
            npc = livingEntity;
        } else if (npcEntity instanceof ArmorStandEntity armorStandEntity
                && armorStandEntity.getFirstPassenger() instanceof net.minecraft.entity.LivingEntity livingPassenger) {
            npc = livingPassenger;
        }
        if (npc == null) {
            despawnFor(world, player, orderId, DespawnReason.COMPLETED);
            return;
        }
        orderToNpc.remove(orderId);
        clearCompletionTags(npc);
        net.minecraft.entity.Entity vehicle = npc.getVehicle();
        if (vehicle != null) {
            clearCompletionTags(vehicle);
        }
        if (ConfigManager.isDevModeEnabled() && player != null) {
            player.sendMessage(Text.translatable("message.ordertocook.npc_despawn", orderId, reasonText(DespawnReason.COMPLETED)).formatted(Formatting.GRAY), false);
        }
        if (TouhouLittleMaidCustomerCompat.isTouhouMaid(npc)) {
            beginTouhouMaidCompletion(world, orderId, npc, animation);
            return;
        }
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, npc.getX(), npc.getY() + 0.5, npc.getZ(), 30, 0.3, 0.5, 0.3, 0.1);
        world.playSound(
                null,
                npc.getBlockPos(),
                getRandomPositiveVillagerSound(world),
                net.minecraft.sound.SoundCategory.NEUTRAL,
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

    private static void beginTouhouMaidCompletion(ServerWorld world, String orderId, net.minecraft.entity.LivingEntity npc, CompletionAnimation animation) {
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
        long startTick = world.getTime();
        long finishTick = startTick + durationTicks;
        PendingCompatCompletion pending = new PendingCompatCompletion(orderId, startTick, finishTick, eating, animation);
        pendingCompatCompletions.put(npc.getUuid(), pending);
        pendingTouhouSwingStages.put(npc.getUuid(), 0);
        writeTouhouCompletionTags(npc, pending);
        if (ConfigManager.isDevModeEnabled()) {
            cn.breezeth.ordertocook.OrderToCookMod.LOGGER.info("[OTC Dev] Touhou maid completion start orderId={} uuid={} animation={} duration={}", orderId, npc.getUuid(), animation, durationTicks);
        }
    }

    public static Text interruptTouhouMaidEatingNear(ServerWorld world, BlockPos platePos) {
        java.util.List<net.minecraft.entity.LivingEntity> maids = world.getEntitiesByClass(
                net.minecraft.entity.LivingEntity.class,
                new net.minecraft.util.math.Box(platePos).expand(3.0),
                entity -> TouhouLittleMaidCustomerCompat.isTouhouMaid(entity) && isTouhouMaidEating(entity)
        );
        if (maids.isEmpty()) {
            return null;
        }
        net.minecraft.entity.LivingEntity maid = maids.get(0);
        String orderId = "touhou";
        PendingCompatCompletion previous = pendingCompatCompletions.get(maid.getUuid());
        if (previous == null) {
            previous = readTouhouCompletionTags(maid);
        }
        if (previous != null) {
            orderId = previous.orderId();
        }

        pendingTouhouBoomBonuses.remove(maid.getUuid());
        clearTouhouCompletionTags(maid);
        long startTick = world.getTime();
        PendingCompatCompletion pending = new PendingCompatCompletion(orderId, startTick, startTick + 10L, true, CompletionAnimation.EAT_SCALE);
        pendingCompatCompletions.put(maid.getUuid(), pending);
        writeTouhouCompletionTags(maid, pending);
        TouhouLittleMaidCustomerCompat.startCompletionAnimation(maid, true, isChairCustomer(maid));
        return maid.getCustomName() != null
                ? maid.getCustomName()
                : Text.translatable("keyword.ordertocook.customer");
    }

    private static boolean isTouhouMaidEating(net.minecraft.entity.LivingEntity entity) {
        PendingCompatCompletion pending = pendingCompatCompletions.get(entity.getUuid());
        if (pending == null) {
            pending = readTouhouCompletionTags(entity);
        }
        return pending != null && pending.eating();
    }

    private static void beginTouhouSpawn(ServerWorld world, net.minecraft.entity.LivingEntity npc, boolean seated) {
        long startTick = world.getTime();
        PendingTouhouSpawn pending = new PendingTouhouSpawn(startTick, startTick + 12L, seated);
        pendingTouhouSpawns.put(npc.getUuid(), pending);
        writeTouhouSpawnTags(npc, pending);
        TouhouLittleMaidCustomerCompat.stopIdleAnimation(npc);
        TouhouLittleMaidCustomerCompat.setSitting(npc, seated);
        world.playSound(null, npc.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 0.55f, 1.35f);
        world.spawnParticles(ParticleTypes.PORTAL, npc.getX(), npc.getY() + 0.65, npc.getZ(), 24, 0.24, 0.35, 0.24, 0.09);
    }

    private static void tickPendingTouhouSpawns(ServerWorld world, boolean recoveryScan) {
        java.util.HashSet<UUID> ids = new java.util.HashSet<>(pendingTouhouSpawns.keySet());
        if (recoveryScan) {
            for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
                if (entity.getCommandTags().contains(TAG_TOUHOU_SPAWN)) ids.add(entity.getUuid());
            }
        }
        long now = world.getTime();
        for (UUID uuid : ids) {
            net.minecraft.entity.Entity entity = world.getEntity(uuid);
            if (!(entity instanceof net.minecraft.entity.LivingEntity npc) || npc.isRemoved()) {
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
            TouhouLittleMaidCustomerCompat.setSitting(npc, pending.seated());
            if (elapsed >= 0L && elapsed < 12L && elapsed % 3L == 0L) {
                world.spawnParticles(ParticleTypes.CLOUD, npc.getX(), npc.getY() + 0.25 + elapsed * 0.035, npc.getZ(), 5, 0.18, 0.14, 0.18, 0.025);
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

    private static void tickPendingCompatCompletions(ServerWorld world, boolean recoveryScan) {
        java.util.HashSet<UUID> ids = new java.util.HashSet<>(pendingCompatCompletions.keySet());
        if (recoveryScan) {
            for (UUID id : OrderNpcRegistry.ids()) {
                net.minecraft.entity.Entity entity = world.getEntity(id);
                if (entity != null && entity.getCommandTags().contains(TAG_TOUHOU_COMPLETION)) ids.add(id);
            }
            for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
                if (entity.getCommandTags().contains(TAG_TOUHOU_COMPLETION)) ids.add(entity.getUuid());
            }
        }
        long now = world.getTime();
        for (UUID uuid : ids) {
            net.minecraft.entity.Entity entity = world.getEntity(uuid);
            if (!(entity instanceof net.minecraft.entity.LivingEntity npc) || npc.isRemoved()) {
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
            boolean seated = npc.hasVehicle() && isChairCustomer(npc);
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
                world.playSound(null, npc.getBlockPos(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.NEUTRAL, 0.7f, 0.9f + world.random.nextFloat() * 0.2f);
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, npc.getX(), npc.getY() + 0.75, npc.getZ(), 2, 0.18, 0.22, 0.18, 0.02);
            } else if (!pending.eating() && elapsed % 12 == 0) {
                TouhouLittleMaidCustomerCompat.tickCompletionAnimation(npc, false, seated);
            }
            if (pending.animation() == CompletionAnimation.SIT_TAKE && elapsed == 35) {
                world.playSound(null, npc.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 1.0f, 1.0f);
                world.spawnParticles(ParticleTypes.PORTAL, npc.getX(), npc.getY() + 0.8, npc.getZ(), 40, 0.35, 0.5, 0.35, 0.2);
            } else if (pending.animation() == CompletionAnimation.STAND_TAKE && elapsed == 55) {
                world.playSound(null, npc.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 1.0f, 1.0f);
                world.spawnParticles(ParticleTypes.PORTAL, npc.getX(), npc.getY() + 0.8, npc.getZ(), 40, 0.35, 0.5, 0.35, 0.2);
            }
            if (pending.animation() == CompletionAnimation.EAT_BOOM && elapsed == 97) {
                world.playSound(null, npc.getBlockPos(), SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.NEUTRAL, 0.9f, 1.0f);
            }
            if (pending.animation() == CompletionAnimation.EAT_BOOM && elapsed == 118) {
                world.playSound(null, npc.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.NEUTRAL, 1.0f, 1.0f);
                world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, npc.getX(), npc.getY() + 0.8, npc.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                world.spawnParticles(ParticleTypes.CLOUD, npc.getX(), npc.getY() + 0.8, npc.getZ(), 18, 0.3, 0.4, 0.3, 0.05);
                grantPendingTouhouBoomBonus(world, uuid);
            } else if (pending.animation() == CompletionAnimation.EAT_SCALE && elapsed == getEatScaleSignalTick(pending)) {
                world.playSound(null, npc.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 1.0f, 1.0f);
                world.spawnParticles(ParticleTypes.PORTAL, npc.getX(), npc.getY() + 0.8, npc.getZ(), 40, 0.35, 0.5, 0.35, 0.2);
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
                world.spawnParticles(ParticleTypes.EXPLOSION, npc.getX(), npc.getY() + 0.8, npc.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            } else if (pending.animation() == CompletionAnimation.EAT_SCALE) {
                world.spawnParticles(ParticleTypes.PORTAL, npc.getX(), npc.getY() + 0.8, npc.getZ(), 35, 0.25, 0.45, 0.25, 0.16);
            }
            despawnEntity(world, null, pending.orderId(), npc, DespawnReason.COMPLETED);
        }
    }

    private static long getEatScaleSignalTick(PendingCompatCompletion pending) {
        return pending.finishTick() - pending.startTick() <= 10L ? 5L : 105L;
    }

    private static void grantPendingTouhouBoomBonus(ServerWorld world, UUID uuid) {
        PendingBoomBonusData data = pendingTouhouBoomBonuses.remove(uuid);
        if (data == null || data.bonusCoin() <= 0 || world.getServer() == null) {
            return;
        }
        try {
            PlayerEntity player = data.playerUuid().isBlank() ? null : world.getServer().getPlayerManager().getPlayer(java.util.UUID.fromString(data.playerUuid()));
            if (player != null) {
                cn.breezeth.ordertocook.util.CoinUtils.giveCoins(player, data.bonusCoin());
                cn.breezeth.ordertocook.core.PrestigeManager.addPlayerPrestige(player, data.bonusCoin());
                if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                    int totalEarned = cn.breezeth.ordertocook.core.PrestigeManager.getPlayerPrestige(player);
                    cn.breezeth.ordertocook.registry.ModCriteria.TOTAL_COIN.trigger(serverPlayer, totalEarned);
                }
            }
            if (data.machineId() > 0) {
                cn.breezeth.ordertocook.core.RestaurantRegistry.applyProfitBonusById(world, data.machineId(), data.bonusCoin(), data.delivery(), data.longDistance(), data.deliveryDistance(), data.walkIn());
            }
            String restaurantName = data.restaurantName().isBlank()
                    ? Text.translatable("keyword.ordertocook.unknown").getString()
                    : data.restaurantName();
            String customerName = data.customerName().isBlank()
                    ? Text.translatable("keyword.ordertocook.customer").getString()
                    : data.customerName();
            world.getServer().getPlayerManager().broadcast(Text.translatable(
                    "message.ordertocook.order_boom_bonus",
                    restaurantName,
                    customerName,
                    data.bonusCoin()
            ).formatted(net.minecraft.util.Formatting.GOLD), false);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static void writeTouhouSpawnTags(net.minecraft.entity.Entity entity, PendingTouhouSpawn pending) {
        clearTouhouSpawnTags(entity);
        entity.addCommandTag(TAG_TOUHOU_SPAWN);
        entity.addCommandTag(TAG_TOUHOU_SPAWN_START_PREFIX + pending.startTick());
        entity.addCommandTag(TAG_TOUHOU_SPAWN_FINISH_PREFIX + pending.finishTick());
        entity.addCommandTag(TAG_TOUHOU_SPAWN_SEATED_PREFIX + pending.seated());
    }

    private static PendingTouhouSpawn readTouhouSpawnTags(net.minecraft.entity.Entity entity) {
        if (!entity.getCommandTags().contains(TAG_TOUHOU_SPAWN)) {
            return null;
        }
        long startTick = -1L;
        long finishTick = -1L;
        boolean seated = false;
        for (String tag : entity.getCommandTags()) {
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

    private static void clearTouhouSpawnTags(net.minecraft.entity.Entity entity) {
        java.util.List<String> tags = java.util.List.copyOf(entity.getCommandTags());
        for (String tag : tags) {
            if (tag.equals(TAG_TOUHOU_SPAWN)
                    || tag.startsWith(TAG_TOUHOU_SPAWN_START_PREFIX)
                    || tag.startsWith(TAG_TOUHOU_SPAWN_FINISH_PREFIX)
                    || tag.startsWith(TAG_TOUHOU_SPAWN_SEATED_PREFIX)) {
                entity.removeCommandTag(tag);
            }
        }
    }

    private static void writeTouhouCompletionTags(net.minecraft.entity.Entity entity, PendingCompatCompletion pending) {
        clearTouhouCompletionTags(entity);
        entity.addCommandTag(TAG_NPC);
        entity.addCommandTag(TAG_TOUHOU_COMPLETION);
        entity.addCommandTag(TAG_TOUHOU_COMPLETION_ORDER_PREFIX + pending.orderId());
        entity.addCommandTag(TAG_TOUHOU_COMPLETION_START_PREFIX + pending.startTick());
        entity.addCommandTag(TAG_TOUHOU_COMPLETION_FINISH_PREFIX + pending.finishTick());
        entity.addCommandTag(TAG_TOUHOU_COMPLETION_EATING_PREFIX + pending.eating());
        entity.addCommandTag(TAG_TOUHOU_COMPLETION_ANIMATION_PREFIX + pending.animation().name());
        OrderNpcRegistry.register(entity.getUuid());
    }

    private static PendingCompatCompletion readTouhouCompletionTags(net.minecraft.entity.Entity entity) {
        if (!entity.getCommandTags().contains(TAG_TOUHOU_COMPLETION)) {
            return null;
        }
        String orderId = "touhou";
        long startTick = -1L;
        long finishTick = -1L;
        boolean eating = false;
        CompletionAnimation animation = CompletionAnimation.STAND_TAKE;
        for (String tag : entity.getCommandTags()) {
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

    private static void clearTouhouCompletionTags(net.minecraft.entity.Entity entity) {
        java.util.List<String> tags = java.util.List.copyOf(entity.getCommandTags());
        for (String tag : tags) {
            if (tag.equals(TAG_TOUHOU_COMPLETION)
                    || tag.startsWith(TAG_TOUHOU_COMPLETION_ORDER_PREFIX)
                    || tag.startsWith(TAG_TOUHOU_COMPLETION_START_PREFIX)
                    || tag.startsWith(TAG_TOUHOU_COMPLETION_FINISH_PREFIX)
                    || tag.startsWith(TAG_TOUHOU_COMPLETION_EATING_PREFIX)
                    || tag.startsWith(TAG_TOUHOU_COMPLETION_ANIMATION_PREFIX)) {
                entity.removeCommandTag(tag);
            }
        }
    }

    private record PendingCompatCompletion(String orderId, long startTick, long finishTick, boolean eating, CompletionAnimation animation) {
    }

    private record PendingTouhouSpawn(long startTick, long finishTick, boolean seated) {
    }

    public static void finishAnimatedDeparture(ServerWorld world, cn.breezeth.ordertocook.entity.CustomerEntity npc) {
        if (npc == null || npc.isRemoved()) {
            return;
        }
        net.minecraft.entity.Entity seat = npc.getVehicle();
        if (seat != null) {
            npc.stopRiding();
        }
        if (!npc.isRemoved()) {
            npc.discard();
        }
        OrderNpcRegistry.unregister(npc.getUuid());
        if (seat != null && !seat.isRemoved()) {
            seat.discard();
        }
    }

    private static net.minecraft.sound.SoundEvent getRandomPositiveVillagerSound(ServerWorld world) {
        return switch (world.random.nextInt(3)) {
            case 0 -> net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_YES;
            case 1 -> net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_AMBIENT;
            default -> net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_CELEBRATE;
        };
    }

    public static boolean despawnOwnedNpcNear(ServerWorld world, PlayerEntity player, String preferredOrderId, BlockPos center, int radius, DespawnReason reason) {
        if (player == null) return false;
        net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(center).expand(radius);

        net.minecraft.entity.LivingEntity target = null;
        String orderId = null;

        if (preferredOrderId != null && !preferredOrderId.isBlank()) {
            UUID id = orderToNpc.get(preferredOrderId);
            if (id != null) {
                net.minecraft.entity.Entity e = world.getEntity(id);
                if (e instanceof net.minecraft.entity.LivingEntity le) {
                    double distSq = le.squaredDistanceTo(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
                    if (distSq <= (double) radius * (double) radius) {
                        target = le;
                        orderId = preferredOrderId;
                    }
                }
            }
            if (target == null) {
                String preferredOrderTag = TAG_ORDER_PREFIX + preferredOrderId;
                java.util.List<net.minecraft.entity.LivingEntity> list = world.getEntitiesByClass(
                        net.minecraft.entity.LivingEntity.class,
                        box,
                        ent -> ent.getCommandTags().contains(TAG_NPC) && hasOrderTag(ent, preferredOrderTag)
                );
                if (!list.isEmpty()) {
                    target = list.get(0);
                    orderId = preferredOrderId;
                }
            }
        } else {
            java.util.List<net.minecraft.entity.LivingEntity> list = world.getEntitiesByClass(
                    net.minecraft.entity.LivingEntity.class,
                    box,
                    ent -> ent.getCommandTags().contains(TAG_NPC)
            );
            if (!list.isEmpty()) {
                double best = Double.MAX_VALUE;
                for (net.minecraft.entity.LivingEntity ent : list) {
                    double d = ent.squaredDistanceTo(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
                    if (d < best) {
                        best = d;
                        target = ent;
                    }
                }
                if (target != null) {
                    for (String tag : target.getCommandTags()) {
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

    public static int despawnAllNpcNear(ServerWorld world, BlockPos center, int radius) {
        net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(center).expand(radius);
        java.util.List<net.minecraft.entity.LivingEntity> list = world.getEntitiesByClass(
                net.minecraft.entity.LivingEntity.class,
                box,
                ent -> ent.getCommandTags().contains(TAG_NPC) || ent.getCommandTags().contains(TAG_WALKIN)
        );
        if (list.isEmpty()) return 0;

        java.util.HashSet<java.util.UUID> handled = new java.util.HashSet<>();
        int removed = 0;

        for (net.minecraft.entity.LivingEntity ent : list) {
            net.minecraft.entity.LivingEntity handle = ent;
            net.minecraft.entity.Entity veh = ent.getVehicle();
            if (veh instanceof net.minecraft.entity.decoration.ArmorStandEntity as && (as.getCommandTags().contains(TAG_NPC) || as.getCommandTags().contains(TAG_WALKIN))) {
                handle = as;
            }

            if (!handled.add(handle.getUuid())) continue;

            String orderId = null;
            for (String tag : handle.getCommandTags()) {
                if (tag.startsWith(TAG_ORDER_PREFIX)) {
                    orderId = tag.substring(TAG_ORDER_PREFIX.length());
                    break;
                }
            }
            if (orderId == null) {
                for (String tag : ent.getCommandTags()) {
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

    private static boolean hasOrderTag(net.minecraft.entity.Entity entity, String orderTag) {
        if (entity.getCommandTags().contains(orderTag)) return true;
        net.minecraft.entity.Entity vehicle = entity.getVehicle();
        if (vehicle != null && vehicle.getCommandTags().contains(orderTag)) return true;
        if (entity.hasPassengers()) {
            for (net.minecraft.entity.Entity passenger : entity.getPassengerList()) {
                if (passenger.getCommandTags().contains(orderTag)) return true;
            }
        }
        return false;
    }

    public static boolean isNpcForOrder(PlayerEntity player, String orderId, net.minecraft.entity.Entity entity) {
        String orderTag = TAG_ORDER_PREFIX + orderId;
        
        boolean hasOrderTag = entity.getCommandTags().contains(orderTag);
        if (!hasOrderTag) {
            net.minecraft.entity.Entity vehicle = entity.getVehicle();
            if (vehicle != null && vehicle.getCommandTags().contains(orderTag)) {
                hasOrderTag = true;
            }
        }
        if (!hasOrderTag && entity.hasPassengers()) {
            for (net.minecraft.entity.Entity passenger : entity.getPassengerList()) {
                if (passenger.getCommandTags().contains(orderTag)) {
                    hasOrderTag = true;
                    break;
                }
            }
        }
        
        if (ConfigManager.isDevModeEnabled() && player != null && !player.getWorld().isClient) {
            player.sendMessage(Text.literal("[OTC Dev] Check Order:" + orderId + " Match:" + hasOrderTag).formatted(Formatting.GRAY), false);
        }

        if (!hasOrderTag) return false;
        
        if (!OrderNpcRegistry.isNpc(entity.getUuid())) {
            OrderNpcRegistry.register(entity.getUuid());
        }
        return true;
    }

    public static void despawnNpcForOrder(ServerWorld world, PlayerEntity player, String orderId, net.minecraft.entity.LivingEntity entity, boolean delivered) {
        despawnNpcForOrder(world, player, orderId, entity, delivered ? DespawnReason.COMPLETED : DespawnReason.EXPIRED);
    }

    public static void despawnNpcForOrder(ServerWorld world, PlayerEntity player, String orderId, net.minecraft.entity.LivingEntity entity, DespawnReason reason) {
        if (!isNpcForOrder(player, orderId, entity)) return;
        orderToNpc.remove(orderId);
        despawnEntity(world, player, orderId, entity, reason);
    }

    static void despawnNpcForWalkIn(ServerWorld world, net.minecraft.entity.LivingEntity entity) {
        despawnEntity(world, null, "walkin", entity, DespawnReason.EXPIRED);
    }

    private static void despawnEntity(ServerWorld world, PlayerEntity player, String orderId, net.minecraft.entity.LivingEntity npc, DespawnReason reason) {
        net.minecraft.entity.Entity seat;
        net.minecraft.entity.LivingEntity primary;
        if (npc instanceof net.minecraft.entity.decoration.ArmorStandEntity as) {
            seat = as;
            primary = null;
            for (net.minecraft.entity.Entity p : seat.getPassengerList()) {
                if (p instanceof net.minecraft.entity.LivingEntity le) {
                    primary = le;
                    break;
                }
            }
            if (primary == null) {
                primary = npc;
            }
        } else {
            net.minecraft.entity.Entity veh = npc.getVehicle();
            if (veh instanceof net.minecraft.entity.decoration.ArmorStandEntity || veh instanceof SeatEntity) {
                seat = veh;
            } else {
                seat = null;
            }
            primary = npc;
        }
        if (ConfigManager.isDevModeEnabled() && player != null) {
            player.sendMessage(Text.translatable("message.ordertocook.npc_despawn", orderId, reasonText(reason)).formatted(Formatting.GRAY), false);
        }
        if (reason == DespawnReason.COMPLETED) {
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, primary.getX(), primary.getY() + 0.5, primary.getZ(), 30, 0.3, 0.5, 0.3, 0.1);
        } else {
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, primary.getX(), primary.getY() + 0.5, primary.getZ(), 20, 0.3, 0.5, 0.3, 0.0);
            world.playSound(null, primary.getBlockPos(), net.minecraft.sound.SoundEvents.BLOCK_FIRE_EXTINGUISH, net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
        if (seat != null) {
            for (net.minecraft.entity.Entity p : seat.getPassengerList()) {
                if (p instanceof net.minecraft.entity.LivingEntity le) {
                    le.discard();
                    OrderNpcRegistry.unregister(le.getUuid());
                }
            }
            seat.discard();
        }
        if (!primary.isRemoved()) {
            primary.discard();
        }
        OrderNpcRegistry.unregister(primary.getUuid());
    }

    private static Text reasonText(DespawnReason reason) {
        return switch (reason) {
            case COMPLETED -> Text.translatable("message.ordertocook.npc_despawn_reason.completed");
            case EXPIRED -> Text.translatable("message.ordertocook.npc_despawn_reason.expired");
        };
    }

    public static void tagNpc(PlayerEntity player, String orderId, net.minecraft.entity.LivingEntity npc) {
        tagNpc(player, orderId, -1L, -1L, npc);
    }

    public static void tagNpc(PlayerEntity player, String orderId, long expiryTick, long expirySys, net.minecraft.entity.LivingEntity npc) {
        stripWalkInTags(npc);
        npc.addCommandTag(TAG_NPC);
        npc.addCommandTag(TAG_ORDER_PREFIX + orderId);

        stripExpiryTags(npc);
        if (expiryTick >= 0L) {
            npc.addCommandTag(TAG_ORDER_EXPIRY_TICK_PREFIX + expiryTick);
        } else if (expirySys >= 0L) {
            npc.addCommandTag(TAG_ORDER_EXPIRY_SYS_PREFIX + expirySys);
        }

        net.minecraft.entity.Entity veh = npc.getVehicle();
        if (veh instanceof SeatEntity seat) {
            stripWalkInTags(seat);
            seat.addCommandTag(TAG_NPC);
            seat.addCommandTag(TAG_ORDER_PREFIX + orderId);
            stripExpiryTags(seat);
            if (expiryTick >= 0L) {
                seat.addCommandTag(TAG_ORDER_EXPIRY_TICK_PREFIX + expiryTick);
            } else if (expirySys >= 0L) {
                seat.addCommandTag(TAG_ORDER_EXPIRY_SYS_PREFIX + expirySys);
            }
        } else if (veh instanceof ArmorStandEntity seat) {
            stripWalkInTags(seat);
            seat.addCommandTag(TAG_NPC);
            seat.addCommandTag(TAG_ORDER_PREFIX + orderId);
            stripExpiryTags(seat);
            if (expiryTick >= 0L) {
                seat.addCommandTag(TAG_ORDER_EXPIRY_TICK_PREFIX + expiryTick);
            } else if (expirySys >= 0L) {
                seat.addCommandTag(TAG_ORDER_EXPIRY_SYS_PREFIX + expirySys);
            }
        }
        if (ConfigManager.isDevModeEnabled() && player != null && !player.getWorld().isClient) {
            player.sendMessage(Text.literal("[OTC Dev] tagNpc orderId=" + orderId + " npc=" + npc.getUuid() + " tags=" + npc.getCommandTags()).formatted(Formatting.GRAY), false);
        }
    }

    private static void stripExpiryTags(net.minecraft.entity.Entity entity) {
        java.util.List<String> toRemove = new java.util.ArrayList<>();
        for (String tag : entity.getCommandTags()) {
            if (tag.startsWith(TAG_ORDER_EXPIRY_TICK_PREFIX) || tag.startsWith(TAG_ORDER_EXPIRY_SYS_PREFIX)) {
                toRemove.add(tag);
            }
        }
        for (String tag : toRemove) {
            entity.removeCommandTag(tag);
        }
    }

    private static void clearCompletionTags(net.minecraft.entity.Entity entity) {
        entity.removeCommandTag(TAG_NPC);
        entity.removeCommandTag(TAG_WALKIN);
        entity.removeCommandTag(TAG_CHAIR_CUSTOMER);
        java.util.List<String> tags = java.util.List.copyOf(entity.getCommandTags());
        for (String tag : tags) {
            if (tag.startsWith(TAG_ORDER_PREFIX)
                    || tag.startsWith(TAG_CHAIR_CUSTOMER_POS_PREFIX)
                    || tag.startsWith(TAG_ORDER_EXPIRY_TICK_PREFIX)
                    || tag.startsWith(TAG_ORDER_EXPIRY_SYS_PREFIX)) {
                entity.removeCommandTag(tag);
            }
        }
    }

    private static net.minecraft.entity.LivingEntity findNpcByTags(ServerWorld world, BlockPos center, int radius, String orderId) {
        net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(center).expand(radius);
        String orderTag = TAG_ORDER_PREFIX + orderId;
        java.util.List<net.minecraft.entity.LivingEntity> list = world.getEntitiesByClass(
                net.minecraft.entity.LivingEntity.class,
                box,
                ent -> hasOrderTag(ent, orderTag)
        );
        return list.isEmpty() ? null : list.get(0);
    }

    static net.minecraft.entity.LivingEntity createNpc(ServerWorld world, BlockPos pos, String name, net.minecraft.nbt.NbtCompound customerData) {
        CustomerProfileLibrary.CustomerProfile profile = customerData == null
                ? CustomerProfileLibrary.createWalkInProfile(world, name)
                : CustomerProfileLibrary.fromNbt(customerData, world);
        String customerId = customerData != null && customerData.contains(ModConstants.NBT_CUSTOMER_ID)
                ? customerData.getString(ModConstants.NBT_CUSTOMER_ID)
                : OtcRuntimeIdState.get(world).allocateCustomerId();
        return createNpc(world, pos, profile, customerId);
    }

    static net.minecraft.entity.LivingEntity createNpc(ServerWorld world, BlockPos pos, CustomerProfileLibrary.CustomerProfile profile, String customerId) {
        CustomerProfileLibrary.CustomerProfile finalProfile = profile == null
                ? CustomerProfileLibrary.createWalkInProfile(world, Text.translatable("keyword.ordertocook.customer").getString())
                : profile;
        float yaw = world.random.nextFloat() * 360.0f;

        if (TouhouLittleMaidCustomerCompat.canUseFor(finalProfile)) {
            net.minecraft.entity.LivingEntity maid = TouhouLittleMaidCustomerCompat.createMaid(world, finalProfile.toNameText(), yaw);
            if (maid != null) {
                prepareCommonNpc(maid);
                if (spawnPreparedNpc(world, pos, maid, yaw, true)) {
                    return maid;
                }
                maid.discard();
            }
        }

        cn.breezeth.ordertocook.entity.CustomerEntity entity = ModEntities.CUSTOMER.create(world);
        if (entity == null) return null;
        entity.setTextureVariant(finalProfile.textureVariant());
        entity.setSkinAccount(finalProfile.skinAccount());
        entity.setSkinUuid(finalProfile.skinUuid());
        entity.setEasterEgg(finalProfile.easterEgg());
        entity.assignRandomAnimationVariant(world.random);
        entity.setCustomerId(customerId == null ? "" : customerId);

        entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        entity.setAiDisabled(false);
        entity.setCustomName(finalProfile.toNameText());
        prepareCommonNpc(entity);
        if (spawnPreparedNpc(world, pos, entity, yaw, false)) {
            return entity;
        }
        return null;
    }

    private static void prepareCommonNpc(net.minecraft.entity.LivingEntity entity) {
        if (entity instanceof net.minecraft.entity.mob.MobEntity mob) {
            mob.setPersistent();
        }
        entity.setInvulnerable(true);
        entity.setSilent(true);
        if (entity.getCustomName() == null) {
            entity.setCustomName(Text.translatable("keyword.ordertocook.customer"));
        }
        entity.setCustomNameVisible(true);
        entity.setGlowing(true);
        entity.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.GLOWING, 20 * 60 * 30, 0, false, false, true));
        entity.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.HUNGER, 20 * 60 * 30, 0, false, false, true));
        if (entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED) != null) {
            entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.0);
        }
        if (entity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(0.0);
        }
    }

    private static boolean spawnPreparedNpc(ServerWorld world, BlockPos pos, net.minecraft.entity.LivingEntity entity, float yaw, boolean touhouMaid) {
        if (world.getBlockState(pos.down()).getBlock() == ModBlocks.CHAIR) {
            float chairYaw = world.getBlockState(pos.down()).get(cn.breezeth.ordertocook.block.ChairBlock.FACING).asRotation();
            entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, chairYaw, 0.0f);
            entity.setYaw(chairYaw);
            entity.setHeadYaw(chairYaw);
            entity.setBodyYaw(chairYaw);
            entity.prevYaw = chairYaw;

            SeatEntity seat = new SeatEntity(ModEntities.CUSTOMER_SEAT, world);
            seat.setPos(pos.getX() + 0.5, pos.getY() - (touhouMaid ? TOUHOU_MAID_SEAT_Y_OFFSET : SEAT_Y_OFFSET), pos.getZ() + 0.5);
            seat.setYaw(chairYaw);
            seat.setInvisible(true);
            seat.setNoGravity(true);
            seat.setSilent(true);
            seat.setInvulnerable(true);
            seat.addCommandTag(TAG_CHAIR_SEAT);
            seat.addCommandTag(TAG_CHAIR_SEAT_POS_PREFIX + pos.down().asLong());
            if (!world.spawnEntity(seat)) return false;
            entity.addCommandTag(TAG_CHAIR_CUSTOMER);
            entity.addCommandTag(TAG_CHAIR_CUSTOMER_POS_PREFIX + pos.down().asLong());

            BlockPos spawnPos = pos;
            if (!isHeadClear(world, spawnPos)) {
                spawnPos = findTempSpawnPosNear(world, pos, 2);
                if (spawnPos == null) {
                    seat.discard();
                    return false;
                }
                entity.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, chairYaw, 0.0f);
            }
            if (touhouMaid) {
                TouhouLittleMaidCustomerCompat.initializeSpawnData(world, entity);
            }

            if (!world.spawnEntity(entity)) {
                seat.discard();
                return false;
            }
            entity.startRiding(seat, true);
            if (entity instanceof cn.breezeth.ordertocook.entity.CustomerEntity customer) {
                customer.startSitSpawnAnimation();
            } else if (touhouMaid) {
                beginTouhouSpawn(world, entity, true);
            }
            OrderNpcRegistry.register(entity.getUuid());
            return true;
        }

        entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        if (touhouMaid) {
            TouhouLittleMaidCustomerCompat.initializeSpawnData(world, entity);
        }
        if (!world.spawnEntity(entity)) return false;
        if (entity instanceof cn.breezeth.ordertocook.entity.CustomerEntity customer) {
            customer.startStandSpawnAnimation();
        } else if (touhouMaid) {
            beginTouhouSpawn(world, entity, false);
        }
        OrderNpcRegistry.register(entity.getUuid());
        return true;
    }

    public static BlockPos getCustomerPlateDisplayPos(net.minecraft.entity.LivingEntity npc) {
        if (npc == null) {
            return null;
        }
        BlockPos chairPos = getCustomerChairPos(npc);
        if (chairPos != null) {
            net.minecraft.block.BlockState chairState = npc.getWorld().getBlockState(chairPos);
            if (chairState.getBlock() == ModBlocks.CHAIR && chairState.contains(cn.breezeth.ordertocook.block.ChairBlock.FACING)) {
                net.minecraft.util.math.Direction facing = chairState.get(cn.breezeth.ordertocook.block.ChairBlock.FACING);
                return chairPos.offset(facing).up();
            }
        }
        return npc.getBlockPos().offset(npc.getHorizontalFacing()).up();
    }

    public static boolean isChairCustomer(net.minecraft.entity.LivingEntity npc) {
        return getCustomerChairPos(npc) != null;
    }

    private static void stripWalkInTags(net.minecraft.entity.Entity entity) {
        for (String tag : java.util.List.copyOf(entity.getCommandTags())) {
            if (tag.equals(TAG_WALKIN) || tag.equals("otc_walkin_interacted") || tag.startsWith("otc_level:")
                    || tag.startsWith(TAG_WALKIN_SPAWN_TIME) || tag.startsWith(TAG_WALKIN_SPAWN_SYSTEM_TIME)
                    || tag.startsWith(TAG_WALKIN_MACHINE_POS_PREFIX) || tag.startsWith(TAG_WALKIN_BOARD_POS_PREFIX)) {
                entity.removeCommandTag(tag);
            }
        }
    }

    public static boolean isManagedTouhouMaid(net.minecraft.entity.Entity entity) {
        return entity instanceof net.minecraft.entity.LivingEntity living
                && TouhouLittleMaidCustomerCompat.isTouhouMaid(living)
                && (living.getCommandTags().contains(TAG_NPC)
                || living.getCommandTags().contains(TAG_WALKIN)
                || living.getCommandTags().contains(TAG_TOUHOU_SPAWN)
                || living.getCommandTags().contains(TAG_TOUHOU_COMPLETION));
    }

    public static void migrateLegacyCustomerSeats(ServerWorld world) {
        java.util.List<SeatEntity> legacySeats = new java.util.ArrayList<>();
        for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
            if (entity instanceof SeatEntity seat
                    && seat.getType() == ModEntities.SEAT
                    && seat.getCommandTags().contains(TAG_CHAIR_SEAT)
                    && seat.getFirstPassenger() instanceof net.minecraft.entity.LivingEntity living
                    && living.getCommandTags().contains(TAG_CHAIR_CUSTOMER)) {
                legacySeats.add(seat);
            }
        }
        for (SeatEntity oldSeat : legacySeats) {
            net.minecraft.entity.Entity passenger = oldSeat.getFirstPassenger();
            if (!(passenger instanceof net.minecraft.entity.LivingEntity living) || passenger.isRemoved()) continue;
            SeatEntity newSeat = new SeatEntity(ModEntities.CUSTOMER_SEAT, world);
            newSeat.refreshPositionAndAngles(oldSeat.getX(), oldSeat.getY(), oldSeat.getZ(), oldSeat.getYaw(), oldSeat.getPitch());
            newSeat.setInvisible(true);
            newSeat.setNoGravity(true);
            newSeat.setSilent(true);
            newSeat.setInvulnerable(true);
            for (String tag : oldSeat.getCommandTags()) newSeat.addCommandTag(tag);
            passenger.stopRiding();
            if (world.spawnEntity(newSeat) && living.startRiding(newSeat, true)) {
                oldSeat.discard();
            } else {
                newSeat.discard();
                living.startRiding(oldSeat, true);
            }
        }
    }

    private static void standMaidOnChairForEating(net.minecraft.entity.LivingEntity maid) {
        BlockPos chairPos = getCustomerChairPos(maid);
        net.minecraft.entity.Entity seat = maid.getVehicle();
        maid.stopRiding();
        if (seat instanceof SeatEntity) seat.discard();
        if (chairPos != null) {
            maid.setPosition(chairPos.getX() + 0.5D, chairPos.getY() + 0.5D, chairPos.getZ() + 0.5D);
        }
        maid.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
        maid.setNoGravity(true);
        TouhouLittleMaidCustomerCompat.setSitting(maid, false);
    }

    private static BlockPos getCustomerChairPos(net.minecraft.entity.LivingEntity npc) {
        net.minecraft.entity.Entity vehicle = npc.getVehicle();
        if (vehicle != null) {
            for (String tag : vehicle.getCommandTags()) {
                if (tag.startsWith(TAG_CHAIR_SEAT_POS_PREFIX)) {
                    try {
                        return BlockPos.fromLong(Long.parseLong(tag.substring(TAG_CHAIR_SEAT_POS_PREFIX.length())));
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                }
            }
        }
        for (String tag : npc.getCommandTags()) {
            if (tag.startsWith(TAG_CHAIR_CUSTOMER_POS_PREFIX)) {
                try {
                    return BlockPos.fromLong(Long.parseLong(tag.substring(TAG_CHAIR_CUSTOMER_POS_PREFIX.length())));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    static net.minecraft.entity.LivingEntity createNpc(ServerWorld world, BlockPos pos, String name) {
        CustomerProfileLibrary.CustomerProfile profile = CustomerProfileLibrary.createWalkInProfile(world, name);
        String customerId = OtcRuntimeIdState.get(world).allocateCustomerId();
        return createNpc(world, pos, profile, customerId);
    }

    private static BlockPos findTempSpawnPosNear(ServerWorld world, BlockPos origin, int r) {
        java.util.List<BlockPos> candidates = new java.util.ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx == 0 && dz == 0) continue;
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
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

    static BlockPos findSpawnPosNormal(ServerWorld world, BlockPos machinePos, FailureCounters counters) {
        int radius = 24;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        boolean anyChair = false;
        int minY = machinePos.getY() - 8;
        int maxY = machinePos.getY() + 8;
        int worldMinY = world.getBottomY();
        int worldMaxY = world.getTopY();
        if (minY < worldMinY) minY = worldMinY;
        if (maxY > worldMaxY) maxY = worldMaxY;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                int x = machinePos.getX() + dx;
                int z = machinePos.getZ() + dz;
                for (int y = maxY; y >= minY; y--) {
                    mutable.set(x, y, z);
                    if (world.getBlockState(mutable).getBlock() != ModBlocks.CHAIR) {
                        continue;
                    }
                    anyChair = true;
                    BlockPos spawn = mutable.up();
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

    static BlockPos findSpawnPosDelivery(ServerWorld world, BlockPos targetPos, FailureCounters counters) {
        int x = targetPos.getX();
        int z = targetPos.getZ();
        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        int spawnY = topY;
        if (topY <= world.getBottomY()) {
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

    private static BlockPos tryFindDrySpot(ServerWorld world, BlockPos targetPos, int radius, int attempts, FailureCounters counters) {
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

    private static BlockPos checkLoadedDeliveryColumn(ServerWorld world, int x, int z, FailureCounters counters) {
        if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) return null;
        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (topY <= world.getBottomY() + 1) counters.lowY++;
        BlockPos pos = new BlockPos(x, topY, z);
        if (!isHeadClear(world, pos)) { counters.space++; return null; }
        if (!isDry(world, pos)) { counters.water++; return null; }
        return pos;
    }

    private static boolean isHeadClear(World world, BlockPos pos) {
        if (pos.getY() >= world.getTopY()) return false;
        // 涓婃柟蹇呴』鏄┖姘旓紱鑴氫笅鍏佽瀹屾暣瀹炰綋鏂瑰潡锛屾垨妯＄粍妞呭瓙杩欑鍙潗浣嗛潪瀹屾暣绔嬫柟浣撶殑鎵挎墭闈€?
        BlockPos below = pos.down();
        var state = world.getBlockState(below);
        return world.isAir(pos) 
            && world.isAir(pos.up()) 
            && !state.isAir()
            && isSpawnBaseValid(world, below, state);
    }

    private static boolean isSpawnBaseValid(World world, BlockPos below, net.minecraft.block.BlockState state) {
        if (state.getBlock() == ModBlocks.CHAIR) {
            return true;
        }
        return state.isSolidBlock(world, below);
    }

    private static boolean isDry(World world, BlockPos pos) {
        // 妫€鏌ュ綋鍓嶄綅缃拰涓嬫柟鍧囦笉鍦ㄦ按閲?
        if (!world.getBlockState(pos).getFluidState().isEmpty()) {
            return false;
        }
        BlockPos below = pos.down();
        if (below.getY() < world.getBottomY()) {
            return true; // 涓栫晫杈圭晫涓嬫柟瑙嗕负鏃犳按
        }
        return world.getBlockState(below).getFluidState().isEmpty();
    }
    private static BlockPos findFallbackPosNearMachine(ServerWorld world, BlockPos machinePos, int r) {
        java.util.List<BlockPos> candidates = new java.util.ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                int x = machinePos.getX() + dx;
                int z = machinePos.getZ() + dz;
                int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
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
            if (space > 0) s += Text.translatable("keyword.ordertocook.no_space").getString() + "(" + space + ")";
            if (water > 0) s += (s.isEmpty() ? "" : ", ") + Text.translatable("keyword.ordertocook.water").getString() + "(" + water + ")";
            if (lowY > 0) s += (s.isEmpty() ? "" : ", ") + Text.translatable("keyword.ordertocook.too_low").getString() + "(" + lowY + ")";
            return s.isEmpty() ? Text.translatable("keyword.ordertocook.unknown").getString() : s;
        }
        String describeForDelivery() {
            String s = "";
            if (space > 0) s += Text.translatable("keyword.ordertocook.no_space").getString() + "(" + space + ")";
            if (water > 0) s += (s.isEmpty() ? "" : ", ") + Text.translatable("keyword.ordertocook.water").getString() + "(" + water + ")";
            if (lowY > 0) s += (s.isEmpty() ? "" : ", ") + Text.translatable("keyword.ordertocook.too_low").getString() + "(" + lowY + ")";
            return s.isEmpty() ? Text.translatable("keyword.ordertocook.unknown").getString() : s;
        }
    }

    static void addMapping(PlayerEntity player, String orderId, net.minecraft.entity.LivingEntity npc) {
        if (orderId == null || orderId.isBlank()) return;
        orderToNpc.put(orderId, npc.getUuid());
    }

    public static void cleanupChairSeats(ServerWorld world) {
        java.util.List<net.minecraft.entity.Entity> toDiscard = new java.util.ArrayList<>();
        for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
            if (!(entity instanceof net.minecraft.entity.decoration.ArmorStandEntity) && !(entity instanceof SeatEntity)) continue;
            if (!entity.getCommandTags().contains(TAG_CHAIR_SEAT)) continue;
            if (entity.hasPassengers()) continue;
            toDiscard.add(entity);
        }
        for (var seat : toDiscard) {
            seat.discard();
        }
    }
}
