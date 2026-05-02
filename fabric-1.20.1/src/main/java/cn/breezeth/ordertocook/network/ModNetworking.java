package cn.breezeth.ordertocook.network;

import cn.breezeth.ordertocook.core.PrestigeManager;
import cn.breezeth.ordertocook.core.WashingTableManager;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.registry.ModSounds;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import io.netty.buffer.Unpooled;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ModNetworking {
    private static final Identifier PRESTIGE_QUERY_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "prestige_query_c2s");
    private static final Identifier PRESTIGE_QUERY_S2C = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "prestige_query_s2c");
    private static final Identifier RESTAURANT_RANKING_QUERY_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_ranking_query_c2s");
    private static final Identifier RESTAURANT_RANKING_S2C = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_ranking_s2c");
    private static final Identifier RESTAURANT_RENAME_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_rename_c2s");
    private static final Identifier RESTAURANT_NAME_QUERY_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_name_query_c2s");
    private static final Identifier RESTAURANT_NAME_S2C = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_name_s2c");
    private static final Identifier OPEN_MOTORCYCLE_COOLER_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "open_motorcycle_cooler_c2s");
    private static final Identifier START_WASHING_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "start_washing_c2s");
    private static final Identifier STOP_WASHING_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "stop_washing_c2s");
    private static final Identifier TOGGLE_MOTORCYCLE_LIGHT_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "toggle_motorcycle_light_c2s");
    private static final Identifier RIDER_SOUND_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "rider_sound_c2s");
    private static final Identifier RIDER_ANIM_C2S = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "rider_anim_c2s");
    private static final Identifier RIDER_ANIM_S2C = new Identifier(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "rider_anim_s2c");
    public static final int RIDER_SOUND_HORN = 1;
    public static final int RIDER_SOUND_CHAIR_AMBIENT = 2;
    public static final int RIDER_SOUND_CHAIR_TRADE = 3;
    public static final int RIDER_SOUND_CHAIR_CELEBRATE = 4;
    public static final int RIDER_ANIM_HORN = 1;
    public static final int RIDER_ANIM_LIGHT_TOGGLE = 2;
    public static final int RIDER_ANIM_CHAIR_CLAP = 3;
    public static final int RIDER_ANIM_WASH_START = 4;
    public static final int RIDER_ANIM_WASH_STOP = 5;

    private ModNetworking() {
    }

    public static void registerPayloadTypes() { }

    public static class StartWashingPayload {
        private final BlockPos pos;
        public StartWashingPayload(BlockPos pos) { this.pos = pos; }
        public BlockPos getPos() { return pos; }
        public void write(PacketByteBuf buf) {
            buf.writeBlockPos(pos);
        }
    }

    public static class StopWashingPayload {
        public static final StopWashingPayload INSTANCE = new StopWashingPayload();
        private StopWashingPayload() {}
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(PRESTIGE_QUERY_C2S, (server, player, handler, buf, responseSender) -> {
            int prestige = PrestigeManager.getPlayerPrestige(player);
            PacketByteBuf out = new PacketByteBuf(Unpooled.buffer());
            out.writeVarInt(prestige);
            ServerPlayNetworking.send(player, PRESTIGE_QUERY_S2C, out);
        });
        ServerPlayNetworking.registerGlobalReceiver(RESTAURANT_RANKING_QUERY_C2S, (server, player, handler, buf, responseSender) -> {
            java.util.List<cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats> list =
                    cn.breezeth.ordertocook.core.RestaurantRegistry.allPersistedWithOnline((net.minecraft.server.world.ServerWorld) player.getWorld());
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            for (var s : list) {
                com.google.gson.JsonObject o = new com.google.gson.JsonObject();
                o.addProperty("dimension", s.dimension());
                o.addProperty("posLong", s.posLong());
                o.addProperty("name", s.name());
                o.addProperty("owner", s.owner());
                o.addProperty("level", s.level());
                o.addProperty("accepted", s.accepted());
                o.addProperty("delivery", s.delivery());
                o.addProperty("longDistance", s.longDistance());
                o.addProperty("totalProfit", s.totalProfit());
                o.addProperty("deliveryProfit", s.deliveryProfit());
                o.addProperty("maxDeliveryDist", s.maxDeliveryDist());
                o.addProperty("walkIn", s.walkIn());
                arr.add(o);
            }
            String json = new com.google.gson.Gson().toJson(arr);
            PacketByteBuf out = new PacketByteBuf(Unpooled.buffer());
            out.writeString(json);
            ServerPlayNetworking.send(player, RESTAURANT_RANKING_S2C, out);
        });
        // 改名后广播全量在线餐厅列表：与 fabric-1.21.1 ModNetworking 中同名包处理保持一致，避免客户端列表不同步。
        ServerPlayNetworking.registerGlobalReceiver(RESTAURANT_RENAME_C2S, (server, player, handler, buf, responseSender) -> {
            String newName = buf.readString(64);
            var sp = (net.minecraft.server.network.ServerPlayerEntity) player;
            var screenHandler = player.currentScreenHandler;
            if (!(screenHandler instanceof cn.breezeth.ordertocook.screen.OrderMachineScreenHandler h)) {
                return;
            }
            cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity be = h.getMachineIfServer();
            if (be == null) {
                return;
            }
            var world = be.getWorld();
            if (!(world instanceof net.minecraft.server.world.ServerWorld sw)) {
                return;
            }
            be.ensureMachineId(sw);
            be.setOwnerIfEmpty(sp);
            String owner = player.getGameProfile().getName();
            String beOwner = be.snapshotStats().owner();
            if (!(beOwner == null || beOwner.isEmpty() || owner.equals(beOwner))) {
                player.closeHandledScreen();
                player.sendMessage(Text.translatable("message.ordertocook.rename_not_owner").formatted(Formatting.RED), true);
                return;
            }
            boolean ok = be.tryRename(sp, newName);
            if (!ok) {
                player.closeHandledScreen();
                return;
            }
            cn.breezeth.ordertocook.core.RestaurantRegistry.update(be);
            java.util.List<cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats> list2 =
                    cn.breezeth.ordertocook.core.RestaurantRegistry.allOnline();
            com.google.gson.JsonArray arr2 = new com.google.gson.JsonArray();
            for (var s2 : list2) {
                com.google.gson.JsonObject o2 = new com.google.gson.JsonObject();
                o2.addProperty("dimension", s2.dimension());
                o2.addProperty("posLong", s2.posLong());
                o2.addProperty("name", s2.name());
                o2.addProperty("owner", s2.owner());
                o2.addProperty("level", s2.level());
                o2.addProperty("accepted", s2.accepted());
                o2.addProperty("delivery", s2.delivery());
                o2.addProperty("longDistance", s2.longDistance());
                o2.addProperty("totalProfit", s2.totalProfit());
                o2.addProperty("deliveryProfit", s2.deliveryProfit());
                o2.addProperty("maxDeliveryDist", s2.maxDeliveryDist());
                o2.addProperty("walkIn", s2.walkIn());
                arr2.add(o2);
            }
            String json2 = new com.google.gson.Gson().toJson(arr2);
            PacketByteBuf out = new PacketByteBuf(Unpooled.buffer());
            out.writeString(json2);
            ServerPlayNetworking.send(player, RESTAURANT_RANKING_S2C, out);
        });
        ServerPlayNetworking.registerGlobalReceiver(RESTAURANT_NAME_QUERY_C2S, (server, player, handler, buf, responseSender) -> {
            String name = "";
            String owner = player.getGameProfile().getName();
            if (player.currentScreenHandler instanceof cn.breezeth.ordertocook.screen.OrderMachineScreenHandler sh) {
                var be = sh.getMachineIfServer();
                if (be != null) {
                    var s = be.snapshotStats();
                    name = s.name();
                    if (s.owner() != null && !s.owner().isEmpty()) owner = s.owner();
                }
            }
            PacketByteBuf out = new PacketByteBuf(Unpooled.buffer());
            out.writeString(name, 128);
            out.writeString(owner, 64);
            ServerPlayNetworking.send(player, RESTAURANT_NAME_S2C, out);
        });

        ServerPlayNetworking.registerGlobalReceiver(OPEN_MOTORCYCLE_COOLER_C2S, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
                if (motorcycle == null || !motorcycle.controlsThisMotorcycle(player)) {
                    return;
                }
                player.openHandledScreen(motorcycle);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(START_WASHING_C2S, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            server.execute(() -> {
                WashingTableManager.start(player, pos);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(STOP_WASHING_C2S, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                WashingTableManager.stop(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_MOTORCYCLE_LIGHT_C2S, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
                if (motorcycle == null || !motorcycle.controlsThisMotorcycle(player)) {
                    return;
                }
                motorcycle.toggleHeadlight();
                player.getWorld().playSound(
                        player,
                        motorcycle.getX(),
                        motorcycle.getY(),
                        motorcycle.getZ(),
                        SoundEvents.BLOCK_LEVER_CLICK,
                        SoundCategory.PLAYERS,
                        0.8f,
                        1.0f
                );
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RIDER_SOUND_C2S, (server, player, handler, buf, responseSender) -> {
            int soundType = buf.readVarInt();
            float pitch = buf.readFloat();
            server.execute(() -> {
                SoundEvent sound = switch (soundType) {
                    case RIDER_SOUND_HORN -> ModSounds.HORN;
                    case RIDER_SOUND_CHAIR_AMBIENT -> SoundEvents.ENTITY_VILLAGER_AMBIENT;
                    case RIDER_SOUND_CHAIR_TRADE -> SoundEvents.ENTITY_VILLAGER_TRADE;
                    case RIDER_SOUND_CHAIR_CELEBRATE -> SoundEvents.ENTITY_VILLAGER_CELEBRATE;
                    default -> null;
                };
                if (sound == null) {
                    return;
                }

                boolean onMotorcycle = MotorcycleEntity.fromVehicle(player.getVehicle()) != null;
                boolean onChairSeat = player.getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null;
                if (soundType == RIDER_SOUND_HORN && !onMotorcycle) {
                    return;
                }
                if (soundType != RIDER_SOUND_HORN && !onChairSeat) {
                    return;
                }

                float safePitch = Math.max(0.5f, Math.min(2.0f, pitch));
                float volume = soundType == RIDER_SOUND_HORN ? 1.0f : 0.9f;
                player.getWorld().playSound(
                        player,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        sound,
                        SoundCategory.PLAYERS,
                        volume,
                        safePitch
                );
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RIDER_ANIM_C2S, (server, player, handler, buf, responseSender) -> {
            int animType = buf.readVarInt();
            boolean animate = buf.readBoolean();
            server.execute(() -> {
                MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
                boolean onMotorcycle = motorcycle != null;
                boolean onChairSeat = player.getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null;
                boolean valid = switch (animType) {
                    case RIDER_ANIM_HORN, RIDER_ANIM_LIGHT_TOGGLE -> onMotorcycle;
                    case RIDER_ANIM_CHAIR_CLAP -> onChairSeat;
                    case RIDER_ANIM_WASH_START, RIDER_ANIM_WASH_STOP -> true;
                    default -> false;
                };
                if (!valid) {
                    return;
                }
                broadcastRiderAnim(player, animType, animate);
            });
        });
    }

    private static void broadcastRiderAnim(ServerPlayerEntity source, int animType, boolean animate) {
        for (ServerPlayerEntity target : source.getServerWorld().getPlayers()) {
            if (target == source) {
                continue;
            }
            PacketByteBuf out = new PacketByteBuf(Unpooled.buffer());
            out.writeUuid(source.getUuid());
            out.writeVarInt(animType);
            out.writeBoolean(animate);
            ServerPlayNetworking.send(target, RIDER_ANIM_S2C, out);
        }
    }
}
