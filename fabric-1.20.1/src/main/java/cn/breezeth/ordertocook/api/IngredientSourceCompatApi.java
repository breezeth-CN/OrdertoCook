package cn.breezeth.ordertocook.api;

import cn.breezeth.ordertocook.OrderToCookMod;
import cn.breezeth.ordertocook.block.entity.RefrigeratorBlockEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Public adapters for inventories that can supply countertop ingredients. */
public final class IngredientSourceCompatApi {
    public enum Probe { NOT_SOURCE, SOURCE }
    public interface Handler {
        Probe probe(World world, BlockPos pos);
        int count(World world, BlockPos pos, String itemId);
        int extract(World world, BlockPos pos, String itemId, int amount, boolean simulate);
    }
    private record Entry(Identifier id, int priority, Handler handler) {}
    private static final List<Entry> HANDLERS = new ArrayList<>();
    static {
        register(new Identifier("ordertocook", "refrigerator"), 0, new Handler() {
            public Probe probe(World world, BlockPos pos) { return world.getBlockEntity(pos) instanceof RefrigeratorBlockEntity ? Probe.SOURCE : Probe.NOT_SOURCE; }
            public int count(World world, BlockPos pos, String itemId) { return world.getBlockEntity(pos) instanceof RefrigeratorBlockEntity fridge ? fridge.countItem(itemId) : 0; }
            public int extract(World world, BlockPos pos, String itemId, int amount, boolean simulate) {
                if (!(world.getBlockEntity(pos) instanceof RefrigeratorBlockEntity fridge) || amount <= 0) return 0;
                return simulate ? Math.min(amount, fridge.countItem(itemId)) : fridge.consumeItem(itemId, amount);
            }
        });
    }
    public static synchronized void register(Identifier id, int priority, Handler handler) {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(handler, "handler");
        if (HANDLERS.stream().anyMatch(entry -> entry.id().equals(id))) throw new IllegalArgumentException("Duplicate ingredient source handler: " + id);
        HANDLERS.add(new Entry(id, priority, handler));
        HANDLERS.sort(Comparator.comparingInt(Entry::priority).reversed().thenComparing(e -> e.id().toString()));
    }
    public static int countNearby(World world, BlockPos origin, int radius, String itemId) {
        if (itemId == null || itemId.isBlank()) return 0;
        return countAllNearby(world, origin, radius, List.of(itemId)).getOrDefault(itemId, 0);
    }
    public static Map<String, Integer> countAllNearby(World world, BlockPos origin, int radius, Collection<String> itemIds) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        if (world == null || origin == null || itemIds == null || radius < 0) return totals;
        for (String id : itemIds) if (id != null && !id.isBlank()) totals.putIfAbsent(id, 0);
        BlockPos.Mutable cursor = new BlockPos.Mutable(); int radiusSq = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (dx * dx + dy * dy + dz * dz > radiusSq) continue; cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz); BlockPos pos = cursor.toImmutable();
            for (Entry entry : snapshot()) try { if (entry.handler().probe(world, pos) == Probe.NOT_SOURCE) continue; for (String id : totals.keySet()) totals.put(id, totals.get(id) + Math.max(0, entry.handler().count(world, pos, id))); break; } catch (Throwable error) { OrderToCookMod.LOGGER.error("Ingredient source handler {} failed at {}", entry.id(), pos, error); }
        }
        return Map.copyOf(totals);
    }
    public static int extractNearby(World world, BlockPos origin, int radius, String itemId, int amount, boolean simulate) { return transferNearby(world, origin, radius, itemId, amount, simulate); }
    private static int transferNearby(World world, BlockPos origin, int radius, String itemId, int amount, boolean simulate) {
        if (world == null || origin == null || itemId == null || itemId.isBlank() || radius < 0 || amount <= 0 || (!simulate && world.isClient)) return 0;
        int total = 0; BlockPos.Mutable cursor = new BlockPos.Mutable(); int radiusSq = radius * radius;
        for (int dy = -radius; dy <= radius && total < amount; dy++) for (int dx = -radius; dx <= radius && total < amount; dx++) for (int dz = -radius; dz <= radius && total < amount; dz++) {
            if (dx * dx + dy * dy + dz * dz > radiusSq) continue;
            cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
            BlockPos pos = cursor.toImmutable();
            for (Entry entry : snapshot()) {
                try {
                    if (entry.handler().probe(world, pos) == Probe.NOT_SOURCE) continue;
                    int moved = simulate ? entry.handler().count(world, pos, itemId) : entry.handler().extract(world, pos, itemId, amount - total, false);
                    total += Math.min(Math.max(0, moved), amount - total);
                    break;
                } catch (Throwable error) { OrderToCookMod.LOGGER.error("Ingredient source handler {} failed at {}", entry.id(), pos, error); }
            }
        }
        return total;
    }
    private static synchronized List<Entry> snapshot() { return List.copyOf(HANDLERS); }
    private IngredientSourceCompatApi() {}
}
