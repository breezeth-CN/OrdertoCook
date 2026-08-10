package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import cn.breezeth.ordertocook.item.ChefCoinItem;
import cn.breezeth.ordertocook.item.FoodPlateItem;
import cn.breezeth.ordertocook.item.HelmetItem;
import cn.breezeth.ordertocook.item.MotorcycleItem;
import cn.breezeth.ordertocook.item.OrderItem;
import cn.breezeth.ordertocook.item.OtcUpgradeBoxItem;
import cn.breezeth.ordertocook.item.PrefilledShelfBlockItem;
import cn.breezeth.ordertocook.item.TakeoutBagItem;

public class ModItems {
    
    // 注册方块对应的物品
    public static final Item ORDER_MACHINE = registerItem("order_machine", 
            new BlockItem(ModBlocks.ORDER_MACHINE, new Item.Settings()));
            
    // 操作台
    public static final Item COUNTERTOP = registerItem("countertop",
            new BlockItem(ModBlocks.COUNTERTOP, new Item.Settings()));

    public static final Item CHAIR = registerItem("chair",
            new BlockItem(ModBlocks.CHAIR, new Item.Settings()));

    public static final Item SHELF = registerItem("shelf",
            new PrefilledShelfBlockItem(ModBlocks.SHELF, new Item.Settings(), PrefilledShelfBlockItem.PREFILL_PACKAGING));

    public static final Item BOARD = registerItem("board",
            new BlockItem(ModBlocks.BOARD, new Item.Settings()));

    // 订单 - 使用自定义 OrderItem
    public static final Item ORDER = registerItem("order", 
            new OrderItem(new Item.Settings().maxCount(1)));

    // 厨师币
    public static final Item CHEF_COIN_1 = registerItem("chef_coin_1", new ChefCoinItem(1, new Item.Settings()));
    public static final Item CHEF_COIN_5 = registerItem("chef_coin_5", new ChefCoinItem(5, new Item.Settings()));
    public static final Item CHEF_COIN_20 = registerItem("chef_coin_20", new ChefCoinItem(20, new Item.Settings()));
    public static final Item CHEF_COIN_100 = registerItem("chef_coin_100", new ChefCoinItem(100, new Item.Settings()));
    public static final Item CHEF_COIN_10000 = registerItem("chef_coin_10000", new ChefCoinItem(10000, new Item.Settings()));

    // 打包袋 - 使用自定义 TakeoutBagItem
    public static final Item TAKEOUT_BAG = registerItem("takeout_bag", 
            new TakeoutBagItem(new Item.Settings().maxCount(1)));

    public static final Item CLEAN_PLATE = registerItem("clean_plate", new Item(new Item.Settings()));

    public static final Item DIRTY_PLATE = registerItem("dirty_plate", new Item(new Item.Settings()));

    // 餐盘
    public static final Item FOOD_PLATE = registerItem("food_plate",
            new FoodPlateItem(new Item.Settings().maxCount(1)));

    // 餐盘展示方块
    public static final Item FOOD_PLATE_DISPLAY = registerItem("food_plate_display",
            new BlockItem(ModBlocks.FOOD_PLATE_DISPLAY, new Item.Settings()));

    // 餐盘货架
    public static final Item PLATE_SHELF = registerItem("plate_shelf",
            new PrefilledShelfBlockItem(ModBlocks.PLATE_SHELF, new Item.Settings(), PrefilledShelfBlockItem.PREFILL_PLATE));

    // 洗碗池
    public static final Item WASHINGTABLE = registerItem("washingtable",
            new BlockItem(ModBlocks.WASHINGTABLE, new Item.Settings()));

    public static final Item DISHWASHER = registerItem("dishwasher",
            new BlockItem(ModBlocks.DISHWASHER, new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, net.minecraft.world.World world, java.util.List<net.minecraft.text.Text> tooltip, net.minecraft.client.item.TooltipContext context) {
                    tooltip.add(net.minecraft.text.Text.translatable("block.ordertocook.dishwasher.desc"));
                }
            });

    public static final Item REFRIGERATOR = registerItem("refrigerator",
            new BlockItem(ModBlocks.REFRIGERATOR, new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, net.minecraft.world.World world, java.util.List<net.minecraft.text.Text> tooltip, net.minecraft.client.item.TooltipContext context) {
                    tooltip.add(net.minecraft.text.Text.translatable("block.ordertocook.refrigerator.desc"));
                }
            });

    public static final Item STOVE = registerItem("stove",
            new BlockItem(ModBlocks.STOVE, new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, net.minecraft.world.World world, java.util.List<net.minecraft.text.Text> tooltip, net.minecraft.client.item.TooltipContext context) {
                    tooltip.add(net.minecraft.text.Text.translatable("block.ordertocook.stove.desc"));
                }
            });

    // 电瓶车
    public static final Item MOTORCYCLE = registerItem("motorcycle",
            new MotorcycleItem(new Item.Settings().maxCount(1)));

    public static final Item HELMET = registerItem("helmet", new HelmetItem(new Item.Settings()));

    public static final Item OTC_UPGRADE_BOX = registerItem("otc_upgrade_box",
            new OtcUpgradeBoxItem(new Item.Settings().maxCount(16)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(ModConstants.MOD_ID, name), item);
    }

	public static void registerModItems() {
		// 物品已注册到 Registries.ITEM，并在 ModItemGroups 中添加到创造模式物品栏
	}
}
