package cn.breezeth.ordertocook.config;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonPrimitive;
import cn.breezeth.ordertocook.OrderToCookMod;
import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.util.DataCompat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {
    private static final class CustomMenuEntry {
        final Item item;
        final Identifier itemId;
        final int hunger;
        final String nbt;

        CustomMenuEntry(Item item, Identifier itemId, int hunger, String nbt) {
            this.item = item;
            this.itemId = itemId;
            this.hunger = hunger;
            this.nbt = (nbt == null || nbt.isBlank()) ? null : nbt;
        }
    }

    private static ModConfig config;
    private static Map<Item, Integer> customMenuNutritionMap;
    private static List<CustomMenuEntry> customMenuItems = new ArrayList<>();
    private static final Jankson JANKSON = Jankson.builder().build();
    
    private static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("ordertocook").toFile();
    private static final File CONFIG_FILE = new File(CONFIG_DIR, ModConstants.MOD_ID + ".json5"); // Use .json5 to imply comments support
    private static final File CUSTOM_MENU_FILE = new File(CONFIG_DIR, "custom_menu_items.json5");

    public static void load() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }
        
        config = loadConfig(CONFIG_FILE, ModConfig.class, new ModConfig());
        if (config != null && sanitize(config)) {
            save();
        }
        customMenuItems = loadCustomMenuItems(CUSTOM_MENU_FILE);
        rebuildCustomMenuNutritionMap();
    }

    public static void load(File configFile) {
        File dir = configFile.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        config = loadConfig(configFile, ModConfig.class, new ModConfig());
        if (config != null && sanitize(config)) {
            save(configFile);
        }
        customMenuItems = loadCustomMenuItems(new File(configFile.getParentFile(), "custom_menu_items.json5"));
        rebuildCustomMenuNutritionMap();
    }

    private static boolean sanitize(ModConfig cfg) {
        boolean changed = false;
        ModConfig defaults = new ModConfig();

        if (cfg.devMode) {
            cfg.devMode = false;
            changed = true;
        }

        if (cfg.namesLanguage == null || !(cfg.namesLanguage.equalsIgnoreCase("zh_cn") || cfg.namesLanguage.equalsIgnoreCase("en_us"))) {
            cfg.namesLanguage = defaults.namesLanguage;
            changed = true;
        }

        if (cfg.orderMachineRefreshSeconds < 1) {
            cfg.orderMachineRefreshSeconds = defaults.orderMachineRefreshSeconds;
            changed = true;
        }

        if (cfg.walkInAttemptIntervalSeconds < 1) {
            cfg.walkInAttemptIntervalSeconds = defaults.walkInAttemptIntervalSeconds;
            changed = true;
        }

        if (cfg.orderMachineUpgradeBoardHunger == null) {
            cfg.orderMachineUpgradeBoardHunger = defaults.orderMachineUpgradeBoardHunger;
            changed = true;
        } else {
            int requiredSize = 9;
            List<Integer> def = defaults.orderMachineUpgradeBoardHunger;
            while (cfg.orderMachineUpgradeBoardHunger.size() < requiredSize) {
                int idx = cfg.orderMachineUpgradeBoardHunger.size();
                cfg.orderMachineUpgradeBoardHunger.add(idx < def.size() ? def.get(idx) : def.get(def.size() - 1));
                changed = true;
            }
        }

        return changed;
    }

    private static <T> T loadConfig(File file, Class<T> type, T defaultInstance) {
        if (file.exists()) {
            try {
                JsonObject json = JANKSON.load(file);
                boolean migrated = migrateLegacyConfigKeys(json);
                T loaded = JANKSON.fromJson(json, type);
                if (migrated) {
                    saveConfig(file, loaded);
                }
                return loaded;
            } catch (Exception e) {
                OrderToCookMod.LOGGER.error("Failed to load config " + file.getName() + ", using defaults", e);
            }
        } else {
            saveConfig(file, defaultInstance);
        }
        return defaultInstance;
    }

    private static boolean migrateLegacyConfigKeys(JsonObject json) {
        boolean changed = false;
        changed |= renameConfigKey(json, "villagerRate", "normalCustomerRate");
        changed |= renameConfigKey(json, "zombieRate", "easterEggCustomerRate");
        changed |= renameConfigKey(json, "tipZombieChance", "tipEasterEggCustomerChance");
        changed |= migrateMinutesToSeconds(json, "orderMachineCdMinutes", "orderMachineRefreshSeconds");
        return changed;
    }

    private static boolean migrateMinutesToSeconds(JsonObject json, String oldKey, String newKey) {
        if (!json.containsKey(oldKey)) {
            return false;
        }
        if (!json.containsKey(newKey)) {
            JsonElement legacyValue = json.get(oldKey);
            int minutes = 10;
            if (legacyValue instanceof JsonPrimitive primitive && primitive.getValue() != null) {
                try {
                    minutes = Integer.parseInt(String.valueOf(primitive.getValue()).trim());
                } catch (NumberFormatException ignored) {
                }
            }
            json.put(newKey, new JsonPrimitive(Math.max(1, minutes) * 60));
        }
        json.remove(oldKey);
        return true;
    }

    private static boolean renameConfigKey(JsonObject json, String oldKey, String newKey) {
        if (oldKey == null || oldKey.isBlank() || newKey == null || newKey.isBlank()) {
            return false;
        }
        if (!json.containsKey(oldKey)) {
            return false;
        }
        JsonElement legacyValue = json.get(oldKey);
        if (!json.containsKey(newKey)) {
            if (legacyValue != null) {
                json.put(newKey, legacyValue);
            }
        }
        json.remove(oldKey);
        return true;
    }

    private static <T> void saveConfig(File file, T instance) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            String result = JANKSON.toJson(instance).toJson(true, true);
            result = compactNumericArrays(result);
            result = normalizeToLineComments(result);
            out.write(result.getBytes());
        } catch (IOException e) {
            OrderToCookMod.LOGGER.error("Failed to save config " + file.getName(), e);
        }
    }

    public static void save() {
        saveConfig(CONFIG_FILE, config);
    }

    public static void save(File configFile) {
        saveConfig(configFile, config);
    }

    public static ModConfig get() {
        if (config == null) load();
        return config;
    }

    public static boolean isDevModeEnabled() {
        return get().devMode;
    }

    public static void setDevModeEnabled(boolean enabled) {
        ModConfig cfg = get();
        cfg.devMode = enabled;
        save();
    }

    public static void clearCache() {
        config = null;
        customMenuNutritionMap = null;
        customMenuItems = new ArrayList<>();
    }

    public static int getCustomMenuNutrition(Item item) {
        if (config == null) {
            load();
        }
        if (item == null) return 0;
        if (customMenuNutritionMap == null) {
            rebuildCustomMenuNutritionMap();
        }
        return customMenuNutritionMap.getOrDefault(item, 0);
    }

    public static int getCustomMenuNutrition(ItemStack stack) {
        if (config == null) {
            load();
        }
        if (stack == null || stack.isEmpty()) return 0;
        if (customMenuNutritionMap == null) {
            rebuildCustomMenuNutritionMap();
        }
        String signature = stackMatchSignature(stack);
        String customData = stackCustomDataString(stack);
        for (CustomMenuEntry entry : customMenuItems) {
            if (entry.item != stack.getItem()) continue;
            if (entry.nbt == null) continue;
            if (java.util.Objects.equals(entry.nbt, signature) || java.util.Objects.equals(entry.nbt, customData)) {
                return entry.hunger;
            }
        }
        return customMenuNutritionMap.getOrDefault(stack.getItem(), 0);
    }

    public static synchronized boolean upsertCustomMenuItem(ItemStack stack, int hunger) {
        if (config == null) {
            load();
        }
        if (stack == null || stack.isEmpty() || hunger <= 0) return false;
        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (id == null) return false;
        String nbt = null;

        boolean replaced = false;
        for (int i = 0; i < customMenuItems.size(); i++) {
            CustomMenuEntry entry = customMenuItems.get(i);
            if (entry.item != stack.getItem()) continue;
            if (!java.util.Objects.equals(entry.nbt, nbt)) continue;
            customMenuItems.set(i, new CustomMenuEntry(stack.getItem(), id, hunger, nbt));
            replaced = true;
            break;
        }
        if (!replaced) {
            customMenuItems.add(new CustomMenuEntry(stack.getItem(), id, hunger, nbt));
        }
        rebuildCustomMenuNutritionMap();
        writeCustomMenuItemsFile(CUSTOM_MENU_FILE, customMenuItems);
        return true;
    }

    private static synchronized void rebuildCustomMenuNutritionMap() {
        Map<Item, Integer> map = new HashMap<>();
        for (CustomMenuEntry entry : customMenuItems) {
            map.put(entry.item, entry.hunger);
        }
        customMenuNutritionMap = map;
    }

    private static List<CustomMenuEntry> loadCustomMenuItems(File file) {
        List<CustomMenuEntry> entries = new ArrayList<>();
        if (!file.exists()) {
            saveDefaultCustomMenuItems(file);
            return entries;
        }
        try {
            JsonObject root = JANKSON.load(file);
            JsonElement itemsElement = root.get("items");
            if (itemsElement instanceof JsonArray arr) {
                for (JsonElement element : arr) {
                    if (element instanceof JsonObject obj) {
                        CustomMenuEntry parsed = parseObjectEntry(obj);
                        if (parsed != null) entries.add(parsed);
                    }
                }
            }
        } catch (Exception e) {
            OrderToCookMod.LOGGER.error("Failed to load custom menu items config: {}", file.getName(), e);
        }
        return entries;
    }

    private static void saveDefaultCustomMenuItems(File file) {
        writeCustomMenuItemsFile(file, new ArrayList<>());
    }

    private static void writeCustomMenuItemsFile(File file, List<CustomMenuEntry> entries) {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (CustomMenuEntry entry : entries) {
            JsonObject obj = new JsonObject();
            obj.put("item", new JsonPrimitive(Objects.requireNonNull(entry.itemId.toString())));
            obj.put("hunger", new JsonPrimitive(entry.hunger));
            if (entry.nbt != null && !entry.nbt.isBlank()) {
                obj.put("nbt_data", new JsonPrimitive(Objects.requireNonNull(entry.nbt)));
            }
            arr.add(obj);
        }
        root.put("items", arr);
        try (FileOutputStream out = new FileOutputStream(file)) {
            String result = root.toJson(true, true);
            out.write(result.getBytes());
        } catch (IOException e) {
            OrderToCookMod.LOGGER.error("Failed to save custom menu items config {}", file.getName(), e);
        }
    }

    private static CustomMenuEntry parseObjectEntry(JsonObject obj) {
        try {
            JsonElement itemEl = obj.get("item");
            JsonElement hungerEl = obj.get("hunger");
            if (!(itemEl instanceof JsonPrimitive ip) || !(hungerEl instanceof JsonPrimitive hp)) {
                return null;
            }
            Identifier id = Identifier.tryParse(String.valueOf(ip.getValue()).trim());
            if (id == null || !Registries.ITEM.containsId(id)) return null;
            int hunger = Integer.parseInt(String.valueOf(hp.getValue()).trim());
            if (hunger <= 0) return null;
            String nbt = null;
            JsonElement nbtDataEl = obj.get("nbt_data");
            if (nbtDataEl instanceof JsonPrimitive sp && sp.getValue() != null) {
                nbt = String.valueOf(sp.getValue()).trim();
            }
            return new CustomMenuEntry(Registries.ITEM.get(id), id, hunger, nbt);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String stackCustomDataString(ItemStack stack) {
        var tag = DataCompat.copy(stack);
        if (tag == null || tag.isEmpty()) return null;
        tag.remove("OtcLastSetTime");
        String text = tag.toString();
        return text == null || text.isBlank() || "{}".equals(text) ? null : text;
    }

    private static String stackMatchSignature(ItemStack stack) {
        String customData = stackCustomDataString(stack);
        if (customData != null) {
            return customData;
        }
        var nbt = stack.getNbt();
        if (nbt == null || nbt.isEmpty()) return null;
        nbt.remove("OtcLastSetTime");
        if (nbt.isEmpty()) return null;
        String raw = nbt.toString();
        if (raw == null || raw.isBlank() || "{}".equals(raw)) return null;

        Pattern keyPattern = Pattern.compile("([a-z0-9_.-]+:[a-z0-9_./-]+)");
        Matcher matcher = keyPattern.matcher(raw);
        List<String> keys = new ArrayList<>();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (key.startsWith("minecraft:")) continue;
            keys.add(key);
        }
        if (keys.isEmpty()) return raw;
        Collections.sort(keys);
        return String.join(";", keys) + "|" + raw;
    }

    private static String compactNumericArrays(String text) {
        Pattern p = Pattern.compile("\\[(\\s*-?\\d+(?:\\.\\d+)?\\s*(?:,\\s*-?\\d+(?:\\.\\d+)?\\s*)*)\\]");
        Matcher m = p.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String body = m.group(1).replaceAll("\\s+", "");
            m.appendReplacement(sb, Matcher.quoteReplacement("[" + body + "]"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String normalizeToLineComments(String text) {
        Pattern block = Pattern.compile("/\\*([\\s\\S]*?)\\*/");
        Matcher bm = block.matcher(text);
        StringBuffer converted = new StringBuffer();
        while (bm.find()) {
            String body = bm.group(1);
            StringBuilder replacement = new StringBuilder();
            String[] lines = body.split("\\r?\\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.startsWith("*")) {
                    line = line.substring(1).trim();
                }
                replacement.append("// ").append(line);
                if (i < lines.length - 1) {
                    replacement.append(System.lineSeparator());
                }
            }
            bm.appendReplacement(converted, Matcher.quoteReplacement(replacement.toString()));
        }
        bm.appendTail(converted);

        String[] lines = converted.toString().split("\\r?\\n", -1);
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                normalized.append("//");
            } else {
                normalized.append(line);
            }
            if (i < lines.length - 1) {
                normalized.append(System.lineSeparator());
            }
        }
        return normalized.toString();
    }
}
