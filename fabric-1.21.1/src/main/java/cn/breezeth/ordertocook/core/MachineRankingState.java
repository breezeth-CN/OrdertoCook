package cn.breezeth.ordertocook.core;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.datafixer.DataFixTypes;

import java.util.HashMap;
import java.util.Map;

public class MachineRankingState extends PersistentState {
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

    public static MachineRankingState get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(
                new PersistentState.Type<>(MachineRankingState::new, MachineRankingState::read, DataFixTypes.SAVED_DATA_COMMAND_STORAGE),
                "ordertocook_machine_ranking"
        );
    }

    public static MachineRankingState read(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        MachineRankingState state = new MachineRankingState();
        state.nextId = nbt.contains("nextId") ? nbt.getInt("nextId") : INITIAL_ID;
        NbtList list = nbt.getList("machines", NbtElement.COMPOUND_TYPE);
        for (NbtElement e : list) {
            NbtCompound comp = (NbtCompound) e;
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
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putInt("nextId", nextId);
        NbtList list = new NbtList();
        for (Map.Entry<Integer, Stats> en : machines.entrySet()) {
            int id = en.getKey();
            Stats s = en.getValue();
            NbtCompound c = new NbtCompound();
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
        markDirty();
        return id;
    }

    public Stats getOrCreate(int id) {
        return machines.computeIfAbsent(id, k -> new Stats());
    }

    public void put(int id, Stats stats) {
        machines.put(id, stats);
        markDirty();
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
        markDirty();
    }

    public Map<Integer, Stats> all() {
        return new HashMap<>(machines);
    }

    public void setPlaced(int id, boolean placed) {
        Stats s = machines.computeIfAbsent(id, k -> new Stats());
        s.isPlaced = placed;
        markDirty();
    }
}
