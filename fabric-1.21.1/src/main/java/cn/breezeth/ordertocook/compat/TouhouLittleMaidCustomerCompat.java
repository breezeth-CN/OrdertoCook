package cn.breezeth.ordertocook.compat;

import cn.breezeth.ordertocook.OrderToCookMod;
import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.core.CustomerProfileLibrary;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Method;

public final class TouhouLittleMaidCustomerCompat {
    public static final byte EAT_LEFT_ARM_EVENT = 120;
    public static final byte EAT_RIGHT_ARM_EVENT = 121;
    private static final String[] MOD_IDS = {
            "touhou_little_maid",
            "touhou_little_maid_orihime",
            "touhoulittlemaid_orihime"
    };
    private static final Identifier MAID_ID = Identifier.of("touhou_little_maid", "maid");
    private static final String TAG_ANIMATION_PREFIX = "otc_touhou_anim:";
    private static final String TAG_IDLE_SIT_1 = TAG_ANIMATION_PREFIX + "idle_sit_1";
    private static Boolean loaded;

    public static boolean canUseFor(CustomerProfileLibrary.CustomerProfile profile) {
        return ConfigManager.get().touhouLittleMaidCustomerCompat
                && profile != null
                && !profile.easterEgg()
                && isLoaded()
                && Registries.ENTITY_TYPE.containsId(MAID_ID);
    }

    public static LivingEntity createMaid(ServerWorld world, Text name, float yaw) {
        EntityType<?> type = Registries.ENTITY_TYPE.get(MAID_ID);
        Entity entity = type.create(world);
        if (!(entity instanceof LivingEntity maid)) {
            return null;
        }
        maid.setYaw(yaw);
        maid.setHeadYaw(yaw);
        maid.setBodyYaw(yaw);
        maid.prevYaw = yaw;
        maid.setInvulnerable(true);
        maid.setSilent(true);
        maid.setCustomName(name);
        maid.setCustomNameVisible(true);
        if (maid instanceof MobEntity mob) {
            mob.setPersistent();
            mob.setAiDisabled(false);
        }
        if (maid.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED) != null) {
            maid.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.0);
        }
        if (maid.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE) != null) {
            maid.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(0.0);
        }
        invokeBoolean(maid, "setHomeModeEnable", false);
        invokeBoolean(maid, "setPickup", false);
        invokeBoolean(maid, "setRideable", false);
        invokeBoolean(maid, "setBegging", false);
        return maid;
    }

    public static void setSitting(LivingEntity entity, boolean sitting) {
        invokeBoolean(entity, "setInSittingPose", sitting);
        if (!sitting) {
            stopIdleAnimation(entity);
        }
    }

    public static boolean isTouhouMaid(LivingEntity entity) {
        return entity != null && MAID_ID.equals(Registries.ENTITY_TYPE.getId(entity.getType()));
    }

    public static void startCompletionAnimation(LivingEntity entity, boolean eating, boolean seated) {
        if (!isTouhouMaid(entity)) {
            return;
        }
        stopIdleAnimation(entity);
        invokeBoolean(entity, "setBegging", false);
        invokeBoolean(entity, "setSwingingArms", !eating);
        setSitting(entity, seated);
        if (eating) {
            entity.stopUsingItem();
            entity.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            entity.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    public static void tickCompletionAnimation(LivingEntity entity, boolean eating, boolean seated) {
        tickCompletionAnimation(entity, eating, seated, false);
    }

    public static void tickCompletionAnimation(LivingEntity entity, boolean eating, boolean seated, boolean leftHand) {
        if (!isTouhouMaid(entity)) {
            return;
        }
        maintainCompletionPose(entity, seated);
        if (eating) {
            invokeBoolean(entity, "setBegging", false);
            invokeBoolean(entity, "setSwingingArms", false);
        } else {
            invokeBoolean(entity, "setSwingingArms", true);
        }
        if (eating) {
            Hand hand = leftHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
            entity.stopUsingItem();
            entity.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            entity.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
            entity.swingHand(hand, true);
        }
    }

    public static void maintainCompletionPose(LivingEntity entity, boolean seated) {
        if (!isTouhouMaid(entity)) {
            return;
        }
        if (seated) {
            setSitting(entity, true);
        }
        invokeBoolean(entity, "setBegging", false);
    }

    public static void faceFood(LivingEntity entity, BlockPos foodPos) {
        if (!isTouhouMaid(entity) || foodPos == null) return;
        double dx = foodPos.getX() + 0.5D - entity.getX();
        double dz = foodPos.getZ() + 0.5D - entity.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (Math.atan2(entity.getEyeY() - (foodPos.getY() + 0.15D), horizontal) * 180.0D / Math.PI);
        entity.setYaw(yaw);
        entity.prevYaw = yaw;
        entity.setBodyYaw(yaw);
        entity.setHeadYaw(yaw);
        entity.setPitch(pitch);
        entity.prevPitch = pitch;
    }

    public static void stopCompletionAnimation(LivingEntity entity) {
        if (!isTouhouMaid(entity)) {
            return;
        }
        invokeBoolean(entity, "setBegging", false);
        invokeBoolean(entity, "setSwingingArms", false);
        entity.stopUsingItem();
        entity.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        entity.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
    }

    public static void startIdleSitAnimation(LivingEntity entity) {
        if (!isTouhouMaid(entity)) {
            return;
        }
        stopIdleAnimation(entity);
        entity.addCommandTag(TAG_IDLE_SIT_1);
        setSitting(entity, true);
        invokeBoolean(entity, "setBegging", false);
        invokeBoolean(entity, "setSwingingArms", false);
    }

    public static void tickIdleSitAnimation(LivingEntity entity) {
        if (!isTouhouMaid(entity) || !entity.getCommandTags().contains(TAG_IDLE_SIT_1)) {
            return;
        }
        setSitting(entity, true);
        invokeBoolean(entity, "setBegging", false);
    }

    public static void stopIdleAnimation(LivingEntity entity) {
        if (!isTouhouMaid(entity)) {
            return;
        }
        for (String tag : java.util.List.copyOf(entity.getCommandTags())) {
            if (tag.startsWith(TAG_ANIMATION_PREFIX)) {
                entity.removeCommandTag(tag);
            }
        }
        invokeBoolean(entity, "setBegging", false);
    }

    public static void initializeSpawnData(ServerWorld world, LivingEntity entity) {
        if (entity instanceof MobEntity mob) {
            mob.initialize(world, world.getLocalDifficulty(entity.getBlockPos()), SpawnReason.MOB_SUMMONED, null);
        }
    }

    private static boolean isLoaded() {
        if (loaded != null) {
            return loaded;
        }
        FabricLoader loader = FabricLoader.getInstance();
        for (String id : MOD_IDS) {
            if (loader.isModLoaded(id)) {
                loaded = true;
                return true;
            }
        }
        loaded = false;
        return false;
    }

    private static void invokeBoolean(Object target, String methodName, boolean value) {
        try {
            Method method = findBooleanMethod(target.getClass(), methodName);
            if (method == null) {
                OrderToCookMod.LOGGER.debug("Touhou Little Maid customer compat skipped {} on {}", methodName, target.getClass().getName());
                return;
            }
            method.invoke(target, value);
        } catch (ReflectiveOperationException | RuntimeException e) {
            OrderToCookMod.LOGGER.debug("Touhou Little Maid customer compat skipped {} on {}", methodName, target.getClass().getName());
        }
    }

    private static Method findBooleanMethod(Class<?> type, String methodName) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName, boolean.class);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private TouhouLittleMaidCustomerCompat() {
    }
}
