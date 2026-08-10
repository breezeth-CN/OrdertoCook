package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.block.entity.StoveBlockEntity;
import cn.breezeth.ordertocook.registry.ModScreenHandlers;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class StoveScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate data;

    public StoveScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(StoveBlockEntity.SLOT_COUNT), new ArrayPropertyDelegate(StoveBlockEntity.DATA_COUNT));
    }

    public StoveScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate data) {
        super(ModScreenHandlers.STOVE, syncId);
        checkSize(inventory, StoveBlockEntity.SLOT_COUNT);
        checkDataCount(data, StoveBlockEntity.DATA_COUNT);
        this.inventory = inventory;
        this.data = data;

        addSlot(new StoveInputSlot(inventory, 0, 38, 17));
        addSlot(new StoveInputSlot(inventory, 1, 56, 17));
        addSlot(new StoveInputSlot(inventory, 2, 74, 17));
        addSlot(new FuelSlot(inventory, 3, 56, 53));
        addSlot(new OutputSlot(inventory, 4, 116, 35));
        addSlot(new OutputSlot(inventory, 5, 134, 35));
        addSlot(new OutputSlot(inventory, 6, 152, 35));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
        addProperties(data);
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
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index < StoveBlockEntity.SLOT_COUNT) {
                if (!insertItem(stack, StoveBlockEntity.SLOT_COUNT, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (isFuel(stack)) {
                if (!insertItem(stack, 3, 4, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!insertItem(stack, 0, 3, false)) {
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

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    private static boolean isFuel(ItemStack stack) {
        Integer burnTime = FuelRegistry.INSTANCE.get(stack.getItem());
        return burnTime != null && burnTime > 0;
    }

    private static final class StoveInputSlot extends Slot {
        private StoveInputSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }
    }

    private static final class FuelSlot extends Slot {
        private FuelSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return isFuel(stack);
        }
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }
    }
}
