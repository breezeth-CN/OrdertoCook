package cn.breezeth.ordertocook.util;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.item.TakeoutBagItem;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class MotorcycleDeliveryFeedback {
    private MotorcycleDeliveryFeedback() {
    }

    public static Text build(ServerPlayerEntity player, MotorcycleEntity motorcycle) {
        Inventory inventory = motorcycle.getCoolerInventory();
        MutableText result = Text.empty();
        boolean hasLine = false;

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            DeliveryInfo info = getDeliveryInfo(player.getWorld(), stack);
            if (info == null) {
                continue;
            }

            if (hasLine) {
                result.append(Text.literal("\n"));
            }
            result.append(Text.translatable(
                    "message.ordertocook.motorcycle_delivery_row",
                    getDistance(player.getX(), player.getZ(), info.targetX(), info.targetZ()),
                    getDirectionArrow(motorcycle, info.targetX(), info.targetZ())
            ).formatted(Formatting.GRAY));
            hasLine = true;
        }

        return hasLine ? result : null;
    }

    private static DeliveryInfo getDeliveryInfo(World world, ItemStack stack) {
        if (!(stack.getItem() instanceof TakeoutBagItem)) {
            return null;
        }

        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null || !nbt.contains(ModConstants.NBT_DELIVERY_POS) || isExpired(world, nbt)) {
            return null;
        }

        NbtCompound pos = nbt.getCompound(ModConstants.NBT_DELIVERY_POS);
        if (!pos.contains(ModConstants.NBT_X) || !pos.contains(ModConstants.NBT_Z)) {
            return null;
        }

        String customerName = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
        if (customerName == null || customerName.isBlank()) {
            customerName = Text.translatable("keyword.ordertocook.customer").getString();
        }

        return new DeliveryInfo(customerName, pos.getInt(ModConstants.NBT_X), pos.getInt(ModConstants.NBT_Z));
    }

    private static boolean isExpired(World world, NbtCompound nbt) {
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
            return world.getTime() >= nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
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
        Vec3d targetVector = new Vec3d(targetX + 0.5 - motorcycle.getX(), 0, targetZ + 0.5 - motorcycle.getZ());
        if (targetVector.lengthSquared() < 1.0E-6) {
            return "↑";
        }

        Vec3d forward = Vec3d.fromPolar(0, motorcycle.getYaw()).normalize().multiply(-1.0);
        Vec3d right = new Vec3d(-forward.z, 0, forward.x);
        Vec3d normalizedTarget = targetVector.normalize();
        double localX = normalizedTarget.dotProduct(right);
        double localZ = normalizedTarget.dotProduct(forward);
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
