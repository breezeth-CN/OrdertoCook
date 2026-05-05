package cn.breezeth.ordertocook.network;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.core.PrestigeManager;
import cn.breezeth.ordertocook.core.WashingTableManager;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.registry.ModSounds;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {}
    public static final int RIDER_SOUND_HORN = 1;
    public static final int RIDER_SOUND_CHAIR_AMBIENT = 2;
    public static final int RIDER_SOUND_CHAIR_TRADE = 3;
    public static final int RIDER_SOUND_CHAIR_CELEBRATE = 4;
    public static final int RIDER_ANIM_HORN = 1;
    public static final int RIDER_ANIM_LIGHT_TOGGLE = 2;
    public static final int RIDER_ANIM_CHAIR_CLAP = 3;
    public static final int RIDER_ANIM_WASH_START = 4;
    public static final int RIDER_ANIM_WASH_STOP = 5;

    public static final class PrestigeQueryC2SPayload implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PrestigeQueryC2SPayload> ID =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "prestige_query_c2s"));
        public static final PrestigeQueryC2SPayload INSTANCE = new PrestigeQueryC2SPayload();
        public static final StreamCodec<FriendlyByteBuf, PrestigeQueryC2SPayload> CODEC = StreamCodec.unit(INSTANCE);
        private PrestigeQueryC2SPayload() {}
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public static final class PrestigeQueryS2CPayload implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PrestigeQueryS2CPayload> ID =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "prestige_query_s2c"));
        public static final StreamCodec<FriendlyByteBuf, PrestigeQueryS2CPayload> CODEC =
                StreamCodec.ofMember((payload, buf) -> buf.writeVarInt(payload.prestige()), buf -> new PrestigeQueryS2CPayload(buf.readVarInt()));
        private final int prestige;
        public PrestigeQueryS2CPayload(int prestige) { this.prestige = prestige; }
        public int prestige() { return prestige; }
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public static final class RestaurantRankingQueryC2SPayload implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RestaurantRankingQueryC2SPayload> ID =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_ranking_query_c2s"));
        public static final RestaurantRankingQueryC2SPayload INSTANCE = new RestaurantRankingQueryC2SPayload();
        public static final StreamCodec<FriendlyByteBuf, RestaurantRankingQueryC2SPayload> CODEC = StreamCodec.unit(INSTANCE);
        private RestaurantRankingQueryC2SPayload() {}
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public static final class RestaurantRankingS2CPayload implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RestaurantRankingS2CPayload> ID =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_ranking_s2c"));
        public static final StreamCodec<FriendlyByteBuf, RestaurantRankingS2CPayload> CODEC =
                StreamCodec.ofMember((payload, buf) -> buf.writeUtf(payload.json()), buf -> new RestaurantRankingS2CPayload(buf.readUtf()));
        private final String json;
        public RestaurantRankingS2CPayload(String json) { this.json = json; }
        public String json() { return json; }
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public static final class RestaurantRenameC2SPayload implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RestaurantRenameC2SPayload> ID =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_rename_c2s"));
        public static final StreamCodec<FriendlyByteBuf, RestaurantRenameC2SPayload> CODEC =
                StreamCodec.ofMember((payload, buf) -> buf.writeUtf(payload.newName(), 64), buf -> new RestaurantRenameC2SPayload(buf.readUtf(64)));
        private final String newName;
        public RestaurantRenameC2SPayload(String newName) { this.newName = newName; }
        public String newName() { return newName; }
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public static final class RestaurantNameQueryC2SPayload implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RestaurantNameQueryC2SPayload> ID =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_name_query_c2s"));
        public static final RestaurantNameQueryC2SPayload INSTANCE = new RestaurantNameQueryC2SPayload();
        public static final StreamCodec<FriendlyByteBuf, RestaurantNameQueryC2SPayload> CODEC = StreamCodec.unit(INSTANCE);
        private RestaurantNameQueryC2SPayload() {}
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public static final class RestaurantNameS2CPayload implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RestaurantNameS2CPayload> ID =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_name_s2c"));
        public static final StreamCodec<FriendlyByteBuf, RestaurantNameS2CPayload> CODEC =
                StreamCodec.ofMember((payload, buf) -> { buf.writeUtf(payload.name(), 128); buf.writeUtf(payload.owner(), 64); },
                        buf -> new RestaurantNameS2CPayload(buf.readUtf(128), buf.readUtf(64)));
        private final String name;
        private final String owner;
        public RestaurantNameS2CPayload(String name, String owner) { this.name = name; this.owner = owner; }
        public String name() { return name; }
        public String owner() { return owner; }
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public static final class ToggleMotorcycleLightPayload implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ToggleMotorcycleLightPayload> ID =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "toggle_motorcycle_light"));
        public static final ToggleMotorcycleLightPayload INSTANCE = new ToggleMotorcycleLightPayload();
        public static final StreamCodec<FriendlyByteBuf, ToggleMotorcycleLightPayload> CODEC = StreamCodec.unit(INSTANCE);

        private ToggleMotorcycleLightPayload() {
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record StartWashingPayload(BlockPos pos) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<StartWashingPayload> ID =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "start_washing"));
        public static final StreamCodec<FriendlyByteBuf, StartWashingPayload> CODEC =
                StreamCodec.ofMember((payload, buf) -> buf.writeBlockPos(payload.pos()), buf -> new StartWashingPayload(buf.readBlockPos()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public static final class StopWashingPayload implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<StopWashingPayload> ID =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "stop_washing"));
        public static final StopWashingPayload INSTANCE = new StopWashingPayload();
        public static final StreamCodec<FriendlyByteBuf, StopWashingPayload> CODEC = StreamCodec.unit(INSTANCE);

        private StopWashingPayload() {
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public static final class OpenMotorcycleCoolerPayload implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenMotorcycleCoolerPayload> ID =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "open_motorcycle_cooler"));
        public static final OpenMotorcycleCoolerPayload INSTANCE = new OpenMotorcycleCoolerPayload();
        public static final StreamCodec<FriendlyByteBuf, OpenMotorcycleCoolerPayload> CODEC = StreamCodec.unit(INSTANCE);

        private OpenMotorcycleCoolerPayload() {
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record RiderSoundPayload(int soundType, float pitch) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RiderSoundPayload> ID =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "rider_sound"));
        public static final StreamCodec<FriendlyByteBuf, RiderSoundPayload> CODEC =
                StreamCodec.ofMember((payload, buf) -> {
                            buf.writeVarInt(payload.soundType());
                            buf.writeFloat(payload.pitch());
                        },
                        buf -> new RiderSoundPayload(buf.readVarInt(), buf.readFloat()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record RiderAnimC2SPayload(int animType, boolean animate) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RiderAnimC2SPayload> ID =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "rider_anim_c2s"));
        public static final StreamCodec<FriendlyByteBuf, RiderAnimC2SPayload> CODEC =
                StreamCodec.ofMember((payload, buf) -> {
                            buf.writeVarInt(payload.animType());
                            buf.writeBoolean(payload.animate());
                        },
                        buf -> new RiderAnimC2SPayload(buf.readVarInt(), buf.readBoolean()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record RiderAnimS2CPayload(java.util.UUID playerUuid, int animType, boolean animate) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RiderAnimS2CPayload> ID =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "rider_anim_s2c"));
        public static final StreamCodec<FriendlyByteBuf, RiderAnimS2CPayload> CODEC =
                StreamCodec.ofMember((payload, buf) -> {
                            buf.writeUUID(payload.playerUuid());
                            buf.writeVarInt(payload.animType());
                            buf.writeBoolean(payload.animate());
                        },
                        buf -> new RiderAnimS2CPayload(buf.readUUID(), buf.readVarInt(), buf.readBoolean()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(PrestigeQueryC2SPayload.ID, PrestigeQueryC2SPayload.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            int prestige = PrestigeManager.getPlayerPrestige(player);
            PacketDistributor.sendToPlayer(player, new PrestigeQueryS2CPayload(prestige));
        });
        registrar.playToClient(PrestigeQueryS2CPayload.ID, PrestigeQueryS2CPayload.CODEC, ModNetworkingClientBridge::handlePrestigeQuery);

        registrar.playToServer(RestaurantRankingQueryC2SPayload.ID, RestaurantRankingQueryC2SPayload.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            java.util.List<cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats> list =
                    cn.breezeth.ordertocook.core.RestaurantRegistry.allPersistedWithOnline(player.serverLevel());
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
            PacketDistributor.sendToPlayer(player, new RestaurantRankingS2CPayload(json));
        });
        registrar.playToClient(RestaurantRankingS2CPayload.ID, RestaurantRankingS2CPayload.CODEC, ModNetworkingClientBridge::handleRestaurantRanking);

        registrar.playToServer(RestaurantRenameC2SPayload.ID, RestaurantRenameC2SPayload.CODEC, (payload, context) -> {
            String newName = payload.newName();
            var player = (net.minecraft.server.level.ServerPlayer) context.player();
            var handler = player.containerMenu;
            if (handler instanceof cn.breezeth.ordertocook.screen.OrderMachineScreenHandler h) {
                cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity be = h.getMachineIfServer();
                if (be == null) return;
                var world = be.getLevel();
                if (!(world instanceof net.minecraft.server.level.ServerLevel sw)) return;
                be.ensureMachineId(sw);
                be.setOwnerIfEmpty(player);
                String owner = player.getGameProfile().getName();
                String beOwner = be.snapshotStats().owner();
                if (!(beOwner == null || beOwner.isEmpty() || owner.equals(beOwner))) {
                    player.closeContainer();
                    player.displayClientMessage(Component.translatable("message.ordertocook.rename_not_owner").withStyle(ChatFormatting.RED), true);
                    return;
                }
                boolean ok = be.tryRename(player, newName);
                if (!ok) {
                    player.closeContainer();
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
                PacketDistributor.sendToPlayer(player, new RestaurantRankingS2CPayload(json2));
            }
        });

        registrar.playToServer(RestaurantNameQueryC2SPayload.ID, RestaurantNameQueryC2SPayload.CODEC, (payload, context) -> {
            var player = (net.minecraft.server.level.ServerPlayer) context.player();
            String name = "";
            String owner = player.getGameProfile().getName();
            var handler = player.containerMenu;
            if (handler instanceof cn.breezeth.ordertocook.screen.OrderMachineScreenHandler h) {
                var be = h.getMachineIfServer();
                if (be != null) {
                    var s = be.snapshotStats();
                    name = s.name();
                    if (s.owner() != null && !s.owner().isEmpty()) owner = s.owner();
                }
            }
            PacketDistributor.sendToPlayer(player, new RestaurantNameS2CPayload(name, owner));
        });
        registrar.playToClient(RestaurantNameS2CPayload.ID, RestaurantNameS2CPayload.CODEC, ModNetworkingClientBridge::handleRestaurantName);

        registrar.playToServer(ToggleMotorcycleLightPayload.ID, ToggleMotorcycleLightPayload.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
            if (motorcycle == null || !motorcycle.hasPassenger(player)) {
                return;
            }
            motorcycle.setLightEnabled(!motorcycle.isLightEnabled());
            player.level().playSound(
                    player,
                    motorcycle.getX(),
                    motorcycle.getY(),
                    motorcycle.getZ(),
                    SoundEvents.LEVER_CLICK,
                    SoundSource.PLAYERS,
                    0.8f,
                    1.0f
            );
        });

        registrar.playToServer(StartWashingPayload.ID, StartWashingPayload.CODEC, (payload, context) ->
                WashingTableManager.start((ServerPlayer) context.player(), payload.pos()));
        registrar.playToServer(StopWashingPayload.ID, StopWashingPayload.CODEC, (payload, context) ->
                WashingTableManager.stop((ServerPlayer) context.player()));
        registrar.playToServer(OpenMotorcycleCoolerPayload.ID, OpenMotorcycleCoolerPayload.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
            if (motorcycle != null && motorcycle.hasPassenger(player)) {
                player.openMenu(motorcycle);
            }
        });
        registrar.playToServer(RiderSoundPayload.ID, RiderSoundPayload.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            SoundEvent sound = switch (payload.soundType()) {
                case RIDER_SOUND_HORN -> ModSounds.HORN.get();
                case RIDER_SOUND_CHAIR_AMBIENT -> SoundEvents.VILLAGER_AMBIENT;
                case RIDER_SOUND_CHAIR_TRADE -> SoundEvents.VILLAGER_TRADE;
                case RIDER_SOUND_CHAIR_CELEBRATE -> SoundEvents.VILLAGER_CELEBRATE;
                default -> null;
            };
            if (sound == null) {
                return;
            }

            boolean onMotorcycle = MotorcycleEntity.fromVehicle(player.getVehicle()) != null;
            boolean onChairSeat = player.getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null;
            if (payload.soundType() == RIDER_SOUND_HORN && !onMotorcycle) {
                return;
            }
            if (payload.soundType() != RIDER_SOUND_HORN && !onChairSeat) {
                return;
            }

            float safePitch = Math.max(0.5f, Math.min(2.0f, payload.pitch()));
            float volume = payload.soundType() == RIDER_SOUND_HORN ? 1.0f : 0.9f;
            player.level().playSound(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    sound,
                    SoundSource.PLAYERS,
                    volume,
                    safePitch
            );
        });
        registrar.playToServer(RiderAnimC2SPayload.ID, RiderAnimC2SPayload.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
            boolean onMotorcycle = motorcycle != null;
            boolean onChairSeat = player.getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null;
            boolean valid = switch (payload.animType()) {
                case RIDER_ANIM_HORN, RIDER_ANIM_LIGHT_TOGGLE -> onMotorcycle;
                case RIDER_ANIM_CHAIR_CLAP -> onChairSeat;
                case RIDER_ANIM_WASH_START, RIDER_ANIM_WASH_STOP -> true;
                default -> false;
            };
            if (!valid) {
                return;
            }
            for (var target : player.serverLevel().players()) {
                if (target == player) {
                    continue;
                }
                PacketDistributor.sendToPlayer(target, new RiderAnimS2CPayload(player.getUUID(), payload.animType(), payload.animate()));
            }
        });
        registrar.playToClient(RiderAnimS2CPayload.ID, RiderAnimS2CPayload.CODEC, ModNetworkingClientBridge::handleRiderAnim);
    }
}
