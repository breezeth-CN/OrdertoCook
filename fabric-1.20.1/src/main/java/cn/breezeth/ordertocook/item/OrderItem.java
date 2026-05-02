package cn.breezeth.ordertocook.item;
import cn.breezeth.ordertocook.util.DataCompat;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.core.OrderNpcManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;


public class OrderItem extends Item {
    public OrderItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null) return;
        boolean insideMachine = isInsideOrderMachine();
        boolean delivery = nbt.contains(ModConstants.NBT_DELIVERY) && nbt.getBoolean(ModConstants.NBT_DELIVERY);
        long remainMs = -1L;
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK) && world != null) {
            long tick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
            long remainTicks = Math.max(0, tick - world.getTime());
            remainMs = remainTicks * 50L;
        } else if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
            long expiry = nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
            remainMs = Math.max(0, expiry - System.currentTimeMillis());
        }
        if (remainMs >= 0) {
            long seconds = (remainMs + 999) / 1000;
            long minutes = seconds / 60;
            long secs = seconds % 60;
            String mm = String.format("%02d", minutes);
            String ss = String.format("%02d", secs);
            if (seconds <= 0) {
                tooltip.add(Text.translatable("tooltip.ordertocook.expired").formatted(Formatting.RED));
                tooltip.add(Text.translatable("tooltip.ordertocook.expired_desc").formatted(Formatting.DARK_RED));
            } else {
                tooltip.add(Text.translatable("tooltip.ordertocook.time_left", mm + ":" + ss).formatted(Formatting.GRAY));
            }
        }
        tooltip.add(Text.literal("----------").formatted(Formatting.GRAY));
        if (nbt.contains(ModConstants.NBT_FOOD_LIST)) {
            NbtCompound foodList = nbt.getCompound(ModConstants.NBT_FOOD_LIST);
            int type = nbt.contains("Type") ? nbt.getInt("Type") : -1;
            int limit = Integer.MAX_VALUE;
            if (insideMachine) {
                if (type == 3) limit = 3;
                if (type == 4) limit = 3;
            }
            int shown = 0;
            for (String key : foodList.getKeys()) {
                if (shown >= limit) {
                    tooltip.add(Text.literal("...").formatted(Formatting.GRAY));
                    break;
                }
                net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(key);
                if (id == null || !net.minecraft.registry.Registries.ITEM.containsId(id)) continue;
                net.minecraft.item.Item item = net.minecraft.registry.Registries.ITEM.get(id);
                int count = foodList.getInt(key);
                net.minecraft.item.ItemStack is = new net.minecraft.item.ItemStack(item);
                Text name = is.getName();
                tooltip.add(Text.literal("— ").formatted(Formatting.GRAY)
                        .append(name.copy().formatted(Formatting.WHITE))
                        .append(Text.literal(" x" + count).formatted(Formatting.GRAY)));
                shown++;
            }
        }
        if (delivery && nbt.contains(ModConstants.NBT_DELIVERY_POS)) {
            NbtCompound dp = nbt.getCompound(ModConstants.NBT_DELIVERY_POS);
            int tx = dp.getInt(ModConstants.NBT_X);
            int tz = dp.getInt(ModConstants.NBT_Z);
            tooltip.add(Text.translatable("tooltip.ordertocook.delivery_to", tx, tz).formatted(Formatting.GRAY));
        }
        int coin = nbt.contains(ModConstants.NBT_PRESTIGE) ? nbt.getInt(ModConstants.NBT_PRESTIGE) : 0;
        if (coin > 0) {
            tooltip.add(Text.translatable("tooltip.ordertocook.coin_reward", coin).formatted(Formatting.GOLD));
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
                if (customer == null || customer.isBlank()) customer = "顾客";
                player.sendMessage(Text.translatable("message.ordertocook.order_expiring_soon_1", customer).formatted(Formatting.RED), false);
                nbt.putBoolean("ExpiryWarned1m", true);
                DataCompat.set(stack, nbt);
            }
        } else if (remainingTicks <= 5 * 60 * 20) {
            if (!nbt.getBoolean("ExpiryWarned5m")) {
                String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
                if (customer == null || customer.isBlank()) customer = "顾客";
                player.sendMessage(Text.translatable("message.ordertocook.order_expiring_soon_5", customer).formatted(Formatting.YELLOW), false);
                nbt.putBoolean("ExpiryWarned5m", true);
                DataCompat.set(stack, nbt);
            }
        }
    }

    // Tooltip helpers removed for server compile compatibility

    private boolean isInsideOrderMachine() {
        try {
            Class<?> clientClass = Class.forName("net.minecraft.client.MinecraftClient");
            Object client = clientClass.getMethod("getInstance").invoke(null);
            Object screen = clientClass.getField("currentScreen").get(client);
            if (screen != null) {
                return screen.getClass().getName().endsWith("OrderMachineScreen");
            }
        } catch (Exception ignored) {
        }
        return false;
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

    // unused helpers removed
}
