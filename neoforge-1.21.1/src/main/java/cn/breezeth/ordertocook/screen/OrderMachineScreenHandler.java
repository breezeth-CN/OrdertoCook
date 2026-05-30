package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity;
import cn.breezeth.ordertocook.registry.ModScreenHandlers;
import cn.breezeth.ordertocook.util.DataCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import cn.breezeth.ordertocook.core.ModConstants;

public class OrderMachineScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public OrderMachineScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, null);
    }

    public OrderMachineScreenHandler(int syncId, Inventory playerInventory, Object data) {
        this(syncId, playerInventory, new SimpleContainer(5), new SimpleContainerData(5));
    }

    public OrderMachineScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData delegate) {
        super(ModScreenHandlers.ORDER_MACHINE_SCREEN_HANDLER.get(), syncId);
        checkContainerSize(inventory, 5);
        this.inventory = inventory;
        inventory.startOpen(playerInventory.player);
        this.propertyDelegate = delegate;

        // Five slots on the same row: columns 1,3,5,7,9
        this.addSlot(new OrderSlot(inventory, 0, 8, 35));    // col 1
        this.addSlot(new OrderSlot(inventory, 1, 44, 35));   // col 3
        this.addSlot(new OrderSlot(inventory, 2, 80, 35));   // col 5
        this.addSlot(new OrderSlot(inventory, 3, 116, 35));  // col 7
        this.addSlot(new OrderSlot(inventory, 4, 152, 35));  // col 9

        // Player Inventory
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        addDataSlots(delegate);
    }

    public int getProgress() {
        return propertyDelegate.get(0);
    }

    public int getLevel() {
        return propertyDelegate.get(1);
    }

    public int getBenefitLevel() {
        try {
            return propertyDelegate.get(3);
        } catch (Throwable t) {
            return getLevel();
        }
    }

    public boolean isActive() {
        return propertyDelegate.get(2) != 0;
    }

    public int getCooldownSeconds() {
        try {
            return Math.max(1, propertyDelegate.get(4));
        } catch (Throwable t) {
            return 600;
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 1 && inventory instanceof OrderMachineBlockEntity be) {
            return be.tryUpgrade(player);
        }
        if (id == 2 && inventory instanceof OrderMachineBlockEntity be) {
            return be.tryToggleActive(player);
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (invSlot < 5) { // One of our order slots
                // Trigger the onTakeItem logic WITH the full stack before it gets modified
                slot.onTake(player, newStack);
                // Insert the processed stack into player inventory
                if (!this.moveItemStackTo(newStack, 5, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                // Empty the source slot manually since we used newStack for insertion
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                // Player inventory -> Machine (Not allowed per requirement)
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

    private class OrderSlot extends Slot {
        public OrderSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false; // 只可拿取不可放入
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            // Clear other slots
            for (int i = 0; i < 5; i++) {
                if (i != this.getContainerSlot()) {
                    OrderMachineScreenHandler.this.inventory.setItem(i, ItemStack.EMPTY);
                }
            }
            if (OrderMachineScreenHandler.this.inventory instanceof OrderMachineBlockEntity be && !player.level().isClientSide) {
                be.onOrderAccepted();
                CompoundTag nbt = DataCompat.copy(stack);
                if (nbt != null) {
                    // 写入机器ID（用于排行榜归属，若缺失则分配）
                    int idToUse = be.getMachineId();
                    if (player.level() instanceof ServerLevel swEnsure) {
                        idToUse = be.ensureMachineId(swEnsure);
                    }
                    nbt.putInt(ModConstants.NBT_MACHINE_ID, idToUse);
                    // 兼容保留：机器坐标与维度（不参与排行逻辑）
                    nbt.putLong(ModConstants.NBT_MACHINE_POS, be.getBlockPos().asLong());
                    if (player.level() instanceof ServerLevel sw2) {
                        nbt.putString(ModConstants.NBT_MACHINE_DIM, sw2.dimension().location().toString());
                    }
                    // 若为外卖订单且尚未包含配送距离，则计算并写入
                    if (nbt.getBoolean(ModConstants.NBT_DELIVERY) && nbt.contains(ModConstants.NBT_DELIVERY_POS) && !nbt.contains(ModConstants.NBT_DELIVERY_DIST)) {
                        CompoundTag dp = nbt.getCompound(ModConstants.NBT_DELIVERY_POS);
                        int tx = dp.getInt(ModConstants.NBT_X);
                        int tz = dp.getInt(ModConstants.NBT_Z);
                        int dx = tx - be.getBlockPos().getX();
                        int dz = tz - be.getBlockPos().getZ();
                        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
                        nbt.putInt(ModConstants.NBT_DELIVERY_DIST, dist);
                    }
                    if (!nbt.contains(ModConstants.NBT_ORDER_TYPE) || nbt.getInt(ModConstants.NBT_ORDER_TYPE) != 1) {
                        nbt.putInt(ModConstants.NBT_ORDER_TYPE, 0);
                    }
                    DataCompat.set(stack, nbt);
                    boolean delivery = nbt.getBoolean("Delivery");
                    String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
                    if (!delivery && player.level() instanceof ServerLevel sw) {
                        String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
                        long expiryTick = nbt.contains(ModConstants.NBT_EXPIRY_TICK) ? nbt.getLong(ModConstants.NBT_EXPIRY_TICK) : -1L;
                        long expirySys = nbt.contains(ModConstants.NBT_EXPIRY_TIME) ? nbt.getLong(ModConstants.NBT_EXPIRY_TIME) : -1L;
                        cn.breezeth.ordertocook.core.NormalOrderNpcManager.spawn(sw, player, be.getBlockPos(), orderId, customer, expiryTick, expirySys, nbt);
                    }
                    if (player instanceof ServerPlayer sp) {
                        be.recordAcceptedOrder(sp, nbt);
                    }
                }
            }
        }
    }

    public cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity getMachine() {
        return (this.inventory instanceof cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity be) ? be : null;
    }

    public cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity getMachineIfServer() {
        if (!(this.inventory instanceof cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity be)) return null;
        return (be.getLevel() instanceof net.minecraft.server.level.ServerLevel) ? be : null;
    }
}
