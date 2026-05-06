package cn.breezeth.ordertocook.core;

import cn.breezeth.ordertocook.OrderToCookMod;
import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.config.ModConfig;
import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.util.DataCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

public class OrderGenerator {
    public static ItemStack generateWalkInOrder(ServerLevel world, BlockPos pos, int level, long spawnTick, String customerName) {
        return generateWalkInOrder(world, pos, level, spawnTick, customerName, List.of());
    }

    public static ItemStack generateWalkInOrder(ServerLevel world, BlockPos pos, int level, long spawnTick, String customerName, List<Item> menuFoods) {
        ItemStack order = new ItemStack(ModItems.ORDER.get());
        CompoundTag nbt = new CompoundTag();
        ModConfig config = ConfigManager.get();

        // 订单类型判定（按权重随等级动态调整）
        int type = determineOrderType(world.random, config, level);
        
        // 到店订单：不外卖，始终加急
        boolean delivery = false; 
        boolean urgent = true;
        
        long durationTicks = ModConstants.URGENT_DURATION_TICKS;
        long expiryTick = spawnTick + durationTicks;
        
        boolean isLongDistance = false;
        
        // 生成菜品列表（去重后按类型取若干种）
        generateFoodList(world.random, type, nbt, menuFoods);
        
        // 计算奖励与饥饿值加成
        int totalCoin = calculateCoin(type, urgent, delivery, isLongDistance, level);
        int hunger = computeOrderHunger(nbt);
        totalCoin += (int) Math.ceil(hunger * baseHungerBonusRate(level));
        
        String customer = customerName;
        nbt.putString(ModConstants.NBT_ORDER_ID, OtcRuntimeIdState.get(world).allocateOrderId());
        nbt.putString(ModConstants.NBT_CUSTOMER_NAME, customer);
        nbt.putString(ModConstants.NBT_CUSTOMER_ID, OtcRuntimeIdState.get(world).allocateCustomerId());
        nbt.putInt(ModConstants.NBT_TYPE, type);
        nbt.putBoolean(ModConstants.NBT_DELIVERY, delivery);
        nbt.putBoolean(ModConstants.NBT_IS_LONG_DISTANCE, isLongDistance);
        nbt.putBoolean(ModConstants.NBT_URGENT, urgent);
        nbt.putLong(ModConstants.NBT_EXPIRY_TICK, expiryTick);
        long remainTicks = Math.max(0L, expiryTick - world.getGameTime());
        nbt.putLong(ModConstants.NBT_EXPIRY_TIME, System.currentTimeMillis() + remainTicks * 50L);
        nbt.putInt(ModConstants.NBT_PRESTIGE, totalCoin);
        nbt.putInt(ModConstants.NBT_ORDER_TYPE, 1);
        CustomerProfileLibrary.writeToNbt(nbt, CustomerProfileLibrary.createWalkInProfile(world, customer));
        
        DataCompat.set(order, nbt);
        setOrderName(order, customer, type, delivery, urgent, isLongDistance);
        
        return order;
    }

    public static ItemStack generateRandomOrder(ServerLevel world, BlockPos pos, int level) {
        return generateRandomOrder(world, pos, level, List.of());
    }

