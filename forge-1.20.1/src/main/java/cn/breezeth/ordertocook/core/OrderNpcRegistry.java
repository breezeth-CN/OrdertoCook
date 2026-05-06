package cn.breezeth.ordertocook.core;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OrderNpcRegistry {
    private static final ConcurrentHashMap<UUID, Boolean> NPCS = new ConcurrentHashMap<>();
    public static void register(UUID id) {
        NPCS.put(id, Boolean.TRUE);
    }
    public static void unregister(UUID id) {
        NPCS.remove(id);
    }
    public static boolean isNpc(UUID id) {
        return NPCS.containsKey(id);
    }
    public static java.util.Set<UUID> ids() {
        return NPCS.keySet();
    }
    private OrderNpcRegistry() {}
}
