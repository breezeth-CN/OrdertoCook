package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.core.ModConstants;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup ORDER_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.ORDER_MACHINE))
            .displayName(Text.translatable("itemGroup.ordertocook.group"))
            .entries((displayContext, entries) -> {
                entries.add(ModItems.ORDER_MACHINE);
                entries.add(ModItems.COUNTERTOP);
                entries.add(ModItems.WASHINGTABLE);
                entries.add(ModItems.TAKEOUT_BAG);
                entries.add(ModItems.CHAIR);
                entries.add(ModItems.SHELF);
                entries.add(ModItems.PLATE_SHELF);
                entries.add(ModItems.BOARD);
                entries.add(ModItems.CLEAN_PLATE);
                entries.add(ModItems.DIRTY_PLATE);
                entries.add(ModItems.FOOD_PLATE);
                entries.add(ModItems.MOTORCYCLE);
                entries.add(ModItems.HELMET);
                entries.add(ModItems.ORDER);
                entries.add(ModItems.CHEF_COIN_1);
                entries.add(ModItems.CHEF_COIN_5);
                entries.add(ModItems.CHEF_COIN_20);
                entries.add(ModItems.CHEF_COIN_100);
                entries.add(ModItems.CHEF_COIN_10000);
            })
            .build();

    public static void registerItemGroups() {
        Registry.register(Registries.ITEM_GROUP, new Identifier(ModConstants.MOD_ID, "order_group"), ORDER_GROUP);
    }
}
