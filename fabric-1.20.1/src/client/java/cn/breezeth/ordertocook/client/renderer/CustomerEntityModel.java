package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.entity.CustomerEntity;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 玩家皮肤解析与 slim/wide 分支应与 fabric-1.21.1 同名类行为一致；此处使用 1.20.1 客户端 API（如 {@link DefaultSkinHelper}、Profile 拉取方式）。
 */
public final class CustomerEntityModel extends GeoModel<CustomerEntity> {
    private static final Identifier TEXTURE_WIDE = new Identifier(ModConstants.MOD_ID, "textures/entity/otc_zombie_custom.png");
    private static final Identifier TEXTURE_SLIM = new Identifier(ModConstants.MOD_ID, "textures/entity/otc_zombie_custom2.png");
    private static final Identifier MODEL_WIDE = new Identifier(ModConstants.MOD_ID, "geo/drive_idle.geo.json");
    private static final Identifier MODEL_SLIM = new Identifier(ModConstants.MOD_ID, "geo/drive_idle_alex.geo.json");
    private static final Identifier ANIMATION = new Identifier(ModConstants.MOD_ID, "animations/npc.animation.json");
    private static final Identifier TRANSITION_ANIMATION = new Identifier(ModConstants.MOD_ID, "animations/npc.transition.animation.json");
    private static final java.util.Map<String, CompletableFuture<GameProfile>> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final java.util.Map<String, Supplier<Identifier>> SKIN_CACHE = new ConcurrentHashMap<>();
    private static int customWideSkinCount = -1;
    private static int customSlimSkinCount = -1;

    @Override
    public Identifier getModelResource(CustomerEntity animatable) {
        return isSlim(animatable) ? MODEL_SLIM : MODEL_WIDE;
    }

    @Override
    public Identifier getTextureResource(CustomerEntity animatable) {
        if (animatable.usesPlayerSkin()) {
            return getSkinTexture(animatable);
        }
        if (ConfigManager.get().customMultipleCustomerSkins) {
            Identifier customTexture = getCustomCustomerTexture(animatable);
            if (customTexture != null) {
                return customTexture;
            }
        }
        return animatable.getTextureVariant() == 2 ? TEXTURE_SLIM : TEXTURE_WIDE;
    }

    @Override
    public Identifier getAnimationResource(CustomerEntity animatable) {
        return animatable.isTransitionAnimationActive() ? TRANSITION_ANIMATION : ANIMATION;
    }

