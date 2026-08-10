package cn.breezeth.ordertocook.item;

import cn.breezeth.ordertocook.block.PlateShelfBlock;
import cn.breezeth.ordertocook.block.ShelfBlock;
import cn.breezeth.ordertocook.util.DataCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PrefilledShelfBlockItem extends BlockItem {
    public static final String PREFILL_KEY = "OtcPrefill";
    public static final String PREFILL_PACKAGING = "packaging";
    public static final String PREFILL_PLATE = "plate";

    private final String prefillType;


    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level, java.util.List<Component> tooltip, TooltipFlag flag) {
        String key = PREFILL_PACKAGING.equals(prefillType) ? "item.ordertocook.shelf.desc" : "item.ordertocook.plate_shelf.desc";
        tooltip.add(Component.translatable(key));
    }

    public PrefilledShelfBlockItem(Block block, Properties properties, String prefillType) {
        super(block, properties);
        this.prefillType = prefillType;
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null || !hasPrefillMarker(context)) {
            return state;
        }
        if (PREFILL_PACKAGING.equals(prefillType) && state.hasProperty(ShelfBlock.PAPERS)) {
            return state.setValue(ShelfBlock.PAPERS, 1);
        }
        if (PREFILL_PLATE.equals(prefillType) && state.hasProperty(PlateShelfBlock.PLATES)) {
            return state.setValue(PlateShelfBlock.PLATES, 1);
        }
        return state;
    }

    private static boolean hasPrefillMarker(BlockPlaceContext context) {
        CompoundTag tag = DataCompat.copy(context.getItemInHand());
        return tag != null && tag.getBoolean(PREFILL_KEY);
    }
}
