package cn.breezeth.ordertocook.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import cn.breezeth.ordertocook.util.DataCompat;
import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.core.OrderNpcManager;
import java.util.List;

public class OrderItem extends Item {
    public OrderItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, tooltip, type);

        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt == null) return;

        long expiryTick = nbt.contains(ModConstants.NBT_EXPIRY_TICK) ? nbt.getLong(ModConstants.NBT_EXPIRY_TICK) : -1;
        long nowTick = getWorldTickFromTooltipContext(context);
        if (expiryTick >= 0) {
            if (nowTick >= 0) {
                long remainingTicks = expiryTick - nowTick;
                if (remainingTicks <= 0) {
                    tooltip.add(Component.translatable("tooltip.ordertocook.expired").withStyle(ChatFormatting.RED));
                    return;
                }
                long totalSeconds = remainingTicks / 20;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                tooltip.add(Component.translatable("tooltip.ordertocook.time_left", String.format("%02d:%02d", minutes, seconds)).withStyle(ChatFormatting.GRAY));
            } else if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
                long expiryMs = nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
                long remainMs = expiryMs - System.currentTimeMillis();
                if (remainMs <= 0) {
                    tooltip.add(Component.translatable("tooltip.ordertocook.expired").withStyle(ChatFormatting.RED));
                    return;
                }
                long totalSeconds = remainMs / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                tooltip.add(Component.translatable("tooltip.ordertocook.time_left", String.format("%02d:%02d", minutes, seconds)).withStyle(ChatFormatting.GRAY));
            }
        }

        tooltip.add(Component.literal("----------").withStyle(ChatFormatting.GRAY));

        if (nbt.contains("FoodList")) {
            CompoundTag foodList = nbt.getCompound("FoodList");
            int orderType = nbt.getInt("Type");
            int limit = Integer.MAX_VALUE;
            
            // Check context for truncation
            // We use a safe check that doesn't crash on server
            if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                if (isInsideOrderMachine()) {
                    if (orderType == 3) limit = 3; // Purple: show 3
                    if (orderType == 4) limit = 3; // Red: show 3
                }
            }

            int countIndex = 0;
            for (String key : foodList.getAllKeys()) {
                if (countIndex >= limit) {
                    tooltip.add(Component.literal("...").withStyle(ChatFormatting.GRAY));
                    break;
                }
                
                int count = foodList.getInt(key);
                ResourceLocation id = normalizeItemIdentifier(key);
                Item item = (id != null && BuiltInRegistries.ITEM.containsKey(id)) ? BuiltInRegistries.ITEM.get(id) : null;
                Component foodName = item != null ? item.getDescription()
                        : (id != null ? Component.translatable("item." + id.getNamespace() + "." + id.getPath()) : Component.literal(key));
                tooltip.add(Component.literal("- ")
                        .withStyle(ChatFormatting.WHITE)
                        .append(foodName)
                        .append(Component.literal(" x" + count)));
                countIndex++;
            }
        }

        if (nbt.contains("delivery_pos")) {
            CompoundTag pos = nbt.getCompound("delivery_pos");
            tooltip.add(Component.translatable("tooltip.ordertocook.delivery_to", pos.getInt("x"), pos.getInt("z")).withStyle(ChatFormatting.GRAY));
        }

        if (nbt.contains("Prestige")) {
            int prestige = nbt.getInt("Prestige");
            tooltip.add(Component.translatable("tooltip.ordertocook.coin_reward", prestige).withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (world.isClientSide) return;
        if (!(entity instanceof Player player)) return;
        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt == null) return;
        if (ensureExpiryTick(world, stack, nbt)) {
            nbt = DataCompat.copy(stack);
            if (nbt == null) return;
        }
        if (!nbt.contains(ModConstants.NBT_EXPIRY_TIME) && nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
            long expiryTick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
            long remainTicks = Math.max(0, expiryTick - world.getGameTime());
            long expiryMs = System.currentTimeMillis() + remainTicks * 50L;
            nbt.putLong(ModConstants.NBT_EXPIRY_TIME, expiryMs);
            DataCompat.set(stack, nbt);
        }
        if (!nbt.contains(ModConstants.NBT_EXPIRY_TICK)) return;

        long expiryTick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
        long remainingTicks = expiryTick - world.getGameTime();
        if (remainingTicks <= 0) {
            if (nbt.getBoolean("ExpiryExpiredNotified")) return;

            String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
            Component customerText = (customer == null || customer.isBlank()) 
                    ? Component.translatable("keyword.ordertocook.customer") 
                    : Component.literal(customer);
            player.displayClientMessage(Component.translatable("message.ordertocook.order_expired_named", customerText).withStyle(ChatFormatting.RED), false);
            nbt.putBoolean("ExpiryExpiredNotified", true);
            DataCompat.set(stack, nbt);

            if (world instanceof ServerLevel sw) {
                String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
                OrderNpcManager.despawnFor(sw, player, orderId, OrderNpcManager.DespawnReason.EXPIRED);
            }
            return;
        }

        if (remainingTicks <= 60 * 20) {
            if (!nbt.getBoolean("ExpiryWarned1m")) {
                String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
                if (customer == null || customer.isBlank()) customer = "椤惧";
                player.displayClientMessage(Component.translatable("message.ordertocook.order_expiring_soon_1", customer).withStyle(ChatFormatting.RED), false);
                nbt.putBoolean("ExpiryWarned1m", true);
                DataCompat.set(stack, nbt);
            }
        } else if (remainingTicks <= 5 * 60 * 20) {
            if (!nbt.getBoolean("ExpiryWarned5m")) {
                String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
                if (customer == null || customer.isBlank()) customer = "椤惧";
                player.displayClientMessage(Component.translatable("message.ordertocook.order_expiring_soon_5", customer).withStyle(ChatFormatting.YELLOW), false);
                nbt.putBoolean("ExpiryWarned5m", true);
                DataCompat.set(stack, nbt);
            }
        }
    }

    private static long getWorldTickFromTooltipContext(net.minecraft.world.item.Item.TooltipContext context) {
        try {
            java.lang.reflect.Method m = context.getClass().getMethod("getWorld");
            Object w = m.invoke(context);
            if (w instanceof Level world) return world.getGameTime();
        } catch (Exception ignored) {
        }
        return getClientWorldTick();
    }

    private static long getClientWorldTick() {
        try {
        Class<?> clientClass = Class.forName("net.minecraft.client.Minecraft");
        Object client = clientClass.getMethod("getInstance").invoke(null);
        Object w = clientClass.getField("level").get(client);
            if (w instanceof Level world) return world.getGameTime();
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static boolean ensureExpiryTick(Level world, ItemStack stack, CompoundTag nbt) {
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) return false;
        if (!nbt.contains(ModConstants.NBT_EXPIRY_TIME)) return false;

        long expiryTime = nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
        long remainingMs = expiryTime - System.currentTimeMillis();
        long expiryTick;
        if (remainingMs <= 0) {
            expiryTick = world.getGameTime() - 1;
        } else {
            long remainingTicks = (remainingMs + 49) / 50;
            expiryTick = world.getGameTime() + remainingTicks;
        }

        nbt.putLong(ModConstants.NBT_EXPIRY_TICK, expiryTick);
        DataCompat.set(stack, nbt);
        return true;
    }

    private boolean isInsideOrderMachine() {
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) return false;
        try {
            // Use reflection to check client screen without hard dependency on client classes
            Class<?> clientClass = Class.forName("net.minecraft.client.Minecraft");
            Object client = clientClass.getMethod("getInstance").invoke(null);
            Object screen = clientClass.getField("screen").get(client);
            
            if (screen != null) {
                // Check if screen class name ends with OrderMachineScreen
                return screen.getClass().getName().endsWith("OrderMachineScreen");
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
        return false;
    }

    private static ResourceLocation normalizeItemIdentifier(String raw) {
        String k = raw.trim();
        if (k.contains(":")) {
            ResourceLocation id = ResourceLocation.tryParse(k);
            if (id != null) return id;
        }
        if (k.startsWith("item.") || k.startsWith("block.")) {
            String tmp = k.substring(k.indexOf('.') + 1);
            int idx = tmp.lastIndexOf('_');
            if (idx > 0) {
                String suff = tmp.substring(idx + 1).toLowerCase();
                if (suff.equals("en_us") || suff.equals("zh_cn") || suff.equals("de_de") || suff.equals("ja_jp") || suff.equals("ko_kr") || suff.equals("ru_ru") || suff.equals("es_es") || suff.equals("es_mx") || suff.equals("pt_br") || suff.equals("fr_fr") || suff.equals("it_it") || suff.equals("pl_pl") || suff.equals("tr_tr") || suff.equals("cs_cz") || suff.equals("hu_hu") || suff.equals("nl_nl") || suff.equals("sv_se") || suff.equals("uk_ua")) {
                    tmp = tmp.substring(0, idx);
                }
            }
            int dot = tmp.indexOf('.');
            if (dot > 0) {
                String ns = tmp.substring(0, dot);
                String path = tmp.substring(dot + 1);
                return ResourceLocation.tryParse(ns + ":" + path);
            }
        }
        if (k.contains(".")) {
            int dot = k.indexOf('.');
            String ns = k.substring(0, dot);
            String path = k.substring(dot + 1);
            return ResourceLocation.tryParse(ns + ":" + path);
        }
        return ResourceLocation.tryParse(k);
    }
}

