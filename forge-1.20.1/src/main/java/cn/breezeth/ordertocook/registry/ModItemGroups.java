package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;

public class ModItemGroups {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModConstants.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ORDER_GROUP = TABS.register("order_group", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.ORDER_MACHINE.get()))
            .title(Component.translatable("itemGroup.ordertocook.group"))
            .displayItems((displayContext, entries) -> {
                entries.accept(ModItems.ORDER_MACHINE.get());
                entries.accept(ModItems.COUNTERTOP.get());
                entries.accept(ModItems.WASHINGTABLE.get());
                entries.accept(ModItems.TAKEOUT_BAG.get());
                entries.accept(ModItems.CHAIR.get());
                entries.accept(ModItems.SHELF.get());
                entries.accept(ModItems.PLATE_SHELF.get());
                entries.accept(ModItems.BOARD.get());
                entries.accept(ModItems.CLEAN_PLATE.get());
                entries.accept(ModItems.DIRTY_PLATE.get());
                entries.accept(ModItems.FOOD_PLATE.get());
                entries.accept(ModItems.MOTORCYCLE.get());
                entries.accept(ModItems.HELMET.get());
                entries.accept(ModItems.ORDER.get());
                entries.accept(ModItems.CHEF_COIN_1.get());
                entries.accept(ModItems.CHEF_COIN_5.get());
                entries.accept(ModItems.CHEF_COIN_20.get());
                entries.accept(ModItems.CHEF_COIN_100.get());
                entries.accept(ModItems.CHEF_COIN_10000.get());
            })
            .build());

    public static void registerItemGroups(IEventBus modBus) {
        TABS.register(modBus);
    }
}
