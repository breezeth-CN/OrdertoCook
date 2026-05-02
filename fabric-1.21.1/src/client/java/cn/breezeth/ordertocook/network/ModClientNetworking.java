package cn.breezeth.ordertocook.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;

public final class ModClientNetworking {
    private ModClientNetworking() {
    }
    public static volatile java.util.List<cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats> LAST_RANKING = java.util.Collections.emptyList();
    public static volatile String LAST_OPEN_SCREEN_NAME = "";
    public static volatile String LAST_OPEN_SCREEN_OWNER = "";

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.PrestigeQueryS2CPayload.ID, (payload, context) -> {
            int prestige = payload.prestige();
            context.client().execute(() -> {
                if (context.client().player == null) return;
                context.client().player.sendMessage(Text.translatable("command.ordertocook.prestige_result", prestige), false);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.RestaurantRankingS2CPayload.ID, (payload, context) -> {
            String json = payload.json();
            context.client().execute(() -> {
                if (context.client().currentScreen instanceof cn.breezeth.ordertocook.screen.RestaurantRankingScreen screen) {
                    java.util.List<cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats> list =
                            parseRestaurantStatsJson(json);
                    LAST_RANKING = list;
                    screen.updateData(list);
                } else if (context.client().player != null) {
                    java.util.List<cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats> list =
                            parseRestaurantStatsJson(json);
                    LAST_RANKING = list;
                    if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                        context.client().player.sendMessage(Text.translatable("message.ordertocook.ranking_received", list.size()), false);
                    }
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.RestaurantNameS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                LAST_OPEN_SCREEN_NAME = payload.name();
                LAST_OPEN_SCREEN_OWNER = payload.owner();
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.RiderAnimS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> applyRemoteRiderAnimation(context.client(), payload.playerUuid(), payload.animType(), payload.animate())));
    }

    public static boolean sendPrestigeQuery() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return false;
        ClientPlayNetworking.send(ModNetworking.PrestigeQueryC2SPayload.INSTANCE);
        return true;
    }

    public static boolean sendRestaurantRankingQuery() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return false;
        ClientPlayNetworking.send(ModNetworking.RestaurantRankingQueryC2SPayload.INSTANCE);
        return true;
    }

    public static boolean sendRestaurantRename(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return false;
        ClientPlayNetworking.send(new ModNetworking.RestaurantRenameC2SPayload(name));
        return true;
    }

    public static boolean sendRestaurantNameQuery() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return false;
        ClientPlayNetworking.send(ModNetworking.RestaurantNameQueryC2SPayload.INSTANCE);
        return true;
    }

    public static void sendRiderSound(int soundType, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;
        ClientPlayNetworking.send(new ModNetworking.RiderSoundPayload(soundType, pitch));
    }

    public static void sendRiderAnimation(int animType, boolean animate) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;
        ClientPlayNetworking.send(new ModNetworking.RiderAnimC2SPayload(animType, animate));
    }

    private static void applyRemoteRiderAnimation(MinecraftClient client, java.util.UUID uuid, int animType, boolean animate) {
        if (client.world == null) {
            return;
        }
        var player = client.world.getPlayerByUuid(uuid);
        if (!(player instanceof AbstractClientPlayerEntity ap)) {
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

    public static cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats findSelfRestaurant() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return null;
            String playerName = client.player.getGameProfile().getName();
            String dim = client.world.getRegistryKey().getValue().toString();
            cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats best = null;
            double bestDist = Double.MAX_VALUE;
            net.minecraft.util.math.BlockPos playerPos = client.player.getBlockPos();
            for (var s : LAST_RANKING) {
                if (!dim.equals(s.dimension())) continue;
                if (!playerName.equals(s.owner())) continue;
                net.minecraft.util.math.BlockPos pos = net.minecraft.util.math.BlockPos.fromLong(s.posLong());
                double d = pos.getSquaredDistance(playerPos);
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
