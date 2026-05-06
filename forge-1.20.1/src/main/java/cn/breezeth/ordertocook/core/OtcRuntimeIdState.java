package cn.breezeth.ordertocook.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public final class OtcRuntimeIdState extends SavedData {
    private int nextOrderId = 1;
    private int nextCustomerId = 1;

    public static OtcRuntimeIdState get(ServerLevel world) {
        DimensionDataStorage manager = world.getDataStorage();
        return manager.computeIfAbsent(OtcRuntimeIdState::read, OtcRuntimeIdState::new, "ordertocook_runtime_ids");
    }

    public static OtcRuntimeIdState read(CompoundTag nbt) {
        OtcRuntimeIdState state = new OtcRuntimeIdState();
        state.nextOrderId = Math.max(1, nbt.getInt("nextOrderId"));
        state.nextCustomerId = Math.max(1, nbt.getInt("nextCustomerId"));
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putInt("nextOrderId", nextOrderId);
        nbt.putInt("nextCustomerId", nextCustomerId);
        return nbt;
    }

    public String allocateOrderId() {
        String id = String.format("dd%06d", nextOrderId);
        nextOrderId++;
        setDirty();
        return id;
    }

    public String allocateCustomerId() {
        String id = String.format("gk%06d", nextCustomerId);
        nextCustomerId++;
        setDirty();
        return id;
    }
}
