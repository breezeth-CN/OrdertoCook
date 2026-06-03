package cn.breezeth.ordertocook.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLLoader;

public final class ModClientNetworking {
    private ModClientNetworking() {
    }
    public static volatile java.util.List<cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats> LAST_RANKING = java.util.Collections.emptyList();
    public static volatile String LAST_OPEN_SCREEN_NAME = "";
    public static volatile String LAST_OPEN_SCREEN_OWNER = "";
    private static volatile java.util.Map<Integer, java.util.List<ExtraUpgradeRequirement>> VANILLA_ERA_FARES_CHRON_REQUIREMENTS = java.util.Collections.emptyMap();

    public static void registerClientReceivers() {
    }

    public static void handlePrestigeQuery(ModNetworking.PrestigeQueryS2CPayload payload) {
        Minecraft client = Minecraft.getInstance();
        int prestige = payload.prestige();
        client.execute(() -> {
            if (client.player == null) return;
            client.player.sendSystemMessage(Component.translatable("command.ordertocook.prestige_result", prestige));
        });
    }

    public static void handleRestaurantRanking(ModNetworking.RestaurantRankingS2CPayload payload) {
        Minecraft client = Minecraft.getInstance();
        String json = payload.json();
        client.execute(() -> {
            java.util.List<cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats> list =
                    parseRestaurantStatsJson(json);
            LAST_RANKING = list;
            if (client.screen instanceof cn.breezeth.ordertocook.screen.RestaurantRankingScreen screen) {
                screen.updateData(list);
            } else if (client.player != null && !FMLLoader.isProduction()) {
                client.player.sendSystemMessage(Component.translatable("message.ordertocook.ranking_received", list.size()));
            }
        });
    }

    public static void handleRestaurantName(ModNetworking.RestaurantNameS2CPayload payload) {
        Minecraft.getInstance().execute(() -> {
            LAST_OPEN_SCREEN_NAME = payload.name();
            LAST_OPEN_SCREEN_OWNER = payload.owner();
        });
    }

    public static void handleVanillaEraFaresChronRequirements(ModNetworking.VanillaEraFaresChronRequirementsS2CPayload payload) {
        Minecraft.getInstance().execute(() -> VANILLA_ERA_FARES_CHRON_REQUIREMENTS = parseExtraUpgradeRequirementsJson(payload.json()));
    }

