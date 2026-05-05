package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.block.BoardBlock;
import cn.breezeth.ordertocook.block.ChairBlock;
import cn.breezeth.ordertocook.block.FoodPlateBlock;
import cn.breezeth.ordertocook.block.OrderMachineBlock;
import cn.breezeth.ordertocook.block.PlateShelfBlock;
import cn.breezeth.ordertocook.block.ShelfBlock;
import cn.breezeth.ordertocook.block.TakeoutBagBlock;
import cn.breezeth.ordertocook.block.TakeoutBoxBlock;
import cn.breezeth.ordertocook.block.WashingTableBlock;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, ModConstants.MOD_ID);

    public static final DeferredHolder<Block, Block> ORDER_MACHINE = BLOCKS.register("order_machine",
            () -> new OrderMachineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONECUTTER)));
    public static final DeferredHolder<Block, Block> COUNTERTOP = BLOCKS.register("countertop",
            () -> new TakeoutBoxBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)));
    public static final DeferredHolder<Block, Block> WASHINGTABLE = BLOCKS.register("washingtable",
            () -> new WashingTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));
    public static final DeferredHolder<Block, Block> CHAIR = BLOCKS.register("chair",
            () -> new ChairBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)));
    public static final DeferredHolder<Block, Block> SHELF = BLOCKS.register("shelf",
            () -> new ShelfBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredHolder<Block, Block> PLATE_SHELF = BLOCKS.register("plate_shelf",
            () -> new PlateShelfBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredHolder<Block, Block> TAKEOUT_BAG = BLOCKS.register("takeout_bag",
            () -> new TakeoutBagBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL).noOcclusion()));
    public static final DeferredHolder<Block, Block> FOOD_PLATE_DISPLAY = BLOCKS.register("food_plate_display",
            () -> new FoodPlateBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL).noOcclusion()));
    public static final DeferredHolder<Block, Block> BOARD = BLOCKS.register("board",
            () -> new BoardBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));

    public static void registerModBlocks(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
