package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.block.entity.StoveBlockEntity;
import cn.breezeth.ordertocook.registry.ModScreenHandlers;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;

public class StoveScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData data;

    public StoveScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(StoveBlockEntity.SLOT_COUNT), new SimpleContainerData(StoveBlockEntity.DATA_COUNT));
    }

    public StoveScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData data) {
        super(ModScreenHandlers.STOVE.get(), syncId);
        checkContainerSize(inventory, StoveBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, StoveBlockEntity.DATA_COUNT);
        this.inventory = inventory;
        this.data = data;

        this.addSlot(new StoveInputSlot(inventory, 0, 38, 17));
        this.addSlot(new StoveInputSlot(inventory, 1, 56, 17));
        this.addSlot(new StoveInputSlot(inventory, 2, 74, 17));
        this.addSlot(new FuelSlot(inventory, 3, 56, 53));
        this.addSlot(new OutputSlot(inventory, 4, 116, 35));
        this.addSlot(new OutputSlot(inventory, 5, 134, 35));
        this.addSlot(new OutputSlot(inventory, 6, 152, 35));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
        this.addDataSlots(data);
    }

    public int litProgress() {
        int litTime = data.get(0);
        int litDuration = data.get(1);
        return litDuration > 0 && litTime > 0 ? litTime * 13 / litDuration : 0;
    }

    public int cookProgress(int lane) {
        int progress = data.get(2);
        int total = data.get(3);
        return total > 0 && progress > 0 ? progress * 24 / total : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < StoveBlockEntity.SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, StoveBlockEntity.SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (ForgeHooks.getBurnTime(stack, RecipeType.SMOKING) > 0) {
                if (!this.moveItemStackTo(stack, 3, 4, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, 3, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    private static final class StoveInputSlot extends Slot {
        private StoveInputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }
    }

    private static final class FuelSlot extends Slot {
        private FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ForgeHooks.getBurnTime(stack, RecipeType.SMOKING) > 0;
        }
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
