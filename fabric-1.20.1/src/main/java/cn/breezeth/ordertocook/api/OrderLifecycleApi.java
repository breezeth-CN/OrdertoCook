package cn.breezeth.ordertocook.api;

import cn.breezeth.ordertocook.OrderToCookMod;
import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.util.DataCompat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/** Read-only notifications for the order lifecycle. Listeners cannot cancel settlement. */
public final class OrderLifecycleApi {
    public enum Type { GENERATED, ACCEPTED, PACKED, COMPLETED, EXPIRED, CANCELLED }
    public record OrderView(String orderId,String customerName,int machineId,boolean delivery,boolean longDistance,boolean urgent,long expiryTick,int prestige,Map<String,Integer> foods) {}
    public record Event(Type type,ServerWorld world,@Nullable PlayerEntity player,OrderView order) {}
    @FunctionalInterface public interface Listener { void onOrderEvent(Event event); }
    private record Entry(Identifier id,int priority,Listener listener){} private static final List<Entry> LISTENERS=new ArrayList<>();
    public static synchronized void register(Identifier id,int priority,Listener listener){Objects.requireNonNull(id,"id");Objects.requireNonNull(listener,"listener");if(LISTENERS.stream().anyMatch(e->e.id().equals(id)))throw new IllegalArgumentException("Duplicate order lifecycle listener: "+id);LISTENERS.add(new Entry(id,priority,listener));LISTENERS.sort(Comparator.comparingInt(Entry::priority).reversed().thenComparing(e->e.id().toString()));}
    public static void emit(Type type,ServerWorld world,@Nullable PlayerEntity player,ItemStack stack){if(type==null||world==null||stack==null||stack.isEmpty())return;NbtCompound nbt=DataCompat.copy(stack);if(nbt==null)return;emit(type,world,player,view(nbt));}
    public static void emit(Type type,ServerWorld world,@Nullable PlayerEntity player,OrderView view){if(type==null||world==null||view==null)return;Event event=new Event(type,world,player,view);for(Entry entry:snapshot())try{entry.listener().onOrderEvent(event);}catch(Throwable error){OrderToCookMod.LOGGER.error("Order lifecycle listener {} failed for {}",entry.id(),type,error);}}
    private static OrderView view(NbtCompound nbt){Map<String,Integer> foods=new LinkedHashMap<>();if(nbt.contains(ModConstants.NBT_FOOD_LIST)){NbtCompound list=nbt.getCompound(ModConstants.NBT_FOOD_LIST);for(String id:list.getKeys())foods.put(id,list.getInt(id));}return new OrderView(nbt.getString(ModConstants.NBT_ORDER_ID),nbt.getString(ModConstants.NBT_CUSTOMER_NAME),nbt.getInt(ModConstants.NBT_MACHINE_ID),nbt.getBoolean(ModConstants.NBT_DELIVERY),nbt.getBoolean(ModConstants.NBT_IS_LONG_DISTANCE),nbt.getBoolean(ModConstants.NBT_URGENT),nbt.getLong(ModConstants.NBT_EXPIRY_TICK),nbt.getInt(ModConstants.NBT_PRESTIGE),Map.copyOf(foods));}
    private static synchronized List<Entry> snapshot(){return List.copyOf(LISTENERS);}private OrderLifecycleApi(){}
}
