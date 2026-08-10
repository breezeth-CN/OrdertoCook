package cn.breezeth.ordertocook.item;

import cn.breezeth.ordertocook.block.PlateShelfBlock;
import cn.breezeth.ordertocook.block.ShelfBlock;
import cn.breezeth.ordertocook.util.DataCompat;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.List;

public class PrefilledShelfBlockItem extends BlockItem {
    public static final String PREFILL_KEY = "OtcPrefill";
    public static final String PREFILL_PACKAGING = "packaging";
    public static final String PREFILL_PLATE = "plate";

    private final String prefillType;

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        String key = PREFILL_PACKAGING.equals(prefillType) ? "item.ordertocook.shelf.desc" : "item.ordertocook.plate_shelf.desc";
        tooltip.add(Text.translatable(key));
    }

    public PrefilledShelfBlockItem(Block block, Settings settings, String prefillType) {
        super(block, settings);
        this.prefillType = prefillType;
    }

    @Override
    protected BlockState getPlacementState(ItemPlacementContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null || !hasPrefillMarker(context.getStack())) {
            return state;
        }
        if (PREFILL_PACKAGING.equals(prefillType) && state.contains(ShelfBlock.PAPERS)) {
            return state.with(ShelfBlock.PAPERS, 1);
        }
        if (PREFILL_PLATE.equals(prefillType) && state.contains(PlateShelfBlock.PLATES)) {
            return state.with(PlateShelfBlock.PLATES, 1);
        }
        return state;
    }

    private static boolean hasPrefillMarker(ItemStack stack) {
        NbtCompound tag = DataCompat.copy(stack);
        return tag != null && tag.getBoolean(PREFILL_KEY);
    }
}
