package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.screen.BoardScreenHandler;
import cn.breezeth.ordertocook.screen.OrderMachineScreenHandler;
import cn.breezeth.ordertocook.screen.TakeoutBoxScreenHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModScreenHandlers {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, ModConstants.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<OrderMachineScreenHandler>> ORDER_MACHINE_SCREEN_HANDLER = MENUS.register("order_machine",
            () -> new MenuType<>(OrderMachineScreenHandler::new, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<TakeoutBoxScreenHandler>> COUNTERTOP_SCREEN_HANDLER = MENUS.register("countertop",
            () -> new MenuType<>(TakeoutBoxScreenHandler::new, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<BoardScreenHandler>> BOARD_SCREEN_HANDLER = MENUS.register("board",
            () -> new MenuType<>((syncId, inv) -> new BoardScreenHandler(syncId, inv), FeatureFlags.VANILLA_SET));

    public static void registerScreenHandlers(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
