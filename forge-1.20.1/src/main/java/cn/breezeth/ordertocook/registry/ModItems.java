package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.item.ChefCoinItem;
import cn.breezeth.ordertocook.item.FoodPlateItem;
import cn.breezeth.ordertocook.item.MotorcycleItem;
import cn.breezeth.ordertocook.item.OrderItem;
import cn.breezeth.ordertocook.item.TakeoutBagItem;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ModConstants.MOD_ID);

    public static final RegistryObject<Item> ORDER_MACHINE = ITEMS.register("order_machine",
            () -> new BlockItem(ModBlocks.ORDER_MACHINE.get(), new Item.Properties()));
    public static final RegistryObject<Item> COUNTERTOP = ITEMS.register("countertop",
            () -> new BlockItem(ModBlocks.COUNTERTOP.get(), new Item.Properties()));
    public static final RegistryObject<Item> WASHINGTABLE = ITEMS.register("washingtable",
            () -> new BlockItem(ModBlocks.WASHINGTABLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> CHAIR = ITEMS.register("chair",
            () -> new BlockItem(ModBlocks.CHAIR.get(), new Item.Properties()));
    public static final RegistryObject<Item> SHELF = ITEMS.register("shelf",
            () -> new BlockItem(ModBlocks.SHELF.get(), new Item.Properties()));
    public static final RegistryObject<Item> PLATE_SHELF = ITEMS.register("plate_shelf",
            () -> new BlockItem(ModBlocks.PLATE_SHELF.get(), new Item.Properties()));
    public static final RegistryObject<Item> BOARD = ITEMS.register("board",
            () -> new BlockItem(ModBlocks.BOARD.get(), new Item.Properties()));
    public static final RegistryObject<Item> CLEAN_PLATE = ITEMS.register("clean_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DIRTY_PLATE = ITEMS.register("dirty_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FOOD_PLATE = ITEMS.register("food_plate",
            () -> new FoodPlateItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FOOD_PLATE_DISPLAY = ITEMS.register("food_plate_display",
            () -> new BlockItem(ModBlocks.FOOD_PLATE_DISPLAY.get(), new Item.Properties()));
    public static final RegistryObject<Item> MOTORCYCLE = ITEMS.register("motorcycle",
            () -> new MotorcycleItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> HELMET = ITEMS.register("helmet",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ORDER = ITEMS.register("order",
            () -> new OrderItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CHEF_COIN_1 = ITEMS.register("chef_coin_1",
            () -> new ChefCoinItem(1, new Item.Properties()));
    public static final RegistryObject<Item> CHEF_COIN_5 = ITEMS.register("chef_coin_5",
            () -> new ChefCoinItem(5, new Item.Properties()));
    public static final RegistryObject<Item> CHEF_COIN_20 = ITEMS.register("chef_coin_20",
            () -> new ChefCoinItem(20, new Item.Properties()));
    public static final RegistryObject<Item> CHEF_COIN_100 = ITEMS.register("chef_coin_100",
            () -> new ChefCoinItem(100, new Item.Properties()));
    public static final RegistryObject<Item> CHEF_COIN_10000 = ITEMS.register("chef_coin_10000",
            () -> new ChefCoinItem(10000, new Item.Properties()));
    public static final RegistryObject<Item> TAKEOUT_BAG = ITEMS.register("takeout_bag",
            () -> new TakeoutBagItem(new Item.Properties().stacksTo(1)));

    public static void registerModItems(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
