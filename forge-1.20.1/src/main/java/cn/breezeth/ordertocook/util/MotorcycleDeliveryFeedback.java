package cn.breezeth.ordertocook.util;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.item.TakeoutBagItem;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class MotorcycleDeliveryFeedback {
    private MotorcycleDeliveryFeedback() {
    }

    public static Component build(ServerPlayer player, MotorcycleEntity motorcycle) {
        SimpleContainer inventory = motorcycle.getCoolerInventory();
        MutableComponent result = Component.empty();
        boolean hasLine = false;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            DeliveryInfo info = getDeliveryInfo(player.level(), stack);
            if (info == null) {
                continue;
            }

            if (hasLine) {
                result.append(Component.literal("\n"));
            }
            result.append(Component.translatable(
                    "message.ordertocook.motorcycle_delivery_row",
                    getDistance(player.getX(), player.getZ(), info.targetX(), info.targetZ()),
                    getDirectionArrow(motorcycle, info.targetX(), info.targetZ())
            ).withStyle(ChatFormatting.GRAY));
            hasLine = true;
        }

        return hasLine ? result : null;
    }

    private static DeliveryInfo getDeliveryInfo(Level world, ItemStack stack) {
        if (!(stack.getItem() instanceof TakeoutBagItem)) {
            return null;
        }

        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt == null || !nbt.contains(ModConstants.NBT_DELIVERY_POS) || isExpired(world, nbt)) {
            return null;
        }

        CompoundTag pos = nbt.getCompound(ModConstants.NBT_DELIVERY_POS);
        if (!pos.contains("x") || !pos.contains("z")) {
            return null;
        }

        String customerName = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
        if (customerName == null || customerName.isBlank()) {
            customerName = Component.translatable("keyword.ordertocook.customer").getString();
        }

        return new DeliveryInfo(customerName, pos.getInt("x"), pos.getInt("z"));
    }

    private static boolean isExpired(Level world, CompoundTag nbt) {
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
            return world.getGameTime() >= nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
        }
        if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
            return System.currentTimeMillis() >= nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
        }
        return true;
    }

    private static int getDistance(double originX, double originZ, int targetX, int targetZ) {
        double dx = originX - (targetX + 0.5);
        double dz = originZ - (targetZ + 0.5);
        return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
    }

    private static String getDirectionArrow(MotorcycleEntity motorcycle, int targetX, int targetZ) {
        Vec3 targetVector = new Vec3(targetX + 0.5 - motorcycle.getX(), 0, targetZ + 0.5 - motorcycle.getZ());
        if (targetVector.lengthSqr() < 1.0E-6) {
            return "↑";
        }

        Vec3 forward = Vec3.directionFromRotation(0, motorcycle.getYRot()).normalize().scale(-1.0);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 normalizedTarget = targetVector.normalize();
        double localX = normalizedTarget.dot(right);
        double localZ = normalizedTarget.dot(forward);
        int sector = Math.floorMod((int) Math.round(Math.atan2(localX, localZ) / (Math.PI / 4.0)), 8);

        return switch (sector) {
            case 1 -> "↗";
            case 2 -> "→";
            case 3 -> "↘";
            case 4 -> "↓";
            case 5 -> "↙";
            case 6 -> "←";
            case 7 -> "↖";
            default -> "↑";
        };
    }

    private record DeliveryInfo(String customerName, int targetX, int targetZ) {
    }
}
