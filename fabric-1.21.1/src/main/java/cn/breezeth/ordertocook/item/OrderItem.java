package cn.breezeth.ordertocook.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.tooltip.TooltipType;
import cn.breezeth.ordertocook.util.DataCompat;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.core.OrderNpcManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;

public class OrderItem extends Item {
    public OrderItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, net.minecraft.item.Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null) return;

        long expiryTick = nbt.contains(ModConstants.NBT_EXPIRY_TICK) ? nbt.getLong(ModConstants.NBT_EXPIRY_TICK) : -1;
        long nowTick = getWorldTickFromTooltipContext(context);
        if (expiryTick >= 0) {
            if (nowTick >= 0) {
                long remainingTicks = expiryTick - nowTick;
                if (remainingTicks <= 0) {
                    tooltip.add(Text.translatable("tooltip.ordertocook.expired").formatted(Formatting.RED));
                    return;
                }
                long totalSeconds = remainingTicks / 20;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                tooltip.add(Text.translatable("tooltip.ordertocook.time_left", String.format("%02d:%02d", minutes, seconds)).formatted(Formatting.GRAY));
            } else if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
                long expiryMs = nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
                long remainMs = expiryMs - System.currentTimeMillis();
                if (remainMs <= 0) {
                    tooltip.add(Text.translatable("tooltip.ordertocook.expired").formatted(Formatting.RED));
                    return;
                }
                long totalSeconds = remainMs / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                tooltip.add(Text.translatable("tooltip.ordertocook.time_left", String.format("%02d:%02d", minutes, seconds)).formatted(Formatting.GRAY));
            }
        }

        tooltip.add(Text.literal("----------").formatted(Formatting.GRAY));

        if (nbt.contains("FoodList")) {
            NbtCompound foodList = nbt.getCompound("FoodList");
            int orderType = nbt.getInt("Type");
            int limit = Integer.MAX_VALUE;
            
            // Check context for truncation
            // We use a safe check that doesn't crash on server
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                if (isInsideOrderMachine()) {
                    if (orderType == 3) limit = 3; // Purple: show 3
                    if (orderType == 4) limit = 3; // Red: show 3
                }
            }

            int countIndex = 0;
            for (String key : foodList.getKeys()) {
                if (countIndex >= limit) {
                    tooltip.add(Text.literal("...").formatted(Formatting.GRAY));
                    break;
                }
                
                int count = foodList.getInt(key);
                Identifier id = normalizeItemIdentifier(key);
                Item item = (id != null && Registries.ITEM.containsId(id)) ? Registries.ITEM.get(id) : null;
                Text foodName = item != null ? item.getName()
                        : (id != null ? Text.translatable("item." + id.getNamespace() + "." + id.getPath()) : Text.literal(key));
                tooltip.add(Text.literal("- ")
                        .formatted(Formatting.WHITE)
                        .append(foodName)
                        .append(Text.literal(" x" + count)));
                countIndex++;
            }
        }

        if (nbt.contains("delivery_pos")) {
            NbtCompound pos = nbt.getCompound("delivery_pos");
            tooltip.add(Text.translatable("tooltip.ordertocook.delivery_to", pos.getInt("x"), pos.getInt("z")).formatted(Formatting.GRAY));
        }

        if (nbt.contains("Prestige")) {
            int prestige = nbt.getInt("Prestige");
            tooltip.add(Text.translatable("tooltip.ordertocook.coin_reward", prestige).formatted(Formatting.GOLD));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient) return;
        if (!(entity instanceof PlayerEntity player)) return;
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null) return;
        if (ensureExpiryTick(world, stack, nbt)) {
            nbt = DataCompat.copy(stack);
            if (nbt == null) return;
        }
        if (!nbt.contains(ModConstants.NBT_EXPIRY_TIME) && nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
            long expiryTick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
            long remainTicks = Math.max(0, expiryTick - world.getTime());
            long expiryMs = System.currentTimeMillis() + remainTicks * 50L;
            nbt.putLong(ModConstants.NBT_EXPIRY_TIME, expiryMs);
            DataCompat.set(stack, nbt);
        }
        if (!nbt.contains(ModConstants.NBT_EXPIRY_TICK)) return;

        long expiryTick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
        long remainingTicks = expiryTick - world.getTime();
        if (remainingTicks <= 0) {
            if (nbt.getBoolean("ExpiryExpiredNotified")) return;

            String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
            Text customerText = (customer == null || customer.isBlank()) 
                    ? Text.translatable("keyword.ordertocook.customer") 
                    : Text.literal(customer);
            player.sendMessage(Text.translatable("message.ordertocook.order_expired_named", customerText).formatted(Formatting.RED), false);
            nbt.putBoolean("ExpiryExpiredNotified", true);
            DataCompat.set(stack, nbt);

            if (world instanceof ServerWorld sw) {
                String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
                OrderNpcManager.despawnFor(sw, player, orderId, OrderNpcManager.DespawnReason.EXPIRED);
            }
            return;
        }

        if (remainingTicks <= 60 * 20) {
            if (!nbt.getBoolean("ExpiryWarned1m")) {
                String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
                if (customer == null || customer.isBlank()) customer = "椤惧";
                player.sendMessage(Text.translatable("message.ordertocook.order_expiring_soon_1", customer).formatted(Formatting.RED), false);
                nbt.putBoolean("ExpiryWarned1m", true);
                DataCompat.set(stack, nbt);
            }
        } else if (remainingTicks <= 5 * 60 * 20) {
            if (!nbt.getBoolean("ExpiryWarned5m")) {
                String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
                if (customer == null || customer.isBlank()) customer = "椤惧";
                player.sendMessage(Text.translatable("message.ordertocook.order_expiring_soon_5", customer).formatted(Formatting.YELLOW), false);
                nbt.putBoolean("ExpiryWarned5m", true);
                DataCompat.set(stack, nbt);
            }
        }
    }

    private static long getWorldTickFromTooltipContext(net.minecraft.item.Item.TooltipContext context) {
        try {
            java.lang.reflect.Method m = context.getClass().getMethod("getWorld");
            Object w = m.invoke(context);
            if (w instanceof World world) return world.getTime();
        } catch (Exception ignored) {
        }
        return getClientWorldTick();
    }

    private static long getClientWorldTick() {
        try {
            Class<?> clientClass = Class.forName("net.minecraft.client.MinecraftClient");
            Object client = clientClass.getMethod("getInstance").invoke(null);
            Object w = clientClass.getField("world").get(client);
            if (w instanceof World world) return world.getTime();
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static boolean ensureExpiryTick(World world, ItemStack stack, NbtCompound nbt) {
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) return false;
        if (!nbt.contains(ModConstants.NBT_EXPIRY_TIME)) return false;

        long expiryTime = nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
        long remainingMs = expiryTime - System.currentTimeMillis();
        long expiryTick;
        if (remainingMs <= 0) {
            expiryTick = world.getTime() - 1;
        } else {
            long remainingTicks = (remainingMs + 49) / 50;
            expiryTick = world.getTime() + remainingTicks;
        }

        nbt.putLong(ModConstants.NBT_EXPIRY_TICK, expiryTick);
        DataCompat.set(stack, nbt);
        return true;
    }

    private boolean isInsideOrderMachine() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return false;
        try {
            // Use reflection to check client screen without hard dependency on client classes
            Class<?> clientClass = Class.forName("net.minecraft.client.MinecraftClient");
            Object client = clientClass.getMethod("getInstance").invoke(null);
            Object screen = clientClass.getField("currentScreen").get(client);
            
            if (screen != null) {
                // Check if screen class name ends with OrderMachineScreen
                return screen.getClass().getName().endsWith("OrderMachineScreen");
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
        return false;
    }

    private static Identifier normalizeItemIdentifier(String raw) {
        String k = raw.trim();
        if (k.contains(":")) {
            Identifier id = Identifier.tryParse(k);
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
                return Identifier.tryParse(ns + ":" + path);
            }
        }
        if (k.contains(".")) {
            int dot = k.indexOf('.');
            String ns = k.substring(0, dot);
            String path = k.substring(dot + 1);
            return Identifier.tryParse(ns + ":" + path);
        }
        return Identifier.tryParse(k);
    }
}

