package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.registry.ModScreenHandlers;
import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.util.DataCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import cn.breezeth.ordertocook.block.entity.TakeoutBoxBlockEntity;

public class TakeoutBoxScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    private static boolean isExpiredOrder(net.minecraft.world.level.Level world, ItemStack stack) {
        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt == null) return true;
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
            return world.getGameTime() >= nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
        }
        if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
            return System.currentTimeMillis() >= nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
        }
        return true;
    }

    public TakeoutBoxScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(13), new SimpleContainerData(2));
    }

    public TakeoutBoxScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData delegate) {
        super(ModScreenHandlers.COUNTERTOP_SCREEN_HANDLER.get(), syncId);
        checkContainerSize(inventory, 13);
        this.inventory = inventory;
        this.propertyDelegate = delegate;
        inventory.startOpen(playerInventory.player);

        this.addSlot(new Slot(inventory, 0, 26, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.ORDER.get()) && !isExpiredOrder(playerInventory.player.level(), stack);
            }
        });
        
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 4; ++col) {
                this.addSlot(new Slot(inventory, 1 + col + row * 4, 62 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return !stack.is(ModItems.ORDER.get());
                    }
                });
            }
        }

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        addDataSlots(delegate);
    }

    public int getAvailablePackagingCount() {
        if (this.propertyDelegate.getCount() <= 0) return 0;
        return this.propertyDelegate.get(0);
    }

    public int getAvailablePlateCount() {
        if (this.propertyDelegate.getCount() <= 1) return 0;
        return this.propertyDelegate.get(1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (invSlot < 13) {
                if (!this.moveItemStackTo(originalStack, 13, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(originalStack, 0, 13, false)) {
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

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
    
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) { // Pack button
             if (player.level().isClientSide) return true;
             if (inventory instanceof TakeoutBoxBlockEntity be) {
                 be.tryPackOrder(player);
                 be.refreshAvailableCounts();
             }
             return true;
        }
        if (id == 1) {
            if (player.level().isClientSide) return true;
            if (inventory instanceof TakeoutBoxBlockEntity be) {
                be.tryPlateOrder(player);
                be.refreshAvailableCounts();
            }
            return true;
        }
        return false;
    }
}
