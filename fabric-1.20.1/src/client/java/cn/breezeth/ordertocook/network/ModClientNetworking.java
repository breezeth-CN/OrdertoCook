package cn.breezeth.ordertocook.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import io.netty.buffer.Unpooled;

public final class ModClientNetworking {
    private ModClientNetworking() {
    }
    public static volatile java.util.List<cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats> LAST_RANKING = java.util.Collections.emptyList();
    public static volatile String LAST_OPEN_SCREEN_NAME = "";
    public static volatile String LAST_OPEN_SCREEN_OWNER = "";
    private static volatile java.util.Map<Integer, java.util.List<ExtraUpgradeRequirement>> VANILLA_ERA_FARES_CHRON_REQUIREMENTS = java.util.Collections.emptyMap();

    public static void registerClientReceivers() {
        Identifier PRESTIGE_QUERY_S2C = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "prestige_query_s2c");
        Identifier RESTAURANT_RANKING_S2C = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_ranking_s2c");
        Identifier RESTAURANT_NAME_S2C = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_name_s2c");
        Identifier VANILLA_ERA_FARES_CHRON_REQUIREMENTS_S2C = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "vanilla_era_fares_chron_requirements_s2c");
        Identifier RIDER_ANIM_S2C = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "rider_anim_s2c");
        ClientPlayNetworking.registerGlobalReceiver(PRESTIGE_QUERY_S2C, (client, handler, buf, responseSender) -> {
            int prestige = buf.readVarInt();
            client.execute(() -> {
                if (client.player == null) return;
                client.player.sendMessage(Text.translatable("command.ordertocook.prestige_result", prestige), false);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(RESTAURANT_RANKING_S2C, (client, handler, buf, responseSender) -> {
            String json = buf.readString(32767);
            client.execute(() -> {
                if (client.currentScreen instanceof cn.breezeth.ordertocook.screen.RestaurantRankingScreen screen) {
                    java.util.List<cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats> list =
                            parseRestaurantStatsJson(json);
                    LAST_RANKING = list;
                    screen.updateData(list);
                } else if (client.player != null) {
                    java.util.List<cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats> list =
                            parseRestaurantStatsJson(json);
                    LAST_RANKING = list;
                    if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                        client.player.sendMessage(Text.translatable("message.ordertocook.ranking_received", list.size()), false);
                    }
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(RESTAURANT_NAME_S2C, (client, handler, buf, responseSender) -> {
            String name = buf.readString(128);
            String owner = buf.readString(64);
            client.execute(() -> {
                LAST_OPEN_SCREEN_NAME = name;
                LAST_OPEN_SCREEN_OWNER = owner;
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(VANILLA_ERA_FARES_CHRON_REQUIREMENTS_S2C, (client, handler, buf, responseSender) -> {
            String json = buf.readString(32767);
            client.execute(() -> VANILLA_ERA_FARES_CHRON_REQUIREMENTS = parseExtraUpgradeRequirementsJson(json));
        });
        ClientPlayNetworking.registerGlobalReceiver(RIDER_ANIM_S2C, (client, handler, buf, responseSender) -> {
            java.util.UUID uuid = buf.readUuid();
            int animType = buf.readVarInt();
            boolean animate = buf.readBoolean();
            client.execute(() -> applyRemoteRiderAnimation(client, uuid, animType, animate));
        });
    }

    public static boolean sendPrestigeQuery() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return false;
        Identifier PRESTIGE_QUERY_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "prestige_query_c2s");
        ClientPlayNetworking.send(PRESTIGE_QUERY_C2S, new PacketByteBuf(Unpooled.buffer()));
        return true;
    }

    public static boolean sendRestaurantRankingQuery() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return false;
        Identifier RESTAURANT_RANKING_QUERY_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_ranking_query_c2s");
        ClientPlayNetworking.send(RESTAURANT_RANKING_QUERY_C2S, new PacketByteBuf(Unpooled.buffer()));
        return true;
    }

    public static boolean sendRestaurantRename(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return false;
        Identifier RESTAURANT_RENAME_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_rename_c2s");
        Identifier RESTAURANT_RANKING_QUERY_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_ranking_query_c2s");
        PacketByteBuf out = new PacketByteBuf(Unpooled.buffer());
        out.writeString(name, 64);
        ClientPlayNetworking.send(RESTAURANT_RENAME_C2S, out);
        ClientPlayNetworking.send(RESTAURANT_RANKING_QUERY_C2S, new PacketByteBuf(Unpooled.buffer()));
        return true;
    }

    public static boolean sendRestaurantNameQuery() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return false;
        Identifier RESTAURANT_NAME_QUERY_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_name_query_c2s");
        ClientPlayNetworking.send(RESTAURANT_NAME_QUERY_C2S, new PacketByteBuf(Unpooled.buffer()));
        return true;
    }

    public static boolean sendVanillaEraFaresChronRequirementsQuery() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return false;
        Identifier id = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "vanilla_era_fares_chron_requirements_query_c2s");
        ClientPlayNetworking.send(id, new PacketByteBuf(Unpooled.buffer()));
        return true;
    }

    public static java.util.List<ExtraUpgradeRequirement> getVanillaEraFaresChronRequirements(int nextLevel) {
        return VANILLA_ERA_FARES_CHRON_REQUIREMENTS.getOrDefault(nextLevel, java.util.Collections.emptyList());
    }

    public static boolean sendOpenMotorcycleCooler() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return false;
        Identifier OPEN_MOTORCYCLE_COOLER_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "open_motorcycle_cooler_c2s");
        ClientPlayNetworking.send(OPEN_MOTORCYCLE_COOLER_C2S, new PacketByteBuf(Unpooled.buffer()));
        return true;
    }

    public static void sendStartWashing(net.minecraft.util.math.BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;
        Identifier START_WASHING_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "start_washing_c2s");
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        ClientPlayNetworking.send(START_WASHING_C2S, buf);
    }

    public static void sendStopWashing() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;
        Identifier STOP_WASHING_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "stop_washing_c2s");
        ClientPlayNetworking.send(STOP_WASHING_C2S, new PacketByteBuf(Unpooled.buffer()));
    }

    public static void sendToggleMotorcycleLight() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;
        Identifier id = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "toggle_motorcycle_light_c2s");
        ClientPlayNetworking.send(id, new PacketByteBuf(Unpooled.buffer()));
    }

    public static void sendRiderSound(int soundType, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;
        Identifier id = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "rider_sound_c2s");
        PacketByteBuf out = new PacketByteBuf(Unpooled.buffer());
        out.writeVarInt(soundType);
        out.writeFloat(pitch);
        ClientPlayNetworking.send(id, out);
    }

    public static void sendRiderAnimation(int animType, boolean animate) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;
        Identifier id = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "rider_anim_c2s");
        PacketByteBuf out = new PacketByteBuf(Unpooled.buffer());
        out.writeVarInt(animType);
        out.writeBoolean(animate);
        ClientPlayNetworking.send(id, out);
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

    public record ExtraUpgradeRequirement(String translationKey, String itemId, int count) {
    }
}