    private static boolean isSlim(CustomerEntity entity) {
        if (!entity.usesPlayerSkin()) {
            return entity.getTextureVariant() == 2;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        UUID profileId = getProfileId(entity);
        GameProfile stub = new GameProfile(profileId, entity.getSkinAccount());
        if (client == null) {
            String m = DefaultSkinHelper.getModel(profileId);
            return m != null && "slim".equalsIgnoreCase(m);
        }
        String cacheKey = (entity.getSkinAccount() + "|" + profileId).toLowerCase(Locale.ROOT);
        CompletableFuture<GameProfile> future = PROFILE_CACHE.computeIfAbsent(cacheKey, key ->
                CompletableFuture.supplyAsync(() -> resolveProfile(client, stub)));
        GameProfile resolved = future.getNow(stub);
        return isSlimArmsFromProfile(client, resolved, profileId);
    }

    private static boolean isSlimArmsFromProfile(MinecraftClient client, GameProfile profile, UUID fallbackUuid) {
        try {
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = client.getSkinProvider().getTextures(profile);
            MinecraftProfileTexture skin = textures.get(MinecraftProfileTexture.Type.SKIN);
            if (skin != null) {
                String model = skin.getMetadata("model");
                if ("slim".equals(model)) {
                    return true;
                }
                if ("classic".equals(model) || "default".equals(model)) {
                    return false;
                }
            }
        } catch (Throwable ignored) {
        }
        String m = DefaultSkinHelper.getModel(fallbackUuid);
        return m != null && "slim".equalsIgnoreCase(m);
    }

    private static Identifier getSkinTexture(CustomerEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        UUID profileId = getProfileId(entity);
        GameProfile profile = new GameProfile(profileId, entity.getSkinAccount());
        if (client == null) {
            return DefaultSkinHelper.getTexture(profileId);
        }
        String cacheKey = (entity.getSkinAccount() + "|" + profileId).toLowerCase(Locale.ROOT);
        Supplier<Identifier> supplier = SKIN_CACHE.computeIfAbsent(cacheKey, key ->
                createSkinSupplier(client, profile, key));
        Identifier id = supplier.get();
        return id != null ? id : DefaultSkinHelper.getTexture(profileId);
    }

    private static Supplier<Identifier> createSkinSupplier(MinecraftClient client, GameProfile fallbackProfile, String cacheKey) {
        CompletableFuture<GameProfile> profileFuture = PROFILE_CACHE.computeIfAbsent(cacheKey, key ->
                CompletableFuture.supplyAsync(() -> resolveProfile(client, fallbackProfile)));
        return () -> {
            GameProfile resolvedProfile = profileFuture.getNow(fallbackProfile);
            return client.getSkinProvider().loadSkin(resolvedProfile);
        };
    }

    private static GameProfile resolveProfile(MinecraftClient client, GameProfile fallbackProfile) {
        GameProfile resolvedProfile = fallbackProfile;
        try {
            GameProfile filled = client.getSessionService().fillProfileProperties(fallbackProfile, false);
            if (filled != null) {
                resolvedProfile = filled;
            }
        } catch (Throwable ignored) {
        }
        if (hasTextures(resolvedProfile)) {
            return resolvedProfile;
        }
        GameProfile sessionServerProfile = fetchProfileFromSessionServer(fallbackProfile);
        return sessionServerProfile != null ? sessionServerProfile : resolvedProfile;
    }

    private static boolean hasTextures(GameProfile profile) {
        if (profile == null) {
            return false;
        }
        Collection<Property> textures = profile.getProperties().get("textures");
        return textures != null && !textures.isEmpty();
    }

    private static GameProfile fetchProfileFromSessionServer(GameProfile fallbackProfile) {
        try {
            UUID profileId = fallbackProfile.getId();
            if (profileId == null) {
                return fallbackProfile;
            }
            String uuid = profileId.toString().replace("-", "");
            var connection = java.net.URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid).toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                GameProfile profile = new GameProfile(profileId, root.has("name") ? root.get("name").getAsString() : fallbackProfile.getName());
                if (root.has("properties") && root.get("properties").isJsonArray()) {
                    root.getAsJsonArray("properties").forEach(element -> {
                        JsonObject property = element.getAsJsonObject();
                        if (!property.has("name") || !property.has("value")) {
                            return;
                        }
                        String name = property.get("name").getAsString();
                        String value = property.get("value").getAsString();
                        if (name == null || value == null || name.isBlank() || value.isBlank()) {
                            return;
                        }
                        if (property.has("signature")) {
                            String signature = property.get("signature").getAsString();
                            if (signature != null && !signature.isBlank()) {
                                profile.getProperties().put(name, new Property(name, value, signature));
                                return;
                            }
                            profile.getProperties().put(name, new Property(name, value));
                        } else {
                            profile.getProperties().put(name, new Property(name, value));
                        }
                    });
                }
                return profile;
            }
        } catch (Throwable ignored) {
            return fallbackProfile;
        }
    }

    private static UUID getProfileId(CustomerEntity entity) {
        String skinUuid = entity.getSkinUuid();
        if (skinUuid != null && !skinUuid.isBlank()) {
            try {
                return UUID.fromString(skinUuid);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return UUID.nameUUIDFromBytes(("ordertocook:" + entity.getSkinAccount()).getBytes(StandardCharsets.UTF_8));
    }

    private static Identifier getCustomCustomerTexture(CustomerEntity entity) {
        boolean slim = entity.getTextureVariant() == 2;
        int count = getCustomSkinCount(slim);
        if (count <= 0) {
            return null;
        }
        int index = Math.floorMod(entity.getUuid().hashCode(), count) + 1;
        return customTextureId(slim, index);
    }

    private static int getCustomSkinCount(boolean slim) {
        if (slim) {
            if (customSlimSkinCount < 0) {
                customSlimSkinCount = scanCustomSkinCount(true);
            }
            return customSlimSkinCount;
        }
        if (customWideSkinCount < 0) {
            customWideSkinCount = scanCustomSkinCount(false);
        }
        return customWideSkinCount;
    }

    private static int scanCustomSkinCount(boolean slim) {
        int count = 0;
        for (int i = 1; i <= 512; i++) {
            if (!resourceExists(customTextureId(slim, i))) {
                break;
            }
            count = i;
        }
        return count;
    }

    private static Identifier customTextureId(boolean slim, int index) {
        String prefix = slim ? "custom_slim_" : "custom_wide_";
        return new Identifier(ModConstants.MOD_ID, "textures/entity/customs/" + prefix + index + ".png");
    }

    private static boolean resourceExists(Identifier id) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.getResourceManager().getResource(id).isPresent();
    }
}
