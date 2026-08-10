package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.registry.ModScreenHandlers;
import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.util.DataCompat;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import cn.breezeth.ordertocook.block.entity.TakeoutBoxBlockEntity;

public class TakeoutBoxScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    private final PlayerEntity player;
    private String lastOrderSignature;

    private static boolean isExpiredOrder(net.minecraft.world.World world, ItemStack stack) {
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null) return true;
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
            return world.getTime() >= nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
        }
        if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
            return System.currentTimeMillis() >= nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
        }
        return true;
    }

    public TakeoutBoxScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(13), new ArrayPropertyDelegate(22));
    }

    public TakeoutBoxScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate delegate) {
        super(ModScreenHandlers.COUNTERTOP_SCREEN_HANDLER, syncId);
        checkSize(inventory, 13);
        this.inventory = inventory;
        this.propertyDelegate = delegate;
        this.player = playerInventory.player;
        inventory.onOpen(playerInventory.player);

        this.addSlot(new Slot(inventory, 0, 26, 20) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.ORDER) && !isExpiredOrder(playerInventory.player.getWorld(), stack);
            }
        });
        
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 4; ++col) {
                this.addSlot(new Slot(inventory, 1 + col + row * 4, 62 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return !stack.isOf(ModItems.ORDER);
                    }
                });
            }
        }

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        addProperties(delegate);
        this.lastOrderSignature = getOrderSignature();
    }

    public int getAvailablePackagingCount() {
        if (this.propertyDelegate.size() <= 0) return 0;
        return this.propertyDelegate.get(0);
    }

    public int getAvailablePlateCount() {
        if (this.propertyDelegate.size() <= 1) return 0;
        return this.propertyDelegate.get(1);
    }

    public int getIngredientStatus() {
        return this.propertyDelegate.size() > 2 ? this.propertyDelegate.get(2) : 0;
    }

    public int getMissingIngredientCount() {
        return this.propertyDelegate.size() > 3 ? Math.min(6, this.propertyDelegate.get(3)) : 0;
    }

    public int getMissingIngredientId(int index) { return this.propertyDelegate.get(4 + index * 3); }
    public int getMissingIngredientRequired(int index) { return this.propertyDelegate.get(5 + index * 3); }
    public int getMissingIngredientAvailable(int index) { return this.propertyDelegate.get(6 + index * 3); }

    public boolean isQueryButtonVisible(net.minecraft.world.World world) {
        ItemStack stack = inventory.getStack(0);
        return !stack.isEmpty() && stack.isOf(ModItems.ORDER) && !isExpiredOrder(world, stack);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < 13) {
                if (!this.insertItem(originalStack, 13, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, 13, false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
    
    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == 0) {
            if (player.getWorld().isClient) return true;
            if (inventory instanceof TakeoutBoxBlockEntity be) {
                cn.breezeth.ordertocook.api.CountertopAutomationApi.execute(player.getWorld(), be.getPos(), cn.breezeth.ordertocook.api.CountertopAutomationApi.Action.PACK, player, false);
                be.refreshAvailableCounts();
            }
            return true;
        }
        if (id == 1) {
            if (player.getWorld().isClient) return true;
            if (inventory instanceof TakeoutBoxBlockEntity be) {
                cn.breezeth.ordertocook.api.CountertopAutomationApi.execute(player.getWorld(), be.getPos(), cn.breezeth.ordertocook.api.CountertopAutomationApi.Action.PLATE, player, false);
                be.refreshAvailableCounts();
            }
            return true;
        }
        if (id == 2) {
            if (!player.getWorld().isClient && inventory instanceof TakeoutBoxBlockEntity be) {
                be.refreshIngredientStatus(player, true);
            }
            return true;
        }
        return false;
    }

    @Override
    public void sendContentUpdates() {
        String signature = getOrderSignature();
        if (!signature.equals(lastOrderSignature)) {
            lastOrderSignature = signature;
            if (!player.getWorld().isClient && inventory instanceof TakeoutBoxBlockEntity be) {
                be.refreshIngredientStatus(player, false);
            }
        }
        super.sendContentUpdates();
    }

    private String getOrderSignature() {
        ItemStack stack = inventory.getStack(0);
        if (stack.isEmpty() || !stack.isOf(ModItems.ORDER)) return "";
        NbtCompound nbt = DataCompat.copy(stack);
        return nbt == null ? "invalid" : nbt.toString();
    }
}
