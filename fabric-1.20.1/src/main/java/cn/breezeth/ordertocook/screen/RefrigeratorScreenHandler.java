package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.block.entity.RefrigeratorBlockEntity;
import cn.breezeth.ordertocook.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

import java.util.List;

public class RefrigeratorScreenHandler extends ScreenHandler {
    public static final int MAX_STACK_SIZE = 128;
    public static final int SORT_BUTTON_ID = 0;

    private final Inventory inventory;
    private final PropertyDelegate counts;
    private final int rows;
    private final boolean clientView;

    public static RefrigeratorScreenHandler clientUpper(int syncId, PlayerInventory playerInventory) {
        return new RefrigeratorScreenHandler(ModScreenHandlers.REFRIGERATOR_UPPER, syncId, playerInventory, new MaxStackSimpleInventory(27), new ArrayPropertyDelegate(27), 3, true);
    }

    public static RefrigeratorScreenHandler clientLower(int syncId, PlayerInventory playerInventory) {
        return new RefrigeratorScreenHandler(ModScreenHandlers.REFRIGERATOR_LOWER, syncId, playerInventory, new MaxStackSimpleInventory(54), new ArrayPropertyDelegate(54), 6, true);
    }

    public static RefrigeratorScreenHandler upper(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate counts) {
        return new RefrigeratorScreenHandler(ModScreenHandlers.REFRIGERATOR_UPPER, syncId, playerInventory, inventory, counts, 3, false);
    }

    public static RefrigeratorScreenHandler lower(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate counts) {
        return new RefrigeratorScreenHandler(ModScreenHandlers.REFRIGERATOR_LOWER, syncId, playerInventory, inventory, counts, 6, false);
    }

