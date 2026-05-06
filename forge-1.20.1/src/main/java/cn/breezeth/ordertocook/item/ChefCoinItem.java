package cn.breezeth.ordertocook.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ChefCoinItem extends Item {
    private final int value;

    public ChefCoinItem(int value, Properties settings) {
        super(settings);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, world, tooltip, type);
        tooltip.add(Component.translatable("item.ordertocook.chef_coin.desc", value).withStyle(ChatFormatting.GOLD));
    }
}

