package cn.breezeth.ordertocook.config;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import cn.breezeth.ordertocook.OrderToCookMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class VanillaEraFaresChronCompat {
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("ordertocook")
            .resolve("VanillaEraFaresChron.json5")
            .toFile();
    private static final int MAX_LEVEL = 8;
    private static final Jankson JANKSON = Jankson.builder().build();
    private static final List<ItemRequirement>[] UPGRADE_ITEMS = createRequirementArray();
    private static final double[] REWARD_MULTIPLIERS = new double[MAX_LEVEL + 1];

    static {
        resetDefaults();
    }

    private VanillaEraFaresChronCompat() {
    }

    public static void loadIfEnabled() {
        resetDefaults();
        if (!ConfigManager.get().vanillaEraFaresChronCompat) {
            return;
        }
        ensureFileExists();
        load();
    }

    public static boolean hasUpgradeRequirements(ServerPlayerEntity player, int nextLevel) {
        if (!ConfigManager.get().vanillaEraFaresChronCompat) {
            return true;
        }
        List<ItemRequirement> requirements = requirementsForLevel(nextLevel);
        if (requirements.isEmpty()) {
            return true;
        }
        for (ItemRequirement requirement : requirements) {
            Item item = requirement.resolveItem();
            if (item == null) {
                player.closeHandledScreen();
                player.sendMessage(Text.literal("Invalid upgrade item: " + requirement.itemId), true);
                return false;
            }
            if (countItem(player, item) < requirement.count) {
                player.closeHandledScreen();
                player.sendMessage(Text.literal("Not enough upgrade item: ")
                        .append(Text.translatable(item.getTranslationKey()))
                        .append(Text.literal(" x" + requirement.count)), true);
                return false;
            }
        }
        return true;
    }

    public static boolean consumeUpgradeRequirements(ServerPlayerEntity player, int nextLevel) {
        if (!ConfigManager.get().vanillaEraFaresChronCompat) {
            return true;
        }
        List<ItemRequirement> requirements = requirementsForLevel(nextLevel);
        if (requirements.isEmpty()) {
            return true;
        }
        for (ItemRequirement requirement : requirements) {
            Item item = requirement.resolveItem();
            if (item == null) {
                player.closeHandledScreen();
                player.sendMessage(Text.literal("Invalid upgrade item: " + requirement.itemId), true);
                return false;
            }
            if (!consumeItem(player, item, requirement.count)) {
                player.closeHandledScreen();
                player.sendMessage(Text.literal("Failed to consume extra upgrade item: ")
                        .append(Text.translatable(item.getTranslationKey()))
                        .append(Text.literal(" x" + requirement.count)), true);
                return false;
            }
        }
        return true;
    }

    public static int applyRewardMultiplier(int coin, int level) {
        if (coin <= 0 || !ConfigManager.get().vanillaEraFaresChronCompat) {
            return coin;
        }
        int idx = Math.max(0, Math.min(MAX_LEVEL, level));
        double multiplier = REWARD_MULTIPLIERS[idx];
        if (multiplier <= 0.0) {
            multiplier = 1.0;
        }
        return (int) Math.ceil(coin * multiplier);
    }

    public static List<ItemRequirementView> getUpgradeRequirementViews(int nextLevel) {
        if (!ConfigManager.get().vanillaEraFaresChronCompat) {
            return List.of();
        }
        List<ItemRequirement> requirements = requirementsForLevel(nextLevel);
        if (requirements.isEmpty()) {
            return List.of();
        }
        List<ItemRequirementView> views = new ArrayList<>();
        for (ItemRequirement requirement : requirements) {
            Item item = requirement.resolveItem();
            String translationKey = item != null ? item.getTranslationKey() : requirement.itemId;
            views.add(new ItemRequirementView(translationKey, requirement.itemId, requirement.count));
        }
        return views;
    }

    private static List<ItemRequirement> requirementsForLevel(int level) {
        if (level < 1 || level > MAX_LEVEL) {
            return List.of();
        }
        return UPGRADE_ITEMS[level];
    }

    private static void load() {
        try {
            JsonObject root = JANKSON.load(stripBom(Files.readString(CONFIG_FILE.toPath(), StandardCharsets.UTF_8)));
            loadUpgradeRequirements(root.getObject("upgradeRequirements"));
            loadRewardMultipliers(root.getObject("rewardMultipliers"));
        } catch (Exception e) {
            OrderToCookMod.LOGGER.error("Failed to load {}", CONFIG_FILE.getName(), e);
        }
    }

    private static String stripBom(String text) {
        if (text != null && !text.isEmpty() && text.charAt(0) == '\uFEFF') {
            return text.substring(1);
        }
        return text;
    }

    private static void loadUpgradeRequirements(JsonObject root) {
        if (root == null) {
            return;
        }
        for (int level = 1; level <= MAX_LEVEL; level++) {
            JsonElement element = root.get(String.valueOf(level));
            if (!(element instanceof JsonArray array)) {
                continue;
            }
            for (JsonElement entry : array) {
                ItemRequirement requirement = parseRequirement(entry);
                if (requirement != null) {
                    UPGRADE_ITEMS[level].add(requirement);
                }
            }
        }
    }

    private static void loadRewardMultipliers(JsonObject root) {
        if (root == null) {
            return;
        }
        for (int level = 0; level <= MAX_LEVEL; level++) {
            JsonElement element = root.get(String.valueOf(level));
            Double value = readDouble(element);
            if (value != null && value > 0.0) {
                REWARD_MULTIPLIERS[level] = value;
            }
        }
    }

    private static ItemRequirement parseRequirement(JsonElement element) {
        if (!(element instanceof JsonObject object)) {
            return null;
        }
        String itemId = readString(object.get("item"));
        Integer count = readInt(object.get("count"));
        if (itemId == null || count == null || count <= 0) {
            return null;
        }
        Identifier id = Identifier.tryParse(itemId.trim());
        if (id == null) {
            return null;
        }
        return new ItemRequirement(id.toString(), count);
    }

    private static String readString(JsonElement element) {
        if (element instanceof JsonPrimitive primitive && primitive.getValue() != null) {
            return String.valueOf(primitive.getValue());
        }
        return null;
    }

    private static Integer readInt(JsonElement element) {
        if (element instanceof JsonPrimitive primitive && primitive.getValue() != null) {
            try {
                return Integer.parseInt(String.valueOf(primitive.getValue()).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private static Double readDouble(JsonElement element) {
        if (element instanceof JsonPrimitive primitive && primitive.getValue() != null) {
            try {
                return Double.parseDouble(String.valueOf(primitive.getValue()).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private static int countItem(PlayerEntity player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static boolean consumeItem(PlayerEntity player, Item item, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty() || !stack.isOf(item)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.decrement(take);
            remaining -= take;
        }
        player.getInventory().markDirty();
        return remaining == 0;
    }

    private static void ensureFileExists() {
        File dir = CONFIG_FILE.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        if (CONFIG_FILE.exists()) {
            return;
        }
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            out.write(defaultFileText().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            OrderToCookMod.LOGGER.error("Failed to create {}", CONFIG_FILE.getName(), e);
        }
    }

    private static String defaultFileText() {
        String line = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("{").append(line);
        sb.append("  // Extra items consumed when upgrading the order machine.").append(line);
        sb.append("  upgradeRequirements: {").append(line);
        for (int level = 1; level <= MAX_LEVEL; level++) {
            sb.append("    \"").append(level).append("\": []");
            sb.append(level == MAX_LEVEL ? line : "," + line);
        }
        sb.append("  },").append(line);
        sb.append(line);
        sb.append("  // Extra reward multiplier by order machine level. Final reward is rounded up.").append(line);
        sb.append("  rewardMultipliers: {").append(line);
        for (int level = 0; level <= MAX_LEVEL; level++) {
            sb.append("    \"").append(level).append("\": 1.0");
            sb.append(level == MAX_LEVEL ? line : "," + line);
        }
        sb.append("  }").append(line);
        sb.append("}").append(line);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<ItemRequirement>[] createRequirementArray() {
        List<ItemRequirement>[] arr = new List[MAX_LEVEL + 1];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = new ArrayList<>();
        }
        return arr;
    }

    private static void resetDefaults() {
        for (List<ItemRequirement> requirements : UPGRADE_ITEMS) {
            requirements.clear();
        }
        for (int i = 0; i < REWARD_MULTIPLIERS.length; i++) {
            REWARD_MULTIPLIERS[i] = 1.0;
        }
    }

    private record ItemRequirement(String itemId, int count) {
        private Item resolveItem() {
            Identifier id = Identifier.tryParse(itemId);
            if (id == null || !Registries.ITEM.containsId(id)) {
                return null;
            }
            return Registries.ITEM.get(id);
        }
    }

    public record ItemRequirementView(String translationKey, String itemId, int count) {
    }
}
