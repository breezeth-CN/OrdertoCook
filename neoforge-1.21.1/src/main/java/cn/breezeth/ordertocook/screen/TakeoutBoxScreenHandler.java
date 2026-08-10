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
    private final Player player;
    private String lastOrderSignature;

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
        this(syncId, playerInventory, new SimpleContainer(13), new SimpleContainerData(22));
    }

    public TakeoutBoxScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData delegate) {
        super(ModScreenHandlers.COUNTERTOP_SCREEN_HANDLER.get(), syncId);
        checkContainerSize(inventory, 13);
        this.inventory = inventory;
        this.propertyDelegate = delegate;
        this.player = playerInventory.player;
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
        this.lastOrderSignature = getOrderSignature();
    }

    public int getAvailablePackagingCount() {
        if (this.propertyDelegate.getCount() <= 0) return 0;
        return this.propertyDelegate.get(0);
    }

    public int getAvailablePlateCount() {
        if (this.propertyDelegate.getCount() <= 1) return 0;
        return this.propertyDelegate.get(1);
    }

    public int getIngredientStatus() {
        return this.propertyDelegate.getCount() > 2 ? this.propertyDelegate.get(2) : 0;
    }

    public int getMissingIngredientCount() {
        return this.propertyDelegate.getCount() > 3 ? Math.min(6, this.propertyDelegate.get(3)) : 0;
    }

    public int getMissingIngredientId(int index) { return this.propertyDelegate.get(4 + index * 3); }
    public int getMissingIngredientRequired(int index) { return this.propertyDelegate.get(5 + index * 3); }
    public int getMissingIngredientAvailable(int index) { return this.propertyDelegate.get(6 + index * 3); }

    public boolean isQueryButtonVisible(net.minecraft.world.level.Level world) {
        ItemStack stack = inventory.getItem(0);
        return !stack.isEmpty() && stack.is(ModItems.ORDER.get()) && !isExpiredOrder(world, stack);
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
                 cn.breezeth.ordertocook.api.CountertopAutomationApi.execute(player.level(), be.getBlockPos(), cn.breezeth.ordertocook.api.CountertopAutomationApi.Action.PACK, player, false);
                 be.refreshAvailableCounts();
             }
             return true;
        }
        if (id == 1) {
            if (player.level().isClientSide) return true;
            if (inventory instanceof TakeoutBoxBlockEntity be) {
                cn.breezeth.ordertocook.api.CountertopAutomationApi.execute(player.level(), be.getBlockPos(), cn.breezeth.ordertocook.api.CountertopAutomationApi.Action.PLATE, player, false);
                be.refreshAvailableCounts();
            }
            return true;
        }
        if (id == 2) {
            if (!player.level().isClientSide && inventory instanceof TakeoutBoxBlockEntity be) {
                be.refreshIngredientStatus(player, true);
            }
            return true;
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        String signature = getOrderSignature();
        if (!signature.equals(lastOrderSignature)) {
            lastOrderSignature = signature;
            if (!player.level().isClientSide && inventory instanceof TakeoutBoxBlockEntity be) {
                be.refreshIngredientStatus(player, false);
            }
        }
        super.broadcastChanges();
    }

    private String getOrderSignature() {
        ItemStack stack = inventory.getItem(0);
        if (stack.isEmpty() || !stack.is(ModItems.ORDER.get())) return "";
        CompoundTag nbt = DataCompat.copy(stack);
        return nbt == null ? "invalid" : nbt.toString();
    }
}
