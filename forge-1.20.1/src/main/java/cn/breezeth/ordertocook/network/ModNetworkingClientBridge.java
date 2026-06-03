package cn.breezeth.ordertocook.network;

import net.minecraftforge.fml.loading.FMLEnvironment;
import java.util.function.Supplier;

final class ModNetworkingClientBridge {
    private ModNetworkingClientBridge() {
    }

    static void handlePrestigeQuery(ModNetworking.PrestigeQueryS2CPayload payload, Supplier<net.minecraftforge.network.NetworkEvent.Context> context) {
        invoke("handlePrestigeQuery", new Class<?>[] { ModNetworking.PrestigeQueryS2CPayload.class }, payload);
    }

    static void handleRestaurantRanking(ModNetworking.RestaurantRankingS2CPayload payload, Supplier<net.minecraftforge.network.NetworkEvent.Context> context) {
        invoke("handleRestaurantRanking", new Class<?>[] { ModNetworking.RestaurantRankingS2CPayload.class }, payload);
    }

    static void handleRestaurantName(ModNetworking.RestaurantNameS2CPayload payload, Supplier<net.minecraftforge.network.NetworkEvent.Context> context) {
        invoke("handleRestaurantName", new Class<?>[] { ModNetworking.RestaurantNameS2CPayload.class }, payload);
    }

    static void handleVanillaEraFaresChronRequirements(ModNetworking.VanillaEraFaresChronRequirementsS2CPayload payload, Supplier<net.minecraftforge.network.NetworkEvent.Context> context) {
        invoke("handleVanillaEraFaresChronRequirements", new Class<?>[] { ModNetworking.VanillaEraFaresChronRequirementsS2CPayload.class }, payload);
    }

    static void handleRiderAnim(ModNetworking.RiderAnimS2CPayload payload, Supplier<net.minecraftforge.network.NetworkEvent.Context> context) {
        invoke("handleRiderAnim", new Class<?>[] { ModNetworking.RiderAnimS2CPayload.class }, payload);
    }

    private static void invoke(String method, Class<?>[] parameterTypes, Object payload) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        try {
            Class<?> clientNetworking = Class.forName("cn.breezeth.ordertocook.network.ModClientNetworking");
            clientNetworking.getMethod(method, parameterTypes).invoke(null, payload);
        } catch (ReflectiveOperationException e) {
            cn.breezeth.ordertocook.OrderToCookMod.LOGGER.warn("Failed to dispatch client networking handler {}", method, e);
        }
    }
}
