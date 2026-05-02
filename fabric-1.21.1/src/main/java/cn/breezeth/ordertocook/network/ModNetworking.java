package cn.breezeth.ordertocook.network;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.core.PrestigeManager;
import cn.breezeth.ordertocook.core.WashingTableManager;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.registry.ModSounds;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

    public static final class PrestigeQueryC2SPayload implements CustomPayload {
        public static final CustomPayload.Id<PrestigeQueryC2SPayload> ID =
                new CustomPayload.Id<>(Identifier.of(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "prestige_query_c2s"));
        public static final PrestigeQueryC2SPayload INSTANCE = new PrestigeQueryC2SPayload();
        public static final PacketCodec<PacketByteBuf, PrestigeQueryC2SPayload> CODEC = PacketCodec.unit(INSTANCE);
        private PrestigeQueryC2SPayload() {}
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public static final class PrestigeQueryS2CPayload implements CustomPayload {
        public static final CustomPayload.Id<PrestigeQueryS2CPayload> ID =
                new CustomPayload.Id<>(Identifier.of(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "prestige_query_s2c"));
        public static final PacketCodec<PacketByteBuf, PrestigeQueryS2CPayload> CODEC =
                PacketCodec.of((payload, buf) -> buf.writeVarInt(payload.prestige()), buf -> new PrestigeQueryS2CPayload(buf.readVarInt()));
        private final int prestige;
        public PrestigeQueryS2CPayload(int prestige) { this.prestige = prestige; }
        public int prestige() { return prestige; }
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public static final class RestaurantRankingQueryC2SPayload implements CustomPayload {
        public static final CustomPayload.Id<RestaurantRankingQueryC2SPayload> ID =
                new CustomPayload.Id<>(Identifier.of(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_ranking_query_c2s"));
        public static final RestaurantRankingQueryC2SPayload INSTANCE = new RestaurantRankingQueryC2SPayload();
        public static final PacketCodec<PacketByteBuf, RestaurantRankingQueryC2SPayload> CODEC = PacketCodec.unit(INSTANCE);
        private RestaurantRankingQueryC2SPayload() {}
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public static final class RestaurantRankingS2CPayload implements CustomPayload {
        public static final CustomPayload.Id<RestaurantRankingS2CPayload> ID =
                new CustomPayload.Id<>(Identifier.of(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_ranking_s2c"));
        public static final PacketCodec<PacketByteBuf, RestaurantRankingS2CPayload> CODEC =
                PacketCodec.of((payload, buf) -> buf.writeString(payload.json()), buf -> new RestaurantRankingS2CPayload(buf.readString()));
        private final String json;
        public RestaurantRankingS2CPayload(String json) { this.json = json; }
        public String json() { return json; }
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public static final class RestaurantRenameC2SPayload implements CustomPayload {
        public static final CustomPayload.Id<RestaurantRenameC2SPayload> ID =
                new CustomPayload.Id<>(Identifier.of(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_rename_c2s"));
        public static final PacketCodec<PacketByteBuf, RestaurantRenameC2SPayload> CODEC =
                PacketCodec.of((payload, buf) -> buf.writeString(payload.newName(), 64), buf -> new RestaurantRenameC2SPayload(buf.readString(64)));
        private final String newName;
        public RestaurantRenameC2SPayload(String newName) { this.newName = newName; }
        public String newName() { return newName; }
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public static final class RestaurantNameQueryC2SPayload implements CustomPayload {
        public static final CustomPayload.Id<RestaurantNameQueryC2SPayload> ID =
                new CustomPayload.Id<>(Identifier.of(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_name_query_c2s"));
        public static final RestaurantNameQueryC2SPayload INSTANCE = new RestaurantNameQueryC2SPayload();
        public static final PacketCodec<PacketByteBuf, RestaurantNameQueryC2SPayload> CODEC = PacketCodec.unit(INSTANCE);
        private RestaurantNameQueryC2SPayload() {}
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public static final class RestaurantNameS2CPayload implements CustomPayload {
        public static final CustomPayload.Id<RestaurantNameS2CPayload> ID =
                new CustomPayload.Id<>(Identifier.of(cn.breezeth.ordertocook.core.ModConstants.MOD_ID, "restaurant_name_s2c"));
        public static final PacketCodec<PacketByteBuf, RestaurantNameS2CPayload> CODEC =
                PacketCodec.of((payload, buf) -> { buf.writeString(payload.name(), 128); buf.writeString(payload.owner(), 64); },
                        buf -> new RestaurantNameS2CPayload(buf.readString(128), buf.readString(64)));
        private final String name;
        private final String owner;
        public RestaurantNameS2CPayload(String name, String owner) { this.name = name; this.owner = owner; }
        public String name() { return name; }
        public String owner() { return owner; }
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public static final class ToggleMotorcycleLightPayload implements CustomPayload {
        public static final CustomPayload.Id<ToggleMotorcycleLightPayload> ID =
                new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "toggle_motorcycle_light"));
        public static final ToggleMotorcycleLightPayload INSTANCE = new ToggleMotorcycleLightPayload();
        public static final PacketCodec<PacketByteBuf, ToggleMotorcycleLightPayload> CODEC = PacketCodec.unit(INSTANCE);

        private ToggleMotorcycleLightPayload() {
        }

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record StartWashingPayload(BlockPos pos) implements CustomPayload {
        public static final CustomPayload.Id<StartWashingPayload> ID =
                new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "start_washing"));
        public static final PacketCodec<PacketByteBuf, StartWashingPayload> CODEC =
                PacketCodec.of((payload, buf) -> buf.writeBlockPos(payload.pos()), buf -> new StartWashingPayload(buf.readBlockPos()));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static final class StopWashingPayload implements CustomPayload {
        public static final CustomPayload.Id<StopWashingPayload> ID =
                new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "stop_washing"));
        public static final StopWashingPayload INSTANCE = new StopWashingPayload();
        public static final PacketCodec<PacketByteBuf, StopWashingPayload> CODEC = PacketCodec.unit(INSTANCE);

        private StopWashingPayload() {
        }

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static final class OpenMotorcycleCoolerPayload implements CustomPayload {
        public static final CustomPayload.Id<OpenMotorcycleCoolerPayload> ID =
                new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "open_motorcycle_cooler"));
        public static final OpenMotorcycleCoolerPayload INSTANCE = new OpenMotorcycleCoolerPayload();
        public static final PacketCodec<PacketByteBuf, OpenMotorcycleCoolerPayload> CODEC = PacketCodec.unit(INSTANCE);

        private OpenMotorcycleCoolerPayload() {
        }

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record RiderSoundPayload(int soundType, float pitch) implements CustomPayload {
        public static final CustomPayload.Id<RiderSoundPayload> ID =
                new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "rider_sound"));
        public static final PacketCodec<PacketByteBuf, RiderSoundPayload> CODEC =
                PacketCodec.of((payload, buf) -> {
                            buf.writeVarInt(payload.soundType());
                            buf.writeFloat(payload.pitch());
                        },
                        buf -> new RiderSoundPayload(buf.readVarInt(), buf.readFloat()));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record RiderAnimC2SPayload(int animType, boolean animate) implements CustomPayload {
        public static final CustomPayload.Id<RiderAnimC2SPayload> ID =
                new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "rider_anim_c2s"));
        public static final PacketCodec<PacketByteBuf, RiderAnimC2SPayload> CODEC =
                PacketCodec.of((payload, buf) -> {
                            buf.writeVarInt(payload.animType());
                            buf.writeBoolean(payload.animate());
                        },
                        buf -> new RiderAnimC2SPayload(buf.readVarInt(), buf.readBoolean()));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record RiderAnimS2CPayload(java.util.UUID playerUuid, int animType, boolean animate) implements CustomPayload {
        public static final CustomPayload.Id<RiderAnimS2CPayload> ID =
                new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "rider_anim_s2c"));
        public static final PacketCodec<PacketByteBuf, RiderAnimS2CPayload> CODEC =
                PacketCodec.of((payload, buf) -> {
                            buf.writeUuid(payload.playerUuid());
                            buf.writeVarInt(payload.animType());
                            buf.writeBoolean(payload.animate());
                        },
                        buf -> new RiderAnimS2CPayload(buf.readUuid(), buf.readVarInt(), buf.readBoolean()));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playC2S().register(PrestigeQueryC2SPayload.ID, PrestigeQueryC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PrestigeQueryS2CPayload.ID, PrestigeQueryS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RestaurantRankingQueryC2SPayload.ID, RestaurantRankingQueryC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RestaurantRankingS2CPayload.ID, RestaurantRankingS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RestaurantRenameC2SPayload.ID, RestaurantRenameC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RestaurantNameQueryC2SPayload.ID, RestaurantNameQueryC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RestaurantNameS2CPayload.ID, RestaurantNameS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleMotorcycleLightPayload.ID, ToggleMotorcycleLightPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StartWashingPayload.ID, StartWashingPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StopWashingPayload.ID, StopWashingPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OpenMotorcycleCoolerPayload.ID, OpenMotorcycleCoolerPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RiderSoundPayload.ID, RiderSoundPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RiderAnimC2SPayload.ID, RiderAnimC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RiderAnimS2CPayload.ID, RiderAnimS2CPayload.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(PrestigeQueryC2SPayload.ID, (payload, context) -> {
            int prestige = PrestigeManager.getPlayerPrestige(context.player());
            ServerPlayNetworking.send(context.player(), new PrestigeQueryS2CPayload(prestige));
        });

        ServerPlayNetworking.registerGlobalReceiver(RestaurantRankingQueryC2SPayload.ID, (payload, context) -> {
            java.util.List<cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.RestaurantStats> list =
                    cn.breezeth.ordertocook.core.RestaurantRegistry.allPersistedWithOnline((net.minecraft.server.world.ServerWorld) context.player().getWorld());
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
            ServerPlayNetworking.send(context.player(), new RestaurantRankingS2CPayload(json));
        });

        ServerPlayNetworking.registerGlobalReceiver(RestaurantRenameC2SPayload.ID, (payload, context) -> {
            String newName = payload.newName();
            var player = (net.minecraft.server.network.ServerPlayerEntity) context.player();
            var handler = player.currentScreenHandler;
            if (handler instanceof cn.breezeth.ordertocook.screen.OrderMachineScreenHandler h) {
                cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity be = h.getMachineIfServer();
                if (be == null) return;
                var world = be.getWorld();
                if (!(world instanceof net.minecraft.server.world.ServerWorld sw)) return;
                be.ensureMachineId(sw);
                be.setOwnerIfEmpty(player);
                String owner = player.getGameProfile().getName();
                String beOwner = be.snapshotStats().owner();
                if (!(beOwner == null || beOwner.isEmpty() || owner.equals(beOwner))) {
                    player.closeHandledScreen();
                    player.sendMessage(Text.translatable("message.ordertocook.rename_not_owner").formatted(Formatting.RED), true);
                    return;
                }
                boolean ok = be.tryRename(player, newName);
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
                ServerPlayNetworking.send(player, new RestaurantRankingS2CPayload(json2));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(RestaurantNameQueryC2SPayload.ID, (payload, context) -> {
            var player = (net.minecraft.server.network.ServerPlayerEntity) context.player();
            String name = "";
            String owner = player.getGameProfile().getName();
            var handler = player.currentScreenHandler;
            if (handler instanceof cn.breezeth.ordertocook.screen.OrderMachineScreenHandler h) {
                var be = h.getMachineIfServer();
                if (be != null) {
                    var s = be.snapshotStats();
                    name = s.name();
                    if (s.owner() != null && !s.owner().isEmpty()) owner = s.owner();
                }
            }
            ServerPlayNetworking.send(player, new RestaurantNameS2CPayload(name, owner));
        });

        ServerPlayNetworking.registerGlobalReceiver(ToggleMotorcycleLightPayload.ID, (payload, context) -> {
            MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(context.player().getVehicle());
            if (motorcycle == null || !motorcycle.hasPassenger(context.player())) {
                return;
            }
            motorcycle.setLightEnabled(!motorcycle.isLightEnabled());
            context.player().getWorld().playSound(
                    context.player(),
                    motorcycle.getX(),
                    motorcycle.getY(),
                    motorcycle.getZ(),
                    SoundEvents.BLOCK_LEVER_CLICK,
                    SoundCategory.PLAYERS,
                    0.8f,
                    1.0f
            );
        });

        ServerPlayNetworking.registerGlobalReceiver(StartWashingPayload.ID, (payload, context) ->
                WashingTableManager.start(context.player(), payload.pos()));
        ServerPlayNetworking.registerGlobalReceiver(StopWashingPayload.ID, (payload, context) ->
                WashingTableManager.stop(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(OpenMotorcycleCoolerPayload.ID, (payload, context) -> {
            MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(context.player().getVehicle());
            if (motorcycle != null && motorcycle.hasPassenger(context.player())) {
                context.player().openHandledScreen(motorcycle);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(RiderSoundPayload.ID, (payload, context) -> {
            SoundEvent sound = switch (payload.soundType()) {
                case RIDER_SOUND_HORN -> ModSounds.HORN;
                case RIDER_SOUND_CHAIR_AMBIENT -> SoundEvents.ENTITY_VILLAGER_AMBIENT;
                case RIDER_SOUND_CHAIR_TRADE -> SoundEvents.ENTITY_VILLAGER_TRADE;
                case RIDER_SOUND_CHAIR_CELEBRATE -> SoundEvents.ENTITY_VILLAGER_CELEBRATE;
                default -> null;
            };
            if (sound == null) {
                return;
            }

            boolean onMotorcycle = MotorcycleEntity.fromVehicle(context.player().getVehicle()) != null;
            boolean onChairSeat = context.player().getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null;
            if (payload.soundType() == RIDER_SOUND_HORN && !onMotorcycle) {
                return;
            }
            if (payload.soundType() != RIDER_SOUND_HORN && !onChairSeat) {
                return;
            }

            float safePitch = Math.max(0.5f, Math.min(2.0f, payload.pitch()));
            float volume = payload.soundType() == RIDER_SOUND_HORN ? 1.0f : 0.9f;
            context.player().getWorld().playSound(
                    context.player(),
                    context.player().getX(),
                    context.player().getY(),
                    context.player().getZ(),
                    sound,
                    SoundCategory.PLAYERS,
                    volume,
                    safePitch
            );
        });
        ServerPlayNetworking.registerGlobalReceiver(RiderAnimC2SPayload.ID, (payload, context) -> {
            MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(context.player().getVehicle());
            boolean onMotorcycle = motorcycle != null;
            boolean onChairSeat = context.player().getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null;
            boolean valid = switch (payload.animType()) {
                case RIDER_ANIM_HORN, RIDER_ANIM_LIGHT_TOGGLE -> onMotorcycle;
                case RIDER_ANIM_CHAIR_CLAP -> onChairSeat;
                case RIDER_ANIM_WASH_START, RIDER_ANIM_WASH_STOP -> true;
                default -> false;
            };
            if (!valid) {
                return;
            }
            for (var target : context.player().getServerWorld().getPlayers()) {
                if (target == context.player()) {
                    continue;
                }
                ServerPlayNetworking.send(target, new RiderAnimS2CPayload(context.player().getUuid(), payload.animType(), payload.animate()));
            }
        });
    }
}
