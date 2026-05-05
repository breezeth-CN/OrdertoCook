package cn.breezeth.ordertocook.util;

import cn.breezeth.ordertocook.api.IDataAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class DataCompat implements IDataAccessor {
    public static final DataCompat INSTANCE = new DataCompat();

    private DataCompat() {}

    public static CompoundTag copy(ItemStack stack) {
        CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
        if (comp == null) return null;
        return comp.copyTag();
    }

    public static void set(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
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
