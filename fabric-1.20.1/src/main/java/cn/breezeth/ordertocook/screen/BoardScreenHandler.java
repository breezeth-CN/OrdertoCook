package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.block.entity.BoardBlockEntity;
import cn.breezeth.ordertocook.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.slot.Slot;

public class BoardScreenHandler extends ScreenHandler {
    private final Inventory templates;
    private final PlayerInventory playerInv;
    private final PropertyDelegate delegate;

    public BoardScreenHandler(int syncId, PlayerInventory playerInventory, Inventory templates) {
        super(ModScreenHandlers.BOARD_SCREEN_HANDLER, syncId);
        this.templates = templates;
        this.playerInv = playerInventory;
        if (templates instanceof BoardBlockEntity be) {
            this.delegate = new PropertyDelegate() {
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
                public int size() {
                    return 1;
                }
            };
        } else {
            this.delegate = new ArrayPropertyDelegate(1);
        }
        addProperties(this.delegate);
        for (int i = 0; i < templates.size(); i++) {
            this.addSlot(new Slot(templates, i, -1000, -1000));
        }
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public BoardScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new net.minecraft.inventory.SimpleInventory(150));
    }

    public Inventory getTemplates() {
        return templates;
    }

    public int getSortMode() {
        return delegate.get(0);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + (row + 1) * 9, 8 + col * 18, 140 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(PlayerInventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(templates instanceof BoardBlockEntity be)) return false;
        if (id == 1000) {
            be.toggleSortMode();
            return true;
        }
        if (id >= 0 && id < 36) {
            ItemStack src = playerInv.getStack(id);
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