    public static ItemStack generateRandomOrder(ServerLevel world, BlockPos pos, int level, List<Item> menuFoods) {
        ItemStack order = new ItemStack(ModItems.ORDER.get());
        CompoundTag nbt = new CompoundTag();
        ModConfig config = ConfigManager.get();

        // 订单类型（按权重随机）
        int type = determineOrderType(world.random, config, level);

        boolean delivery = world.random.nextDouble() < config.deliveryRate;
        boolean urgent = world.random.nextDouble() < Math.min(1.0, config.urgentRate + urgentBonus(level));

        long nowTick = world.getGameTime();
        long durationTicks = urgent ? ModConstants.URGENT_DURATION_TICKS : ModConstants.NORMAL_DURATION_TICKS;
        long expiryTick = nowTick + durationTicks;

        // 生成外卖坐标（避免海洋/河流/水面）
        boolean isLongDistance = false;
        if (delivery) {
            isLongDistance = world.random.nextDouble() < config.longDistanceDeliveryChance;
            generateDeliveryPosition(world, world.random, pos, nbt, isLongDistance);
        }

        // 按类型生成菜品列表
        generateFoodList(world.random, type, nbt, menuFoods);

        // 奖励计算（类型/加急/外卖与长距离倍率）
        int totalCoin = calculateCoin(type, urgent, delivery, isLongDistance, level);
        int hunger = computeOrderHunger(nbt);
        totalCoin += (int) Math.ceil(hunger * baseHungerBonusRate(level));

        CustomerProfileLibrary.CustomerProfile profile = CustomerProfileLibrary.createOrderProfile(world);
        String customer = profile.displayName();
        nbt.putString(ModConstants.NBT_ORDER_ID, OtcRuntimeIdState.get(world).allocateOrderId());
        nbt.putString(ModConstants.NBT_CUSTOMER_NAME, customer);
        nbt.putString(ModConstants.NBT_CUSTOMER_ID, OtcRuntimeIdState.get(world).allocateCustomerId());
        nbt.putInt(ModConstants.NBT_TYPE, type);
        nbt.putBoolean(ModConstants.NBT_DELIVERY, delivery);
        nbt.putBoolean(ModConstants.NBT_IS_LONG_DISTANCE, isLongDistance);
        nbt.putBoolean(ModConstants.NBT_URGENT, urgent);
        nbt.putLong(ModConstants.NBT_EXPIRY_TICK, expiryTick);
        nbt.putLong(ModConstants.NBT_EXPIRY_TIME, System.currentTimeMillis() + durationTicks * 50L);
        nbt.putInt(ModConstants.NBT_PRESTIGE, totalCoin); // Keep NBT key for compatibility
        if (delivery && nbt.contains(ModConstants.NBT_DELIVERY_DIST)) {
            nbt.putInt(ModConstants.NBT_ORDER_TYPE, nbt.getInt(ModConstants.NBT_DELIVERY_DIST));
        } else {
            nbt.putInt(ModConstants.NBT_ORDER_TYPE, 0);
        }
        CustomerProfileLibrary.writeToNbt(nbt, profile);

        DataCompat.set(order, nbt);
        setOrderName(order, customer, type, delivery, urgent, isLongDistance);

        return order;
    }

    public static int recalcPrestigeFromNbt(CompoundTag nbt, int level) {
        if (nbt == null) return 0;
        int type = nbt.contains(ModConstants.NBT_TYPE) ? nbt.getInt(ModConstants.NBT_TYPE) : 0;
        boolean urgent = nbt.getBoolean(ModConstants.NBT_URGENT);
        boolean delivery = nbt.getBoolean(ModConstants.NBT_DELIVERY);
        boolean isLongDistance = nbt.getBoolean(ModConstants.NBT_IS_LONG_DISTANCE);
        int totalCoin = calculateCoin(type, urgent, delivery, isLongDistance, level);
        int hunger = computeOrderHunger(nbt);
        totalCoin += (int) Math.ceil(hunger * baseHungerBonusRate(level));
        return totalCoin;
    }

    private static int determineOrderType(net.minecraft.util.RandomSource random, ModConfig config, int level) {
        int w0 = config.weightWhite;
        int w1 = config.weightGreen;
        int w2 = config.weightBlue;
        int w3 = config.weightPurple;
        int w4 = config.weightRed;

        int baseTotal = w0 + w1 + w2 + w3 + w4;
        if (baseTotal <= 0) baseTotal = 1;
        double purplePct = 0.0;
        double redPct = 0.0;
        switch (level) {
            case 3 -> { purplePct = 0.05; redPct = 0.02; }
            case 4 -> { purplePct = 0.05; redPct = 0.03; }
            case 5 -> { purplePct = 0.08; redPct = 0.03; }
            case 6 -> { purplePct = 0.10; redPct = 0.05; }
            case 7 -> { purplePct = 0.20; redPct = 0.10; }
            case 8 -> { purplePct = 0.20; redPct = 0.20; }
            default -> {
            }
        }
        if (purplePct > 0.0) {
            w3 += Math.max(1, (int) Math.round(baseTotal * purplePct));
        }
        if (redPct > 0.0) {
            w4 += Math.max(1, (int) Math.round(baseTotal * redPct));
        }

        int totalWeight = w0 + w1 + w2 + w3 + w4;
        int randomWeight = random.nextInt(totalWeight);

        int currentWeight = 0;
        if (randomWeight < (currentWeight += w0)) return 0;
        if (randomWeight < (currentWeight += w1)) return 1;
        if (randomWeight < (currentWeight += w2)) return 2;
        if (randomWeight < (currentWeight += w3)) return 3;
        return 4;
    }

