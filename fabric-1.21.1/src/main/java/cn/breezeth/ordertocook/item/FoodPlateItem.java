package cn.breezeth.ordertocook.item;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.registry.ModSounds;
import cn.breezeth.ordertocook.util.DataCompat;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class FoodPlateItem extends Item {
    public FoodPlateItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null || nbt.contains(ModConstants.NBT_DELIVERY_POS)) {
            return TypedActionResult.pass(stack);
        }

        if (!world.isClient && world instanceof ServerWorld sw) {
            if (TakeoutBagItem.trySubmitDineInNearby(sw, user, stack, nbt, ModSounds.FOOD_PLATE_PLACE)) {
                return TypedActionResult.success(stack);
            }
        }
        return TypedActionResult.pass(stack);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, net.minecraft.entity.player.PlayerEntity user, net.minecraft.entity.LivingEntity entity, Hand hand) {
        if (user.getWorld().isClient || !(user.getWorld() instanceof ServerWorld sw)) {
            return ActionResult.PASS;
        }
        return TakeoutBagItem.trySubmitDineInFromEntityUse(sw, user, stack, entity, ModSounds.FOOD_PLATE_PLACE);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient || !(world instanceof ServerWorld sw)) {
            return ActionResult.SUCCESS;
        }
        ItemStack stack = context.getStack();
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null || nbt.contains(ModConstants.NBT_DELIVERY_POS)) {
            return ActionResult.PASS;
        }
        if (context.getPlayer() == null) {
            return ActionResult.PASS;
        }
        net.minecraft.util.math.BlockPos clickedPos = context.getBlockPos();
        ItemPlacementContext placementContext = new ItemPlacementContext(context);
        net.minecraft.util.math.BlockPos intendedPos = world.getBlockState(clickedPos).canReplace(placementContext)
                ? clickedPos
                : clickedPos.offset(context.getSide());
        return TakeoutBagItem.trySubmitDineInFromBlockUse(sw, context.getPlayer(), stack, intendedPos, ModSounds.FOOD_PLATE_PLACE);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null) {
            tooltip.add(Text.translatable("tooltip.ordertocook.invalid_bag").formatted(Formatting.RED));
            return;
        }
        boolean expired = false;
        long expiryTick = nbt.contains(ModConstants.NBT_EXPIRY_TICK) ? nbt.getLong(ModConstants.NBT_EXPIRY_TICK) : -1;
        long nowTick = getWorldTickFromTooltipContext(context);
        if (expiryTick >= 0) {
            if (nowTick >= 0) {
                long remainingTicks = expiryTick - nowTick;
                if (remainingTicks > 0) {
                    long totalSeconds = remainingTicks / 20;
                    long minutes = totalSeconds / 60;
                    long seconds = totalSeconds % 60;
                    tooltip.add(Text.translatable("tooltip.ordertocook.time_left", String.format("%02d:%02d", minutes, seconds)).formatted(Formatting.GRAY));
                } else {
                    expired = true;
                }
            } else if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
                long remainMs = nbt.getLong(ModConstants.NBT_EXPIRY_TIME) - System.currentTimeMillis();
                if (remainMs > 0) {
                    long totalSeconds = remainMs / 1000;
                    long minutes = totalSeconds / 60;
                    long seconds = totalSeconds % 60;
                    tooltip.add(Text.translatable("tooltip.ordertocook.time_left", String.format("%02d:%02d", minutes, seconds)).formatted(Formatting.GRAY));
                } else {
                    expired = true;
                }
            }
        }
        if (nbt.contains("Prestige")) {
            tooltip.add(Text.translatable("tooltip.ordertocook.coin_reward", nbt.getInt("Prestige")).formatted(Formatting.GOLD));
        }
        if (nbt.contains(ModConstants.NBT_CUSTOMER_NAME)) {
            tooltip.add(Text.translatable("tooltip.ordertocook.food_plate.customer", nbt.getString(ModConstants.NBT_CUSTOMER_NAME)).formatted(Formatting.GRAY));
        }
        tooltip.add(Text.translatable(expired ? "tooltip.ordertocook.expired_hint" : "tooltip.ordertocook.submit_hint")
                .formatted(expired ? Formatting.RED : Formatting.GRAY));
    }

    private static long getWorldTickFromTooltipContext(TooltipContext context) {
        try {
            java.lang.reflect.Method m = context.getClass().getMethod("getWorld");
            Object w = m.invoke(context);
            if (w instanceof World world) {
                return world.getTime();
            }
        } catch (Exception ignored) {
        }
        try {
            Class<?> clientClass = Class.forName("net.minecraft.client.MinecraftClient");
            Object client = clientClass.getMethod("getInstance").invoke(null);
            Object w = clientClass.getField("world").get(client);
            if (w instanceof World world) {
                return world.getTime();
            }
        } catch (Exception ignored) {
        }
        return -1L;
    }
}
