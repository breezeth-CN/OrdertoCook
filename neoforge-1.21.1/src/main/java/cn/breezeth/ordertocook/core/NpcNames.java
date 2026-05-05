package cn.breezeth.ordertocook.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cn.breezeth.ordertocook.config.ConfigManager;

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
        names = loadNames(lang);
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

    private static List<String> loadNames(String lang) {
        String chosenPath = "en_us".equals(lang) ? EN_ASSET_PATH : DEFAULT_ASSET_PATH;
        List<String> parsed = parseNames(chosenPath);
        if (!parsed.isEmpty() || DEFAULT_ASSET_PATH.equals(chosenPath)) {
            return parsed;
        }
        return parseNames(DEFAULT_ASSET_PATH);
    }

    private static List<String> parseNames(String assetPath) {
        try (InputStream stream = NpcNames.class.getResourceAsStream(assetPath)) {
            if (stream == null) {
                return List.of();
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    return List.of();
                }
                JsonObject obj = root.getAsJsonObject();
                JsonElement arr = obj.get("npc_names");
                if (arr == null || !arr.isJsonArray()) {
                    return List.of();
                }
                Set<String> set = new LinkedHashSet<>();
                arr.getAsJsonArray().forEach(e -> {
                    if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                        String s = e.getAsString().trim();
                        if (!s.isEmpty()) {
                            set.add(s);
                        }
                    }
                });
                return new ArrayList<>(set);
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    private NpcNames() {}
}
