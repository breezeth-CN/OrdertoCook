package cn.breezeth.ordertocook.compat.jade;

import cn.breezeth.ordertocook.block.DishwasherBlock;
import cn.breezeth.ordertocook.block.PlateShelfBlock;
import cn.breezeth.ordertocook.block.RefrigeratorBlock;
import cn.breezeth.ordertocook.block.ShelfBlock;
import cn.breezeth.ordertocook.block.WashingTableBlock;
import cn.breezeth.ordertocook.block.entity.DishwasherBlockEntity;
import cn.breezeth.ordertocook.block.entity.RefrigeratorBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class OrderToCookJadePlugin implements IWailaPlugin {
    private static final Identifier PACKAGING_SHELF = id("packaging_shelf");
    private static final Identifier PLATE_SHELF = id("plate_shelf");
    private static final Identifier WASHING_TABLE = id("washing_table");
    private static final Identifier DISHWASHER = id("dishwasher");
    private static final Identifier REFRIGERATOR = id("refrigerator");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new StateCountProvider(PACKAGING_SHELF, StateCountProvider.Kind.LEATHER), ShelfBlock.class);
        registration.registerBlockComponent(new StateCountProvider(PLATE_SHELF, StateCountProvider.Kind.CLEAN_PLATE), PlateShelfBlock.class);
        registration.registerBlockComponent(new StateCountProvider(WASHING_TABLE, StateCountProvider.Kind.DIRTY_PLATE), WashingTableBlock.class);
        registration.registerBlockComponent(DishwasherProvider.INSTANCE, DishwasherBlock.class);
        registration.registerBlockComponent(RefrigeratorProvider.INSTANCE, RefrigeratorBlock.class);
    }

    private record StateCountProvider(Identifier uid, Kind kind) implements IBlockComponentProvider {
        enum Kind { LEATHER, CLEAN_PLATE, DIRTY_PLATE }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            BlockState state = accessor.getBlockState();
            int count;
            int max;
            String key;
            if (kind == Kind.LEATHER && state.contains(ShelfBlock.PAPERS)) {
                count = state.get(ShelfBlock.PAPERS);
                max = ShelfBlock.MAX_PAPERS;
                key = "jade.ordertocook.packaging_shelf.leather";
            } else if (kind == Kind.CLEAN_PLATE && state.contains(PlateShelfBlock.PLATES)) {
                count = state.get(PlateShelfBlock.PLATES);
                max = 18;
                key = "jade.ordertocook.plate_shelf.clean_plates";
            } else if (kind == Kind.DIRTY_PLATE && state.contains(WashingTableBlock.PLATES)) {
                count = state.get(WashingTableBlock.PLATES);
                if (count <= 0) return;
                max = 10;
                key = "jade.ordertocook.washing_table.dirty_plates";
            } else {
                return;
            }
            tooltip.add(Text.translatable(key, count, max));
        }

        @Override
        public Identifier getUid() {
            return uid;
        }
    }

    private enum DishwasherProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof DishwasherBlockEntity dishwasher)) return;
            if (dishwasher.canTakeCleanPlates()) {
                tooltip.add(Text.translatable("jade.ordertocook.dishwasher.ready", dishwasher.getStoredPlates(), 16));
            } else if (dishwasher.canInsertDirtyPlates()) {
                tooltip.add(Text.translatable("jade.ordertocook.dishwasher.waiting"));
            } else {
                tooltip.add(Text.translatable("jade.ordertocook.dishwasher.washing", dishwasher.getStoredPlates(), 16));
            }
        }

        @Override
        public Identifier getUid() {
            return DISHWASHER;
        }
    }

    private enum RefrigeratorProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            BlockState state = accessor.getBlockState();
            if (!state.contains(RefrigeratorBlock.HALF)) return;
            boolean upper = state.get(RefrigeratorBlock.HALF) == DoubleBlockHalf.UPPER;
            var blockEntity = accessor.getLevel().getBlockEntity(upper ? accessor.getPosition().down() : accessor.getPosition());
            if (!(blockEntity instanceof RefrigeratorBlockEntity refrigerator)) return;
            tooltip.add(Text.translatable("jade.ordertocook.refrigerator.storage",
                    refrigerator.getOccupiedSlotCount(upper), refrigerator.getStorageSlotCount(upper)));
        }

        @Override
        public Identifier getUid() {
            return REFRIGERATOR;
        }
    }

    private static Identifier id(String path) {
        return new Identifier("ordertocook", path);
    }
}
