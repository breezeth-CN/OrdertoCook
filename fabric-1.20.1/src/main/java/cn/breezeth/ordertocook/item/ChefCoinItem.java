package cn.breezeth.ordertocook.item;

import net.minecraft.item.Item;

public class ChefCoinItem extends Item {
    private final int value;

    public ChefCoinItem(int value, Settings settings) {
        super(settings);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    // Tooltip moved out for 1.20.1 server compile compatibility
}
