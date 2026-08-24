package cn.breezeth.ordertocook.block.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/** Item automation view used by Maid Wireless IO and other Forge automation. */
final class RefrigeratorAutomationItemHandler implements IItemHandler {
    private static final int UPPER_SIZE=27, TOTAL_SIZE=81, SLOT_LIMIT=128;
    private static final long HEAT_MERGE_WINDOW=5L*60L*20L;
    private static final String HOT_UNTIL="HotUntil";
    private static final String[] MERGE_IGNORED={HOT_UNTIL,"SkewerModelVariants","Creator","CreatorName","CreatorUuid"};
    private final RefrigeratorBlockEntity refrigerator;

    RefrigeratorAutomationItemHandler(RefrigeratorBlockEntity refrigerator){this.refrigerator=refrigerator;}
    @Override public int getSlots(){return TOTAL_SIZE;}
    @Override public @NotNull ItemStack getStackInSlot(int slot){validate(slot);return inventory(slot).get(localSlot(slot));}

    @Override public @NotNull ItemStack insertItem(int slot,@NotNull ItemStack stack,boolean simulate){
        validate(slot);if(stack.isEmpty()||!isItemValid(slot,stack))return stack;
        NonNullList<ItemStack> items=inventory(slot);int local=localSlot(slot);ItemStack stored=items.get(local);
        boolean exact=!stored.isEmpty()&&ItemStack.isSameItemSameTags(stored,stack);boolean heatCompatible=!stored.isEmpty()&&canMergeHeat(stored,stack);
        if(!stored.isEmpty()&&!exact&&!heatCompatible)return stack;
        int inserted=Math.min(SLOT_LIMIT-stored.getCount(),stack.getCount());if(inserted<=0)return stack;
        if(!simulate){if(stored.isEmpty())items.set(local,stack.copyWithCount(inserted));else{if(heatCompatible)mergeHeat(stored,stack,inserted);stored.grow(inserted);}refrigerator.setChanged();refrigerator.onAutomationAccess(slot<UPPER_SIZE);}
        return inserted==stack.getCount()?ItemStack.EMPTY:stack.copyWithCount(stack.getCount()-inserted);
    }

    @Override public @NotNull ItemStack extractItem(int slot,int amount,boolean simulate){
        validate(slot);if(amount<=0)return ItemStack.EMPTY;NonNullList<ItemStack> items=inventory(slot);int local=localSlot(slot);ItemStack stored=items.get(local);if(stored.isEmpty())return ItemStack.EMPTY;
        int extracted=Math.min(amount,stored.getCount());ItemStack result=stored.copyWithCount(extracted);
        if(!simulate){stored.shrink(extracted);if(stored.isEmpty())items.set(local,ItemStack.EMPTY);refrigerator.setChanged();refrigerator.onAutomationAccess(slot<UPPER_SIZE);}return result;
    }

    @Override public int getSlotLimit(int slot){validate(slot);return SLOT_LIMIT;}
    @Override public boolean isItemValid(int slot,@NotNull ItemStack stack){validate(slot);return RefrigeratorBlockEntity.isAllowedFoodStorage(stack);}
    private NonNullList<ItemStack> inventory(int slot){return refrigerator.automationItems(slot<UPPER_SIZE);}
    private static int localSlot(int slot){return slot<UPPER_SIZE?slot:slot-UPPER_SIZE;}
    private static void validate(int slot){if(slot<0||slot>=TOTAL_SIZE)throw new RuntimeException("Invalid refrigerator slot "+slot);}

    private boolean canMergeHeat(ItemStack first,ItemStack second){
        if(!isGrillingSkewer(first)||!isGrillingSkewer(second))return false;long now=refrigerator.getLevel()==null?0L:refrigerator.getLevel().getGameTime();long a=remainingHeat(first,now),b=remainingHeat(second,now);
        if((a>0L)!=(b>0L)||(a>0L&&Math.abs(a-b)>HEAT_MERGE_WINDOW))return false;ItemStack firstCopy=first.copy(),secondCopy=second.copy();removeMergeIgnoredData(firstCopy);removeMergeIgnoredData(secondCopy);return ItemStack.isSameItemSameTags(firstCopy,secondCopy);
    }
    private void mergeHeat(ItemStack target,ItemStack source,int inserted){long now=refrigerator.getLevel()==null?0L:refrigerator.getLevel().getGameTime();long averaged=(remainingHeat(target,now)*target.getCount()+remainingHeat(source,now)*inserted)/(target.getCount()+inserted);CompoundTag tag=customTag(target);if(averaged>0L)tag.putLong(HOT_UNTIL,now+averaged);else tag.remove(HOT_UNTIL);setTag(target,tag);}
    private static long remainingHeat(ItemStack stack,long now){return Math.max(0L,customTag(stack).getLong(HOT_UNTIL)-now);}
    private static boolean isGrillingSkewer(ItemStack stack){var id=BuiltInRegistries.ITEM.getKey(stack.getItem());return "kaleidoscope_grilling".equals(id.getNamespace())&&id.getPath().contains("skewer");}
    private static CompoundTag customTag(ItemStack stack){return stack.hasTag()?stack.getTag().copy():new CompoundTag();}
    private static void removeMergeIgnoredData(ItemStack stack){CompoundTag tag=customTag(stack);for(String key:MERGE_IGNORED)tag.remove(key);setTag(stack,tag);}
    private static void setTag(ItemStack stack,CompoundTag tag){stack.setTag(tag.isEmpty()?null:tag);}
}
