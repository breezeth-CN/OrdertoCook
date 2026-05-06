package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.screen.BoardScreenHandler;
import cn.breezeth.ordertocook.screen.OrderMachineScreenHandler;
import cn.breezeth.ordertocook.screen.TakeoutBoxScreenHandler;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModScreenHandlers {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ModConstants.MOD_ID);

    public static final RegistryObject<MenuType<OrderMachineScreenHandler>> ORDER_MACHINE_SCREEN_HANDLER = MENUS.register("order_machine",
            () -> new MenuType<>(OrderMachineScreenHandler::new, FeatureFlags.VANILLA_SET));
    public static final RegistryObject<MenuType<TakeoutBoxScreenHandler>> COUNTERTOP_SCREEN_HANDLER = MENUS.register("countertop",
            () -> new MenuType<>(TakeoutBoxScreenHandler::new, FeatureFlags.VANILLA_SET));
    public static final RegistryObject<MenuType<BoardScreenHandler>> BOARD_SCREEN_HANDLER = MENUS.register("board",
            () -> new MenuType<>((syncId, inv) -> new BoardScreenHandler(syncId, inv), FeatureFlags.VANILLA_SET));

    public static void registerScreenHandlers(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
