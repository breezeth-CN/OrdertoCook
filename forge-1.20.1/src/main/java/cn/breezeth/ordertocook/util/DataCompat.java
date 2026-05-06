package cn.breezeth.ordertocook.util;

import cn.breezeth.ordertocook.api.IDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class DataCompat implements IDataAccessor {
    public static final DataCompat INSTANCE = new DataCompat();

    private DataCompat() {}

    public static CompoundTag copy(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? null : tag.copy();
    }

    public static void set(ItemStack stack, CompoundTag tag) {
        stack.setTag(tag);
    }

    @Override
    public Object getData(Object stack) {
        if (stack instanceof ItemStack s) return copy(s);
        return null;
    }

    @Override
    public void setData(Object stack, Object nbt) {
        if (stack instanceof ItemStack s && nbt instanceof CompoundTag n) set(s, n);
    }
}
