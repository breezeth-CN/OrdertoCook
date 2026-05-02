package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import cn.breezeth.ordertocook.block.OrderMachineBlock;

import cn.breezeth.ordertocook.block.TakeoutBoxBlock;
import cn.breezeth.ordertocook.block.ChairBlock;
import cn.breezeth.ordertocook.block.ShelfBlock;
import cn.breezeth.ordertocook.block.TakeoutBagBlock;
import cn.breezeth.ordertocook.block.BoardBlock;
import cn.breezeth.ordertocook.block.FoodPlateBlock;
import cn.breezeth.ordertocook.block.PlateShelfBlock;
import cn.breezeth.ordertocook.block.WashingTableBlock;

public class ModBlocks {
    // 打单机 - 使用自定义 Block 类
    public static final Block ORDER_MACHINE = registerBlock("order_machine",
            new OrderMachineBlock(AbstractBlock.Settings.copy(Blocks.STONECUTTER)));

    // 操作台
    public static final Block COUNTERTOP = registerBlock("countertop",
            new TakeoutBoxBlock(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE)));

    // 椅子 - 暂用橡木台阶的属性
    public static final Block CHAIR = registerBlock("chair",
            new ChairBlock(AbstractBlock.Settings.copy(Blocks.OAK_SLAB)));

    public static final Block SHELF = registerBlock("shelf",
            new ShelfBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)));

    public static final Block TAKEOUT_BAG = registerBlock("takeout_bag",
            new TakeoutBagBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).nonOpaque()));
    
    public static final Block BOARD = registerBlock("board",
            new BoardBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque()));

    // 餐盘展示方块
    public static final Block FOOD_PLATE_DISPLAY = registerBlock("food_plate_display",
            new FoodPlateBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).nonOpaque()));

    // 餐盘货架
    public static final Block PLATE_SHELF = registerBlock("plate_shelf",
            new PlateShelfBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)));

    public static final Block WASHINGTABLE = registerBlock("washingtable",
            new WashingTableBlock(AbstractBlock.Settings.copy(Blocks.BRICKS)));

    private static Block registerBlock(String name, Block block) {
        return Registry.register(Registries.BLOCK, new Identifier(ModConstants.MOD_ID, name), block);
    }

    public static void registerModBlocks() {
        // 用于主类调用初始化
    }
}
