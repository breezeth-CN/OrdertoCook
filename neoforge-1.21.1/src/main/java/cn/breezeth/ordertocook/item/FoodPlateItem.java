package cn.breezeth.ordertocook.item;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.registry.ModSounds;
import cn.breezeth.ordertocook.util.DataCompat;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class FoodPlateItem extends Item {
    public FoodPlateItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, net.minecraft.world.entity.player.Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt == null || nbt.contains(ModConstants.NBT_DELIVERY_POS)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!world.isClientSide && world instanceof ServerLevel sw) {
            if (TakeoutBagItem.trySubmitDineInNearby(sw, user, stack, nbt, ModSounds.FOOD_PLATE_PLACE.get())) {
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, net.minecraft.world.entity.player.Player user, net.minecraft.world.entity.LivingEntity entity, InteractionHand hand) {
        if (user.level().isClientSide || !(user.level() instanceof ServerLevel sw)) {
            return InteractionResult.PASS;
        }
        return TakeoutBagItem.trySubmitDineInFromEntityUse(sw, user, stack, entity, ModSounds.FOOD_PLATE_PLACE.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (world.isClientSide || !(world instanceof ServerLevel sw)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt == null || nbt.contains(ModConstants.NBT_DELIVERY_POS)) {
            return InteractionResult.PASS;
        }
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        net.minecraft.core.BlockPos clickedPos = context.getClickedPos();
        BlockPlaceContext placementContext = new BlockPlaceContext(context);
        net.minecraft.core.BlockPos intendedPos = world.getBlockState(clickedPos).canBeReplaced(placementContext)
                ? clickedPos
                : clickedPos.relative(context.getClickedFace());
        return TakeoutBagItem.trySubmitDineInFromBlockUse(sw, context.getPlayer(), stack, intendedPos, ModSounds.FOOD_PLATE_PLACE.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt == null) {
            tooltip.add(Component.translatable("tooltip.ordertocook.invalid_bag").withStyle(ChatFormatting.RED));
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
                    tooltip.add(Component.translatable("tooltip.ordertocook.time_left", String.format("%02d:%02d", minutes, seconds)).withStyle(ChatFormatting.GRAY));
                } else {
                    expired = true;
                }
            } else if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
                long remainMs = nbt.getLong(ModConstants.NBT_EXPIRY_TIME) - System.currentTimeMillis();
                if (remainMs > 0) {
                    long totalSeconds = remainMs / 1000;
                    long minutes = totalSeconds / 60;
                    long seconds = totalSeconds % 60;
                    tooltip.add(Component.translatable("tooltip.ordertocook.time_left", String.format("%02d:%02d", minutes, seconds)).withStyle(ChatFormatting.GRAY));
                } else {
                    expired = true;
                }
            }
        }
        if (nbt.contains("Prestige")) {
            tooltip.add(Component.translatable("tooltip.ordertocook.coin_reward", nbt.getInt("Prestige")).withStyle(ChatFormatting.GOLD));
        }
        if (nbt.contains(ModConstants.NBT_CUSTOMER_NAME)) {
            tooltip.add(Component.translatable("tooltip.ordertocook.food_plate.customer", nbt.getString(ModConstants.NBT_CUSTOMER_NAME)).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(expired ? "tooltip.ordertocook.expired_hint" : "tooltip.ordertocook.submit_hint")
                .withStyle(expired ? ChatFormatting.RED : ChatFormatting.GRAY));
    }

    private static long getWorldTickFromTooltipContext(TooltipContext context) {
        try {
            java.lang.reflect.Method m = context.getClass().getMethod("getWorld");
            Object w = m.invoke(context);
            if (w instanceof Level world) {
                return world.getGameTime();
            }
        } catch (Exception ignored) {
        }
        try {
            Class<?> clientClass = Class.forName("net.minecraft.client.Minecraft");
            Object client = clientClass.getMethod("getInstance").invoke(null);
            Object w = clientClass.getField("level").get(client);
            if (w instanceof Level world) {
                return world.getGameTime();
            }
        } catch (Exception ignored) {
        }
        return -1L;
    }
}
