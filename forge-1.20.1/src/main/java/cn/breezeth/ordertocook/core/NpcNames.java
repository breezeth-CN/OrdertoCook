package cn.breezeth.ordertocook.core;

import cn.breezeth.ordertocook.config.ConfigManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class NpcNames {
    private static final String DEFAULT_ASSET_PATH = "/assets/" + ModConstants.MOD_ID + "/npc_names.json";
    private static final String EN_ASSET_PATH = "/assets/" + ModConstants.MOD_ID + "/npc_names_en_us.json";
    private static final String DEFAULT_RESOURCE_PATH = "npc_names.json";
    private static final String EN_RESOURCE_PATH = "npc_names_en_us.json";
    private static volatile List<String> names = List.of();
    private static volatile String loadedLanguage = "";

    public static void init() {
        reload();
    }

    public static String random(net.minecraft.util.RandomSource random) {
        ensureLoaded();
        List<String> list = names;
        if (list.isEmpty()) return net.minecraft.network.chat.Component.translatable("keyword.ordertocook.customer").getString();
        return list.get(random.nextInt(list.size()));
    }

    public static synchronized void reload() {
        String lang = normalizeLanguage(ConfigManager.get().namesLanguage);
        names = loadNames(lang, null);
        loadedLanguage = lang;
    }

    public static synchronized void reloadFromClientResources(ResourceManager resourceManager) {
        String lang = normalizeLanguage(ConfigManager.get().namesLanguage);
        names = loadNames(lang, resourceManager);
        loadedLanguage = lang;
    }

    private static void ensureLoaded() {
        String lang = normalizeLanguage(ConfigManager.get().namesLanguage);
        if (names.isEmpty() || !lang.equals(loadedLanguage)) {
            reload();
        }
    }

    private static String normalizeLanguage(String lang) {
        return lang != null && lang.equalsIgnoreCase("en_us") ? "en_us" : "default";
    }

    private static List<String> loadNames(String lang, ResourceManager resourceManager) {
        String chosenPath = "en_us".equals(lang) ? EN_ASSET_PATH : DEFAULT_ASSET_PATH;
        String chosenResourcePath = "en_us".equals(lang) ? EN_RESOURCE_PATH : DEFAULT_RESOURCE_PATH;
        List<String> parsed = parseNames(resourceManager, chosenResourcePath);
        if (parsed.isEmpty()) {
            parsed = parseNames(chosenPath);
        }
        if (!parsed.isEmpty() || DEFAULT_ASSET_PATH.equals(chosenPath)) {
            return parsed;
        }
        parsed = parseNames(resourceManager, DEFAULT_RESOURCE_PATH);
        if (!parsed.isEmpty()) {
            return parsed;
        }
        return parseNames(DEFAULT_ASSET_PATH);
    }

    private static List<String> parseNames(ResourceManager resourceManager, String path) {
        if (resourceManager == null) {
            return List.of();
        }
        try {
            java.util.Optional<Resource> resource = resourceManager.getResource(new ResourceLocation(ModConstants.MOD_ID, path));
            if (resource.isEmpty()) {
                return List.of();
            }
            try (InputStream stream = resource.get().open()) {
                return parseNames(stream);
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<String> parseNames(String assetPath) {
        try (InputStream stream = NpcNames.class.getResourceAsStream(assetPath)) {
            if (stream == null) {
                return List.of();
            }
            return parseNames(stream);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<String> parseNames(InputStream stream) throws java.io.IOException {
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return List.of();
            }
            JsonObject obj = root.getAsJsonObject();
            Set<String> nameSet = new LinkedHashSet<>();
            JsonElement arr = obj.get("npc_names");
            if (arr != null && arr.isJsonArray()) {
                arr.getAsJsonArray().forEach(e -> {
                    if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                        String s = e.getAsString().trim();
                        if (!s.isEmpty()) {
                            nameSet.add(s);
                        }
                    }
                });
            }
            return new ArrayList<>(nameSet);
        }
    }

    private NpcNames() {}
}
