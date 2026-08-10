package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.block.entity.RefrigeratorBlockEntity;
import cn.breezeth.ordertocook.registry.ModScreenHandlers;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RefrigeratorScreenHandler extends AbstractContainerMenu {
    public static final int MAX_STACK_SIZE = 128;
    public static final int SORT_BUTTON_ID = 0;

    private final Container inventory;
    private final ContainerData counts;
    private final int rows;

    public static RefrigeratorScreenHandler clientUpper(int syncId, Inventory playerInventory) {
        return new RefrigeratorScreenHandler(ModScreenHandlers.REFRIGERATOR_UPPER.get(), syncId, playerInventory, new MaxStackSimpleContainer(27), new SimpleContainerData(27), 3);
    }

    public static RefrigeratorScreenHandler clientLower(int syncId, Inventory playerInventory) {
        return new RefrigeratorScreenHandler(ModScreenHandlers.REFRIGERATOR_LOWER.get(), syncId, playerInventory, new MaxStackSimpleContainer(54), new SimpleContainerData(54), 6);
    }

    public static RefrigeratorScreenHandler upper(int syncId, Inventory playerInventory, Container inventory, ContainerData counts) {
        return new RefrigeratorScreenHandler(ModScreenHandlers.REFRIGERATOR_UPPER.get(), syncId, playerInventory, inventory, counts, 3);
    }

    public static RefrigeratorScreenHandler lower(int syncId, Inventory playerInventory, Container inventory, ContainerData counts) {
        return new RefrigeratorScreenHandler(ModScreenHandlers.REFRIGERATOR_LOWER.get(), syncId, playerInventory, inventory, counts, 6);
    }

    private RefrigeratorScreenHandler(MenuType<?> type, int syncId, Inventory playerInventory, Container inventory, ContainerData counts, int rows) {
        super(type, syncId);
        checkContainerSize(inventory, rows * 9);
        checkContainerDataCount(counts, rows * 9);
        this.inventory = inventory;
        this.counts = counts;
        this.rows = rows;
        inventory.startOpen(playerInventory.player);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new RefrigeratorSlot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        int playerInventoryY = 18 + rows * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInventoryY + row * 18));
            }
        }

        int hotbarY = playerInventoryY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }
        this.addDataSlots(counts);
    }

    public int rows() {
        return rows;
    }

    public int containerSlotCount() {
        return rows * 9;
    }

    public int actualCount(int slot) {
        return slot >= 0 && slot < counts.getCount() ? counts.get(slot) : 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != SORT_BUTTON_ID || inventory instanceof MaxStackSimpleContainer) return false;
        compactRefrigerator();
        return true;
    }

    private void compactRefrigerator() {
        java.util.List<ItemStack> representatives = new java.util.ArrayList<>();
        java.util.List<Integer> totals = new java.util.ArrayList<>();
        for (int slot = 0; slot < containerSlotCount(); slot++) {
            ItemStack source = inventory.getItem(slot);
            if (source.isEmpty()) continue;
            int group = -1;
            for (int i = 0; i < representatives.size(); i++) {
                if (ItemStack.isSameItemSameComponents(representatives.get(i), source)) { group = i; break; }
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
            inventory.setItem(slot, slot < compacted.size() ? compacted.get(slot) : ItemStack.EMPTY);
        }
        inventory.setChanged();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            int containerSlots = rows * 9;
            if (invSlot < containerSlots) {
                if (!this.moveItemStackWithLimits(originalStack, containerSlots, this.slots.size(), true, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!RefrigeratorBlockEntity.isAllowedFoodStorage(originalStack)
                    || !this.moveItemStackWithLimits(originalStack, 0, containerSlots, false, true)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return newStack;
    }

    private boolean moveItemStackWithLimits(ItemStack stack, int startIndex, int endIndex, boolean fromLast, boolean refrigeratorTarget) {
        boolean moved = false;
        int index = fromLast ? endIndex - 1 : startIndex;

        while (!stack.isEmpty() && (fromLast ? index >= startIndex : index < endIndex)) {
            Slot target = this.slots.get(index);
            ItemStack existing = target.getItem();
            if (!existing.isEmpty() && target.mayPlace(stack) && ItemStack.isSameItemSameComponents(existing, stack)) {
                int limit = refrigeratorTarget ? MAX_STACK_SIZE : target.getMaxStackSize(stack);
                int transfer = Math.min(stack.getCount(), Math.max(0, limit - existing.getCount()));
                if (transfer > 0) {
                    existing.grow(transfer);
                    stack.shrink(transfer);
                    target.setChanged();
                    moved = true;
                }
            }
            index += fromLast ? -1 : 1;
        }

        index = fromLast ? endIndex - 1 : startIndex;
        while (!stack.isEmpty() && (fromLast ? index >= startIndex : index < endIndex)) {
            Slot target = this.slots.get(index);
            if (!target.hasItem() && target.mayPlace(stack)) {
                int limit = refrigeratorTarget ? MAX_STACK_SIZE : target.getMaxStackSize(stack);
                if (limit > 0) {
                    target.set(stack.split(Math.min(stack.getCount(), limit)));
                    target.setChanged();
                    moved = true;
                }
            }
            index += fromLast ? -1 : 1;
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        inventory.stopOpen(player);
    }

    private static final class RefrigeratorSlot extends Slot {
        private RefrigeratorSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return RefrigeratorBlockEntity.isAllowedFoodStorage(stack);
        }

        @Override
        public int getMaxStackSize() {
            return MAX_STACK_SIZE;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return MAX_STACK_SIZE;
        }
    }

    private static final class MaxStackSimpleContainer extends SimpleContainer {
        private MaxStackSimpleContainer(int size) {
            super(size);
        }

        @Override
        public int getMaxStackSize() {
            return MAX_STACK_SIZE;
        }
    }
}
