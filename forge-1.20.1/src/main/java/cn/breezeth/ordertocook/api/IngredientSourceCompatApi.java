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
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Public adapters for inventories that can supply countertop ingredients. */
public final class IngredientSourceCompatApi {
    public enum Probe { NOT_SOURCE, SOURCE }
    public interface Handler { Probe probe(Level level, BlockPos pos); int count(Level level, BlockPos pos, String itemId); int extract(Level level, BlockPos pos, String itemId, int amount, boolean simulate); }
    private record Entry(ResourceLocation id, int priority, Handler handler) {}
    private static final List<Entry> HANDLERS = new ArrayList<>();
    static { register(new ResourceLocation("ordertocook", "refrigerator"), 0, new Handler() {
        public Probe probe(Level level, BlockPos pos) { return level.getBlockEntity(pos) instanceof RefrigeratorBlockEntity ? Probe.SOURCE : Probe.NOT_SOURCE; }
        public int count(Level level, BlockPos pos, String itemId) { return level.getBlockEntity(pos) instanceof RefrigeratorBlockEntity fridge ? fridge.countItem(itemId) : 0; }
        public int extract(Level level, BlockPos pos, String itemId, int amount, boolean simulate) { if (!(level.getBlockEntity(pos) instanceof RefrigeratorBlockEntity fridge) || amount <= 0) return 0; return simulate ? Math.min(amount, fridge.countItem(itemId)) : fridge.consumeItem(itemId, amount); }
    }); }
    public static synchronized void register(ResourceLocation id, int priority, Handler handler) { Objects.requireNonNull(id, "id"); Objects.requireNonNull(handler, "handler"); if (HANDLERS.stream().anyMatch(entry -> entry.id().equals(id))) throw new IllegalArgumentException("Duplicate ingredient source handler: " + id); HANDLERS.add(new Entry(id, priority, handler)); HANDLERS.sort(Comparator.comparingInt(Entry::priority).reversed().thenComparing(e -> e.id().toString())); }
    public static int countNearby(Level level, BlockPos origin, int radius, String itemId) {
        if (itemId == null || itemId.isBlank()) return 0;
        return countAllNearby(level, origin, radius, List.of(itemId)).getOrDefault(itemId, 0);
    }
    public static Map<String, Integer> countAllNearby(Level level, BlockPos origin, int radius, Collection<String> itemIds) {
        Map<String, Integer> totals = new LinkedHashMap<>(); if (level == null || origin == null || itemIds == null || radius < 0) return totals; for (String id : itemIds) if (id != null && !id.isBlank()) totals.putIfAbsent(id, 0); BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(); int radiusSq = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) { if (dx * dx + dy * dy + dz * dz > radiusSq) continue; cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz); BlockPos pos = cursor.immutable(); for (Entry entry : snapshot()) try { if (entry.handler().probe(level, pos) == Probe.NOT_SOURCE) continue; for (String id : totals.keySet()) totals.put(id, totals.get(id) + Math.max(0, entry.handler().count(level, pos, id))); break; } catch (Throwable error) { OrderToCookMod.LOGGER.error("Ingredient source handler {} failed at {}", entry.id(), pos, error); } }
        return Map.copyOf(totals);
    }
    public static int extractNearby(Level level, BlockPos origin, int radius, String itemId, int amount, boolean simulate) { return transferNearby(level, origin, radius, itemId, amount, simulate); }
    private static int transferNearby(Level level, BlockPos origin, int radius, String itemId, int amount, boolean simulate) {
        if (level == null || origin == null || itemId == null || itemId.isBlank() || radius < 0 || amount <= 0 || (!simulate && level.isClientSide)) return 0; int total = 0; BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(); int radiusSq = radius * radius;
        for (int dy = -radius; dy <= radius && total < amount; dy++) for (int dx = -radius; dx <= radius && total < amount; dx++) for (int dz = -radius; dz <= radius && total < amount; dz++) { if (dx * dx + dy * dy + dz * dz > radiusSq) continue; cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz); BlockPos pos = cursor.immutable(); for (Entry entry : snapshot()) try { if (entry.handler().probe(level, pos) == Probe.NOT_SOURCE) continue; int moved = simulate ? entry.handler().count(level, pos, itemId) : entry.handler().extract(level, pos, itemId, amount - total, false); total += Math.min(Math.max(0, moved), amount - total); break; } catch (Throwable error) { OrderToCookMod.LOGGER.error("Ingredient source handler {} failed at {}", entry.id(), pos, error); } }
        return total;
    }
    private static synchronized List<Entry> snapshot() { return List.copyOf(HANDLERS); }
    private IngredientSourceCompatApi() {}
}
