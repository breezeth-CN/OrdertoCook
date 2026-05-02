package cn.breezeth.ordertocook.core;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

public final class OtcRuntimeIdState extends PersistentState {
    private int nextOrderId = 1;
    private int nextCustomerId = 1;

    public static OtcRuntimeIdState get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(
                new PersistentState.Type<>(OtcRuntimeIdState::new, OtcRuntimeIdState::read, DataFixTypes.SAVED_DATA_COMMAND_STORAGE),
                "ordertocook_runtime_ids"
        );
    }

    public static OtcRuntimeIdState read(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        OtcRuntimeIdState state = new OtcRuntimeIdState();
        state.nextOrderId = Math.max(1, nbt.getInt("nextOrderId"));
        state.nextCustomerId = Math.max(1, nbt.getInt("nextCustomerId"));
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
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
}