    private static void generateDeliveryPosition(ServerLevel world, net.minecraft.util.RandomSource random, BlockPos pos, CompoundTag nbt, boolean isLongDistance) {
        CompoundTag deliveryPosNbt = new CompoundTag();
        int minDistance = isLongDistance ? ModConstants.LONG_MIN_DISTANCE : ModConstants.SHORT_MIN_DISTANCE;
        int maxDistance = isLongDistance ? ModConstants.LONG_MAX_DISTANCE : ModConstants.SHORT_MAX_DISTANCE;
        int range = maxDistance - minDistance;

        int attempts = ModConstants.DELIVERY_POSITION_ATTEMPTS;
        int targetX = pos.getX();
        int targetZ = pos.getZ();

        try {
            for (int i = 0; i < attempts; i++) {
                int signX = random.nextBoolean() ? 1 : -1;
                int signZ = random.nextBoolean() ? 1 : -1;
                int offsetX = signX * (minDistance + random.nextInt(range + 1));
                int offsetZ = signZ * (minDistance + random.nextInt(range + 1));
                int candX = pos.getX() + offsetX;
                int candZ = pos.getZ() + offsetZ;
                int topY = world.getHeight(Heightmap.Types.WORLD_SURFACE, candX, candZ);
                BlockPos top = new BlockPos(candX, topY, candZ);
                Holder<Biome> biomeEntry = world.getBiome(top);
                boolean badBiome = biomeEntry.unwrapKey().map(k -> {
                    String path = k.location().getPath();
                    return path.contains("ocean") || path.contains("river");
                }).orElse(false);
                boolean inWater = !world.getBlockState(top).getFluidState().isEmpty();
                if (!badBiome && !inWater) {
                    targetX = candX;
                    targetZ = candZ;
                    break;
                }
            }
        } catch (Throwable t) {
            OrderToCookMod.LOGGER.warn("Delivery position generation failed, fallback to machine pos", t);
        }

        deliveryPosNbt.putInt(ModConstants.NBT_X, targetX);
        deliveryPosNbt.putInt(ModConstants.NBT_Z, targetZ);
        nbt.put(ModConstants.NBT_DELIVERY_POS, deliveryPosNbt);
        int dx = targetX - pos.getX();
        int dz = targetZ - pos.getZ();
        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        nbt.putInt(ModConstants.NBT_DELIVERY_DIST, dist);
    }

    private static void generateFoodList(net.minecraft.util.RandomSource random, int type, CompoundTag nbt, List<Item> menuFoods) {
        int foodKinds = 1;
        int maxCount = 2;
        switch (type) {
            case 1 -> { foodKinds = 2; maxCount = 3; }
            case 2 -> { foodKinds = 3; maxCount = 4; }
            case 3 -> { foodKinds = 4; maxCount = 5; }
            case 4 -> { foodKinds = 6; maxCount = 6; }
        }

        List<Item> pool = (menuFoods == null) ? List.of() : menuFoods;
        if (pool.isEmpty()) {
            pool = List.of(Items.APPLE);
        }

        List<Item> unique = new ArrayList<>();
        HashSet<ResourceLocation> seen = new HashSet<>();
        for (Item i : pool) {
            if (i == null) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(i);
            if (id == null) continue;
            if (seen.add(id)) {
                unique.add(i);
            }
        }
        if (unique.isEmpty()) {
            unique.add(Items.APPLE);
        }
        Collections.shuffle(unique, new java.util.Random(random.nextLong()));

        CompoundTag foodListNbt = new CompoundTag();
        int kinds = Math.min(foodKinds, unique.size());
        for (int i = 0; i < kinds; i++) {
            Item food = unique.get(i);
            int count = random.nextInt(maxCount) + 1;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(food);
            foodListNbt.putInt(id.toString(), count);
        }
        nbt.put(ModConstants.NBT_FOOD_LIST, foodListNbt);
    }

