package cn.breezeth.ordertocook.config;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {
    private static final class CustomMenuEntry {
        Item item;
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
    private static volatile boolean RUNTIME_DEV_MODE = false;
    
    private static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("ordertocook").toFile();
    private static final File CONFIG_FILE = new File(CONFIG_DIR, ModConstants.MOD_ID + ".json5"); // Use .json5 to imply comments support
    private static final File CUSTOM_MENU_FILE = new File(CONFIG_DIR, "custom_menu_items.json5");
    private static File activeCustomMenuFile = CUSTOM_MENU_FILE;
    private static long activeCustomMenuFileLastModified = -1L;

    public static void load() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }
        
        config = loadConfig(CONFIG_FILE, ModConfig.class, new ModConfig());
        if (config != null) {
            RUNTIME_DEV_MODE = config.devMode;
        }
        if (config != null && sanitize(config)) {
            save();
        }
        activeCustomMenuFile = CUSTOM_MENU_FILE;
        customMenuItems = loadCustomMenuItems(activeCustomMenuFile);
        activeCustomMenuFileLastModified = activeCustomMenuFile.exists() ? activeCustomMenuFile.lastModified() : -1L;
        rebuildCustomMenuNutritionMap();
    }

    public static void load(File configFile) {
        File dir = configFile.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        config = loadConfig(configFile, ModConfig.class, new ModConfig());
        if (config != null) {
            RUNTIME_DEV_MODE = config.devMode;
        }
        if (config != null && sanitize(config)) {
            save(configFile);
        }
        activeCustomMenuFile = new File(configFile.getParentFile(), "custom_menu_items.json5");
        customMenuItems = loadCustomMenuItems(activeCustomMenuFile);
        activeCustomMenuFileLastModified = activeCustomMenuFile.exists() ? activeCustomMenuFile.lastModified() : -1L;
        rebuildCustomMenuNutritionMap();
    }

    private static boolean sanitize(ModConfig cfg) {
        boolean changed = false;
        ModConfig defaults = new ModConfig();

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
        changed |= addMissingConfigKey(json, "sdmShopCurrencyCompat", new JsonPrimitive(false));
        changed |= addMissingConfigKey(json, "sdmShopCurrencyKey", new JsonPrimitive("basic_money"));
        return changed;
    }

    private static boolean addMissingConfigKey(JsonObject json, String key, JsonElement value) {
        if (json.containsKey(key)) {
            return false;
        }
        json.put(key, value);
        return true;
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
        if (!json.containsKey(oldKey)) {
            return false;
        }
        if (!json.containsKey(newKey)) {
            json.put(newKey, json.get(oldKey));
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
        ModConfig cfg = get();
        return (cfg != null && cfg.devMode) || RUNTIME_DEV_MODE;
    }

    public static void setDevModeEnabled(boolean enabled) {
        ModConfig cfg = get();
        if (cfg != null) {
            cfg.devMode = enabled;
            save();
        }
        RUNTIME_DEV_MODE = enabled;
    }

    public static void clearCache() {
        config = null;
        RUNTIME_DEV_MODE = false;
        customMenuNutritionMap = null;
        customMenuItems = new ArrayList<>();
        activeCustomMenuFileLastModified = -1L;
    }

    public static int getCustomMenuNutrition(Item item) {
        if (config == null) {
            load();
        }
        reloadCustomMenuItemsIfChanged();
        if (item == null) return 0;
        if (customMenuNutritionMap == null) {
            rebuildCustomMenuNutritionMap();
        }
        for (CustomMenuEntry entry : customMenuItems) {
            if (resolveEntryItem(entry) == item) {
                customMenuNutritionMap.put(item, entry.hunger);
                return entry.hunger;
            }
        }
        return customMenuNutritionMap.getOrDefault(item, 0);
    }

    public static int getCustomMenuNutrition(ItemStack stack) {
        if (config == null) {
            load();
        }
        reloadCustomMenuItemsIfChanged();
        if (stack == null || stack.isEmpty()) return 0;
        if (customMenuNutritionMap == null) {
            rebuildCustomMenuNutritionMap();
        }
        String signature = stackMatchSignature(stack);
        String customData = stackCustomDataString(stack);
        for (CustomMenuEntry entry : customMenuItems) {
            if (resolveEntryItem(entry) != stack.getItem()) continue;
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
            if (!java.util.Objects.equals(entry.itemId, id)) continue;
            if (!java.util.Objects.equals(entry.nbt, nbt)) continue;
            customMenuItems.set(i, new CustomMenuEntry(stack.getItem(), id, hunger, nbt));
            replaced = true;
            break;
        }
        if (!replaced) {
            customMenuItems.add(new CustomMenuEntry(stack.getItem(), id, hunger, nbt));
        }
        rebuildCustomMenuNutritionMap();
        writeCustomMenuItemsFile(activeCustomMenuFile, customMenuItems);
        activeCustomMenuFileLastModified = activeCustomMenuFile != null && activeCustomMenuFile.exists() ? activeCustomMenuFile.lastModified() : -1L;
        return true;
    }

    private static synchronized void reloadCustomMenuItemsIfChanged() {
        File file = activeCustomMenuFile != null ? activeCustomMenuFile : CUSTOM_MENU_FILE;
        if (file == null || !file.exists()) {
            return;
        }
        long lastModified = file.lastModified();
        if (lastModified <= 0L || lastModified == activeCustomMenuFileLastModified) {
            return;
        }
        customMenuItems = loadCustomMenuItems(file);
        activeCustomMenuFileLastModified = lastModified;
        rebuildCustomMenuNutritionMap();
        OrderToCookMod.LOGGER.info("Reloaded custom menu items config: {} entries from {}", customMenuItems.size(), file.getAbsolutePath());
    }

    private static synchronized void rebuildCustomMenuNutritionMap() {
        Map<Item, Integer> map = new HashMap<>();
        for (CustomMenuEntry entry : customMenuItems) {
            Item item = resolveEntryItem(entry);
            if (item != null) {
                map.put(item, entry.hunger);
            }
        }
        customMenuNutritionMap = map;
    }

    private static Item resolveEntryItem(CustomMenuEntry entry) {
        if (entry.item != null) {
            return entry.item;
        }
        if (entry.itemId != null && Registries.ITEM.containsId(entry.itemId)) {
            entry.item = Registries.ITEM.get(entry.itemId);
        }
        return entry.item;
    }

    private static List<CustomMenuEntry> loadCustomMenuItems(File file) {
        List<CustomMenuEntry> entries = new ArrayList<>();
        if (!file.exists()) {
            saveDefaultCustomMenuItems(file);
            return entries;
        }
        try {
            JsonObject root = JANKSON.load(stripBom(Files.readString(file.toPath(), StandardCharsets.UTF_8)));
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
        if (file == null) {
            file = CUSTOM_MENU_FILE;
        }
        if (entries.isEmpty() && existingCustomMenuFileHasEntries(file)) {
            OrderToCookMod.LOGGER.warn("Refusing to overwrite non-empty custom menu config with an empty item list: {}", file.getAbsolutePath());
            return;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        backupCustomMenuFile(file);
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (CustomMenuEntry entry : entries) {
            JsonObject obj = new JsonObject();
            obj.put("item", new JsonPrimitive(entry.itemId.toString()));
            obj.put("hunger", new JsonPrimitive(entry.hunger));
            if (entry.nbt != null && !entry.nbt.isBlank()) {
                obj.put("nbt_data", new JsonPrimitive(entry.nbt));
            }
            arr.add(obj);
        }
        root.put("items", arr);
        try (FileOutputStream out = new FileOutputStream(file)) {
            String result = root.toJson(true, true);
            out.write(result.getBytes());
            activeCustomMenuFileLastModified = file.lastModified();
        } catch (IOException e) {
            OrderToCookMod.LOGGER.error("Failed to save custom menu items config {}", file.getName(), e);
        }
    }

    private static boolean existingCustomMenuFileHasEntries(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return text.contains("\"item\"") || text.contains("item:");
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void backupCustomMenuFile(File file) {
        if (file == null || !file.exists() || !existingCustomMenuFileHasEntries(file)) {
            return;
        }
        File backup = new File(file.getParentFile(), file.getName() + ".bak");
        try {
            Files.copy(file.toPath(), backup.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            OrderToCookMod.LOGGER.warn("Failed to backup custom menu config {}", file.getAbsolutePath(), e);
        }
    }

    private static String stripBom(String text) {
        if (text != null && !text.isEmpty() && text.charAt(0) == '\uFEFF') {
            return text.substring(1);
        }
        return text;
    }

    private static CustomMenuEntry parseObjectEntry(JsonObject obj) {
        try {
            JsonElement itemEl = obj.get("item");
            JsonElement hungerEl = obj.get("hunger");
            if (!(itemEl instanceof JsonPrimitive ip) || !(hungerEl instanceof JsonPrimitive hp)) {
                return null;
            }
            Identifier id = Identifier.tryParse(String.valueOf(ip.getValue()).trim());
            if (id == null) return null;
            int hunger = Integer.parseInt(String.valueOf(hp.getValue()).trim());
            if (hunger <= 0) return null;
            String nbt = null;
            JsonElement nbtDataEl = obj.get("nbt_data");
            if (nbtDataEl instanceof JsonPrimitive sp && sp.getValue() != null) {
                nbt = String.valueOf(sp.getValue()).trim();
            }
            Item item = Registries.ITEM.containsId(id) ? Registries.ITEM.get(id) : null;
            return new CustomMenuEntry(item, id, hunger, nbt);
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
        return stackNonMinecraftComponentsSignature(stack);
    }

    private static String stackNonMinecraftComponentsSignature(ItemStack stack) {
        var components = stack.getComponents();
        if (components == null) return null;
        String raw = components.toString();
        if (raw == null || raw.isBlank() || "{}".equals(raw)) return null;

        Pattern keyPattern = Pattern.compile("([a-z0-9_.-]+:[a-z0-9_./-]+)=>");
        Matcher matcher = keyPattern.matcher(raw);
        List<String> pairs = new ArrayList<>();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (key.startsWith("minecraft:")) continue;
            int valueStart = matcher.end();
            int valueEnd = findComponentValueEnd(raw, valueStart);
            String value = raw.substring(valueStart, valueEnd).trim();
            if (!value.isEmpty()) {
                pairs.add(key + "=" + value);
            }
        }
        if (pairs.isEmpty()) return null;
        Collections.sort(pairs);
        return String.join(";", pairs);
    }

    private static int findComponentValueEnd(String text, int from) {
        int depthBrace = 0;
        int depthBracket = 0;
        int depthParen = 0;
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depthBrace++;
            else if (c == '}') {
                if (depthBrace == 0 && depthBracket == 0 && depthParen == 0) return i;
                depthBrace = Math.max(0, depthBrace - 1);
            } else if (c == '[') depthBracket++;
            else if (c == ']') depthBracket = Math.max(0, depthBracket - 1);
            else if (c == '(') depthParen++;
            else if (c == ')') depthParen = Math.max(0, depthParen - 1);
            else if (c == ',' && depthBrace == 0 && depthBracket == 0 && depthParen == 0) return i;
        }
        return text.length();
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
