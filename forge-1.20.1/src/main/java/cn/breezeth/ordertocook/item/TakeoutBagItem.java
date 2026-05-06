package cn.breezeth.ordertocook.item;

import cn.breezeth.ordertocook.util.DataCompat;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import cn.breezeth.ordertocook.registry.ModCriteria;
import cn.breezeth.ordertocook.core.OrderNpcManager;
import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.util.CoinUtils;


public class TakeoutBagItem extends Item {
    public TakeoutBagItem(Properties settings) {
        super(settings.food(new FoodProperties.Builder()
                .nutrition(4)
                .saturationMod(0.5f)
                .alwaysEat()
                .build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt != null) {
            if (isExpired(world, nbt)) {
                if (!world.isClientSide) {
                    world.playSound(null, user.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_BURP, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.0f);
                    if (user instanceof net.minecraft.server.level.ServerPlayer sp) {
                        cn.breezeth.ordertocook.registry.ModCriteria.EXPIRED_BAG_EATEN.trigger(sp);
                    }
                }
                stack.shrink(1);
                return stack;
            }
        }
        return super.finishUsingItem(stack, world, user);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, world, tooltip, type);
        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt == null) {
            tooltip.add(Component.translatable("tooltip.ordertocook.invalid_bag").withStyle(ChatFormatting.RED));
            return;
        }

