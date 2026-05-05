package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.block.entity.BoardBlockEntity;
import cn.breezeth.ordertocook.block.entity.FoodPlateBlockEntity;
import cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity;
import cn.breezeth.ordertocook.block.entity.TakeoutBagBlockEntity;
import cn.breezeth.ordertocook.block.entity.TakeoutBoxBlockEntity;
import cn.breezeth.ordertocook.block.entity.WashingTableBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ModConstants.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OrderMachineBlockEntity>> ORDER_MACHINE = BLOCK_ENTITIES.register("order_machine",
            () -> BlockEntityType.Builder.of(OrderMachineBlockEntity::new, ModBlocks.ORDER_MACHINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TakeoutBoxBlockEntity>> COUNTERTOP = BLOCK_ENTITIES.register("countertop",
            () -> BlockEntityType.Builder.of(TakeoutBoxBlockEntity::new, ModBlocks.COUNTERTOP.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TakeoutBagBlockEntity>> TAKEOUT_BAG = BLOCK_ENTITIES.register("takeout_bag",
            () -> BlockEntityType.Builder.of(TakeoutBagBlockEntity::new, ModBlocks.TAKEOUT_BAG.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoodPlateBlockEntity>> FOOD_PLATE_DISPLAY = BLOCK_ENTITIES.register("food_plate_display",
            () -> BlockEntityType.Builder.of(FoodPlateBlockEntity::new, ModBlocks.FOOD_PLATE_DISPLAY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoardBlockEntity>> BOARD = BLOCK_ENTITIES.register("board",
            () -> BlockEntityType.Builder.of(BoardBlockEntity::new, ModBlocks.BOARD.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WashingTableBlockEntity>> WASHING_TABLE = BLOCK_ENTITIES.register("washing_table",
            () -> BlockEntityType.Builder.of(WashingTableBlockEntity::new, ModBlocks.WASHINGTABLE.get()).build(null));

    public static void registerBlockEntities(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