    private RefrigeratorScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate counts, int rows, boolean clientView) {
        super(type, syncId);
        checkSize(inventory, rows * 9);
        checkDataCount(counts, rows * 9);
        this.inventory = inventory;
        this.counts = counts;
        this.rows = rows;
        this.clientView = clientView;
        inventory.onOpen(playerInventory.player);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new RefrigeratorSlot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        int playerInventoryY = 18 + rows * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInventoryY + row * 18));
            }
        }

        int hotbarY = playerInventoryY + 58;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }
        addProperties(counts);
    }

    @Override
    public void setStackInSlot(int slot, int revision, ItemStack stack) {
        super.setStackInSlot(slot, revision, normalizeClientStack(slot, stack));
    }

    @Override
    public void updateSlotStacks(int revision, List<ItemStack> stacks, ItemStack cursorStack) {
        for (int slot = 0; clientView && slot < Math.min(containerSlotCount(), stacks.size()); slot++) {
            normalizeClientStack(slot, stacks.get(slot));
        }
        super.updateSlotStacks(revision, stacks, cursorStack);
    }

    @Override
    public void setProperty(int id, int value) {
        super.setProperty(id, value);
        if (clientView && id >= 0 && id < containerSlotCount()) {
            normalizeClientStack(id, inventory.getStack(id));
        }
    }

    private ItemStack normalizeClientStack(int slot, ItemStack stack) {
        if (clientView && slot >= 0 && slot < containerSlotCount()
                && actualCount(slot) > 127 && stack.getCount() <= 0
                && stack.getItem() != net.minecraft.item.Items.AIR) {
            stack.setCount(127);
        }
        return stack;
    }

    public int rows() {
        return rows;
    }

    public int containerSlotCount() {
        return rows * 9;
    }

    public int actualCount(int slot) {
        return slot >= 0 && slot < counts.size() ? counts.get(slot) : 0;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id != SORT_BUTTON_ID || clientView) return false;
        compactRefrigerator();
        return true;
    }

    private void compactRefrigerator() {
        java.util.List<ItemStack> representatives = new java.util.ArrayList<>();
        java.util.List<Integer> totals = new java.util.ArrayList<>();
        for (int slot = 0; slot < containerSlotCount(); slot++) {
            ItemStack source = inventory.getStack(slot);
            if (source.isEmpty()) continue;
            int group = -1;
            for (int i = 0; i < representatives.size(); i++) {
                if (ItemStack.canCombine(representatives.get(i), source)) { group = i; break; }
            }
            if (group < 0) { representatives.add(source.copyWithCount(1)); totals.add(source.getCount()); }
            else totals.set(group, totals.get(group) + source.getCount());
        }
        java.util.List<ItemStack> compacted = new java.util.ArrayList<>();
        for (int group = 0; group < representatives.size(); group++) {
            int remaining = totals.get(group);
            while (remaining > 0) { int count = Math.min(remaining, MAX_STACK_SIZE); compacted.add(representatives.get(group).copyWithCount(count)); remaining -= count; }
        }
        for (int slot = 0; slot < containerSlotCount(); slot++) {
            inventory.setStack(slot, slot < compacted.size() ? compacted.get(slot) : ItemStack.EMPTY);
        }
        inventory.markDirty();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            int containerSlots = rows * 9;
            if (index < containerSlots) {
                if (!moveItemStackWithLimits(stack, containerSlots, slots.size(), true, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!RefrigeratorBlockEntity.isAllowedFoodStorage(stack)
                    || !moveItemStackWithLimits(stack, 0, containerSlots, false, true)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return result;
    }

    private boolean moveItemStackWithLimits(ItemStack stack, int startIndex, int endIndex, boolean fromLast, boolean refrigeratorTarget) {
        boolean moved = false;
        int index = fromLast ? endIndex - 1 : startIndex;

        while (!stack.isEmpty() && (fromLast ? index >= startIndex : index < endIndex)) {
            Slot target = slots.get(index);
            ItemStack existing = target.getStack();
            if (!existing.isEmpty() && target.canInsert(stack) && ItemStack.canCombine(existing, stack)) {
                int limit = refrigeratorTarget ? refrigeratorTransferLimit() : target.getMaxItemCount(stack);
                int transfer = Math.min(stack.getCount(), Math.max(0, limit - existing.getCount()));
                if (transfer > 0) {
                    existing.increment(transfer);
                    stack.decrement(transfer);
                    target.markDirty();
                    moved = true;
                }
            }
            index += fromLast ? -1 : 1;
        }

        index = fromLast ? endIndex - 1 : startIndex;
        while (!stack.isEmpty() && (fromLast ? index >= startIndex : index < endIndex)) {
            Slot target = slots.get(index);
            if (!target.hasStack() && target.canInsert(stack)) {
                int limit = refrigeratorTarget ? refrigeratorTransferLimit() : target.getMaxItemCount(stack);
                if (limit > 0) {
                    target.setStack(stack.split(Math.min(stack.getCount(), limit)));
                    target.markDirty();
                    moved = true;
                }
            }
            index += fromLast ? -1 : 1;
        }
        return moved;
    }

    private int refrigeratorTransferLimit() {
        // 1.20.1 serializes predicted ItemStack counts as a signed byte. Keep the
        // client prediction representable; the authoritative server still stores 128.
        return clientView ? 127 : MAX_STACK_SIZE;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
    }

    private static final class RefrigeratorSlot extends Slot {
        private RefrigeratorSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return RefrigeratorBlockEntity.isAllowedFoodStorage(stack);
        }

        @Override
        public int getMaxItemCount() {
            return MAX_STACK_SIZE;
        }

        @Override
        public int getMaxItemCount(ItemStack stack) {
            return MAX_STACK_SIZE;
        }
    }

    private static final class MaxStackSimpleInventory extends SimpleInventory {
        private MaxStackSimpleInventory(int size) {
            super(size);
        }

        @Override
        public int getMaxCountPerStack() {
            return MAX_STACK_SIZE;
        }
    }
}
