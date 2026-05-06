package cn.breezeth.ordertocook.core;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class MachineRankingState extends SavedData {
    public static final int INITIAL_ID = 10001;

    private final Map<Integer, Stats> machines = new HashMap<>();
    private int nextId = INITIAL_ID;

    public static class Stats {
        public String name = "";
        public String owner = "";
        public int level = 1;
        public int accepted = 0;
        public int delivery = 0;
        public int longDistance = 0;
        public int totalProfit = 0;
        public int deliveryProfit = 0;
        public int maxDeliveryDist = 0;
        public int walkIn = 0;
        public boolean isPlaced = false;
    }

    public static MachineRankingState get(ServerLevel world) {
        DimensionDataStorage manager = world.getDataStorage();
        return manager.computeIfAbsent(MachineRankingState::read, MachineRankingState::new, "ordertocook_machine_ranking");
    }

    public static MachineRankingState read(CompoundTag nbt) {
        MachineRankingState state = new MachineRankingState();
        state.nextId = nbt.contains("nextId") ? nbt.getInt("nextId") : INITIAL_ID;
        ListTag list = nbt.getList("machines", Tag.TAG_COMPOUND);
        for (Tag e : list) {
            CompoundTag comp = (CompoundTag) e;
            int id = comp.getInt("id");
            Stats s = new Stats();
            s.name = comp.getString("name");
            s.owner = comp.getString("owner");
            s.level = comp.getInt("level");
            s.accepted = comp.getInt("accepted");
            s.delivery = comp.getInt("delivery");
            s.longDistance = comp.getInt("longDistance");
            s.totalProfit = comp.getInt("totalProfit");
            s.deliveryProfit = comp.getInt("deliveryProfit");
            s.maxDeliveryDist = comp.getInt("maxDeliveryDist");
            s.walkIn = comp.getInt("walkIn");
            s.isPlaced = comp.contains("isPlaced") ? comp.getBoolean("isPlaced") : true;
            state.machines.put(id, s);
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putInt("nextId", nextId);
        ListTag list = new ListTag();
        for (Map.Entry<Integer, Stats> en : machines.entrySet()) {
            int id = en.getKey();
            Stats s = en.getValue();
            CompoundTag c = new CompoundTag();
            c.putInt("id", id);
            c.putString("name", s.name);
            c.putString("owner", s.owner);
            c.putInt("level", s.level);
            c.putInt("accepted", s.accepted);
            c.putInt("delivery", s.delivery);
            c.putInt("longDistance", s.longDistance);
            c.putInt("totalProfit", s.totalProfit);
            c.putInt("deliveryProfit", s.deliveryProfit);
            c.putInt("maxDeliveryDist", s.maxDeliveryDist);
            c.putInt("walkIn", s.walkIn);
            c.putBoolean("isPlaced", s.isPlaced);
            list.add(c);
        }
        nbt.put("machines", list);
        return nbt;
    }

    public int allocateId() {
        int id = nextId;
        nextId = Math.max(nextId + 1, id + 1);
        setDirty();
        return id;
    }

    public Stats getOrCreate(int id) {
        return machines.computeIfAbsent(id, k -> new Stats());
    }

    public void put(int id, Stats stats) {
        machines.put(id, stats);
        setDirty();
    }

    public void applyCompletedDelta(int id, int coin, boolean delivery, boolean isLongDistance, int deliveryDist, boolean walkIn) {
        Stats prev = machines.computeIfAbsent(id, k -> new Stats());
        prev.accepted += 1;
        prev.totalProfit += Math.max(0, coin);
        if (delivery && deliveryDist > 0) {
            prev.delivery += 1;
            prev.deliveryProfit += Math.max(0, coin);
            if (isLongDistance) prev.longDistance += 1;
            if (deliveryDist > prev.maxDeliveryDist) prev.maxDeliveryDist = deliveryDist;
        }
        if (walkIn) prev.walkIn += 1;
        setDirty();
    }

    public Map<Integer, Stats> all() {
        return new HashMap<>(machines);
    }

    public void setPlaced(int id, boolean placed) {
        Stats s = machines.computeIfAbsent(id, k -> new Stats());
        s.isPlaced = placed;
        setDirty();
    }
}
