package cn.breezeth.ordertocook.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 可装备于头部的配送头盔。骑摩托且 Geo 身体替换时由 {@link cn.breezeth.ordertocook.client.gecko.RiderGeoRenderer} 在头上叠绘与车色一致的模型。
 */
public class HelmetItem extends ArmorItem {
    public HelmetItem(Item.Settings settings) {
        super(ArmorMaterials.LEATHER, Type.HELMET, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.ordertocook.helmet.tooltip"));
    }
}