    private static int calculateCoin(int type, boolean urgent, boolean delivery, boolean isLongDistance, int level) {
        ModConfig config = ConfigManager.get();
        int baseCoin = 1;
        if (type < config.defaultCoinArray.size()) {
            baseCoin = config.defaultCoinArray.get(type);
        }

        int totalCoin = baseCoin;
        if (urgent) {
            totalCoin += config.rushBonus;
        }

        if (delivery) {
            double multiplier;
            if (isLongDistance) {
                multiplier = config.longDistanceDeliveryMultiplier * (1.0 + longDistanceMultiplierBonus(level));
            } else {
                multiplier = config.deliveryMultiplier * (1.0 + longDistanceMultiplierBonus(level));
            }
            totalCoin = (int) Math.ceil(totalCoin * multiplier);
        }
        return totalCoin;
    }

    private static int computeOrderHunger(CompoundTag nbt) {
        if (nbt == null || !nbt.contains(ModConstants.NBT_FOOD_LIST)) return 0;
        CompoundTag foodList = nbt.getCompound(ModConstants.NBT_FOOD_LIST);
        int sum = 0;
        for (String key : foodList.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) continue;
            Item item = BuiltInRegistries.ITEM.get(id);
            FoodProperties fc = item.getFoodProperties();
            int nutrition = (fc != null) ? fc.getNutrition() : 0;
            if (nutrition <= 0) {
                nutrition = ConfigManager.getCustomMenuNutrition(item);
            }
            if (nutrition > 0) {
                sum += nutrition * foodList.getInt(key);
            }
        }
        return sum;
    }

    private static double baseHungerBonusRate(int level) {
        return switch (level) {
            case 1 -> 0.10;
            case 2 -> 0.15;
            case 3 -> 0.20;
            case 4 -> 0.25;
            case 5 -> 0.30;
            case 6 -> 0.40;
            case 7 -> 0.50;
            case 8 -> 0.75;
            default -> 0.0;
        };
    }

    private static double urgentBonus(int level) {
        return switch (level) {
            case 3 -> 0.05;
            case 4 -> 0.05;
            case 5 -> 0.10;
            case 6 -> 0.15;
            case 7 -> 0.25;
            case 8 -> 0.30;
            default -> 0.0;
        };
    }

    private static double longDistanceMultiplierBonus(int level) {
        return switch (level) {
            case 5 -> 0.30;
            case 6 -> 0.40;
            case 7 -> 0.50;
            case 8 -> 1.00;
            default -> 0.0;
        };
    }

    private static void setOrderName(ItemStack order, String customer, int type, boolean delivery, boolean urgent, boolean isLongDistance) {
        Component nameText;
        ChatFormatting color;

        switch (type) {
            case 1 -> { nameText = Component.translatable("order.ordertocook.name.type1"); color = ChatFormatting.GREEN; }
            case 2 -> { nameText = Component.translatable("order.ordertocook.name.type2"); color = ChatFormatting.BLUE; }
            case 3 -> { nameText = Component.translatable("order.ordertocook.name.type3"); color = ChatFormatting.LIGHT_PURPLE; }
            case 4 -> { nameText = Component.translatable("order.ordertocook.name.type4"); color = ChatFormatting.RED; }
            default -> { nameText = Component.translatable("order.ordertocook.name.type0"); color = ChatFormatting.WHITE; }
        }

        net.minecraft.network.chat.MutableComponent builder = Component.translatable("message.ordertocook.possessive", customer).append(nameText.copy().withStyle(color));
        if (delivery) {
            if (isLongDistance) {
                builder.append(Component.translatable("order.ordertocook.suffix.long_distance").withStyle(ChatFormatting.DARK_PURPLE));
            } else {
                builder.append(Component.translatable("order.ordertocook.suffix.delivery").withStyle(color));
            }
        }
        if (urgent) {
            builder.append(Component.translatable("order.ordertocook.suffix.urgent").withStyle(ChatFormatting.GOLD));
        }

        order.setHoverName(builder);
    }
}