        boolean expired = false;
        long expiryTick = nbt.contains(ModConstants.NBT_EXPIRY_TICK) ? nbt.getLong(ModConstants.NBT_EXPIRY_TICK) : -1;
        long nowTick = getWorldTickFromTooltipContext(world);
        if (expiryTick >= 0) {
            if (nowTick >= 0) {
                long remainingTicks = expiryTick - nowTick;
                if (remainingTicks > 0) {
                    long totalSeconds = remainingTicks / 20;
                    long minutes = totalSeconds / 60;
                    long seconds = totalSeconds % 60;
                    tooltip.add(Component.translatable("tooltip.ordertocook.time_left", String.format("%02d:%02d", minutes, seconds))
                            .withStyle(ChatFormatting.GRAY));
                } else {
                    expired = true;
                }
            } else if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
                long remainMs = nbt.getLong(ModConstants.NBT_EXPIRY_TIME) - System.currentTimeMillis();
                if (remainMs > 0) {
                    long totalSeconds = remainMs / 1000;
                    long minutes = totalSeconds / 60;
                    long seconds = totalSeconds % 60;
                    tooltip.add(Component.translatable("tooltip.ordertocook.time_left", String.format("%02d:%02d", minutes, seconds))
                            .withStyle(ChatFormatting.GRAY));
                } else {
                    expired = true;
                }
            }
        }

        if (nbt.contains("Prestige")) {
            int prestige = nbt.getInt("Prestige");
            tooltip.add(Component.translatable("tooltip.ordertocook.coin_reward", prestige).withStyle(ChatFormatting.GOLD));
        }
        
        if (nbt.contains("delivery_pos")) {
            CompoundTag pos = nbt.getCompound("delivery_pos");
            if (pos.contains("x") && pos.contains("z")) {
                tooltip.add(Component.translatable("tooltip.ordertocook.delivery_to", pos.getInt("x"), pos.getInt("z")).withStyle(ChatFormatting.BLUE));
            }
        }

        if (!expired) {
            tooltip.add(Component.translatable("tooltip.ordertocook.submit_hint").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.ordertocook.expired_hint").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt != null) {
            String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
            if (customer != null && !customer.isBlank()) {
                return Component.translatable("item.ordertocook.takeout_bag.named", customer);
            }
        }
        return super.getName(stack);
    }

    // --- 浣跨敤 (鍙抽敭绌烘皵/鏂瑰潡) 瑙﹀彂鍧愭爣閰嶉€佸垽瀹?---
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if (ConfigManager.isDevModeEnabled()) {
            String side = world.isClientSide ? "Client" : "Server";
            user.displayClientMessage(Component.literal("[OTC Dev] Item#use (" + side + ") Hand=" + hand).withStyle(ChatFormatting.DARK_PURPLE), false);
        }

        ItemStack stack = user.getItemInHand(hand);
        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt == null) return InteractionResultHolder.pass(stack);

        if (isExpired(world, nbt)) {
            user.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (!nbt.contains(ModConstants.NBT_DELIVERY_POS)) {

            if (!world.isClientSide && world instanceof ServerLevel sw) {
                if (trySubmitDineInNearby(sw, user, stack, nbt)) {
                    return InteractionResultHolder.success(stack);
                }
            }

            return InteractionResultHolder.pass(stack);
        }

        if (isExpired(world, nbt)) {
            user.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        if (!world.isClientSide && world instanceof ServerLevel sw) {
            if (trySubmitDelivery(sw, user, stack, nbt)) {
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    private static boolean trySubmitDineInNearby(ServerLevel world, Player player, ItemStack stack, CompoundTag nbt) {
        return trySubmitDineInNearby(world, player, stack, nbt, null);
    }

    public static boolean trySubmitDineInNearby(ServerLevel world, Player player, ItemStack stack, CompoundTag nbt, SoundEvent submitSound) {
        final String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
        if (orderId == null || orderId.isBlank()) return false;

        final String customerName = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);

        net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(6);
        java.util.List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, searchBox, e -> OrderNpcManager.isNpcForOrder(player, orderId, e));
        if (ConfigManager.isDevModeEnabled()) {
            player.displayClientMessage(Component.literal("[OTC Dev] trySubmitDineInNearby(v2) orderId=" + orderId + " customerName=" + customerName + " nearbyMatch=" + nearby.size()).withStyle(ChatFormatting.DARK_GRAY), false);
        }
        if (nearby.isEmpty()) {
            java.util.List<LivingEntity> tagged = world.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player && e.getTags().contains("otc_npc"));
            if (ConfigManager.isDevModeEnabled()) {
                player.displayClientMessage(Component.literal("[OTC Dev] trySubmitDineInNearby taggedNpc=" + tagged.size() + " customerName=" + customerName).withStyle(ChatFormatting.DARK_GRAY), false);
                for (LivingEntity e : tagged) {
                    String orderTag = "";
                    for (String t : e.getTags()) {
                        if (t.startsWith("otc_order:")) {
                            orderTag = t;
                            break;
                        }
                    }
                    String name = e.getCustomName() != null ? e.getCustomName().getString() : "";
                    player.displayClientMessage(Component.literal("[OTC Dev] npc=" + e.getType().getDescriptionId() + " name=" + name + " orderTag=" + orderTag + " tags=" + e.getTags()).withStyle(ChatFormatting.DARK_GRAY), false);
                }
            }

            if (customerName != null && !customerName.isBlank() && !tagged.isEmpty()) {
                java.util.List<LivingEntity> byName = new java.util.ArrayList<>();
                for (LivingEntity e : tagged) {
                    if (e.getCustomName() != null && customerName.equals(e.getCustomName().getString())) {
                        byName.add(e);
                    }
                }
                if (byName.size() == 1) {
                    LivingEntity npc = byName.get(0);
                    if (ConfigManager.isDevModeEnabled()) {
                        player.displayClientMessage(Component.literal("[OTC Dev] DineIn fallback matched by customerName -> completeDelivery").withStyle(ChatFormatting.GRAY), false);
                    }
                    if (submitSound != null) {
                        world.playSound(null, npc.blockPosition(), submitSound, SoundSource.NEUTRAL, 1.0f, 1.0f);
                    }
                    completeDelivery(world, player, stack, nbt, npc);
                    return true;
                }
            }

            return false;
        }

        LivingEntity npc = nearby.get(0);
        if (submitSound != null) {
            world.playSound(null, npc.blockPosition(), submitSound, SoundSource.NEUTRAL, 1.0f, 1.0f);
        }
        completeDelivery(world, player, stack, nbt, npc);
        return true;
    }

    public static InteractionResult trySubmitDineInFromBlockUse(ServerLevel world, Player player, ItemStack stack, BlockPos intendedPlatePos, SoundEvent submitSound) {
        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt == null || nbt.contains(ModConstants.NBT_DELIVERY_POS) || isExpired(world, nbt)) {
            return InteractionResult.PASS;
        }
        String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
        if (orderId == null || orderId.isBlank()) {
            return InteractionResult.PASS;
        }
        net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(intendedPlatePos).inflate(4.0);
        java.util.List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, searchBox, e -> OrderNpcManager.isNpcForOrder(player, orderId, e));
        for (LivingEntity npc : nearby) {
            BlockPos displayPos = OrderNpcManager.getCustomerPlateDisplayPos(npc);
            if (displayPos != null && displayPos.equals(intendedPlatePos) && submitDineInWithPlacedPlate(world, player, stack, nbt, npc, submitSound)) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt == null) return InteractionResult.PASS;

        if (!nbt.contains(ModConstants.NBT_DELIVERY_POS)) {
            if (user.level().isClientSide) return InteractionResult.PASS;
            if (isExpired(user.level(), nbt)) {
                return InteractionResult.PASS;
            }
            
            String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
            if (OrderNpcManager.isNpcForOrder(user, orderId, entity)) {
                completeDelivery((ServerLevel) user.level(), user, stack, nbt, entity);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        if (ConfigManager.isDevModeEnabled()) {
             user.displayClientMessage(Component.literal("[OTC Dev] useOnEntity -> PASS (Delegate to use)").withStyle(ChatFormatting.AQUA), false);
        }
        
        return InteractionResult.PASS;
    }

    public static InteractionResult trySubmitDeliveryFromEntityUse(ServerLevel world, Player player, ItemStack stack, Entity interactedEntity) {
        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt == null) return InteractionResult.PASS;

        if (!nbt.contains(ModConstants.NBT_DELIVERY_POS)) return InteractionResult.PASS;
        
        if (isExpired(world, nbt)) {
            return InteractionResult.PASS;
        }
        
        LivingEntity targetNpc = null;
        if (interactedEntity instanceof LivingEntity le) targetNpc = le;
        else if (interactedEntity instanceof ArmorStand && interactedEntity.isVehicle()) {
            if (interactedEntity.getFirstPassenger() instanceof LivingEntity le) targetNpc = le;
        }
        
        if (targetNpc == null) return InteractionResult.PASS;

        String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
        if (!orderId.isBlank() && OrderNpcManager.isNpcForOrder(player, orderId, targetNpc)) {
            completeDelivery(world, player, stack, nbt, targetNpc);
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.PASS;
    }

    public static InteractionResult trySubmitDineInFromEntityUse(ServerLevel world, Player player, ItemStack stack, Entity interactedEntity) {
        return trySubmitDineInFromEntityUse(world, player, stack, interactedEntity, null);
    }

    public static InteractionResult trySubmitDineInFromEntityUse(ServerLevel world, Player player, ItemStack stack, Entity interactedEntity, SoundEvent submitSound) {
        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt == null) return InteractionResult.PASS;

        if (nbt.contains(ModConstants.NBT_DELIVERY_POS)) return InteractionResult.PASS;

        if (isExpired(world, nbt)) {
            return InteractionResult.PASS;
        }

        LivingEntity targetNpc = null;
        if (interactedEntity instanceof LivingEntity le) targetNpc = le;
        else if (interactedEntity instanceof ArmorStand && interactedEntity.isVehicle()) {
            if (interactedEntity.getFirstPassenger() instanceof LivingEntity le) targetNpc = le;
        }

        if (targetNpc == null) return InteractionResult.PASS;

        String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
        if (orderId == null || orderId.isBlank()) return InteractionResult.PASS;

        if (OrderNpcManager.isNpcForOrder(player, orderId, targetNpc)) {
            return submitDineInWithPlacedPlate(world, player, stack, nbt, targetNpc, submitSound)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        String customerName = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
        if (customerName != null && !customerName.isBlank()
                && targetNpc.getTags().contains("otc_npc")
                && targetNpc.getCustomName() != null
                && customerName.equals(targetNpc.getCustomName().getString())) {
            if (ConfigManager.isDevModeEnabled()) {
                player.displayClientMessage(Component.literal("[OTC Dev] DineIn fallback matched by customerName (entityUse) -> completeDelivery").withStyle(ChatFormatting.GRAY), false);
            }
            return submitDineInWithPlacedPlate(world, player, stack, nbt, targetNpc, submitSound)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        return InteractionResult.PASS;
    }


    private static boolean trySubmitDelivery(ServerLevel world, Player player, ItemStack stack, CompoundTag nbt) {
        final String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
        if (orderId == null || orderId.isBlank()) return false;

        net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(12);
        java.util.List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, searchBox, e -> OrderNpcManager.isNpcForOrder(player, orderId, e));
        if (ConfigManager.isDevModeEnabled()) {
            player.displayClientMessage(Component.literal("[OTC Dev] trySubmitDelivery orderId=" + orderId + " nearbyMatch=" + nearby.size()).withStyle(ChatFormatting.DARK_GRAY), false);
        }
        if (nearby.isEmpty()) return false;

        LivingEntity npc = nearby.get(0);
        boolean isEggCustomer = npc instanceof cn.breezeth.ordertocook.entity.CustomerEntity customerEntity && customerEntity.isEasterEggCustomer();

        int baseCoin = nbt.getInt(ModConstants.NBT_PRESTIGE);
        boolean isUrgent = nbt.getBoolean(ModConstants.NBT_URGENT);
        boolean isLongDistance = nbt.getBoolean(ModConstants.NBT_IS_LONG_DISTANCE);
        String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
        if (customer == null || customer.isBlank()) customer = Component.translatable("keyword.ordertocook.customer").getString();

        int tipCoin = 0;
        var cfg = cn.breezeth.ordertocook.config.ConfigManager.get();
        double chance = isEggCustomer ? cfg.tipEasterEggCustomerChance : (isUrgent ? cfg.tipUrgentChance : cfg.tipNormalChance);
        int min = world.isRaining() ? cfg.rainTipMin : cfg.tipMin;
        int max = world.isRaining() ? cfg.rainTipMax : cfg.tipMax;
        if (world.random.nextDouble() < chance) {
            int span = Math.max(1, max - min + 1);
            tipCoin = min + world.random.nextInt(span);
        }

        int finalCoin = baseCoin + tipCoin;
        CoinUtils.giveCoins(player, finalCoin);
        cn.breezeth.ordertocook.core.PrestigeManager.addPlayerPrestige(player, finalCoin);

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ModCriteria.ORDER_COMPLETED.trigger(serverPlayer, finalCoin, isUrgent, isLongDistance);
            int totalEarned = cn.breezeth.ordertocook.core.PrestigeManager.getPlayerPrestige(player);
            ModCriteria.TOTAL_COIN.trigger(serverPlayer, totalEarned);
        }

        if (tipCoin > 0) {
            player.displayClientMessage(Component.translatable("message.ordertocook.order_completed_with_tip", finalCoin, customer, tipCoin).withStyle(ChatFormatting.GOLD), false);
        } else {
            player.displayClientMessage(Component.translatable("message.ordertocook.order_complete", finalCoin).withStyle(ChatFormatting.GOLD), false);
        }

        if (player instanceof net.minecraft.server.level.ServerPlayer) {
            int machineId = nbt.contains(ModConstants.NBT_MACHINE_ID) ? nbt.getInt(ModConstants.NBT_MACHINE_ID) : 0;
            boolean isLong = nbt.getBoolean(ModConstants.NBT_IS_LONG_DISTANCE);
            int orderType = nbt.contains(ModConstants.NBT_ORDER_TYPE) ? nbt.getInt(ModConstants.NBT_ORDER_TYPE) : 0;
            int deliveryDist = orderType > 1 ? orderType : (nbt.contains(ModConstants.NBT_DELIVERY_DIST) ? nbt.getInt(ModConstants.NBT_DELIVERY_DIST) : 0);
            boolean isDelivery = deliveryDist > 1;
            boolean isWalkIn = orderType == 1;
            if (machineId > 0) {
                cn.breezeth.ordertocook.core.RestaurantRegistry.applyCompletedDeltaById(world, machineId, finalCoin, isDelivery, isLong, deliveryDist, isWalkIn);
            }
        }
        OrderNpcManager.beginCompletedAnimation(world, player, orderId, npc, resolveCompletionAnimation(world, player, stack, nbt, npc));

        stack.shrink(1);
        return true;
    }

    private static void completeDelivery(ServerLevel world, Player player, ItemStack stack, CompoundTag nbt, Entity npc) {
        final String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);

        int baseCoin = nbt.getInt(ModConstants.NBT_PRESTIGE);
        boolean isUrgent = nbt.getBoolean(ModConstants.NBT_URGENT);
        boolean isLongDistance = nbt.getBoolean(ModConstants.NBT_IS_LONG_DISTANCE);
        String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
        if (customer == null || customer.isBlank()) customer = Component.translatable("keyword.ordertocook.customer").getString();

        boolean isEggCustomer = npc instanceof cn.breezeth.ordertocook.entity.CustomerEntity customerEntity && customerEntity.isEasterEggCustomer();

        int tipCoin = 0;
        var cfg = cn.breezeth.ordertocook.config.ConfigManager.get();
        double chance = isEggCustomer ? cfg.tipEasterEggCustomerChance : (isUrgent ? cfg.tipUrgentChance : cfg.tipNormalChance);
        int min = world.isRaining() ? cfg.rainTipMin : cfg.tipMin;
        int max = world.isRaining() ? cfg.rainTipMax : cfg.tipMax;
        if (world.random.nextDouble() < chance) {
            int span = Math.max(1, max - min + 1);
            tipCoin = min + world.random.nextInt(span);
        }

        int finalCoin = baseCoin + tipCoin;
        CoinUtils.giveCoins(player, finalCoin);
        cn.breezeth.ordertocook.core.PrestigeManager.addPlayerPrestige(player, finalCoin);

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ModCriteria.ORDER_COMPLETED.trigger(serverPlayer, finalCoin, isUrgent, isLongDistance);
            int totalEarned = cn.breezeth.ordertocook.core.PrestigeManager.getPlayerPrestige(player);
            ModCriteria.TOTAL_COIN.trigger(serverPlayer, totalEarned);
        }

        if (tipCoin > 0) {
            player.displayClientMessage(Component.translatable("message.ordertocook.order_completed_with_tip", finalCoin, customer, tipCoin).withStyle(ChatFormatting.GOLD), false);
        } else {
            player.displayClientMessage(Component.translatable("message.ordertocook.order_complete", finalCoin).withStyle(ChatFormatting.GOLD), false);
        }

        if (player instanceof net.minecraft.server.level.ServerPlayer) {
            int machineId = nbt.contains(ModConstants.NBT_MACHINE_ID) ? nbt.getInt(ModConstants.NBT_MACHINE_ID) : 0;
            int orderType = nbt.contains(ModConstants.NBT_ORDER_TYPE) ? nbt.getInt(ModConstants.NBT_ORDER_TYPE) : 0;
            int deliveryDist = orderType > 1 ? orderType : (nbt.contains(ModConstants.NBT_DELIVERY_DIST) ? nbt.getInt(ModConstants.NBT_DELIVERY_DIST) : 0);
            boolean isDelivery = deliveryDist > 1;
            boolean isWalkIn = orderType == 1;
            if (machineId > 0) {
                cn.breezeth.ordertocook.core.RestaurantRegistry.applyCompletedDeltaById(world, machineId, finalCoin, isDelivery, isLongDistance, deliveryDist, isWalkIn);
            }
        }
        OrderNpcManager.beginCompletedAnimation(world, player, orderId, npc, resolveCompletionAnimation(world, player, stack, nbt, npc));
        stack.shrink(1);
    }

    private static void settleDirectly(ServerLevel world, Player player, ItemStack stack, CompoundTag nbt) {
        int baseCoin = nbt.getInt(ModConstants.NBT_PRESTIGE);
        boolean isUrgent = nbt.getBoolean(ModConstants.NBT_URGENT);
        boolean isLongDistance = nbt.getBoolean(ModConstants.NBT_IS_LONG_DISTANCE);
        String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
        if (customer == null || customer.isBlank()) customer = Component.translatable("keyword.ordertocook.customer").getString();
        int tipCoin = 0;
        var cfg = cn.breezeth.ordertocook.config.ConfigManager.get();
        double chance = isUrgent ? cfg.tipUrgentChance : cfg.tipNormalChance;
        int min = world.isRaining() ? cfg.rainTipMin : cfg.tipMin;
        int max = world.isRaining() ? cfg.rainTipMax : cfg.tipMax;
        if (world.random.nextDouble() < chance) {
            int span = Math.max(1, max - min + 1);
            tipCoin = min + world.random.nextInt(span);
        }
        int finalCoin = baseCoin + tipCoin;
        cn.breezeth.ordertocook.util.CoinUtils.giveCoins(player, finalCoin);
        cn.breezeth.ordertocook.core.PrestigeManager.addPlayerPrestige(player, finalCoin);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            cn.breezeth.ordertocook.registry.ModCriteria.ORDER_COMPLETED.trigger(serverPlayer, finalCoin, isUrgent, isLongDistance);
            int totalEarned = cn.breezeth.ordertocook.core.PrestigeManager.getPlayerPrestige(player);
            cn.breezeth.ordertocook.registry.ModCriteria.TOTAL_COIN.trigger(serverPlayer, totalEarned);
        }
        if (tipCoin > 0) {
            player.displayClientMessage(Component.translatable("message.ordertocook.order_completed_with_tip", finalCoin, customer, tipCoin).withStyle(ChatFormatting.GOLD), false);
        } else {
            player.displayClientMessage(Component.translatable("message.ordertocook.order_complete", finalCoin).withStyle(ChatFormatting.GOLD), false);
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer) {
            int machineId = nbt.contains(ModConstants.NBT_MACHINE_ID) ? nbt.getInt(ModConstants.NBT_MACHINE_ID) : 0;
            int orderType = nbt.contains(ModConstants.NBT_ORDER_TYPE) ? nbt.getInt(ModConstants.NBT_ORDER_TYPE) : 0;
            int deliveryDist = orderType > 1 ? orderType : (nbt.contains(ModConstants.NBT_DELIVERY_DIST) ? nbt.getInt(ModConstants.NBT_DELIVERY_DIST) : 0);
            boolean isDelivery = deliveryDist > 1;
            boolean isWalkIn = orderType == 1;
            if (machineId > 0) {
                cn.breezeth.ordertocook.core.RestaurantRegistry.applyCompletedDeltaById(world, machineId, finalCoin, isDelivery, isLongDistance, deliveryDist, isWalkIn);
            }
        }
        String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
        OrderNpcManager.despawnFor(world, player, orderId, OrderNpcManager.DespawnReason.COMPLETED);
        stack.shrink(1);
    }

    private static OrderNpcManager.CompletionAnimation resolveCompletionAnimation(ServerLevel world, Player player, ItemStack stack, CompoundTag nbt, Entity npc) {
        boolean chairCustomer = npc instanceof cn.breezeth.ordertocook.entity.CustomerEntity customerEntity && customerEntity.isChairCustomer();
        if (chairCustomer && stack.getItem() instanceof FoodPlateItem) {
            boolean isEasterEggCustomer = npc instanceof cn.breezeth.ordertocook.entity.CustomerEntity customerEntity && customerEntity.isEasterEggCustomer();
            double boomChance = isEasterEggCustomer ? 0.6D : 0.1D;
            boolean useBoom = world.random.nextDouble() < boomChance;
            if (npc instanceof cn.breezeth.ordertocook.entity.CustomerEntity customerEntity) {
                prepareBoomBonus(world, player, nbt, customerEntity, useBoom);
            }
            return useBoom ? OrderNpcManager.CompletionAnimation.EAT_BOOM : OrderNpcManager.CompletionAnimation.EAT_SCALE;
        }
        if (chairCustomer) {
            return OrderNpcManager.CompletionAnimation.SIT_TAKE;
        }
        return OrderNpcManager.CompletionAnimation.STAND_TAKE;
    }

    private static void prepareBoomBonus(ServerLevel world, Player player, CompoundTag nbt, cn.breezeth.ordertocook.entity.CustomerEntity customerEntity, boolean useBoom) {
        if (!useBoom || player == null) {
            customerEntity.setPendingBoomBonus("", 0, false, false, 0, false, "", "", 0);
            return;
        }
        int machineId = nbt.contains(ModConstants.NBT_MACHINE_ID) ? nbt.getInt(ModConstants.NBT_MACHINE_ID) : 0;
        int bonusCoin = resolveBoomBonusCoin(world, machineId);
        int orderType = nbt.contains(ModConstants.NBT_ORDER_TYPE) ? nbt.getInt(ModConstants.NBT_ORDER_TYPE) : 0;
        int deliveryDist = orderType > 1 ? orderType : (nbt.contains(ModConstants.NBT_DELIVERY_DIST) ? nbt.getInt(ModConstants.NBT_DELIVERY_DIST) : 0);
        boolean isDelivery = deliveryDist > 1;
        boolean isWalkIn = orderType == 1;
        boolean isLongDistance = nbt.getBoolean(ModConstants.NBT_IS_LONG_DISTANCE);
        String customerName = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
        if (customerName == null || customerName.isBlank()) {
            customerName = Component.translatable("keyword.ordertocook.customer").getString();
        }
        String restaurantName = resolveRestaurantName(world, nbt, machineId);
        customerEntity.setPendingBoomBonus(player.getStringUUID(), machineId, isDelivery, isLongDistance, deliveryDist, isWalkIn, restaurantName, customerName, bonusCoin);
    }

    private static int resolveBoomBonusCoin(ServerLevel world, int machineId) {
        var stats = machineId > 0 ? cn.breezeth.ordertocook.core.RestaurantRegistry.getStatsById(world, machineId) : null;
        int level = stats != null ? stats.level() : 0;
        return 1 + world.random.nextInt(4) + level;
    }

    private static String resolveRestaurantName(ServerLevel world, CompoundTag nbt, int machineId) {
        var stats = machineId > 0 ? cn.breezeth.ordertocook.core.RestaurantRegistry.getStatsById(world, machineId) : null;
        if (stats != null && stats.name() != null && !stats.name().isBlank()) {
            return stats.name();
        }
        if (nbt.contains(ModConstants.NBT_MACHINE_POS) && nbt.contains(ModConstants.NBT_MACHINE_DIM)) {
            try {
                net.minecraft.resources.ResourceLocation dimensionId = net.minecraft.resources.ResourceLocation.parse(nbt.getString(ModConstants.NBT_MACHINE_DIM));
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> worldKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId);
                ServerLevel machineWorld = world.getServer() != null ? world.getServer().getLevel(worldKey) : null;
                if (machineWorld != null) {
                    BlockPos machinePos = BlockPos.of(nbt.getLong(ModConstants.NBT_MACHINE_POS));
                    if (machineWorld.getBlockEntity(machinePos) instanceof cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity orderMachineBlockEntity) {
                        String restaurantName = orderMachineBlockEntity.snapshotStats().name();
                        if (restaurantName != null && !restaurantName.isBlank()) {
                            return restaurantName;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return Component.translatable("keyword.ordertocook.unknown").getString();
    }

    private static boolean submitDineInWithPlacedPlate(ServerLevel world, Player player, ItemStack stack, CompoundTag nbt, LivingEntity npc, SoundEvent submitSound) {
        if (submitSound != null) {
            world.playSound(null, npc.blockPosition(), submitSound, SoundSource.NEUTRAL, 1.0f, 1.0f);
        }
        if (stack.getItem() instanceof FoodPlateItem) {
            placeSubmittedPlate(world, npc, stack.copyWithCount(1));
        }
        completeDelivery(world, player, stack, nbt, npc);
        return true;
    }

    private static void placeSubmittedPlate(ServerLevel world, LivingEntity npc, ItemStack plateStack) {
        BlockPos displayPos = OrderNpcManager.getCustomerPlateDisplayPos(npc);
        if (displayPos == null) {
            return;
        }
        if (!world.getBlockState(displayPos).isAir()) {
            world.destroyBlock(displayPos, true);
        }
        net.minecraft.core.Direction plateFacing = resolvePlateFacing(world, npc);
        BlockState state = cn.breezeth.ordertocook.registry.ModBlocks.FOOD_PLATE_DISPLAY.get().defaultBlockState()
                .setValue(cn.breezeth.ordertocook.block.FoodPlateBlock.FACING, plateFacing)
                .setValue(cn.breezeth.ordertocook.block.FoodPlateBlock.STAGE, 0);
        world.setBlock(displayPos, state, 3);
        if (world.getBlockEntity(displayPos) instanceof cn.breezeth.ordertocook.block.entity.FoodPlateBlockEntity plateBlockEntity) {
            plateBlockEntity.startEatingSequence(plateStack);
        } else {
            dropSubmittedPlate(world, displayPos, plateStack);
            world.removeBlock(displayPos, false);
        }
    }

    private static void dropSubmittedPlate(ServerLevel world, BlockPos displayPos, ItemStack plateStack) {
        net.minecraft.world.entity.item.ItemEntity entity = new net.minecraft.world.entity.item.ItemEntity(world, displayPos.getX() + 0.5, displayPos.getY() + 0.2, displayPos.getZ() + 0.5, plateStack);
        entity.setDeltaMovement(0.0, 0.1, 0.0);
        world.addFreshEntity(entity);
    }

    private static net.minecraft.core.Direction resolvePlateFacing(ServerLevel world, LivingEntity npc) {
        net.minecraft.core.Direction fallback = npc.getDirection().getOpposite();
        if (!(npc.getVehicle() instanceof cn.breezeth.ordertocook.entity.SeatEntity seat) || seat.resolveParent() != null) {
            return fallback;
        }
        for (String tag : seat.getTags()) {
            if (!tag.startsWith(OrderNpcManager.TAG_CHAIR_SEAT_POS_PREFIX)) {
                continue;
            }
            try {
                BlockPos chairPos = BlockPos.of(Long.parseLong(tag.substring(OrderNpcManager.TAG_CHAIR_SEAT_POS_PREFIX.length())));
                BlockState chairState = world.getBlockState(chairPos);
                if (chairState.is(cn.breezeth.ordertocook.registry.ModBlocks.CHAIR.get())) {
                    return chairState.getValue(cn.breezeth.ordertocook.block.ChairBlock.FACING).getOpposite();
                }
            } catch (NumberFormatException ignored) {
            }
            break;
        }
        return fallback;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (!world.isClientSide && entity instanceof Player player) {
            CompoundTag nbt = DataCompat.copy(stack);
            if (nbt != null) {
                if (ensureExpiryTick(world, stack, nbt)) {
                    nbt = DataCompat.copy(stack);
                    if (nbt == null) return;
                }
                
                if (!nbt.contains(ModConstants.NBT_EXPIRY_TIME) && nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
                    long expiryTick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
                    long remainTicks = Math.max(0, expiryTick - world.getGameTime());
                    long expiryMs = System.currentTimeMillis() + remainTicks * 50L;
                    nbt.putLong(ModConstants.NBT_EXPIRY_TIME, expiryMs);
                    DataCompat.set(stack, nbt);
                }

                if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
                    long expiryTick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
                    long remainingTicks = expiryTick - world.getGameTime();
                    if (remainingTicks <= 0) {
                        if (!nbt.getBoolean("ExpiryExpiredNotified")) {
                            String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
                            if (customer == null || customer.isBlank()) customer = Component.translatable("keyword.ordertocook.customer").getString();
                            player.displayClientMessage(Component.translatable("message.ordertocook.order_expired_named", customer).withStyle(ChatFormatting.RED), false);
                            nbt.putBoolean("ExpiryExpiredNotified", true);
                    DataCompat.set(stack, nbt);
                            if (world instanceof ServerLevel sw) {
                                String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
                                OrderNpcManager.despawnFor(sw, player, orderId, OrderNpcManager.DespawnReason.EXPIRED);
                            }
                        }
                    } else if (remainingTicks <= 60 * 20) {
                        if (!nbt.getBoolean("ExpiryWarned1m")) {
                            String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
                            if (customer == null || customer.isBlank()) customer = Component.translatable("keyword.ordertocook.customer").getString();
                            player.displayClientMessage(Component.translatable("message.ordertocook.order_expiring_soon_1", customer).withStyle(ChatFormatting.RED), false);
                            nbt.putBoolean("ExpiryWarned1m", true);
                    DataCompat.set(stack, nbt);
                        }
                    } else if (remainingTicks <= 5 * 60 * 20) {
                        if (!nbt.getBoolean("ExpiryWarned5m")) {
                            String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
                            if (customer == null || customer.isBlank()) customer = Component.translatable("keyword.ordertocook.customer").getString();
                            player.displayClientMessage(Component.translatable("message.ordertocook.order_expiring_soon_5", customer).withStyle(ChatFormatting.YELLOW), false);
                            nbt.putBoolean("ExpiryWarned5m", true);
                    DataCompat.set(stack, nbt);
                        }
                    }
                }
                if (nbt.contains("delivery_pos")) {
                    CompoundTag posNbt = nbt.getCompound("delivery_pos");
                    int targetX = posNbt.getInt("x");
                    int targetZ = posNbt.getInt("z");
                    boolean holding = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
                        if (holding && world.getGameTime() % 20L == 0L) {
                        double dx = player.getX() - (targetX + 0.5);
                        double dz = player.getZ() - (targetZ + 0.5);
                        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
                        player.displayClientMessage(Component.translatable("message.ordertocook.delivery_distance", dist).withStyle(ChatFormatting.GRAY), true);
                    }
                }
                if (world instanceof ServerLevel sw) {
                    trySpawnDeliveryWhenNearby(sw, player, stack);
                }
            }
        }
    }

    public static void trySpawnDeliveryWhenNearby(ServerLevel world, Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof TakeoutBagItem)) {
            return;
        }

        CompoundTag nbt = DataCompat.copy(stack);
        if (nbt == null) {
            return;
        }

        if (ensureExpiryTick(world, stack, nbt)) {
            nbt = DataCompat.copy(stack);
            if (nbt == null) {
                return;
            }
        }

        if (isExpired(world, nbt) || !nbt.contains(ModConstants.NBT_DELIVERY_POS) || nbt.getBoolean("delivery_spawned")) {
            return;
        }

        CompoundTag posNbt = nbt.getCompound(ModConstants.NBT_DELIVERY_POS);
        int targetX = posNbt.getInt("x");
        int targetZ = posNbt.getInt("z");
        double distSq = Math.pow(player.getX() - targetX, 2) + Math.pow(player.getZ() - targetZ, 2);
        if (distSq > (48 * 48)) {
            return;
        }

        BlockPos target = new BlockPos(targetX, world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ), targetZ);
        String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
        if (orderId == null || orderId.isBlank()) {
            orderId = cn.breezeth.ordertocook.core.OtcRuntimeIdState.get(world).allocateOrderId();
            nbt.putString(ModConstants.NBT_ORDER_ID, orderId);
        }
        String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
        boolean isLongDistance = nbt.getBoolean(ModConstants.NBT_IS_LONG_DISTANCE);
        long expiryTick = nbt.contains(ModConstants.NBT_EXPIRY_TICK) ? nbt.getLong(ModConstants.NBT_EXPIRY_TICK) : -1L;
        long expirySys = nbt.contains(ModConstants.NBT_EXPIRY_TIME) ? nbt.getLong(ModConstants.NBT_EXPIRY_TIME) : -1L;
        boolean spawned = isLongDistance
                ? cn.breezeth.ordertocook.core.LongDistanceDeliveryNpcManager.spawn(world, player, target, orderId, customer, expiryTick, expirySys, nbt)
                : OrderNpcManager.spawnForDelivery(world, player, target, orderId, customer, expiryTick, expirySys, nbt);
        if (!spawned) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.ordertocook.npc_respawn_fail_settle", customer).withStyle(net.minecraft.ChatFormatting.AQUA), false);
            settleDirectly(world, player, stack, nbt);
            return;
        }

        nbt.putBoolean("delivery_spawned", true);
        DataCompat.set(stack, nbt);

        world.getPlayers(p -> p.distanceToSqr(target.getX(), target.getY(), target.getZ()) < 100 * 100).forEach(p -> {
            if (ConfigManager.isDevModeEnabled()) {
                p.displayClientMessage(Component.literal("[OTC Dev] NPC Spawned dynamically at " + target.toShortString()).withStyle(ChatFormatting.AQUA), false);
            }
        });
    }

    private static long getWorldTickFromTooltipContext(Level world) {
        return world != null ? world.getGameTime() : getClientWorldTick();
    }

    private static long getClientWorldTick() {
        try {
            Class<?> clientClass = Class.forName("net.minecraft.client.Minecraft");
            Object client = clientClass.getMethod("getInstance").invoke(null);
            Object w = clientClass.getField("level").get(client);
            if (w instanceof Level clientWorld) return clientWorld.getGameTime();
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static boolean isExpired(Level world, CompoundTag nbt) {
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
            long expiryTick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
            return world.getGameTime() >= expiryTick;
        }
        if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
            return System.currentTimeMillis() >= nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
        }
        return true;
    }

    private static boolean ensureExpiryTick(Level world, ItemStack stack, CompoundTag nbt) {
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) return false;
        if (!nbt.contains(ModConstants.NBT_EXPIRY_TIME)) return false;

        long expiryTime = nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
        long remainingMs = expiryTime - System.currentTimeMillis();
        long expiryTick;
        if (remainingMs <= 0) {
            expiryTick = world.getGameTime() - 1;
        } else {
            long remainingTicks = (remainingMs + 49) / 50;
            expiryTick = world.getGameTime() + remainingTicks;
        }

        nbt.putLong(ModConstants.NBT_EXPIRY_TICK, expiryTick);
        DataCompat.set(stack, nbt);
        return true;
    }
}
