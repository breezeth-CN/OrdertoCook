package cn.breezeth.ordertocook.item;

import cn.breezeth.ordertocook.util.DataCompat;
import net.minecraft.block.BlockState;
import net.minecraft.item.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

 
import cn.breezeth.ordertocook.registry.ModCriteria;
import net.minecraft.server.world.ServerWorld;
import cn.breezeth.ordertocook.core.OrderNpcManager;
import net.minecraft.util.math.BlockPos;
import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.util.CoinUtils;


public class TakeoutBagItem extends Item {
    public TakeoutBagItem(Settings settings) {
        super(settings.food(new FoodComponent.Builder()
                .hunger(4)
                .saturationModifier(0.5f)
                .alwaysEdible()
                .build()));
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null) {
            tooltip.add(Text.translatable("tooltip.ordertocook.invalid_bag").formatted(Formatting.RED));
            return;
        }
        boolean delivery = nbt.contains(ModConstants.NBT_DELIVERY) && nbt.getBoolean(ModConstants.NBT_DELIVERY);
        long remainMs = -1L;
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK) && world != null) {
            long tick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
            long remainTicks = Math.max(0, tick - world.getTime());
            remainMs = remainTicks * 50L;
        } else if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
            long expiry = nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
            remainMs = Math.max(0, expiry - System.currentTimeMillis());
        }
        boolean expired = false;
        if (remainMs >= 0) {
            long seconds = (remainMs + 999) / 1000;
            long minutes = seconds / 60;
            long secs = seconds % 60;
            String mm = String.format("%02d", minutes);
            String ss = String.format("%02d", secs);
            if (seconds <= 0) {
                tooltip.add(Text.translatable("tooltip.ordertocook.expired").formatted(Formatting.RED));
                expired = true;
            } else {
                tooltip.add(Text.translatable("tooltip.ordertocook.time_left", mm + ":" + ss).formatted(Formatting.GRAY));
            }
        }
        tooltip.add(Text.literal("----------").formatted(Formatting.GRAY));
        if (nbt.contains(ModConstants.NBT_FOOD_LIST)) {
            NbtCompound foodList = nbt.getCompound(ModConstants.NBT_FOOD_LIST);
            for (String key : foodList.getKeys()) {
                net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(key);
                if (id == null || !net.minecraft.registry.Registries.ITEM.containsId(id)) continue;
                net.minecraft.item.Item item = net.minecraft.registry.Registries.ITEM.get(id);
                int count = foodList.getInt(key);
                net.minecraft.item.ItemStack is = new net.minecraft.item.ItemStack(item);
                Text name = is.getName();
                tooltip.add(Text.literal("— ").formatted(Formatting.GRAY)
                        .append(name.copy().formatted(Formatting.WHITE))
                        .append(Text.literal(" x" + count).formatted(Formatting.GRAY)));
            }
        }
        if (delivery && nbt.contains(ModConstants.NBT_DELIVERY_POS)) {
            NbtCompound dp = nbt.getCompound(ModConstants.NBT_DELIVERY_POS);
            int tx = dp.getInt(ModConstants.NBT_X);
            int tz = dp.getInt(ModConstants.NBT_Z);
            tooltip.add(Text.translatable("tooltip.ordertocook.delivery_to", tx, tz).formatted(Formatting.BLUE));
        }
        int coin = nbt.contains(ModConstants.NBT_PRESTIGE) ? nbt.getInt(ModConstants.NBT_PRESTIGE) : 0;
        if (coin > 0) {
            tooltip.add(Text.translatable("tooltip.ordertocook.coin_reward", coin).formatted(Formatting.GOLD));
        }
        if (!expired) {
            tooltip.add(Text.translatable("tooltip.ordertocook.submit_hint").formatted(Formatting.GRAY));
        } else {
            tooltip.add(Text.translatable("tooltip.ordertocook.expired_hint").formatted(Formatting.RED));
        }
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt != null) {
            if (isExpired(world, nbt)) {
                if (!world.isClient) {
                    world.playSound(null, user.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_PLAYER_BURP, net.minecraft.sound.SoundCategory.PLAYERS, 0.8f, 1.0f);
                    if (user instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
                        cn.breezeth.ordertocook.registry.ModCriteria.EXPIRED_BAG_EATEN.trigger(sp);
                    }
                }
                stack.decrement(1);
                return stack;
            }
        }
        return super.finishUsing(stack, world, user);
    }


    // --- 使用 (右键空气/方块) 触发坐标配送判定 ---
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (ConfigManager.isDevModeEnabled()) {
            String side = world.isClient ? "Client" : "Server";
            user.sendMessage(Text.literal("[OTC Dev] Item#use (" + side + ") Hand=" + hand).formatted(Formatting.DARK_PURPLE), false);
        }

        ItemStack stack = user.getStackInHand(hand);
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null) return TypedActionResult.pass(stack);

        if (isExpired(world, nbt)) {
            user.setCurrentHand(hand);
            return TypedActionResult.consume(stack);
        }
        if (!nbt.contains(ModConstants.NBT_DELIVERY_POS)) {

            if (!world.isClient && world instanceof ServerWorld sw) {
                if (trySubmitDineInNearby(sw, user, stack, nbt)) {
                    return TypedActionResult.success(stack);
                }
            }

            return TypedActionResult.pass(stack);
        }

        if (isExpired(world, nbt)) {
            user.setCurrentHand(hand);
            return TypedActionResult.consume(stack);
        }

        if (!world.isClient && world instanceof ServerWorld sw) {
            if (trySubmitDelivery(sw, user, stack, nbt)) {
                return TypedActionResult.success(stack);
            }
        }
        return TypedActionResult.pass(stack);
    }

    private static boolean trySubmitDineInNearby(ServerWorld world, PlayerEntity player, ItemStack stack, NbtCompound nbt) {
        return trySubmitDineInNearby(world, player, stack, nbt, null);
    }

    public static boolean trySubmitDineInNearby(ServerWorld world, PlayerEntity player, ItemStack stack, NbtCompound nbt, net.minecraft.sound.SoundEvent submitSound) {
        final String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
        if (orderId == null || orderId.isBlank()) return false;

        final String customerName = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);

        net.minecraft.util.math.Box searchBox = new net.minecraft.util.math.Box(player.getBlockPos()).expand(6);
        java.util.List<LivingEntity> nearby = world.getEntitiesByClass(LivingEntity.class, searchBox, e -> OrderNpcManager.isNpcForOrder(player, orderId, e));
        if (ConfigManager.isDevModeEnabled()) {
            player.sendMessage(Text.literal("[OTC Dev] trySubmitDineInNearby(v2) orderId=" + orderId + " customerName=" + customerName + " nearbyMatch=" + nearby.size()).formatted(Formatting.DARK_GRAY), false);
        }
        if (nearby.isEmpty()) {
            java.util.List<LivingEntity> tagged = world.getEntitiesByClass(LivingEntity.class, searchBox, e -> e != player && e.getCommandTags().contains("otc_npc"));
            if (ConfigManager.isDevModeEnabled()) {
                player.sendMessage(Text.literal("[OTC Dev] trySubmitDineInNearby taggedNpc=" + tagged.size() + " customerName=" + customerName).formatted(Formatting.DARK_GRAY), false);
                for (LivingEntity e : tagged) {
                    String orderTag = "";
                    for (String t : e.getCommandTags()) {
                        if (t.startsWith("otc_order:")) {
                            orderTag = t;
                            break;
                        }
                    }
                    String name = e.getCustomName() != null ? e.getCustomName().getString() : "";
                    player.sendMessage(Text.literal("[OTC Dev] npc=" + e.getType().getTranslationKey() + " name=" + name + " orderTag=" + orderTag + " tags=" + e.getCommandTags()).formatted(Formatting.DARK_GRAY), false);
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
                        player.sendMessage(Text.literal("[OTC Dev] DineIn fallback matched by customerName -> completeDelivery").formatted(Formatting.GRAY), false);
                    }
                    if (submitSound != null) {
                        world.playSound(null, npc.getBlockPos(), submitSound, net.minecraft.sound.SoundCategory.NEUTRAL, 1.0f, 1.0f);
                    }
                    completeDelivery(world, player, stack, nbt, npc);
                    return true;
                }
            }

            return false;
        }

        LivingEntity npc = nearby.get(0);
        if (submitSound != null) {
            world.playSound(null, npc.getBlockPos(), submitSound, net.minecraft.sound.SoundCategory.NEUTRAL, 1.0f, 1.0f);
        }
        completeDelivery(world, player, stack, nbt, npc);
        return true;
    }

    public static ActionResult trySubmitDineInFromBlockUse(ServerWorld world, PlayerEntity player, ItemStack stack, BlockPos intendedPlatePos, net.minecraft.sound.SoundEvent submitSound) {
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null || nbt.contains(ModConstants.NBT_DELIVERY_POS) || isExpired(world, nbt)) {
            return ActionResult.PASS;
        }
        String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
        if (orderId == null || orderId.isBlank()) {
            return ActionResult.PASS;
        }
        net.minecraft.util.math.Box searchBox = new net.minecraft.util.math.Box(intendedPlatePos).expand(4.0);
        java.util.List<LivingEntity> nearby = world.getEntitiesByClass(LivingEntity.class, searchBox, e -> OrderNpcManager.isNpcForOrder(player, orderId, e));
        for (LivingEntity npc : nearby) {
            BlockPos displayPos = OrderNpcManager.getCustomerPlateDisplayPos(npc);
            if (displayPos != null && displayPos.equals(intendedPlatePos) && submitDineInWithPlacedPlate(world, player, stack, nbt, npc, submitSound)) {
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    private static boolean submitDineInWithPlacedPlate(ServerWorld world, PlayerEntity player, ItemStack stack, NbtCompound nbt, LivingEntity npc, net.minecraft.sound.SoundEvent submitSound) {
        if (submitSound != null) {
            world.playSound(null, npc.getBlockPos(), submitSound, net.minecraft.sound.SoundCategory.NEUTRAL, 1.0f, 1.0f);
        }
        if (stack.getItem() instanceof FoodPlateItem) {
            placeSubmittedPlate(world, npc, stack.copyWithCount(1));
        }
        completeDelivery(world, player, stack, nbt, npc);
        return true;
    }

    private static void placeSubmittedPlate(ServerWorld world, LivingEntity npc, ItemStack plateStack) {
        BlockPos displayPos = OrderNpcManager.getCustomerPlateDisplayPos(npc);
        if (displayPos == null) {
            return;
        }
        if (!world.getBlockState(displayPos).isAir()) {
            world.breakBlock(displayPos, true);
        }
        net.minecraft.util.math.Direction plateFacing = resolvePlateFacing(world, npc);
        BlockState state = cn.breezeth.ordertocook.registry.ModBlocks.FOOD_PLATE_DISPLAY.getDefaultState()
                .with(cn.breezeth.ordertocook.block.FoodPlateBlock.FACING, plateFacing)
                .with(cn.breezeth.ordertocook.block.FoodPlateBlock.STAGE, 0);
        world.setBlockState(displayPos, state, 3);
        if (world.getBlockEntity(displayPos) instanceof cn.breezeth.ordertocook.block.entity.FoodPlateBlockEntity plateBlockEntity) {
            plateBlockEntity.startEatingSequence(plateStack);
        } else {
            dropSubmittedPlate(world, displayPos, plateStack);
            world.removeBlock(displayPos, false);
        }
    }

    private static void dropSubmittedPlate(ServerWorld world, BlockPos displayPos, ItemStack plateStack) {
        net.minecraft.entity.ItemEntity entity = new net.minecraft.entity.ItemEntity(world, displayPos.getX() + 0.5, displayPos.getY() + 0.2, displayPos.getZ() + 0.5, plateStack);
        entity.setVelocity(0.0, 0.1, 0.0);
        world.spawnEntity(entity);
    }

    private static net.minecraft.util.math.Direction resolvePlateFacing(ServerWorld world, LivingEntity npc) {
        net.minecraft.util.math.Direction fallback = npc.getHorizontalFacing().getOpposite();
        if (!(npc.getVehicle() instanceof cn.breezeth.ordertocook.entity.SeatEntity seat) || seat.resolveParent() != null) {
            return fallback;
        }
        for (String tag : seat.getCommandTags()) {
            if (!tag.startsWith(OrderNpcManager.TAG_CHAIR_SEAT_POS_PREFIX)) {
                continue;
            }
            try {
                BlockPos chairPos = BlockPos.fromLong(Long.parseLong(tag.substring(OrderNpcManager.TAG_CHAIR_SEAT_POS_PREFIX.length())));
                BlockState chairState = world.getBlockState(chairPos);
                if (chairState.isOf(cn.breezeth.ordertocook.registry.ModBlocks.CHAIR)) {
                    return chairState.get(cn.breezeth.ordertocook.block.ChairBlock.FACING).getOpposite();
                }
            } catch (NumberFormatException ignored) {
            }
            break;
        }
        return fallback;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null) return ActionResult.PASS;

        if (!nbt.contains(ModConstants.NBT_DELIVERY_POS)) {
            if (user.getWorld().isClient) return ActionResult.PASS;
            if (isExpired(user.getWorld(), nbt)) {
                return ActionResult.PASS;
            }
            
            String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
            if (OrderNpcManager.isNpcForOrder(user, orderId, entity)) {
                user.getWorld().playSound(null, entity.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_YES, net.minecraft.sound.SoundCategory.NEUTRAL, 1.0f, 1.0f);
                completeDelivery((ServerWorld) user.getWorld(), user, stack, nbt, entity);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        }
        
        return ActionResult.PASS;
    }

    public static ActionResult trySubmitDeliveryFromEntityUse(ServerWorld world, PlayerEntity player, ItemStack stack, Entity interactedEntity) {
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null) return ActionResult.PASS;

        if (!nbt.contains(ModConstants.NBT_DELIVERY_POS)) return ActionResult.PASS;
        
        if (isExpired(world, nbt)) {
            return ActionResult.PASS;
        }
        
        LivingEntity targetNpc = null;
        if (interactedEntity instanceof LivingEntity le) targetNpc = le;
        else if (interactedEntity instanceof ArmorStandEntity && interactedEntity.hasPassengers()) {
            if (interactedEntity.getFirstPassenger() instanceof LivingEntity le) targetNpc = le;
        }
        
        if (targetNpc == null) return ActionResult.PASS;

        String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
        if (!orderId.isBlank() && OrderNpcManager.isNpcForOrder(player, orderId, targetNpc)) {
            world.playSound(null, targetNpc.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_YES, net.minecraft.sound.SoundCategory.NEUTRAL, 1.0f, 1.0f);
            completeDelivery(world, player, stack, nbt, targetNpc);
            return ActionResult.SUCCESS;
        }
        
        return ActionResult.PASS;
    }

    public static ActionResult trySubmitDineInFromEntityUse(ServerWorld world, PlayerEntity player, ItemStack stack, Entity interactedEntity) {
        return trySubmitDineInFromEntityUse(world, player, stack, interactedEntity, null);
    }

    public static ActionResult trySubmitDineInFromEntityUse(ServerWorld world, PlayerEntity player, ItemStack stack, Entity interactedEntity, net.minecraft.sound.SoundEvent submitSound) {
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt == null) return ActionResult.PASS;

        if (nbt.contains(ModConstants.NBT_DELIVERY_POS)) return ActionResult.PASS;

        if (isExpired(world, nbt)) {
            return ActionResult.PASS;
        }

        LivingEntity targetNpc = null;
        if (interactedEntity instanceof LivingEntity le) targetNpc = le;
        else if (interactedEntity instanceof ArmorStandEntity && interactedEntity.hasPassengers()) {
            if (interactedEntity.getFirstPassenger() instanceof LivingEntity le) targetNpc = le;
        }

        if (targetNpc == null) return ActionResult.PASS;

        String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
        if (orderId == null || orderId.isBlank()) return ActionResult.PASS;

        if (OrderNpcManager.isNpcForOrder(player, orderId, targetNpc)) {
            return submitDineInWithPlacedPlate(world, player, stack, nbt, targetNpc, submitSound)
                    ? ActionResult.SUCCESS
                    : ActionResult.PASS;
        }

        String customerName = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
        if (customerName != null && !customerName.isBlank()
                && targetNpc.getCommandTags().contains("otc_npc")
                && targetNpc.getCustomName() != null
                && customerName.equals(targetNpc.getCustomName().getString())) {
            return submitDineInWithPlacedPlate(world, player, stack, nbt, targetNpc, submitSound)
                    ? ActionResult.SUCCESS
                    : ActionResult.PASS;
        }

        player.sendMessage(Text.translatable("tooltip.ordertocook.submit_hint").formatted(Formatting.GRAY), true);
        return ActionResult.PASS;
    }


    private static boolean trySubmitDelivery(ServerWorld world, PlayerEntity player, ItemStack stack, NbtCompound nbt) {
        final String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
        if (orderId == null || orderId.isBlank()) return false;

        net.minecraft.util.math.Box searchBox = new net.minecraft.util.math.Box(player.getBlockPos()).expand(12);
        java.util.List<LivingEntity> nearby = world.getEntitiesByClass(LivingEntity.class, searchBox, e -> OrderNpcManager.isNpcForOrder(player, orderId, e));
        if (ConfigManager.isDevModeEnabled()) {
            player.sendMessage(Text.literal("[OTC Dev] trySubmitDelivery orderId=" + orderId + " nearbyMatch=" + nearby.size()).formatted(Formatting.DARK_GRAY), false);
        }
        if (nearby.isEmpty()) return false;

        LivingEntity npc = nearby.get(0);
        boolean isEggZombie = false;
        if (npc instanceof net.minecraft.entity.mob.ZombieEntity zombie) {
            if (zombie instanceof cn.breezeth.ordertocook.core.OrderNpcVisualAccess access) {
                isEggZombie = access.oc$getSkinVariant() == 0;
            } else {
                isEggZombie = true;
            }
        }
        world.playSound(null, npc.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_YES, net.minecraft.sound.SoundCategory.NEUTRAL, 1.0f, 1.0f);

        int baseCoin = nbt.getInt(ModConstants.NBT_PRESTIGE);
        boolean isUrgent = nbt.getBoolean(ModConstants.NBT_URGENT);
        boolean isLongDistance = nbt.getBoolean(ModConstants.NBT_IS_LONG_DISTANCE);
        String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
        if (customer == null || customer.isBlank()) customer = Text.translatable("keyword.ordertocook.customer").getString();

        int tipCoin = 0;
        var cfg = cn.breezeth.ordertocook.config.ConfigManager.get();
        double chance = isEggZombie ? cfg.tipEasterEggCustomerChance : (isUrgent ? cfg.tipUrgentChance : cfg.tipNormalChance);
        int min = world.isRaining() ? cfg.rainTipMin : cfg.tipMin;
        int max = world.isRaining() ? cfg.rainTipMax : cfg.tipMax;
        if (world.random.nextDouble() < chance) {
            int span = Math.max(1, max - min + 1);
            tipCoin = min + world.random.nextInt(span);
        }

        int finalCoin = baseCoin + tipCoin;
        CoinUtils.giveCoins(player, finalCoin);
        cn.breezeth.ordertocook.core.PrestigeManager.addPlayerPrestige(player, finalCoin);

        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            ModCriteria.ORDER_COMPLETED.trigger(serverPlayer, finalCoin, isUrgent, isLongDistance);
            int totalEarned = cn.breezeth.ordertocook.core.PrestigeManager.getPlayerPrestige(player);
            ModCriteria.TOTAL_COIN.trigger(serverPlayer, totalEarned);
        }

        if (tipCoin > 0) {
            player.sendMessage(Text.translatable("message.ordertocook.order_completed_with_tip", finalCoin, customer, tipCoin).formatted(Formatting.GOLD), false);
        } else {
            player.sendMessage(Text.translatable("message.ordertocook.order_complete", finalCoin).formatted(Formatting.GOLD), false);
        }

        if (player instanceof net.minecraft.server.network.ServerPlayerEntity) {
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

        stack.decrement(1);
        return true;
    }

    @Override
    public Text getName(ItemStack stack) {
        NbtCompound nbt = DataCompat.copy(stack);
        if (nbt != null) {
            String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
            if (customer != null && !customer.isBlank()) {
                return Text.translatable("item.ordertocook.takeout_bag.named", customer);
            }
        }
        return super.getName(stack);
    }

    private static void completeDelivery(ServerWorld world, PlayerEntity player, ItemStack stack, NbtCompound nbt, Entity npc) {
        final String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);

        int baseCoin = nbt.getInt(ModConstants.NBT_PRESTIGE);
        boolean isUrgent = nbt.getBoolean(ModConstants.NBT_URGENT);
        boolean isLongDistance = nbt.getBoolean(ModConstants.NBT_IS_LONG_DISTANCE);
        String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
        if (customer == null || customer.isBlank()) customer = Text.translatable("keyword.ordertocook.customer").getString();

        boolean isEggZombie = false;
        if (npc instanceof net.minecraft.entity.mob.ZombieEntity zombie) {
            if (zombie instanceof cn.breezeth.ordertocook.core.OrderNpcVisualAccess access) {
                isEggZombie = access.oc$getSkinVariant() == 0;
            } else {
                isEggZombie = true;
            }
        }

        int tipCoin = 0;
        var cfg = cn.breezeth.ordertocook.config.ConfigManager.get();
        double chance = isEggZombie ? cfg.tipEasterEggCustomerChance : (isUrgent ? cfg.tipUrgentChance : cfg.tipNormalChance);
        int min = world.isRaining() ? cfg.rainTipMin : cfg.tipMin;
        int max = world.isRaining() ? cfg.rainTipMax : cfg.tipMax;
        if (world.random.nextDouble() < chance) {
            int span = Math.max(1, max - min + 1);
            tipCoin = min + world.random.nextInt(span);
        }

        int finalCoin = baseCoin + tipCoin;
        CoinUtils.giveCoins(player, finalCoin);
        cn.breezeth.ordertocook.core.PrestigeManager.addPlayerPrestige(player, finalCoin);

        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            ModCriteria.ORDER_COMPLETED.trigger(serverPlayer, finalCoin, isUrgent, isLongDistance);
            int totalEarned = cn.breezeth.ordertocook.core.PrestigeManager.getPlayerPrestige(player);
            ModCriteria.TOTAL_COIN.trigger(serverPlayer, totalEarned);
        }

        if (tipCoin > 0) {
            player.sendMessage(Text.translatable("message.ordertocook.order_completed_with_tip", finalCoin, customer, tipCoin).formatted(Formatting.GOLD), false);
        } else {
            player.sendMessage(Text.translatable("message.ordertocook.order_complete", finalCoin).formatted(Formatting.GOLD), false);
        }

        if (player instanceof net.minecraft.server.network.ServerPlayerEntity) {
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
        stack.decrement(1);
    }

    private static void settleDirectly(ServerWorld world, PlayerEntity player, ItemStack stack, NbtCompound nbt) {
        int baseCoin = nbt.getInt(ModConstants.NBT_PRESTIGE);
        boolean isUrgent = nbt.getBoolean(ModConstants.NBT_URGENT);
        boolean isLongDistance = nbt.getBoolean(ModConstants.NBT_IS_LONG_DISTANCE);
        String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
        if (customer == null || customer.isBlank()) customer = Text.translatable("keyword.ordertocook.customer").getString();
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
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            cn.breezeth.ordertocook.registry.ModCriteria.ORDER_COMPLETED.trigger(serverPlayer, finalCoin, isUrgent, isLongDistance);
            int totalEarned = cn.breezeth.ordertocook.core.PrestigeManager.getPlayerPrestige(player);
            cn.breezeth.ordertocook.registry.ModCriteria.TOTAL_COIN.trigger(serverPlayer, totalEarned);
        }
        if (tipCoin > 0) {
            player.sendMessage(Text.translatable("message.ordertocook.order_completed_with_tip", finalCoin, customer, tipCoin).formatted(Formatting.GOLD), false);
        } else {
            player.sendMessage(Text.translatable("message.ordertocook.order_complete", finalCoin).formatted(Formatting.GOLD), false);
        }
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity) {
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
        stack.decrement(1);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && entity instanceof PlayerEntity player) {
            NbtCompound nbt = DataCompat.copy(stack);
            if (nbt != null) {
                if (ensureExpiryTick(world, stack, nbt)) {
                    nbt = DataCompat.copy(stack);
                    if (nbt == null) return;
                }
                
                if (!nbt.contains(ModConstants.NBT_EXPIRY_TIME) && nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
                    long expiryTick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
                    long remainTicks = Math.max(0, expiryTick - world.getTime());
                    long expiryMs = System.currentTimeMillis() + remainTicks * 50L;
                    nbt.putLong(ModConstants.NBT_EXPIRY_TIME, expiryMs);
                    DataCompat.set(stack, nbt);
                }

                if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
                    long expiryTick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
                    long remainingTicks = expiryTick - world.getTime();
                    if (remainingTicks <= 0) {
                        if (!nbt.getBoolean("ExpiryExpiredNotified")) {
                            String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
                            if (customer == null || customer.isBlank()) customer = Text.translatable("keyword.ordertocook.customer").getString();
                            player.sendMessage(Text.translatable("message.ordertocook.order_expired_named", customer).formatted(Formatting.RED), false);
                            nbt.putBoolean("ExpiryExpiredNotified", true);
                    DataCompat.set(stack, nbt);
                            if (world instanceof ServerWorld sw) {
                                String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
                                OrderNpcManager.despawnFor(sw, player, orderId, OrderNpcManager.DespawnReason.EXPIRED);
                            }
                        }
                    } else if (remainingTicks <= 60 * 20) {
                        if (!nbt.getBoolean("ExpiryWarned1m")) {
                            String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
                            if (customer == null || customer.isBlank()) customer = Text.translatable("keyword.ordertocook.customer").getString();
                            player.sendMessage(Text.translatable("message.ordertocook.order_expiring_soon_1", customer).formatted(Formatting.RED), false);
                            nbt.putBoolean("ExpiryWarned1m", true);
                    DataCompat.set(stack, nbt);
                        }
                    } else if (remainingTicks <= 5 * 60 * 20) {
                        if (!nbt.getBoolean("ExpiryWarned5m")) {
                            String customer = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
                            if (customer == null || customer.isBlank()) customer = Text.translatable("keyword.ordertocook.customer").getString();
                            player.sendMessage(Text.translatable("message.ordertocook.order_expiring_soon_5", customer).formatted(Formatting.YELLOW), false);
                            nbt.putBoolean("ExpiryWarned5m", true);
                    DataCompat.set(stack, nbt);
                        }
                    }
                }
                if (nbt.contains("delivery_pos")) {
                    NbtCompound posNbt = nbt.getCompound("delivery_pos");
                    int targetX = posNbt.getInt("x");
                    int targetZ = posNbt.getInt("z");
                    boolean holding = player.getMainHandStack() == stack || player.getOffHandStack() == stack;
                    if (holding && world.getTime() % 20L == 0L) {
                        double dx = player.getX() - (targetX + 0.5);
                        double dz = player.getZ() - (targetZ + 0.5);
                        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
                        player.sendMessage(Text.translatable("message.ordertocook.delivery_distance", dist).formatted(Formatting.GRAY), true);
                    }
                }
                if (world instanceof ServerWorld sw) {
                    trySpawnDeliveryWhenNearby(sw, player, stack);
                }
            }
        }
    }

    public static void trySpawnDeliveryWhenNearby(ServerWorld world, PlayerEntity player, ItemStack stack) {
        if (!(stack.getItem() instanceof TakeoutBagItem)) {
            return;
        }

        NbtCompound nbt = DataCompat.copy(stack);
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

        NbtCompound posNbt = nbt.getCompound(ModConstants.NBT_DELIVERY_POS);
        int targetX = posNbt.getInt("x");
        int targetZ = posNbt.getInt("z");
        double distSq = Math.pow(player.getX() - targetX, 2) + Math.pow(player.getZ() - targetZ, 2);
        if (distSq > (48 * 48)) {
            return;
        }

        BlockPos target = new BlockPos(targetX, world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ), targetZ);
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
            player.sendMessage(net.minecraft.text.Text.translatable("message.ordertocook.npc_respawn_fail_settle", customer).formatted(net.minecraft.util.Formatting.AQUA), false);
            settleDirectly(world, player, stack, nbt);
            return;
        }

        nbt.putBoolean("delivery_spawned", true);
        DataCompat.set(stack, nbt);

        world.getPlayers(p -> p.squaredDistanceTo(target.getX(), target.getY(), target.getZ()) < 100 * 100).forEach(p -> {
            if (ConfigManager.isDevModeEnabled()) {
                p.sendMessage(Text.literal("[OTC Dev] NPC Spawned dynamically at " + target.toShortString()).formatted(Formatting.AQUA), false);
            }
        });
    }

    /** 与 fabric-1.21.1 TakeoutBagItem#resolveCompletionAnimation 一致；影响 {@link OrderNpcManager#beginCompletedAnimation} 选用的动画。 */
    private static OrderNpcManager.CompletionAnimation resolveCompletionAnimation(ServerWorld world, PlayerEntity player, ItemStack stack, NbtCompound nbt, Entity npc) {
        if (!(npc instanceof cn.breezeth.ordertocook.entity.CustomerEntity ce)) {
            return OrderNpcManager.CompletionAnimation.STAND_TAKE;
        }
        boolean chairCustomer = ce.isChairCustomer();
        if (chairCustomer && stack.getItem() instanceof FoodPlateItem) {
            double boomChance = ce.isEasterEggCustomer() ? 0.6D : 0.1D;
            boolean useBoom = world.random.nextDouble() < boomChance;
            prepareBoomBonus(world, player, nbt, ce, useBoom);
            return useBoom ? OrderNpcManager.CompletionAnimation.EAT_BOOM : OrderNpcManager.CompletionAnimation.EAT_SCALE;
        }
        if (chairCustomer) {
            return OrderNpcManager.CompletionAnimation.SIT_TAKE;
        }
        return OrderNpcManager.CompletionAnimation.STAND_TAKE;
    }

    private static void prepareBoomBonus(ServerWorld world, PlayerEntity player, NbtCompound nbt, cn.breezeth.ordertocook.entity.CustomerEntity customerEntity, boolean useBoom) {
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
            customerName = Text.translatable("keyword.ordertocook.customer").getString();
        }
        String restaurantName = resolveRestaurantName(world, nbt, machineId);
        customerEntity.setPendingBoomBonus(player.getUuidAsString(), machineId, isDelivery, isLongDistance, deliveryDist, isWalkIn, restaurantName, customerName, bonusCoin);
    }

    private static int resolveBoomBonusCoin(ServerWorld world, int machineId) {
        var stats = machineId > 0 ? cn.breezeth.ordertocook.core.RestaurantRegistry.getStatsById(world, machineId) : null;
        int level = stats != null ? stats.level() : 0;
        return 1 + world.random.nextInt(4) + level;
    }

    /** 解析餐厅名逻辑与 fabric-1.21.1 同名方法一致；维度键使用 1.20.1 的 RegistryKey/Identifier API。 */
    private static String resolveRestaurantName(ServerWorld world, NbtCompound nbt, int machineId) {
        var stats = machineId > 0 ? cn.breezeth.ordertocook.core.RestaurantRegistry.getStatsById(world, machineId) : null;
        if (stats != null && stats.name() != null && !stats.name().isBlank()) {
            return stats.name();
        }
        if (nbt.contains(ModConstants.NBT_MACHINE_POS) && nbt.contains(ModConstants.NBT_MACHINE_DIM)) {
            try {
                net.minecraft.util.Identifier dimensionId = new net.minecraft.util.Identifier(nbt.getString(ModConstants.NBT_MACHINE_DIM));
                net.minecraft.registry.RegistryKey<net.minecraft.world.World> worldKey = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, dimensionId);
                ServerWorld machineWorld = world.getServer() != null ? world.getServer().getWorld(worldKey) : null;
                if (machineWorld != null) {
                    net.minecraft.util.math.BlockPos machinePos = net.minecraft.util.math.BlockPos.fromLong(nbt.getLong(ModConstants.NBT_MACHINE_POS));
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
        return Text.translatable("keyword.ordertocook.unknown").getString();
    }

    private static boolean isExpired(World world, NbtCompound nbt) {
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) {
            long expiryTick = nbt.getLong(ModConstants.NBT_EXPIRY_TICK);
            return world.getTime() >= expiryTick;
        }
        if (nbt.contains(ModConstants.NBT_EXPIRY_TIME)) {
            return System.currentTimeMillis() >= nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
        }
        return true;
    }

    private static boolean ensureExpiryTick(World world, ItemStack stack, NbtCompound nbt) {
        if (nbt.contains(ModConstants.NBT_EXPIRY_TICK)) return false;
        if (!nbt.contains(ModConstants.NBT_EXPIRY_TIME)) return false;

        long expiryTime = nbt.getLong(ModConstants.NBT_EXPIRY_TIME);
        long remainingMs = expiryTime - System.currentTimeMillis();
        long expiryTick;
        if (remainingMs <= 0) {
            expiryTick = world.getTime() - 1;
        } else {
            long remainingTicks = (remainingMs + 49) / 50;
            expiryTick = world.getTime() + remainingTicks;
        }

        nbt.putLong(ModConstants.NBT_EXPIRY_TICK, expiryTick);
        DataCompat.set(stack, nbt);
        return true;
    }
}
