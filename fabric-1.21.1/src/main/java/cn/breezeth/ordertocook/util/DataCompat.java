package cn.breezeth.ordertocook.util;

import cn.breezeth.ordertocook.api.IDataAccessor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public final class DataCompat implements IDataAccessor {
    public static final DataCompat INSTANCE = new DataCompat();

    private DataCompat() {}

    public static NbtCompound copy(ItemStack stack) {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return null;
        return comp.copyNbt();
    }

    public static void set(ItemStack stack, NbtCompound tag) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
    }

    @Override
    public Object getData(Object stack) {
        if (stack instanceof ItemStack s) return copy(s);
        return null;
    }

    @Override
    public void setData(Object stack, Object nbt) {
        if (stack instanceof ItemStack s && nbt instanceof NbtCompound n) set(s, n);
    }
}
