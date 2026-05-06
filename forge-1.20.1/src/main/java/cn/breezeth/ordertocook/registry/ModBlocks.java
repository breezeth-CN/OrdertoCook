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
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ModConstants.MOD_ID);

    public static final RegistryObject<Block> ORDER_MACHINE = BLOCKS.register("order_machine",
            () -> new OrderMachineBlock(BlockBehaviour.Properties.copy(Blocks.STONECUTTER)));
    public static final RegistryObject<Block> COUNTERTOP = BLOCKS.register("countertop",
            () -> new TakeoutBoxBlock(BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE)));
    public static final RegistryObject<Block> WASHINGTABLE = BLOCKS.register("washingtable",
            () -> new WashingTableBlock(BlockBehaviour.Properties.copy(Blocks.BRICKS)));
    public static final RegistryObject<Block> CHAIR = BLOCKS.register("chair",
            () -> new ChairBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SLAB)));
    public static final RegistryObject<Block> SHELF = BLOCKS.register("shelf",
            () -> new ShelfBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));
    public static final RegistryObject<Block> PLATE_SHELF = BLOCKS.register("plate_shelf",
            () -> new PlateShelfBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));
    public static final RegistryObject<Block> TAKEOUT_BAG = BLOCKS.register("takeout_bag",
            () -> new TakeoutBagBlock(BlockBehaviour.Properties.copy(Blocks.WHITE_WOOL).noOcclusion()));
    public static final RegistryObject<Block> FOOD_PLATE_DISPLAY = BLOCKS.register("food_plate_display",
            () -> new FoodPlateBlock(BlockBehaviour.Properties.copy(Blocks.WHITE_WOOL).noOcclusion()));
    public static final RegistryObject<Block> BOARD = BLOCKS.register("board",
            () -> new BoardBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).noOcclusion()));

    public static void registerModBlocks(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
