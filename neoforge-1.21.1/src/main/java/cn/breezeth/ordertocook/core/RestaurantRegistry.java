package cn.breezeth.ordertocook.core;

import cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class RestaurantRegistry {
    // 在线排行缓存：仅包含当前世界中“已放置”的机器，以机器ID为键
    private static final ConcurrentHashMap<Integer, OrderMachineBlockEntity.RestaurantStats> ONLINE = new ConcurrentHashMap<>();
    private static final ResourceLocation GOLDEN_RESTAURANT_ADVANCEMENT = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "ordertocook/golden_restaurant");
    private static final int GOLDEN_RESTAURANT_MIN_OWNED = 4;

    private RestaurantRegistry() {}

    public static void registerById(ServerLevel world, int id, OrderMachineBlockEntity be) {
        MachineRankingState state = MachineRankingState.get(world);
        MachineRankingState.Stats s = state.getOrCreate(id);
        String name = be.snapshotStats().name();
        String owner = be.snapshotStats().owner();
        int level = be.getRestaurantLevel();
        s.name = name;
        s.owner = owner;
        s.level = level;
        state.setPlaced(id, true);
        state.put(id, s);
        OrderMachineBlockEntity.RestaurantStats online = new OrderMachineBlockEntity.RestaurantStats(
                world.dimension().location().toString(),
                be.getBlockPos().asLong(),
                name,
                owner,
                level,
                s.accepted,
                s.delivery,
                s.longDistance,
                s.totalProfit,
                s.deliveryProfit,
                s.maxDeliveryDist,
                s.walkIn
        );
        ONLINE.put(id, online);
        tryGrantGoldenRestaurant(world);
    }

    public static void unregisterById(ServerLevel world, int id) {
        ONLINE.remove(id);
        MachineRankingState state = MachineRankingState.get(world);
        state.setPlaced(id, false);
        // 不删除持久化，以保证掉落/未放置期间数据保留（但 isPlaced=false 不会出现在榜单）
    }

    public static void update(OrderMachineBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel sw)) return;
        int id = be.getMachineId();
        if (id <= 0) return;
        MachineRankingState state = MachineRankingState.get(sw);
        MachineRankingState.Stats s = state.getOrCreate(id);
        s.name = be.snapshotStats().name();
        s.owner = be.snapshotStats().owner();
        s.level = be.getRestaurantLevel();
        state.put(id, s);
        OrderMachineBlockEntity.RestaurantStats online = new OrderMachineBlockEntity.RestaurantStats(
                sw.dimension().location().toString(),
                be.getBlockPos().asLong(),
                s.name,
                s.owner,
                s.level,
                s.accepted,
                s.delivery,
                s.longDistance,
                s.totalProfit,
                s.deliveryProfit,
                s.maxDeliveryDist,
                s.walkIn
        );
        ONLINE.put(id, online);
        tryGrantGoldenRestaurant(sw);
    }

    public static void applyCompletedDeltaById(ServerLevel world, int id, int coin, boolean delivery, boolean isLongDistance, int deliveryDist, boolean walkIn) {
        MachineRankingState state = MachineRankingState.get(world);
        state.applyCompletedDelta(id, coin, delivery, isLongDistance, deliveryDist, walkIn);

        // 同步在线缓存（如果该ID当前已放置）
        OrderMachineBlockEntity.RestaurantStats prev = ONLINE.get(id);
        String name = prev == null ? state.getOrCreate(id).name : prev.name();
        String owner = prev == null ? state.getOrCreate(id).owner : prev.owner();
        int level = prev == null ? state.getOrCreate(id).level : prev.level();
        int accepted = (prev == null ? state.getOrCreate(id).accepted : prev.accepted()) + 1;
        int deliveryCnt = prev == null ? state.getOrCreate(id).delivery : prev.delivery();
        int longCnt = prev == null ? state.getOrCreate(id).longDistance : prev.longDistance();
        int totalProfit = (prev == null ? state.getOrCreate(id).totalProfit : prev.totalProfit()) + Math.max(0, coin);
        int deliveryProfit = prev == null ? state.getOrCreate(id).deliveryProfit : prev.deliveryProfit();
        int maxDist = prev == null ? state.getOrCreate(id).maxDeliveryDist : prev.maxDeliveryDist();
        int walkInCnt = prev == null ? state.getOrCreate(id).walkIn : prev.walkIn();
        if (delivery && deliveryDist > 0) {
            deliveryCnt += 1;
            deliveryProfit += Math.max(0, coin);
            if (isLongDistance) longCnt += 1;
            if (deliveryDist > maxDist) maxDist = deliveryDist;
        }
        if (walkIn) {
            walkInCnt += 1;
        }
        String dimension = prev == null ? "" : prev.dimension();
        long posLong = prev == null ? 0L : prev.posLong();
        OrderMachineBlockEntity.RestaurantStats updated = new OrderMachineBlockEntity.RestaurantStats(
                dimension, posLong, name, owner, level,
                accepted, deliveryCnt, longCnt, totalProfit, deliveryProfit, maxDist, walkInCnt
        );
        ONLINE.put(id, updated);
        tryGrantGoldenRestaurant(world);
    }

    public static void applyProfitBonusById(ServerLevel world, int id, int coin, boolean delivery, boolean isLongDistance, int deliveryDist, boolean walkIn) {
        if (coin <= 0) {
            return;
        }
        MachineRankingState state = MachineRankingState.get(world);
        MachineRankingState.Stats stats = state.getOrCreate(id);
        stats.totalProfit += Math.max(0, coin);
        if (delivery) {
            stats.deliveryProfit += Math.max(0, coin);
            if (deliveryDist > stats.maxDeliveryDist) {
                stats.maxDeliveryDist = deliveryDist;
            }
        }
        state.put(id, stats);

        OrderMachineBlockEntity.RestaurantStats prev = ONLINE.get(id);
        if (prev != null) {
            ONLINE.put(id, new OrderMachineBlockEntity.RestaurantStats(
                    prev.dimension(),
                    prev.posLong(),
                    prev.name(),
                    prev.owner(),
                    prev.level(),
                    prev.accepted(),
                    prev.delivery(),
                    prev.longDistance(),
                    prev.totalProfit() + Math.max(0, coin),
                    prev.deliveryProfit() + (delivery ? Math.max(0, coin) : 0),
                    Math.max(prev.maxDeliveryDist(), delivery ? deliveryDist : prev.maxDeliveryDist()),
                    prev.walkIn()
            ));
        }
        tryGrantGoldenRestaurant(world);
    }

    public static OrderMachineBlockEntity.RestaurantStats getStatsById(ServerLevel world, int id) {
        OrderMachineBlockEntity.RestaurantStats online = ONLINE.get(id);
        if (online != null) {
            return online;
        }
        MachineRankingState.Stats stats = MachineRankingState.get(world).all().get(id);
        if (stats == null) {
            return null;
        }
        return new OrderMachineBlockEntity.RestaurantStats(
                "",
                0L,
                stats.name,
                stats.owner,
                stats.level,
                stats.accepted,
                stats.delivery,
                stats.longDistance,
                stats.totalProfit,
                stats.deliveryProfit,
                stats.maxDeliveryDist,
                stats.walkIn
        );
    }

    public static List<OrderMachineBlockEntity.RestaurantStats> allOnline() {
        return new ArrayList<>(ONLINE.values());
    }

    public static List<OrderMachineBlockEntity.RestaurantStats> allPersistedWithOnline(ServerLevel world) {
        MachineRankingState state = MachineRankingState.get(world);
        Map<Integer, MachineRankingState.Stats> map = state.all();
        List<OrderMachineBlockEntity.RestaurantStats> list = new ArrayList<>();
        for (Map.Entry<Integer, MachineRankingState.Stats> en : map.entrySet()) {
            int id = en.getKey();
            MachineRankingState.Stats s = en.getValue();
            OrderMachineBlockEntity.RestaurantStats online = ONLINE.get(id);
            if (online == null && !s.isPlaced) continue;
            String dimension = online != null ? online.dimension() : "";
            long posLong = online != null ? online.posLong() : 0L;
            list.add(new OrderMachineBlockEntity.RestaurantStats(
                    dimension,
                    posLong,
                    s.name,
                    s.owner,
                    s.level,
                    s.accepted,
                    s.delivery,
                    s.longDistance,
                    s.totalProfit,
                    s.deliveryProfit,
                    s.maxDeliveryDist,
                    s.walkIn
            ));
        }
        return list;
    }

    private static void tryGrantGoldenRestaurant(ServerLevel world) {
        if (world.getServer() == null) {
            return;
        }
        List<OrderMachineBlockEntity.RestaurantStats> list = allPersistedWithOnline(world);
        if (list.isEmpty()) {
            return;
        }
        int topProfit = list.stream()
                .mapToInt(OrderMachineBlockEntity.RestaurantStats::totalProfit)
                .max()
                .orElse(Integer.MIN_VALUE);
        for (ServerPlayer player : world.getServer().getPlayerList().getPlayers()) {
            String ownerName = player.getGameProfile().getName();
            long ownedCount = list.stream()
                    .filter(stats -> ownerName.equals(stats.owner()))
                    .count();
            if (ownedCount < GOLDEN_RESTAURANT_MIN_OWNED) {
                continue;
            }
            boolean isTopProfitOwner = list.stream()
                    .anyMatch(stats -> ownerName.equals(stats.owner()) && stats.totalProfit() == topProfit);
            if (isTopProfitOwner) {
                grantAdvancement(player, GOLDEN_RESTAURANT_ADVANCEMENT);
            }
        }
    }

    private static void grantAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        if (player.getServer() == null) {
            return;
        }
        var advancement = player.getServer().getAdvancements().get(advancementId);
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            return;
        }
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
