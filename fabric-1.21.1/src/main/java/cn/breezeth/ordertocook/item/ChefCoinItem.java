package cn.breezeth.ordertocook.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class ChefCoinItem extends Item {
    private final int value;

    public ChefCoinItem(int value, Settings settings) {
        super(settings);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public void appendTooltip(ItemStack stack, net.minecraft.item.Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("item.ordertocook.chef_coin.desc", value).formatted(Formatting.GOLD));
    }
}

