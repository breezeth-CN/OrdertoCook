package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.entity.CustomerEntity;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
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

public final class CustomerEntityModel extends GeoModel<CustomerEntity> {
    private static final Identifier TEXTURE_WIDE = Identifier.of(ModConstants.MOD_ID, "textures/entity/otc_zombie_custom.png");
    private static final Identifier TEXTURE_SLIM = Identifier.of(ModConstants.MOD_ID, "textures/entity/otc_zombie_custom2.png");
    private static final Identifier MODEL_WIDE = Identifier.of(ModConstants.MOD_ID, "geo/drive_idle.geo.json");
    private static final Identifier MODEL_SLIM = Identifier.of(ModConstants.MOD_ID, "geo/drive_idle_alex.geo.json");
    private static final Identifier ANIMATION = Identifier.of(ModConstants.MOD_ID, "animations/npc.animation.json");
    private static final Identifier TRANSITION_ANIMATION = Identifier.of(ModConstants.MOD_ID, "animations/npc.transition.animation.json");
    private static final Map<String, CompletableFuture<GameProfile>> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Supplier<SkinTextures>> SKIN_CACHE = new ConcurrentHashMap<>();
    private static int customWideSkinCount = -1;
    private static int customSlimSkinCount = -1;

    @Override
    public Identifier getModelResource(CustomerEntity animatable) {
        return isSlim(animatable) ? MODEL_SLIM : MODEL_WIDE;
    }

    @Override
    public Identifier getTextureResource(CustomerEntity animatable) {
        if (animatable.usesPlayerSkin()) {
            return getSkinTextures(animatable).texture();
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
        if (entity.usesPlayerSkin()) {
            return getSkinTextures(entity).model() == SkinTextures.Model.SLIM;
        }
        return entity.getTextureVariant() == 2;
    }

    private static SkinTextures getSkinTextures(CustomerEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        UUID profileId = getProfileId(entity);
        GameProfile profile = new GameProfile(profileId, entity.getSkinAccount());
        if (client == null) {
            return DefaultSkinHelper.getSkinTextures(profile);
        }
        String cacheKey = (entity.getSkinAccount() + "|" + profileId).toLowerCase(Locale.ROOT);
        Supplier<SkinTextures> supplier = SKIN_CACHE.computeIfAbsent(cacheKey, key ->
                createSkinSupplier(client, profile, key)
        );
        SkinTextures textures = supplier.get();
        return textures != null ? textures : DefaultSkinHelper.getSkinTextures(profile);
    }

    private static Supplier<SkinTextures> createSkinSupplier(MinecraftClient client, GameProfile fallbackProfile, String cacheKey) {
        CompletableFuture<GameProfile> profileFuture = PROFILE_CACHE.computeIfAbsent(cacheKey, key ->
                CompletableFuture.supplyAsync(() -> resolveProfile(client, fallbackProfile))
        );
        return () -> {
            GameProfile resolvedProfile = profileFuture.getNow(fallbackProfile);
            SkinTextures textures = client.getSkinProvider().getSkinTextures(resolvedProfile);
            return textures != null ? textures : DefaultSkinHelper.getSkinTextures(resolvedProfile);
        };
    }

    private static GameProfile resolveProfile(MinecraftClient client, GameProfile fallbackProfile) {
        GameProfile resolvedProfile = fallbackProfile;
        try {
            ProfileResult result = client.getSessionService().fetchProfile(fallbackProfile.getId(), false);
            if (result != null && result.profile() != null) {
                resolvedProfile = result.profile();
            }
        } catch (Exception ignored) {
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
                        String name = property.get("name").getAsString();
                        String value = property.get("value").getAsString();
                        if (property.has("signature")) {
                            profile.getProperties().put(name, new Property(name, value, property.get("signature").getAsString()));
                        } else {
                            profile.getProperties().put(name, new Property(name, value));
                        }
                    });
                }
                return profile;
            }
        } catch (Exception ignored) {
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
        return Identifier.of(ModConstants.MOD_ID, "textures/entity/customs/" + prefix + index + ".png");
    }

    /**
     * 重置自定义皮肤计数缓存，在资源包重载时调用。
     */
    public static void resetCustomSkinCount() {
        customWideSkinCount = -1;
        customSlimSkinCount = -1;
    }

    private static boolean resourceExists(Identifier id) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.getResourceManager().getResource(id).isPresent();
    }
}
