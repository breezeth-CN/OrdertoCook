package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.entity.CustomerEntity;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

public final class CustomerEntityModel extends GeoModel<CustomerEntity> {
    private static final ResourceLocation TEXTURE_WIDE = id("textures/entity/otc_zombie_custom.png");
    private static final ResourceLocation TEXTURE_SLIM = id("textures/entity/otc_zombie_custom2.png");
    private static final ResourceLocation MODEL_WIDE = id("geo/drive_idle.geo.json");
    private static final ResourceLocation MODEL_SLIM = id("geo/drive_idle_alex.geo.json");
    private static final ResourceLocation ANIMATION = id("animations/npc.animation.json");
    private static final ResourceLocation TRANSITION_ANIMATION = id("animations/npc.transition.animation.json");
    private static final Map<String, CompletableFuture<GameProfile>> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Supplier<ResourceLocation>> SKIN_CACHE = new ConcurrentHashMap<>();

    @Override
    public ResourceLocation getModelResource(CustomerEntity animatable) {
        return isSlim(animatable) ? MODEL_SLIM : MODEL_WIDE;
    }

    @Override
    public ResourceLocation getTextureResource(CustomerEntity animatable) {
        if (animatable.usesPlayerSkin()) {
            return getSkinTexture(animatable);
        }
        return animatable.getTextureVariant() == 2 ? TEXTURE_SLIM : TEXTURE_WIDE;
    }

    @Override
    public ResourceLocation getAnimationResource(CustomerEntity animatable) {
        return animatable.isTransitionAnimationActive() ? TRANSITION_ANIMATION : ANIMATION;
    }

    private static boolean isSlim(CustomerEntity entity) {
        if (entity.usesPlayerSkin()) {
            return isSlimSkin(entity);
        }
        return entity.getTextureVariant() == 2;
    }

    private static ResourceLocation getSkinTexture(CustomerEntity entity) {
        Minecraft client = Minecraft.getInstance();
        UUID profileId = getProfileId(entity);
        if (client == null) {
            return DefaultPlayerSkin.getDefaultSkin(profileId);
        }

        GameProfile profile = new GameProfile(profileId, entity.getSkinAccount());
        String cacheKey = cacheKey(entity, profileId);
        Supplier<ResourceLocation> supplier = SKIN_CACHE.computeIfAbsent(cacheKey, key ->
                createSkinSupplier(client, profile, key)
        );
        ResourceLocation texture = supplier.get();
        return texture != null ? texture : DefaultPlayerSkin.getDefaultSkin(profileId);
    }

    private static boolean isSlimSkin(CustomerEntity entity) {
        Minecraft client = Minecraft.getInstance();
        UUID profileId = getProfileId(entity);
        if (client == null) {
            return "slim".equalsIgnoreCase(DefaultPlayerSkin.getSkinModelName(profileId));
        }

        GameProfile fallbackProfile = new GameProfile(profileId, entity.getSkinAccount());
        CompletableFuture<GameProfile> future = PROFILE_CACHE.computeIfAbsent(cacheKey(entity, profileId), key ->
                CompletableFuture.supplyAsync(() -> resolveProfile(client, fallbackProfile))
        );
        GameProfile resolvedProfile = future.getNow(fallbackProfile);
        try {
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures =
                    client.getSkinManager().getInsecureSkinInformation(resolvedProfile);
            MinecraftProfileTexture skin = textures.get(MinecraftProfileTexture.Type.SKIN);
            if (skin != null) {
                String model = skin.getMetadata("model");
                if ("slim".equalsIgnoreCase(model)) {
                    return true;
                }
                if ("classic".equalsIgnoreCase(model) || "default".equalsIgnoreCase(model)) {
                    return false;
                }
            }
        } catch (Throwable ignored) {
        }
        return "slim".equalsIgnoreCase(DefaultPlayerSkin.getSkinModelName(profileId));
    }

    private static Supplier<ResourceLocation> createSkinSupplier(Minecraft client, GameProfile fallbackProfile, String cacheKey) {
        CompletableFuture<GameProfile> profileFuture = PROFILE_CACHE.computeIfAbsent(cacheKey, key ->
                CompletableFuture.supplyAsync(() -> resolveProfile(client, fallbackProfile))
        );
        return () -> {
            GameProfile resolvedProfile = profileFuture.getNow(fallbackProfile);
            try {
                Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures =
                        client.getSkinManager().getInsecureSkinInformation(resolvedProfile);
                MinecraftProfileTexture skin = textures.get(MinecraftProfileTexture.Type.SKIN);
                if (skin != null) {
                    return client.getSkinManager().registerTexture(skin, MinecraftProfileTexture.Type.SKIN);
                }
            } catch (Throwable ignored) {
            }
            return DefaultPlayerSkin.getDefaultSkin(resolvedProfile.getId());
        };
    }

    private static GameProfile resolveProfile(Minecraft client, GameProfile fallbackProfile) {
        GameProfile resolvedProfile = fallbackProfile;
        try {
            GameProfile filled = client.getMinecraftSessionService().fillProfileProperties(fallbackProfile, false);
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
                        }
                        profile.getProperties().put(name, new Property(name, value));
                    });
                }
                return profile;
            }
        } catch (Throwable ignored) {
            return fallbackProfile;
        }
    }

    private static String cacheKey(CustomerEntity entity, UUID profileId) {
        return (entity.getSkinAccount() + "|" + profileId).toLowerCase(Locale.ROOT);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, path);
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
}
