package cn.breezeth.ordertocook.registry;

import cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity;
import cn.breezeth.ordertocook.block.entity.DishwasherBlockEntity;
import cn.breezeth.ordertocook.block.entity.FoodPlateBlockEntity;
import cn.breezeth.ordertocook.block.entity.RefrigeratorBlockEntity;
import cn.breezeth.ordertocook.block.entity.StoveBlockEntity;
import cn.breezeth.ordertocook.block.entity.TakeoutBagBlockEntity;
import cn.breezeth.ordertocook.block.entity.BoardBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import cn.breezeth.ordertocook.block.entity.TakeoutBoxBlockEntity;
import cn.breezeth.ordertocook.block.entity.WashingTableBlockEntity;

public class ModBlockEntities {
    public static final BlockEntityType<OrderMachineBlockEntity> ORDER_MACHINE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "order_machine"),
            BlockEntityType.Builder.create(OrderMachineBlockEntity::new, ModBlocks.ORDER_MACHINE).build(null)
    );

    public static final BlockEntityType<TakeoutBoxBlockEntity> COUNTERTOP = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "countertop"),
            BlockEntityType.Builder.create(TakeoutBoxBlockEntity::new, ModBlocks.COUNTERTOP).build(null)
    );

    public static final BlockEntityType<TakeoutBagBlockEntity> TAKEOUT_BAG = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "takeout_bag"),
            BlockEntityType.Builder.create(TakeoutBagBlockEntity::new, ModBlocks.TAKEOUT_BAG).build(null)
    );

    public static final BlockEntityType<FoodPlateBlockEntity> FOOD_PLATE_DISPLAY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "food_plate_display"),
            BlockEntityType.Builder.create(FoodPlateBlockEntity::new, ModBlocks.FOOD_PLATE_DISPLAY).build(null)
    );

    public static final BlockEntityType<BoardBlockEntity> BOARD = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "board"),
            BlockEntityType.Builder.create(BoardBlockEntity::new, ModBlocks.BOARD).build(null)
    );

    public static final BlockEntityType<WashingTableBlockEntity> WASHING_TABLE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "washing_table"),
            BlockEntityType.Builder.create(WashingTableBlockEntity::new, ModBlocks.WASHINGTABLE).build(null)
    );

    public static final BlockEntityType<DishwasherBlockEntity> DISHWASHER = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "dishwasher"),
            BlockEntityType.Builder.create(DishwasherBlockEntity::new, ModBlocks.DISHWASHER).build(null)
    );

    public static final BlockEntityType<RefrigeratorBlockEntity> REFRIGERATOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "refrigerator"),
            BlockEntityType.Builder.create(RefrigeratorBlockEntity::new, ModBlocks.REFRIGERATOR).build(null)
    );

    public static final BlockEntityType<StoveBlockEntity> STOVE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "stove"),
            BlockEntityType.Builder.create(StoveBlockEntity::new, ModBlocks.STOVE).build(null)
    );

    public static void registerBlockEntities() {
        // Init
    }
}
