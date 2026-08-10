package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.item.ChefCoinItem;
import cn.breezeth.ordertocook.item.FoodPlateItem;
import cn.breezeth.ordertocook.item.MotorcycleItem;
import cn.breezeth.ordertocook.item.OrderItem;
import cn.breezeth.ordertocook.item.OtcUpgradeBoxItem;
import cn.breezeth.ordertocook.item.PrefilledShelfBlockItem;
import cn.breezeth.ordertocook.item.TakeoutBagItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, ModConstants.MOD_ID);

    public static final DeferredHolder<Item, Item> ORDER_MACHINE = ITEMS.register("order_machine",
            () -> new BlockItem(ModBlocks.ORDER_MACHINE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> COUNTERTOP = ITEMS.register("countertop",
            () -> new BlockItem(ModBlocks.COUNTERTOP.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> WASHINGTABLE = ITEMS.register("washingtable",
            () -> new BlockItem(ModBlocks.WASHINGTABLE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> DISHWASHER = ITEMS.register("dishwasher",
            () -> new BlockItem(ModBlocks.DISHWASHER.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("block.ordertocook.dishwasher.desc"));
                }
            });
    public static final DeferredHolder<Item, Item> REFRIGERATOR = ITEMS.register("refrigerator",
            () -> new BlockItem(ModBlocks.REFRIGERATOR.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("block.ordertocook.refrigerator.desc"));
                }
            });
    public static final DeferredHolder<Item, Item> STOVE = ITEMS.register("stove",
            () -> new BlockItem(ModBlocks.STOVE.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("block.ordertocook.stove.desc"));
                }
            });
    public static final DeferredHolder<Item, Item> OTC_UPGRADE_BOX = ITEMS.register("otc_upgrade_box",
            () -> new OtcUpgradeBoxItem(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> CHAIR = ITEMS.register("chair",
            () -> new BlockItem(ModBlocks.CHAIR.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> SHELF = ITEMS.register("shelf",
            () -> new PrefilledShelfBlockItem(ModBlocks.SHELF.get(), new Item.Properties(), PrefilledShelfBlockItem.PREFILL_PACKAGING));
    public static final DeferredHolder<Item, Item> PLATE_SHELF = ITEMS.register("plate_shelf",
            () -> new PrefilledShelfBlockItem(ModBlocks.PLATE_SHELF.get(), new Item.Properties(), PrefilledShelfBlockItem.PREFILL_PLATE));
    public static final DeferredHolder<Item, Item> BOARD = ITEMS.register("board",
            () -> new BlockItem(ModBlocks.BOARD.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> CLEAN_PLATE = ITEMS.register("clean_plate",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> DIRTY_PLATE = ITEMS.register("dirty_plate",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FOOD_PLATE = ITEMS.register("food_plate",
            () -> new FoodPlateItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> FOOD_PLATE_DISPLAY = ITEMS.register("food_plate_display",
            () -> new BlockItem(ModBlocks.FOOD_PLATE_DISPLAY.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> MOTORCYCLE = ITEMS.register("motorcycle",
            () -> new MotorcycleItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> HELMET = ITEMS.register("helmet",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ORDER = ITEMS.register("order",
            () -> new OrderItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> CHEF_COIN_1 = ITEMS.register("chef_coin_1",
            () -> new ChefCoinItem(1, new Item.Properties()));
    public static final DeferredHolder<Item, Item> CHEF_COIN_5 = ITEMS.register("chef_coin_5",
            () -> new ChefCoinItem(5, new Item.Properties()));
    public static final DeferredHolder<Item, Item> CHEF_COIN_20 = ITEMS.register("chef_coin_20",
            () -> new ChefCoinItem(20, new Item.Properties()));
    public static final DeferredHolder<Item, Item> CHEF_COIN_100 = ITEMS.register("chef_coin_100",
            () -> new ChefCoinItem(100, new Item.Properties()));
    public static final DeferredHolder<Item, Item> CHEF_COIN_10000 = ITEMS.register("chef_coin_10000",
            () -> new ChefCoinItem(10000, new Item.Properties()));
    public static final DeferredHolder<Item, Item> TAKEOUT_BAG = ITEMS.register("takeout_bag",
            () -> new TakeoutBagItem(new Item.Properties().stacksTo(1)));

    public static void registerModItems(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
