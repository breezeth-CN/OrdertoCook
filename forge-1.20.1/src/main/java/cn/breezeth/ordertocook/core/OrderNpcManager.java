package cn.breezeth.ordertocook.core;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.registry.ModBlocks;
import cn.breezeth.ordertocook.entity.CustomerEntity;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.registry.ModEntities;



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

    public static boolean spawnWalkIn(ServerLevel world, BlockPos machinePos, int level) {
        return WalkInNpcManager.spawn(world, machinePos, level);
    }

    public static void checkWalkInDespawn(ServerLevel world) {
        WalkInNpcManager.checkDespawn(world);
    }

    public static void checkOrderNpcDespawn(ServerLevel world) {
        long nowTick = world.getGameTime();
        long nowSys = System.currentTimeMillis();
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

    public static void spawnForNormal(ServerLevel world, Player player, BlockPos machinePos, String orderId, String customerName, long expiryTick, long expirySys, CompoundTag customerData) {
        NormalOrderNpcManager.spawn(world, player, machinePos, orderId, customerName, expiryTick, expirySys, customerData);
    }

    public static boolean spawnForDelivery(ServerLevel world, Player player, BlockPos targetPos, String orderId, String customerName, long expiryTick, long expirySys) {
        return spawnForDelivery(world, player, targetPos, orderId, customerName, expiryTick, expirySys, null);
    }

    public static boolean spawnForDelivery(ServerLevel world, Player player, BlockPos targetPos, String orderId, String customerName, long expiryTick, long expirySys, CompoundTag customerData) {
        FailureCounters counters = new FailureCounters();
        BlockPos pos = findSpawnPosDelivery(world, targetPos, counters);
        if (pos == null) {
            player.displayClientMessage(Component.translatable("message.ordertocook.npc_respawn_try").withStyle(ChatFormatting.AQUA), false);
            pos = tryFindDrySpot(world, targetPos, 10, 64, counters);
            if (pos == null) pos = tryFindDrySpot(world, targetPos, 50, 256, counters);
            if (pos == null) {
                if (ConfigManager.isDevModeEnabled()) {
                    player.displayClientMessage(Component.translatable("message.ordertocook.npc_spawn_fail_delivery", counters.describeForDelivery()).withStyle(ChatFormatting.RED), false);
                }
                return false;
            }
        }
        net.minecraft.world.entity.LivingEntity npc = createNpc(world, pos, customerName, customerData);
        if (npc == null) return false;
        setupOrderTeam(world, npc);
        tagNpc(player, orderId, expiryTick, expirySys, npc);
        addMapping(player, orderId, npc);
        player.displayClientMessage(Component.translatable("message.ordertocook.npc_respawn_success", customerName, String.valueOf(pos.getX()), String.valueOf(pos.getZ())).withStyle(ChatFormatting.AQUA), false);
        if (ConfigManager.isDevModeEnabled()) {
            Component typeName = Component.translatable(npc.getType().getDescriptionId());
            player.displayClientMessage(Component.translatable("message.ordertocook.npc_spawn_delivery", orderId, typeName, pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.AQUA), false);
        }
        return true;
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

    public static void beginCompletedAnimation(ServerLevel world, Player player, String orderId, net.minecraft.world.entity.Entity npcEntity, CompletionAnimation animation) {
        net.minecraft.world.entity.LivingEntity npc = null;
        if (npcEntity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
            npc = livingEntity;
        } else if (npcEntity instanceof ArmorStand armorStandEntity && armorStandEntity.getFirstPassenger() instanceof net.minecraft.world.entity.LivingEntity livingPassenger) {
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
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, npc.getX(), npc.getY() + 0.5, npc.getZ(), 30, 0.3, 0.5, 0.3, 0.1);
        world.playSound(null, npc.blockPosition(), getRandomPositiveVillagerSound(world), SoundSource.NEUTRAL, 0.9f, 0.95f + world.random.nextFloat() * 0.1f);
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

    private static SoundEvent getRandomPositiveVillagerSound(ServerLevel world) {
        return switch (world.random.nextInt(3)) {
            case 0 -> SoundEvents.VILLAGER_YES;
            case 1 -> SoundEvents.VILLAGER_AMBIENT;
            default -> SoundEvents.VILLAGER_CELEBRATE;
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

    public static void finishAnimatedDeparture(ServerLevel world, CustomerEntity npc) {
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
            seat.addTag(TAG_NPC);
            seat.addTag(TAG_ORDER_PREFIX + orderId);
            stripExpiryTags(seat);
            if (expiryTick >= 0L) {
                seat.addTag(TAG_ORDER_EXPIRY_TICK_PREFIX + expiryTick);
            } else if (expirySys >= 0L) {
                seat.addTag(TAG_ORDER_EXPIRY_SYS_PREFIX + expirySys);
            }
        } else if (veh instanceof ArmorStand seat) {
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

    static net.minecraft.world.entity.LivingEntity createNpc(ServerLevel world, BlockPos pos, String name) {
        return createNpc(world, pos, name, null);
    }

    static net.minecraft.world.entity.LivingEntity createNpc(ServerLevel world, BlockPos pos, String name, CompoundTag customerData) {
        CustomerProfileLibrary.CustomerProfile profile = customerData == null
                ? CustomerProfileLibrary.createWalkInProfile(world, name)
                : CustomerProfileLibrary.fromNbt(customerData, world);
        String customerId = customerData != null && customerData.contains(ModConstants.NBT_CUSTOMER_ID)
                ? customerData.getString(ModConstants.NBT_CUSTOMER_ID)
                : OtcRuntimeIdState.get(world).allocateCustomerId();
        return createNpc(world, pos, profile, customerId);
    }

    public static BlockPos getCustomerPlateDisplayPos(net.minecraft.world.entity.LivingEntity npc) {
        if (npc == null) {
            return null;
        }
        BlockPos chairPos = getCustomerChairPos(npc);
        if (chairPos != null) {
            BlockState chairState = npc.level().getBlockState(chairPos);
            if (chairState.getBlock() == ModBlocks.CHAIR.get() && chairState.hasProperty(cn.breezeth.ordertocook.block.ChairBlock.FACING)) {
                net.minecraft.core.Direction facing = chairState.getValue(cn.breezeth.ordertocook.block.ChairBlock.FACING);
                return chairPos.relative(facing).above();
            }
        }
        return npc.blockPosition().relative(npc.getDirection()).above();
    }

    public static BlockPos getCustomerPlateSupportPos(net.minecraft.world.entity.LivingEntity npc) {
        BlockPos displayPos = getCustomerPlateDisplayPos(npc);
        return displayPos != null ? displayPos.below() : null;
    }

    private static BlockPos getCustomerChairPos(net.minecraft.world.entity.LivingEntity npc) {
        net.minecraft.world.entity.Entity vehicle = npc.getVehicle();
        if (vehicle == null) {
            return null;
        }
        for (String tag : vehicle.getTags()) {
            if (tag.startsWith(TAG_CHAIR_SEAT_POS_PREFIX)) {
                try {
                    return BlockPos.of(Long.parseLong(tag.substring(TAG_CHAIR_SEAT_POS_PREFIX.length())));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    static net.minecraft.world.entity.LivingEntity createNpc(ServerLevel world, BlockPos pos, CustomerProfileLibrary.CustomerProfile profile, String customerId) {
        CustomerEntity entity = ModEntities.CUSTOMER.get().create(world);
        if (entity == null) return null;
        entity.setTextureVariant(profile.textureVariant());
        entity.setSkinAccount(profile.skinAccount());
        entity.setSkinUuid(profile.skinUuid());
        entity.setEasterEgg(profile.easterEgg());
        entity.assignRandomAnimationVariant(world.random);
        entity.setCustomerId(customerId);

        float yaw = world.random.nextFloat() * 360.0f;
        entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        { net.minecraft.world.entity.Mob m = entity;
            m.setNoAi(false);
            m.setPersistenceRequired();
        }
        entity.setInvulnerable(true);
        entity.setSilent(true);
        net.minecraft.network.chat.Component nameText = profile.toNameText();
        entity.setCustomName(nameText);
        entity.setCustomNameVisible(true);
        entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 20 * 60 * 30, 0, false, false, true));
        entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER, 20 * 60 * 30, 0, false, false, true));
        
        // 设置属性
        if (entity.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
        }
        if (entity.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
        }

        Block blockBelow = world.getBlockState(pos.below()).getBlock();
        if (blockBelow == ModBlocks.CHAIR.get()) {
            float chairYaw = world.getBlockState(pos.below()).getValue(cn.breezeth.ordertocook.block.ChairBlock.FACING).toYRot();
            entity.setYRot(chairYaw);
            entity.setYHeadRot(chairYaw);
            entity.setYBodyRot(chairYaw);
            entity.yRotO = chairYaw;

            SeatEntity seat = new SeatEntity(ModEntities.SEAT.get(), world);
            seat.setPosRaw(pos.getX() + 0.5, pos.getY() - SEAT_Y_OFFSET, pos.getZ() + 0.5);
            seat.setYRot(chairYaw);
            seat.setInvisible(true);
            seat.setNoGravity(true);
            seat.setSilent(true);
            seat.setInvulnerable(true);
            seat.addTag(TAG_CHAIR_SEAT);
            seat.addTag(TAG_CHAIR_SEAT_POS_PREFIX + pos.below().asLong());
            if (!world.addFreshEntity(seat)) return null;

            BlockPos spawnPos = pos;
            if (!isHeadClear(world, spawnPos)) {
                 spawnPos = findTempSpawnPosNear(world, pos, 2);
                 if (spawnPos == null) {
                     seat.discard();
                     return null;
                 }
                 entity.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, chairYaw, 0.0f);
            }

            boolean ok = world.addFreshEntity(entity);
            if (!ok) {
                seat.discard();
                return null;
            }
            entity.startRiding(seat, true);
            entity.startSitSpawnAnimation();
            OrderNpcRegistry.register(entity.getUUID());
            return entity;
        }

        boolean ok = world.addFreshEntity(entity);
        if (!ok) return null;
        entity.startStandSpawnAnimation();
        OrderNpcRegistry.register(entity.getUUID());
        return entity;
    }

    private static void clearCompletionTags(net.minecraft.world.entity.Entity entity) {
        entity.removeTag(TAG_NPC);
        entity.removeTag(TAG_WALKIN);
        java.util.List<String> tags = java.util.List.copyOf(entity.getTags());
        for (String tag : tags) {
            if (tag.startsWith(TAG_ORDER_PREFIX)
                    || tag.startsWith(TAG_ORDER_EXPIRY_TICK_PREFIX)
                    || tag.startsWith(TAG_ORDER_EXPIRY_SYS_PREFIX)) {
                entity.removeTag(tag);
            }
        }
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
        for (int i = 0; i < attempts; i++) {
            int dx = world.random.nextIntBetweenInclusive(-radius, radius);
            int dz = world.random.nextIntBetweenInclusive(-radius, radius);
            int candX = x + dx;
            int candZ = z + dz;
            int topY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candX, candZ);
            int spawnY = topY;
            if (topY <= world.getMinBuildHeight() + 1) {
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

    private static boolean isSpawnBaseValid(Level world, BlockPos below, BlockState state) {
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
                if (topY <= world.getMinBuildHeight() + 1) {
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
            if (space > 0) s += Component.translatable("keyword.ordertocook.no_space").getString() + "(" + space + ")";
            if (water > 0) s += (s.isEmpty() ? "" : "，") + Component.translatable("keyword.ordertocook.water").getString() + "(" + water + ")";
            if (lowY > 0) s += (s.isEmpty() ? "" : "，") + Component.translatable("keyword.ordertocook.too_low").getString() + "(" + lowY + ")";
            return s.isEmpty() ? Component.translatable("keyword.ordertocook.unknown").getString() : s;
        }
        String describeForDelivery() {
            String s = "";
            if (space > 0) s += Component.translatable("keyword.ordertocook.no_space").getString() + "(" + space + ")";
            if (water > 0) s += (s.isEmpty() ? "" : "，") + Component.translatable("keyword.ordertocook.water").getString() + "(" + water + ")";
            if (lowY > 0) s += (s.isEmpty() ? "" : "，") + Component.translatable("keyword.ordertocook.too_low").getString() + "(" + lowY + ")";
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