    public static void handleRiderAnim(ModNetworking.RiderAnimS2CPayload payload) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> applyRemoteRiderAnimation(client, payload.playerUuid(), payload.animType(), payload.animate()));
    }

    public static boolean sendPrestigeQuery() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return false;
        ModNetworking.sendToServer(ModNetworking.PrestigeQueryC2SPayload.INSTANCE);
        return true;
    }

    public static boolean sendRestaurantRankingQuery() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return false;
        ModNetworking.sendToServer(ModNetworking.RestaurantRankingQueryC2SPayload.INSTANCE);
        return true;
    }

    public static boolean sendRestaurantRename(String name) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return false;
        ModNetworking.sendToServer(new ModNetworking.RestaurantRenameC2SPayload(name));
        return true;
    }

    public static boolean sendRestaurantNameQuery() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return false;
        ModNetworking.sendToServer(ModNetworking.RestaurantNameQueryC2SPayload.INSTANCE);
        return true;
    }

    public static boolean sendVanillaEraFaresChronRequirementsQuery() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return false;
        ModNetworking.sendToServer(ModNetworking.VanillaEraFaresChronRequirementsQueryC2SPayload.INSTANCE);
        return true;
    }

    public static java.util.List<ExtraUpgradeRequirement> getVanillaEraFaresChronRequirements(int nextLevel) {
        return VANILLA_ERA_FARES_CHRON_REQUIREMENTS.getOrDefault(nextLevel, java.util.Collections.emptyList());
    }

    public static void sendRiderSound(int soundType, float pitch) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return;
        ModNetworking.sendToServer(new ModNetworking.RiderSoundPayload(soundType, pitch));
    }

    public static void sendRiderAnimation(int animType, boolean animate) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return;
        ModNetworking.sendToServer(new ModNetworking.RiderAnimC2SPayload(animType, animate));
    }

    private static void applyRemoteRiderAnimation(Minecraft client, java.util.UUID uuid, int animType, boolean animate) {
        if (client.level == null) {
            return;
        }
        var player = client.level.getPlayerByUUID(uuid);
        if (!(player instanceof AbstractClientPlayer ap)) {
            return;
        }
        switch (animType) {
            case ModNetworking.RIDER_ANIM_HORN -> cn.breezeth.ordertocook.client.gecko.RiderRenderBridge.triggerHorn(ap, animate);
            case ModNetworking.RIDER_ANIM_LIGHT_TOGGLE -> cn.breezeth.ordertocook.client.gecko.RiderRenderBridge.triggerLightToggle(ap, animate);
            case ModNetworking.RIDER_ANIM_CHAIR_CLAP -> cn.breezeth.ordertocook.client.gecko.RiderRenderBridge.triggerChairClap(ap);
            case ModNetworking.RIDER_ANIM_WASH_START -> cn.breezeth.ordertocook.client.gecko.RiderRenderBridge.triggerWashStart(ap);
            case ModNetworking.RIDER_ANIM_WASH_STOP -> cn.breezeth.ordertocook.client.gecko.RiderRenderBridge.triggerWashStop(ap);
            default -> {
            }
        }
    }

    private static java.util.List<cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats> parseRestaurantStatsJson(String json) {
        java.util.List<cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats> list = new java.util.ArrayList<>();
        try {
            com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(json).getAsJsonArray();
            for (var el : arr) {
                var o = el.getAsJsonObject();
                String dimension = o.get("dimension").getAsString();
                long posLong = o.get("posLong").getAsLong();
                String name = o.get("name").getAsString();
                String owner = o.get("owner").getAsString();
                int level = o.get("level").getAsInt();
                int accepted = o.get("accepted").getAsInt();
                int delivery = o.get("delivery").getAsInt();
                int longDistance = o.get("longDistance").getAsInt();
                int totalProfit = o.get("totalProfit").getAsInt();
                int deliveryProfit = o.get("deliveryProfit").getAsInt();
                int maxDeliveryDist = o.get("maxDeliveryDist").getAsInt();
                int walkIn = o.get("walkIn").getAsInt();
                list.add(new cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats(
                        dimension, posLong, name, owner, level, accepted, delivery, longDistance, totalProfit, deliveryProfit, maxDeliveryDist, walkIn
                ));
            }
        } catch (Throwable ignored) {}
        return list;
    }

    private static java.util.Map<Integer, java.util.List<ExtraUpgradeRequirement>> parseExtraUpgradeRequirementsJson(String json) {
        java.util.Map<Integer, java.util.List<ExtraUpgradeRequirement>> map = new java.util.HashMap<>();
        try {
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            for (int level = 1; level <= 8; level++) {
                com.google.gson.JsonArray arr = root.has(String.valueOf(level))
                        ? root.getAsJsonArray(String.valueOf(level))
                        : new com.google.gson.JsonArray();
                java.util.List<ExtraUpgradeRequirement> list = new java.util.ArrayList<>();
                for (var element : arr) {
                    var obj = element.getAsJsonObject();
                    String translationKey = obj.has("translationKey") ? obj.get("translationKey").getAsString() : "";
                    String itemId = obj.has("itemId") ? obj.get("itemId").getAsString() : "";
                    int count = obj.has("count") ? obj.get("count").getAsInt() : 0;
                    if (!translationKey.isEmpty() && count > 0) {
                        list.add(new ExtraUpgradeRequirement(translationKey, itemId, count));
                    }
                }
                map.put(level, java.util.Collections.unmodifiableList(list));
            }
        } catch (Exception ignored) {
            return java.util.Collections.emptyMap();
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    public record ExtraUpgradeRequirement(String translationKey, String itemId, int count) {
    }

    public static cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats findSelfRestaurant() {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null) return null;
            String playerName = client.player.getGameProfile().getName();
            String dim = client.level.dimension().location().toString();
            cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats best = null;
            double bestDist = Double.MAX_VALUE;
            net.minecraft.core.BlockPos playerPos = client.player.blockPosition();
            for (var s : LAST_RANKING) {
                if (!dim.equals(s.dimension())) continue;
                if (!playerName.equals(s.owner())) continue;
                net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.of(s.posLong());
                double d = pos.distSqr(playerPos);
                if (d < bestDist) {
                    bestDist = d;
                    best = s;
                }
            }
            return best;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
