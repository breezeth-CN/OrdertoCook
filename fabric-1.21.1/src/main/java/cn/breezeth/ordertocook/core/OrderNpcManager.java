package cn.breezeth.ordertocook.core;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.entity.attribute.EntityAttributes;
 

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.registry.ModBlocks;
import cn.breezeth.ordertocook.entity.CustomerEntity;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.registry.ModEntities;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;



public final class OrderNpcManager {
    private static final ConcurrentHashMap<String, UUID> orderToNpc = new ConcurrentHashMap<>();
    private static final String TAG_NPC = "otc_npc";
    private static final String TAG_ORDER_PREFIX = "otc_order:";
    private static final String TAG_ORDER_EXPIRY_TICK_PREFIX = "otc_order_expiry_tick:";
    private static final String TAG_ORDER_EXPIRY_SYS_PREFIX = "otc_order_expiry_sys:";

    public static final String TAG_WALKIN = "otc_walkin";
    public static final String TAG_WALKIN_SPAWN_TIME = "otc_walkin_spawn_time:"; // Stores world time ticks
    public static final String TAG_WALKIN_SPAWN_SYSTEM_TIME = "otc_walkin_spawn_sys:"; // Stores system millis
    public static final String TAG_WALKIN_MACHINE_POS_PREFIX = "otc_walkin_machine_pos:";
    public static final String TAG_WALKIN_BOARD_POS_PREFIX = "otc_walkin_board_pos:";
    public static final String TAG_CHAIR_SEAT = "otc_chair_seat";
    public static final String TAG_CHAIR_SEAT_POS_PREFIX = "otc_chair_seat_pos:";
    private static final double SEAT_Y_OFFSET = 1.0 - (9.0 / 16.0) + (8.0 / 16.0);

    public enum CompletionAnimation {
        EAT_BOOM,
        EAT_SCALE,
        SIT_TAKE,
        STAND_TAKE
    }

    public static boolean spawnWalkIn(ServerWorld world, BlockPos machinePos, int level) {
        return WalkInNpcManager.spawn(world, machinePos, level);
    }

    public static void checkWalkInDespawn(ServerWorld world) {
        WalkInNpcManager.checkDespawn(world);
    }

