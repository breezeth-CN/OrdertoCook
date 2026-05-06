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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ModNetworking {
    private static final String PROTOCOL = "1";
    private static int nextPacketId = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(ModConstants.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

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

    public static final class PrestigeQueryC2SPayload {
        public static final PrestigeQueryC2SPayload INSTANCE = new PrestigeQueryC2SPayload();
        private PrestigeQueryC2SPayload() {}
    }

    public static final class PrestigeQueryS2CPayload {
        private final int prestige;
        public PrestigeQueryS2CPayload(int prestige) { this.prestige = prestige; }
        public int prestige() { return prestige; }
    }

    public static final class RestaurantRankingQueryC2SPayload {
        public static final RestaurantRankingQueryC2SPayload INSTANCE = new RestaurantRankingQueryC2SPayload();
        private RestaurantRankingQueryC2SPayload() {}
    }

    public static final class RestaurantRankingS2CPayload {
        private final String json;
        public RestaurantRankingS2CPayload(String json) { this.json = json; }
        public String json() { return json; }
    }

    public static final class RestaurantRenameC2SPayload {
        private final String newName;
        public RestaurantRenameC2SPayload(String newName) { this.newName = newName; }
        public String newName() { return newName; }
    }

    public static final class RestaurantNameQueryC2SPayload {
        public static final RestaurantNameQueryC2SPayload INSTANCE = new RestaurantNameQueryC2SPayload();
        private RestaurantNameQueryC2SPayload() {}
    }

    public static final class RestaurantNameS2CPayload {
        private final String name;
        private final String owner;
        public RestaurantNameS2CPayload(String name, String owner) { this.name = name; this.owner = owner; }
        public String name() { return name; }
        public String owner() { return owner; }
    }

    public static final class ToggleMotorcycleLightPayload {
        public static final ToggleMotorcycleLightPayload INSTANCE = new ToggleMotorcycleLightPayload();
        private ToggleMotorcycleLightPayload() {}
    }

    public record StartWashingPayload(BlockPos pos) {}

    public static final class StopWashingPayload {
        public static final StopWashingPayload INSTANCE = new StopWashingPayload();
        private StopWashingPayload() {}
    }

    public static final class OpenMotorcycleCoolerPayload {
        public static final OpenMotorcycleCoolerPayload INSTANCE = new OpenMotorcycleCoolerPayload();
        private OpenMotorcycleCoolerPayload() {}
    }

    public record RiderSoundPayload(int soundType, float pitch) {}

    public record RiderAnimC2SPayload(int animType, boolean animate) {}

    public record RiderAnimS2CPayload(UUID playerUuid, int animType, boolean animate) {}

    public static void register() {
        registerServer(PrestigeQueryC2SPayload.class, (msg, buf) -> {}, buf -> PrestigeQueryC2SPayload.INSTANCE, (msg, player) -> {
            int prestige = PrestigeManager.getPlayerPrestige(player);
            sendToPlayer(player, new PrestigeQueryS2CPayload(prestige));
        });
        registerClient(PrestigeQueryS2CPayload.class, (msg, buf) -> buf.writeVarInt(msg.prestige()), buf -> new PrestigeQueryS2CPayload(buf.readVarInt()), ModNetworkingClientBridge::handlePrestigeQuery);

        registerServer(RestaurantRankingQueryC2SPayload.class, (msg, buf) -> {}, buf -> RestaurantRankingQueryC2SPayload.INSTANCE, (msg, player) -> {
            var list = cn.breezeth.ordertocook.core.RestaurantRegistry.allPersistedWithOnline(player.serverLevel());
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
            sendToPlayer(player, new RestaurantRankingS2CPayload(new com.google.gson.Gson().toJson(arr)));
        });
        registerClient(RestaurantRankingS2CPayload.class, (msg, buf) -> buf.writeUtf(msg.json()), buf -> new RestaurantRankingS2CPayload(buf.readUtf()), ModNetworkingClientBridge::handleRestaurantRanking);

        registerServer(RestaurantRenameC2SPayload.class, (msg, buf) -> buf.writeUtf(msg.newName(), 64), buf -> new RestaurantRenameC2SPayload(buf.readUtf(64)), ModNetworking::handleRename);

        registerServer(RestaurantNameQueryC2SPayload.class, (msg, buf) -> {}, buf -> RestaurantNameQueryC2SPayload.INSTANCE, (msg, player) -> {
            String name = "";
            String owner = player.getGameProfile().getName();
            if (player.containerMenu instanceof cn.breezeth.ordertocook.screen.OrderMachineScreenHandler h) {
                var be = h.getMachineIfServer();
                if (be != null) {
                    var s = be.snapshotStats();
                    name = s.name();
                    if (s.owner() != null && !s.owner().isEmpty()) owner = s.owner();
                }
            }
            sendToPlayer(player, new RestaurantNameS2CPayload(name, owner));
        });
        registerClient(RestaurantNameS2CPayload.class, (msg, buf) -> { buf.writeUtf(msg.name(), 128); buf.writeUtf(msg.owner(), 64); },
                buf -> new RestaurantNameS2CPayload(buf.readUtf(128), buf.readUtf(64)), ModNetworkingClientBridge::handleRestaurantName);

        registerServer(ToggleMotorcycleLightPayload.class, (msg, buf) -> {}, buf -> ToggleMotorcycleLightPayload.INSTANCE, (msg, player) -> {
            MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
            if (motorcycle == null || !motorcycle.hasPassenger(player)) return;
            motorcycle.setLightEnabled(!motorcycle.isLightEnabled());
            player.level().playSound(player, motorcycle.getX(), motorcycle.getY(), motorcycle.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.8f, 1.0f);
        });
        registerServer(StartWashingPayload.class, (msg, buf) -> buf.writeBlockPos(msg.pos()), buf -> new StartWashingPayload(buf.readBlockPos()), (msg, player) -> WashingTableManager.start(player, msg.pos()));
        registerServer(StopWashingPayload.class, (msg, buf) -> {}, buf -> StopWashingPayload.INSTANCE, (msg, player) -> WashingTableManager.stop(player));
        registerServer(OpenMotorcycleCoolerPayload.class, (msg, buf) -> {}, buf -> OpenMotorcycleCoolerPayload.INSTANCE, (msg, player) -> {
            MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
            if (motorcycle != null && motorcycle.hasPassenger(player)) player.openMenu(motorcycle);
        });
        registerServer(RiderSoundPayload.class, (msg, buf) -> { buf.writeVarInt(msg.soundType()); buf.writeFloat(msg.pitch()); },
                buf -> new RiderSoundPayload(buf.readVarInt(), buf.readFloat()), ModNetworking::handleRiderSound);
        registerServer(RiderAnimC2SPayload.class, (msg, buf) -> { buf.writeVarInt(msg.animType()); buf.writeBoolean(msg.animate()); },
                buf -> new RiderAnimC2SPayload(buf.readVarInt(), buf.readBoolean()), ModNetworking::handleRiderAnim);
        registerClient(RiderAnimS2CPayload.class, (msg, buf) -> { buf.writeUUID(msg.playerUuid()); buf.writeVarInt(msg.animType()); buf.writeBoolean(msg.animate()); },
                buf -> new RiderAnimS2CPayload(buf.readUUID(), buf.readVarInt(), buf.readBoolean()), ModNetworkingClientBridge::handleRiderAnim);
    }

    public static void sendToServer(Object payload) {
        CHANNEL.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, Object payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    private static <T> void registerServer(Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, ServerPlayer> handler) {
        CHANNEL.messageBuilder(type, nextPacketId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(encoder)
                .decoder(decoder)
                .consumerMainThread((msg, ctx) -> {
                    ServerPlayer player = ctx.get().getSender();
                    if (player != null) handler.accept(msg, player);
                    ctx.get().setPacketHandled(true);
                })
                .add();
    }

    private static <T> void registerClient(Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<net.minecraftforge.network.NetworkEvent.Context>> handler) {
        CHANNEL.messageBuilder(type, nextPacketId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(encoder)
                .decoder(decoder)
                .consumerMainThread((msg, ctx) -> {
                    handler.accept(msg, ctx);
                    ctx.get().setPacketHandled(true);
                })
                .add();
    }

    private static void handleRename(RestaurantRenameC2SPayload payload, ServerPlayer player) {
        String newName = payload.newName();
        if (player.containerMenu instanceof cn.breezeth.ordertocook.screen.OrderMachineScreenHandler h) {
            var be = h.getMachineIfServer();
            if (be == null || !(be.getLevel() instanceof net.minecraft.server.level.ServerLevel sw)) return;
            be.ensureMachineId(sw);
            be.setOwnerIfEmpty(player);
            String owner = player.getGameProfile().getName();
            String beOwner = be.snapshotStats().owner();
            if (!(beOwner == null || beOwner.isEmpty() || owner.equals(beOwner))) {
                player.closeContainer();
                player.displayClientMessage(Component.translatable("message.ordertocook.rename_not_owner").withStyle(ChatFormatting.RED), true);
                return;
            }
            if (!be.tryRename(player, newName)) {
                player.closeContainer();
                return;
            }
            cn.breezeth.ordertocook.core.RestaurantRegistry.update(be);
            sendToPlayer(player, new RestaurantRankingS2CPayload("[]"));
        }
    }

    private static void handleRiderSound(RiderSoundPayload payload, ServerPlayer player) {
        SoundEvent sound = switch (payload.soundType()) {
            case RIDER_SOUND_HORN -> ModSounds.HORN.get();
            case RIDER_SOUND_CHAIR_AMBIENT -> SoundEvents.VILLAGER_AMBIENT;
            case RIDER_SOUND_CHAIR_TRADE -> SoundEvents.VILLAGER_TRADE;
            case RIDER_SOUND_CHAIR_CELEBRATE -> SoundEvents.VILLAGER_CELEBRATE;
            default -> null;
        };
        if (sound == null) return;
        boolean onMotorcycle = MotorcycleEntity.fromVehicle(player.getVehicle()) != null;
        boolean onChairSeat = player.getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null;
        if (payload.soundType() == RIDER_SOUND_HORN && !onMotorcycle) return;
        if (payload.soundType() != RIDER_SOUND_HORN && !onChairSeat) return;
        float safePitch = Math.max(0.5f, Math.min(2.0f, payload.pitch()));
        float volume = payload.soundType() == RIDER_SOUND_HORN ? 1.0f : 0.9f;
        player.level().playSound(player, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, safePitch);
    }

    private static void handleRiderAnim(RiderAnimC2SPayload payload, ServerPlayer player) {
        MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
        boolean onMotorcycle = motorcycle != null;
        boolean onChairSeat = player.getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null;
        boolean valid = switch (payload.animType()) {
            case RIDER_ANIM_HORN, RIDER_ANIM_LIGHT_TOGGLE -> onMotorcycle;
            case RIDER_ANIM_CHAIR_CLAP -> onChairSeat;
            case RIDER_ANIM_WASH_START, RIDER_ANIM_WASH_STOP -> true;
            default -> false;
        };
        if (!valid) return;
        for (var target : player.serverLevel().players()) {
            if (target != player) sendToPlayer(target, new RiderAnimS2CPayload(player.getUUID(), payload.animType(), payload.animate()));
        }
    }
}
