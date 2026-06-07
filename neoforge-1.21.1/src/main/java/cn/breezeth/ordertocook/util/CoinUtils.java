package cn.breezeth.ordertocook.util;

import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.item.ChefCoinItem;
import cn.breezeth.ordertocook.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CoinUtils {
    private CoinUtils() {}

    public static int countCoins(Player player) {
        if (isSdmShopCurrencyConfigured()) {
            return clampToInt(SdmCurrencyBridge.getMoney(player, getSdmCurrencyKey()));
        }
        return countCoins(player.getInventory());
    }

    public static int countCoins(Inventory inv) {
        Item custom = getCustomCurrencyItem();
        if (custom != null) {
            int total = 0;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && stack.is(custom)) {
                    total += stack.getCount();
                }
            }
            return total;
        }
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            Item item = stack.getItem();
            if (item instanceof ChefCoinItem coin) {
                total += coin.getValue() * stack.getCount();
            }
        }
        return total;
    }

    public static boolean tryConsume(Inventory inv, int amount) {
        return tryConsumeWithChangeInternal(null, inv, amount);
    }

    public static boolean tryConsumeWithChange(Player player, int amount) {
        if (isSdmShopCurrencyConfigured()) {
            return SdmCurrencyBridge.tryConsume(player, getSdmCurrencyKey(), amount);
        }
        Inventory inv = player.getInventory();
        return tryConsumeWithChangeInternal(player, inv, amount);
    }

    private static boolean tryConsumeWithChangeInternal(Player player, Inventory inv, int amount) {
        if (amount <= 0) return true;
        Item custom = getCustomCurrencyItem();
        if (custom != null) {
            if (countCoins(inv) < amount) return false;
            int remaining = amount;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (remaining <= 0) break;
                ItemStack stack = inv.getItem(i);
                if (stack.isEmpty() || !stack.is(custom)) continue;
                int dec = Math.min(remaining, stack.getCount());
                if (dec > 0) {
                    stack.shrink(dec);
                    inv.setItem(i, stack);
                    remaining -= dec;
                }
            }
            return remaining <= 0;
        }
        if (countCoins(inv) < amount) return false;
        int[] denoms = new int[]{10000, 100, 20, 5, 1};
        Map<Integer, List<Integer>> slotsByValue = new HashMap<>();
        int[] available = new int[denoms.length];
        for (int d = 0; d < denoms.length; d++) slotsByValue.put(denoms[d], new ArrayList<>());
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            Item item = stack.getItem();
            if (item instanceof ChefCoinItem coin) {
                int v = coin.getValue();
                List<Integer> lst = slotsByValue.get(v);
                if (lst != null) {
                    lst.add(i);
                }
            }
        }
        for (int d = 0; d < denoms.length; d++) {
            int v = denoms[d];
            int cnt = 0;
            for (int slot : slotsByValue.get(v)) {
                cnt += inv.getItem(slot).getCount();
            }
            available[d] = cnt;
        }
        int[] use = new int[denoms.length];
        int remaining = amount;
        for (int di = 0; di < denoms.length; di++) {
            int v = denoms[di];
            int need = remaining / v;
            if (need <= 0) continue;
            int take = Math.min(need, available[di]);
            if (take > 0) {
                use[di] += take;
                remaining -= take * v;
            }
        }
        int removedTotal = amount - remaining;
        if (remaining > 0) {
            int chosen = -1;
            for (int di = 0; di < denoms.length; di++) {
                if (available[di] - use[di] <= 0) continue;
                chosen = di;
                if (denoms[di] >= remaining) break;
            }
            if (chosen < 0) return false;
            use[chosen] += 1;
            removedTotal += denoms[chosen];
            remaining = 0;
        }
        for (int di = 0; di < denoms.length; di++) {
            int v = denoms[di];
            int toRemove = use[di];
            if (toRemove <= 0) continue;
            List<Integer> slots = slotsByValue.get(v);
            for (int slot : slots) {
                if (toRemove <= 0) break;
                ItemStack stack = inv.getItem(slot);
                int dec = Math.min(toRemove, stack.getCount());
                if (dec > 0) {
                    stack.shrink(dec);
                    inv.setItem(slot, stack);
                    toRemove -= dec;
                }
            }
        }
        int change = removedTotal - amount;
        if (player != null && change > 0) {
            giveCoins(player, change);
        }
        return true;
    }

    public static void giveCoins(Player player, int amount) {
        if (amount <= 0) return;
        if (isSdmShopCurrencyConfigured()) {
            if (!SdmCurrencyBridge.addMoney(player, getSdmCurrencyKey(), amount)) {
                SdmCurrencyBridge.warnOnce("Failed to add SDM currency; oTc fallback is disabled because sdmShopCurrencyCompat is enabled.");
            }
            return;
        }
        String customId = ConfigManager.get().customCurrencyItem;
        if (customId != null && !customId.isBlank()) {
            ResourceLocation id = ResourceLocation.tryParse(customId);
            Item item = id != null ? BuiltInRegistries.ITEM.get(id) : null;
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                // Custom currency items are paid as a direct item stack.
                player.getInventory().placeItemBackInInventory(new ItemStack(item, amount));
                return;
            }
        }

        int remaining = amount;
        int c10000 = remaining / 10000;
        remaining %= 10000;
        int c100 = remaining / 100;
        remaining %= 100;
        int c20 = remaining / 20;
        remaining %= 20;
        int c5 = remaining / 5;
        remaining %= 5;
        int c1 = remaining;
        if (c10000 > 0) player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.CHEF_COIN_10000.get(), c10000));
        if (c100 > 0) player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.CHEF_COIN_100.get(), c100));
        if (c20 > 0) player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.CHEF_COIN_20.get(), c20));
        if (c5 > 0) player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.CHEF_COIN_5.get(), c5));
        if (c1 > 0) player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.CHEF_COIN_1.get(), c1));
    }

    private static Item getCustomCurrencyItem() {
        String customId = ConfigManager.get().customCurrencyItem;
        if (customId == null || customId.isBlank()) return null;
        ResourceLocation id = ResourceLocation.tryParse(customId);
        Item item = id != null ? BuiltInRegistries.ITEM.get(id) : null;
        if (item == null || item == net.minecraft.world.item.Items.AIR) return null;
        return item;
    }

    private static boolean isSdmShopCurrencyConfigured() {
        return ConfigManager.get().sdmShopCurrencyCompat;
    }

    private static String getSdmCurrencyKey() {
        String key = ConfigManager.get().sdmShopCurrencyKey;
        return key == null || key.isBlank() ? "basic_money" : key.trim();
    }

    private static int clampToInt(long value) {
        if (value <= 0L) return 0;
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static final class SdmCurrencyBridge {
        private static final String CLASS_NAME = "net.sixik.sdm_economy.api.CurrencyHelper";
        private static final String BASIC_CLASS_NAME = "net.sixik.sdm_economy.api.CurrencyHelper$Basic";
        private static Method addMoney;
        private static Method setMoney;
        private static Method getMoney;
        private static Method basicAddMoney;
        private static Method basicSetMoney;
        private static Method basicGetMoney;
        private static boolean resolved;
        private static boolean available;
        private static boolean warned;

        static long getMoney(Player player, String currencyKey) {
            resolve();
            if (!available) {
                warnOnce("SDM Economy CurrencyHelper was not found; returning 0 balance.");
                return 0L;
            }
            try {
                Object value = invokeGetMoney(player, currencyKey);
                return value instanceof Number number ? number.longValue() : 0L;
            } catch (Throwable e) {
                warnOnce("Failed to read SDM currency balance: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                return 0L;
            }
        }

        static boolean addMoney(Player player, String currencyKey, long amount) {
            resolve();
            if (!available || amount == 0L) return available;
            try {
                invokeAddMoney(player, currencyKey, amount);
                return true;
            } catch (Throwable e) {
                warnOnce("Failed to add SDM currency: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                return false;
            }
        }

        static boolean tryConsume(Player player, String currencyKey, long amount) {
            if (amount <= 0L) return true;
            long current = getMoney(player, currencyKey);
            if (current < amount) return false;
            try {
                invokeSetMoney(player, currencyKey, current - amount);
                return true;
            } catch (Throwable e) {
                warnOnce("Failed to consume SDM currency: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                return false;
            }
        }

        private static void resolve() {
            if (resolved) return;
            resolved = true;
            try {
                Class<?> helper = Class.forName(CLASS_NAME);
                addMoney = find(helper, "addMoney");
                setMoney = find(helper, "setMoney");
                getMoney = find(helper, "getMoney");
                try {
                    Class<?> basicHelper = Class.forName(BASIC_CLASS_NAME);
                    basicAddMoney = findBasic(basicHelper, "addMoney");
                    basicSetMoney = findBasic(basicHelper, "setMoney");
                    basicGetMoney = findBasic(basicHelper, "getMoney");
                } catch (ClassNotFoundException ignored) {
                }
                available = (addMoney != null && setMoney != null && getMoney != null)
                        || (basicAddMoney != null && basicSetMoney != null && basicGetMoney != null);
            } catch (ClassNotFoundException ignored) {
                available = false;
            }
        }

        private static Method find(Class<?> type, String name) {
            for (Method method : type.getMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (method.getName().equals(name) && params.length == 3 && params[1] == String.class) {
                    return method;
                }
            }
            return null;
        }

        private static Method findBasic(Class<?> type, String name) {
            for (Method method : type.getMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (method.getName().equals(name) && (params.length == 1 || params.length == 2)) {
                    return method;
                }
            }
            return null;
        }

        private static Object invokeGetMoney(Player player, String currencyKey) throws Exception {
            if (getMoney != null) {
                try {
                    return getMoney.invoke(null, player, currencyKey);
                } catch (IllegalArgumentException e) {
                    if (basicGetMoney == null || !"basic_money".equals(currencyKey)) throw e;
                }
            }
            return basicGetMoney.invoke(null, player);
        }

        private static void invokeAddMoney(Player player, String currencyKey, long amount) throws Exception {
            if (addMoney != null) {
                try {
                    addMoney.invoke(null, player, currencyKey, amount);
                    return;
                } catch (IllegalArgumentException e) {
                    if (basicAddMoney == null || !"basic_money".equals(currencyKey)) throw e;
                }
            }
            basicAddMoney.invoke(null, player, amount);
        }

        private static void invokeSetMoney(Player player, String currencyKey, long amount) throws Exception {
            if (setMoney != null) {
                try {
                    setMoney.invoke(null, player, currencyKey, amount);
                    return;
                } catch (IllegalArgumentException e) {
                    if (basicSetMoney == null || !"basic_money".equals(currencyKey)) throw e;
                }
            }
            basicSetMoney.invoke(null, player, amount);
        }

        static void warnOnce(String message) {
            if (warned) return;
            warned = true;
            cn.breezeth.ordertocook.OrderToCookMod.LOGGER.warn("[OrderToCook] {}", message);
        }
    }
}