    public static void checkOrderNpcDespawn(ServerWorld world) {
        long nowTick = world.getTime();
        long nowSys = System.currentTimeMillis();
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

    public static void spawnForNormal(ServerWorld world, PlayerEntity player, BlockPos machinePos, String orderId, String customerName, long expiryTick, long expirySys, NbtCompound customerData) {
        NormalOrderNpcManager.spawn(world, player, machinePos, orderId, customerName, expiryTick, expirySys, customerData);
    }

    public static boolean spawnForDelivery(ServerWorld world, PlayerEntity player, BlockPos targetPos, String orderId, String customerName, long expiryTick, long expirySys) {
        return spawnForDelivery(world, player, targetPos, orderId, customerName, expiryTick, expirySys, null);
    }

    public static boolean spawnForDelivery(ServerWorld world, PlayerEntity player, BlockPos targetPos, String orderId, String customerName, long expiryTick, long expirySys, NbtCompound customerData) {
        FailureCounters counters = new FailureCounters();
        BlockPos pos = findSpawnPosDelivery(world, targetPos, counters);
        if (pos == null) {
            player.sendMessage(Text.translatable("message.ordertocook.npc_respawn_try").formatted(Formatting.AQUA), false);
            pos = tryFindDrySpot(world, targetPos, 10, 64, counters);
            if (pos == null) pos = tryFindDrySpot(world, targetPos, 50, 256, counters);
            if (pos == null) {
                if (ConfigManager.isDevModeEnabled()) {
                    player.sendMessage(Text.translatable("message.ordertocook.npc_spawn_fail_delivery", counters.describeForDelivery()).formatted(Formatting.RED), false);
                }
                return false;
            }
        }
        net.minecraft.entity.LivingEntity npc = createNpc(world, pos, customerName, customerData);
        if (npc == null) return false;
        setupOrderTeam(world, npc);
        tagNpc(player, orderId, expiryTick, expirySys, npc);
        addMapping(player, orderId, npc);
        player.sendMessage(Text.translatable("message.ordertocook.npc_respawn_success", customerName, String.valueOf(pos.getX()), String.valueOf(pos.getZ())).formatted(Formatting.AQUA), false);
        if (ConfigManager.isDevModeEnabled()) {
            Text typeName = Text.translatable(npc.getType().getTranslationKey());
            player.sendMessage(Text.translatable("message.ordertocook.npc_spawn_delivery", orderId, typeName, pos.getX(), pos.getY(), pos.getZ()).formatted(Formatting.AQUA), false);
        }
        return true;
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

    public static void beginCompletedAnimation(ServerWorld world, PlayerEntity player, String orderId, net.minecraft.entity.Entity npcEntity, CompletionAnimation animation) {
        net.minecraft.entity.LivingEntity npc = null;
        if (npcEntity instanceof net.minecraft.entity.LivingEntity livingEntity) {
            npc = livingEntity;
        } else if (npcEntity instanceof ArmorStandEntity armorStandEntity && armorStandEntity.getFirstPassenger() instanceof net.minecraft.entity.LivingEntity livingPassenger) {
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
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, npc.getX(), npc.getY() + 0.5, npc.getZ(), 30, 0.3, 0.5, 0.3, 0.1);
        world.playSound(null, npc.getBlockPos(), getRandomPositiveVillagerSound(world), SoundCategory.NEUTRAL, 0.9f, 0.95f + world.random.nextFloat() * 0.1f);
        if (npc instanceof CustomerEntity customerEntity) {
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

    private static SoundEvent getRandomPositiveVillagerSound(ServerWorld world) {
        return switch (world.random.nextInt(3)) {
            case 0 -> SoundEvents.ENTITY_VILLAGER_YES;
            case 1 -> SoundEvents.ENTITY_VILLAGER_AMBIENT;
            default -> SoundEvents.ENTITY_VILLAGER_CELEBRATE;
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

    public static void finishAnimatedDeparture(ServerWorld world, CustomerEntity npc) {
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
            seat.addCommandTag(TAG_NPC);
            seat.addCommandTag(TAG_ORDER_PREFIX + orderId);
            stripExpiryTags(seat);
            if (expiryTick >= 0L) {
                seat.addCommandTag(TAG_ORDER_EXPIRY_TICK_PREFIX + expiryTick);
            } else if (expirySys >= 0L) {
                seat.addCommandTag(TAG_ORDER_EXPIRY_SYS_PREFIX + expirySys);
            }
        } else if (veh instanceof ArmorStandEntity seat) {
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

    static net.minecraft.entity.LivingEntity createNpc(ServerWorld world, BlockPos pos, String name) {
        return createNpc(world, pos, name, null);
    }

    static net.minecraft.entity.LivingEntity createNpc(ServerWorld world, BlockPos pos, String name, NbtCompound customerData) {
        CustomerProfileLibrary.CustomerProfile profile = customerData == null
                ? CustomerProfileLibrary.createWalkInProfile(world, name)
                : CustomerProfileLibrary.fromNbt(customerData, world);
        String customerId = customerData != null && customerData.contains(ModConstants.NBT_CUSTOMER_ID)
                ? customerData.getString(ModConstants.NBT_CUSTOMER_ID)
                : OtcRuntimeIdState.get(world).allocateCustomerId();
        return createNpc(world, pos, profile, customerId);
    }

    public static BlockPos getCustomerPlateDisplayPos(net.minecraft.entity.LivingEntity npc) {
        if (npc == null) {
            return null;
        }
        BlockPos chairPos = getCustomerChairPos(npc);
        if (chairPos != null) {
            BlockState chairState = npc.getWorld().getBlockState(chairPos);
            if (chairState.getBlock() == ModBlocks.CHAIR && chairState.contains(cn.breezeth.ordertocook.block.ChairBlock.FACING)) {
                net.minecraft.util.math.Direction facing = chairState.get(cn.breezeth.ordertocook.block.ChairBlock.FACING);
                return chairPos.offset(facing).up();
            }
        }
        return npc.getBlockPos().offset(npc.getHorizontalFacing()).up();
    }

    public static BlockPos getCustomerPlateSupportPos(net.minecraft.entity.LivingEntity npc) {
        BlockPos displayPos = getCustomerPlateDisplayPos(npc);
        return displayPos != null ? displayPos.down() : null;
    }

    private static BlockPos getCustomerChairPos(net.minecraft.entity.LivingEntity npc) {
        net.minecraft.entity.Entity vehicle = npc.getVehicle();
        if (vehicle == null) {
            return null;
        }
        for (String tag : vehicle.getCommandTags()) {
            if (tag.startsWith(TAG_CHAIR_SEAT_POS_PREFIX)) {
                try {
                    return BlockPos.fromLong(Long.parseLong(tag.substring(TAG_CHAIR_SEAT_POS_PREFIX.length())));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    static net.minecraft.entity.LivingEntity createNpc(ServerWorld world, BlockPos pos, CustomerProfileLibrary.CustomerProfile profile, String customerId) {
        CustomerEntity entity = ModEntities.CUSTOMER.create(world);
        if (entity == null) return null;
        entity.setTextureVariant(profile.textureVariant());
        entity.setSkinAccount(profile.skinAccount());
        entity.setSkinUuid(profile.skinUuid());
        entity.setEasterEgg(profile.easterEgg());
        entity.assignRandomAnimationVariant(world.random);
        entity.setCustomerId(customerId);

        float yaw = world.random.nextFloat() * 360.0f;
        entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        if (entity instanceof net.minecraft.entity.mob.MobEntity m) {
            m.setAiDisabled(false);
            m.setPersistent();
        }
        entity.setInvulnerable(true);
        entity.setSilent(true);
        net.minecraft.text.Text nameText = profile.toNameText();
        entity.setCustomName(nameText);
        entity.setCustomNameVisible(true);
        entity.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.GLOWING, 20 * 60 * 30, 0, false, false, true));
        entity.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.HUNGER, 20 * 60 * 30, 0, false, false, true));
        
        // 设置属性
        if (entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED) != null) {
            entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.0);
        }
        if (entity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(0.0);
        }

        Block blockBelow = world.getBlockState(pos.down()).getBlock();
        if (blockBelow == ModBlocks.CHAIR) {
            float chairYaw = world.getBlockState(pos.down()).get(cn.breezeth.ordertocook.block.ChairBlock.FACING).asRotation();
            entity.setYaw(chairYaw);
            entity.setHeadYaw(chairYaw);
            entity.setBodyYaw(chairYaw);
            entity.prevYaw = chairYaw;

            SeatEntity seat = new SeatEntity(ModEntities.SEAT, world);
            seat.setPos(pos.getX() + 0.5, pos.getY() - SEAT_Y_OFFSET, pos.getZ() + 0.5);
            seat.setYaw(chairYaw);
            seat.setInvisible(true);
            seat.setNoGravity(true);
            seat.setSilent(true);
            seat.setInvulnerable(true);
            seat.addCommandTag(TAG_CHAIR_SEAT);
            seat.addCommandTag(TAG_CHAIR_SEAT_POS_PREFIX + pos.down().asLong());
            if (!world.spawnEntity(seat)) return null;

            BlockPos spawnPos = pos;
            if (!isHeadClear(world, spawnPos)) {
                 spawnPos = findTempSpawnPosNear(world, pos, 2);
                 if (spawnPos == null) {
                     seat.discard();
                     return null;
                 }
                 entity.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, chairYaw, 0.0f);
            }

            boolean ok = world.spawnEntity(entity);
            if (!ok) {
                seat.discard();
                return null;
            }
            entity.startRiding(seat, true);
            entity.startSitSpawnAnimation();
            OrderNpcRegistry.register(entity.getUuid());
            return entity;
        }

        boolean ok = world.spawnEntity(entity);
        if (!ok) return null;
        entity.startStandSpawnAnimation();
        OrderNpcRegistry.register(entity.getUuid());
        return entity;
    }

    private static void clearCompletionTags(net.minecraft.entity.Entity entity) {
        entity.removeCommandTag(TAG_NPC);
        entity.removeCommandTag(TAG_WALKIN);
        java.util.List<String> tags = java.util.List.copyOf(entity.getCommandTags());
        for (String tag : tags) {
            if (tag.startsWith(TAG_ORDER_PREFIX)
                    || tag.startsWith(TAG_ORDER_EXPIRY_TICK_PREFIX)
                    || tag.startsWith(TAG_ORDER_EXPIRY_SYS_PREFIX)) {
                entity.removeCommandTag(tag);
            }
        }
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
        for (int i = 0; i < attempts; i++) {
            int dx = world.random.nextBetween(-radius, radius);
            int dz = world.random.nextBetween(-radius, radius);
            int candX = x + dx;
            int candZ = z + dz;
            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, candX, candZ);
            int spawnY = topY;
            if (topY <= world.getBottomY() + 1) {
                counters.lowY++;
            }
            BlockPos pos = new BlockPos(candX, spawnY, candZ);
            if (!isHeadClear(world, pos)) {
                counters.space++;
                continue;
            }
            if (!isDry(world, pos)) {
                counters.water++;
                continue;
            }
            return pos;
        }
        return null;
    }

    private static boolean isHeadClear(World world, BlockPos pos) {
        if (pos.getY() >= world.getTopY()) return false;
        // 上方必须是空气；脚下允许完整实体方块，或模组椅子这种可坐但非完整立方体的承托面。
        BlockPos below = pos.down();
        var state = world.getBlockState(below);
        return world.isAir(pos) 
            && world.isAir(pos.up()) 
            && !state.isAir()
            && isSpawnBaseValid(world, below, state);
    }

    private static boolean isSpawnBaseValid(World world, BlockPos below, BlockState state) {
        if (state.getBlock() == ModBlocks.CHAIR) {
            return true;
        }
        return state.isSolidBlock(world, below);
    }

    private static boolean isDry(World world, BlockPos pos) {
        // 检查当前位置和下方均不在水里
        if (!world.getBlockState(pos).getFluidState().isEmpty()) {
            return false;
        }
        BlockPos below = pos.down();
        if (below.getY() < world.getBottomY()) {
            return true; // 世界边界下方视为无水
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
                if (topY <= world.getBottomY() + 1) {
                    // lowY will be counted in calling context if needed
                }
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
            if (water > 0) s += (s.isEmpty() ? "" : "，") + Text.translatable("keyword.ordertocook.water").getString() + "(" + water + ")";
            if (lowY > 0) s += (s.isEmpty() ? "" : "，") + Text.translatable("keyword.ordertocook.too_low").getString() + "(" + lowY + ")";
            return s.isEmpty() ? Text.translatable("keyword.ordertocook.unknown").getString() : s;
        }
        String describeForDelivery() {
            String s = "";
            if (space > 0) s += Text.translatable("keyword.ordertocook.no_space").getString() + "(" + space + ")";
            if (water > 0) s += (s.isEmpty() ? "" : "，") + Text.translatable("keyword.ordertocook.water").getString() + "(" + water + ")";
            if (lowY > 0) s += (s.isEmpty() ? "" : "，") + Text.translatable("keyword.ordertocook.too_low").getString() + "(" + lowY + ")";
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
