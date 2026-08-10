package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.screen.OrderMachineScreenHandler;
import cn.breezeth.ordertocook.screen.RefrigeratorScreenHandler;
import cn.breezeth.ordertocook.screen.StoveScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

import cn.breezeth.ordertocook.screen.TakeoutBoxScreenHandler;
import cn.breezeth.ordertocook.screen.BoardScreenHandler;

public class ModScreenHandlers {
    public static final ScreenHandlerType<OrderMachineScreenHandler> ORDER_MACHINE_SCREEN_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(ModConstants.MOD_ID, "order_machine"),
            new ScreenHandlerType<>(OrderMachineScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    public static final ScreenHandlerType<TakeoutBoxScreenHandler> COUNTERTOP_SCREEN_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(ModConstants.MOD_ID, "countertop"),
            new ScreenHandlerType<>(TakeoutBoxScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    public static final ScreenHandlerType<BoardScreenHandler> BOARD_SCREEN_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(ModConstants.MOD_ID, "board"),
            new ScreenHandlerType<>((syncId, inv) -> new BoardScreenHandler(syncId, inv), FeatureFlags.VANILLA_FEATURES)
    );

    public static final ScreenHandlerType<RefrigeratorScreenHandler> REFRIGERATOR_UPPER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(ModConstants.MOD_ID, "refrigerator_upper"),
            new ScreenHandlerType<>(RefrigeratorScreenHandler::clientUpper, FeatureFlags.VANILLA_FEATURES)
    );

    public static final ScreenHandlerType<RefrigeratorScreenHandler> REFRIGERATOR_LOWER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(ModConstants.MOD_ID, "refrigerator_lower"),
            new ScreenHandlerType<>(RefrigeratorScreenHandler::clientLower, FeatureFlags.VANILLA_FEATURES)
    );

    public static final ScreenHandlerType<StoveScreenHandler> STOVE = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(ModConstants.MOD_ID, "stove"),
            new ScreenHandlerType<>(StoveScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    public static void registerScreenHandlers() {
        // Init
    }
}
