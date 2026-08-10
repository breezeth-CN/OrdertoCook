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
    public static final int SORT_BUTTON_ID = -1;
    private static final int PLAYER_BUTTON_COUNT = 36;
    private static final int MAX_POSITIVE_BUTTON_ID = 127;
    private static final int POSITIVE_REMOVE_BUTTONS = MAX_POSITIVE_BUTTON_ID - PLAYER_BUTTON_COUNT + 1;
    private static final int REMOVE_BUTTON_WRAP = 220;
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

    /**
     * Container button ids are signed bytes in Minecraft 1.20.1. Keep every id
     * in [-128, 127] while still reserving 0..35 for player inventory actions.
     */
    public static int removeButtonId(int templateIndex) {
        if (templateIndex < 0 || templateIndex >= 150) return SORT_BUTTON_ID;
        return templateIndex < POSITIVE_REMOVE_BUTTONS
                ? PLAYER_BUTTON_COUNT + templateIndex
                : templateIndex - REMOVE_BUTTON_WRAP;
    }

    private static int templateIndexFromButtonId(int id) {
        if (id >= PLAYER_BUTTON_COUNT && id <= MAX_POSITIVE_BUTTON_ID) {
            return id - PLAYER_BUTTON_COUNT;
        }
        if (id >= -128 && id <= -71) {
            return id + REMOVE_BUTTON_WRAP;
        }
        return -1;
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
        if (id == SORT_BUTTON_ID) {
            be.toggleSortMode();
            return true;
        }
        if (id >= 0 && id < 36) {
            ItemStack src = playerInv.getStack(id);
            be.tryAddTemplate(src);
            return true;
        }
        int idx = templateIndexFromButtonId(id);
        if (idx >= 0 && idx < templates.size()) {
            be.removeAt(idx);
            return true;
        }
        return false;
    }
}
