package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.block.entity.BoardBlockEntity;
import cn.breezeth.ordertocook.registry.ModScreenHandlers;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BoardScreenHandler extends AbstractContainerMenu {
    private final Container templates;
    private final Inventory playerInv;
    private final ContainerData delegate;

    public BoardScreenHandler(int syncId, Inventory playerInventory, Container templates) {
        super(ModScreenHandlers.BOARD_SCREEN_HANDLER.get(), syncId);
        this.templates = templates;
        this.playerInv = playerInventory;
        if (templates instanceof BoardBlockEntity be) {
            this.delegate = new ContainerData() {
                @Override
                public int get(int index) {
                    if (index == 0) return be.getSortMode();
                    return 0;
                }

                @Override
                public void set(int index, int value) {
                    if (index == 0) be.setSortMode(value);
                }

                @Override
                public int getCount() {
                    return 1;
                }
            };
        } else {
            this.delegate = new SimpleContainerData(1);
        }
        addDataSlots(this.delegate);
        for (int i = 0; i < templates.getContainerSize(); i++) {
            this.addSlot(new Slot(templates, i, -1000, -1000));
        }
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public BoardScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new net.minecraft.world.SimpleContainer(150));
    }

    public Container getTemplates() {
        return templates;
    }

    public int getSortMode() {
        return delegate.get(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + (row + 1) * 9, 8 + col * 18, 140 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(templates instanceof BoardBlockEntity be)) return false;
        if (id == 1000) {
            be.toggleSortMode();
            return true;
        }
        if (id >= 0 && id < 36) {
            ItemStack src = playerInv.getItem(id);
            be.tryAddTemplate(src);
            return true;
        }
        if (id >= 100) {
            int idx = id - 100;
            be.removeAt(idx);
            return true;
        }
        return false;
    }
}
