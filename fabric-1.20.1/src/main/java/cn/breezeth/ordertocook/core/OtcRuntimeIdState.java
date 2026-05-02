package cn.breezeth.ordertocook.core;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

public final class OtcRuntimeIdState extends PersistentState {
    private int nextOrderId = 1;
    private int nextCustomerId = 1;

    public static OtcRuntimeIdState get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(
                OtcRuntimeIdState::fromNbt,
                OtcRuntimeIdState::new,
                "ordertocook_runtime_ids"
        );
    }

    public static OtcRuntimeIdState fromNbt(NbtCompound nbt) {
        OtcRuntimeIdState state = new OtcRuntimeIdState();
        if (nbt != null && !nbt.isEmpty()) {
            state.nextOrderId = Math.max(1, nbt.getInt("nextOrderId"));
            state.nextCustomerId = Math.max(1, nbt.getInt("nextCustomerId"));
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("nextOrderId", nextOrderId);
        nbt.putInt("nextCustomerId", nextCustomerId);
        return nbt;
    }

    public String allocateOrderId() {
        String id = String.format("dd%06d", nextOrderId);
        nextOrderId++;
        markDirty();
        return id;
    }

    public String allocateCustomerId() {
        String id = String.format("gk%06d", nextCustomerId);
        nextCustomerId++;
        markDirty();
        return id;
    }

    private OtcRuntimeIdState() {
    }
}
