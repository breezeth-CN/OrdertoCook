package cn.breezeth.ordertocook.core;

import cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;

public class RestaurantPersistentState extends PersistentState {
    private final Map<String, OrderMachineBlockEntity.RestaurantStats> data = new HashMap<>();

    public static RestaurantPersistentState get(ServerWorld world) {
        var manager = world.getServer().getOverworld().getPersistentStateManager();
        return manager.getOrCreate(RestaurantPersistentState::fromNbt, RestaurantPersistentState::new, "ordertocook_restaurant_stats");
    }

    public static RestaurantPersistentState fromNbt(NbtCompound nbt) {
        RestaurantPersistentState state = new RestaurantPersistentState();
        if (nbt == null) return state;
        NbtCompound entries = nbt.getCompound("entries");
        for (String key : entries.getKeys()) {
            NbtCompound e = entries.getCompound(key);
            String dim = e.getString("dimension");
            long pos = e.getLong("posLong");
            String name = e.getString("name");
            String owner = e.getString("owner");
            int level = e.getInt("level");
            int accepted = e.getInt("accepted");
            int delivery = e.getInt("delivery");
            int longDistance = e.getInt("longDistance");
            int totalProfit = e.getInt("totalProfit");
            int deliveryProfit = e.getInt("deliveryProfit");
            int maxDeliveryDist = e.getInt("maxDeliveryDist");
            int walkIn = e.getInt("walkIn");
            OrderMachineBlockEntity.RestaurantStats stats = new OrderMachineBlockEntity.RestaurantStats(
                    dim, pos, name, owner, level, accepted, delivery, longDistance, totalProfit, deliveryProfit, maxDeliveryDist, walkIn
            );
            state.data.put(key, stats);
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound entries = new NbtCompound();
        for (Map.Entry<String, OrderMachineBlockEntity.RestaurantStats> en : data.entrySet()) {
            OrderMachineBlockEntity.RestaurantStats s = en.getValue();
            NbtCompound e = new NbtCompound();
            e.putString("dimension", s.dimension());
            e.putLong("posLong", s.posLong());
            e.putString("name", s.name());
            e.putString("owner", s.owner());
            e.putInt("level", s.level());
            e.putInt("accepted", s.accepted());
            e.putInt("delivery", s.delivery());
            e.putInt("longDistance", s.longDistance());
            e.putInt("totalProfit", s.totalProfit());
            e.putInt("deliveryProfit", s.deliveryProfit());
            e.putInt("maxDeliveryDist", s.maxDeliveryDist());
            e.putInt("walkIn", s.walkIn());
            entries.put(en.getKey(), e);
        }
        nbt.put("entries", entries);
        return nbt;
    }

    public void put(String key, OrderMachineBlockEntity.RestaurantStats stats) {
        data.put(key, stats);
    }

    public void remove(String key) {
        data.remove(key);
    }

    public Map<String, OrderMachineBlockEntity.RestaurantStats> all() {
        return new HashMap<>(data);
    }
}
