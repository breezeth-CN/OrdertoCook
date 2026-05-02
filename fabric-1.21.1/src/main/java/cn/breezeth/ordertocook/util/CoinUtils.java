package cn.breezeth.ordertocook.util;

import cn.breezeth.ordertocook.item.ChefCoinItem;
import cn.breezeth.ordertocook.registry.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.breezeth.ordertocook.config.ConfigManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class CoinUtils {
    private CoinUtils() {}

    public static int countCoins(PlayerInventory inv) {
        Item custom = getCustomCurrencyItem();
        if (custom != null) {
            int total = 0;
            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty() && stack.isOf(custom)) {
                    total += stack.getCount();
                }
            }
            return total;
        }
        int total = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            Item item = stack.getItem();
            if (item instanceof ChefCoinItem coin) {
                total += coin.getValue() * stack.getCount();
            }
        }
        return total;
    }

    public static boolean tryConsume(PlayerInventory inv, int amount) {
        return tryConsumeWithChangeInternal(null, inv, amount);
    }

    public static boolean tryConsumeWithChange(PlayerEntity player, int amount) {
        PlayerInventory inv = player.getInventory();
        return tryConsumeWithChangeInternal(player, inv, amount);
    }

    private static boolean tryConsumeWithChangeInternal(PlayerEntity player, PlayerInventory inv, int amount) {
        if (amount <= 0) return true;
        Item custom = getCustomCurrencyItem();
        if (custom != null) {
            if (countCoins(inv) < amount) return false;
            int remaining = amount;
            for (int i = 0; i < inv.size(); i++) {
                if (remaining <= 0) break;
                ItemStack stack = inv.getStack(i);
                if (stack.isEmpty() || !stack.isOf(custom)) continue;
                int dec = Math.min(remaining, stack.getCount());
                if (dec > 0) {
                    stack.decrement(dec);
                    inv.setStack(i, stack);
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
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
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
                cnt += inv.getStack(slot).getCount();
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
                ItemStack stack = inv.getStack(slot);
                int dec = Math.min(toRemove, stack.getCount());
                if (dec > 0) {
                    stack.decrement(dec);
                    inv.setStack(slot, stack);
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

    public static void giveCoins(PlayerEntity player, int amount) {
        String customId = ConfigManager.get().customCurrencyItem;
        if (customId != null && !customId.isBlank()) {
            Identifier id = Identifier.tryParse(customId);
            Item item = id != null ? Registries.ITEM.get(id) : null;
            if (item != null && item != net.minecraft.item.Items.AIR) {
                // 如果是自定义物品，直接发放对应数量
                player.getInventory().offerOrDrop(new ItemStack(item, amount));
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
        if (c10000 > 0) player.getInventory().offerOrDrop(new ItemStack(ModItems.CHEF_COIN_10000, c10000));
        if (c100 > 0) player.getInventory().offerOrDrop(new ItemStack(ModItems.CHEF_COIN_100, c100));
        if (c20 > 0) player.getInventory().offerOrDrop(new ItemStack(ModItems.CHEF_COIN_20, c20));
        if (c5 > 0) player.getInventory().offerOrDrop(new ItemStack(ModItems.CHEF_COIN_5, c5));
        if (c1 > 0) player.getInventory().offerOrDrop(new ItemStack(ModItems.CHEF_COIN_1, c1));
    }

    private static Item getCustomCurrencyItem() {
        String customId = ConfigManager.get().customCurrencyItem;
        if (customId == null || customId.isBlank()) return null;
        Identifier id = Identifier.tryParse(customId);
        Item item = id != null ? Registries.ITEM.get(id) : null;
        if (item == null || item == net.minecraft.item.Items.AIR) return null;
        return item;
    }
}
