package cn.breezeth.ordertocook.compat;

import cn.breezeth.ordertocook.OrderToCookMod;
import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.core.CustomerProfileLibrary;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

public final class TouhouLittleMaidCustomerCompat {
    public static final byte EAT_LEFT_ARM_EVENT = 120;
    public static final byte EAT_RIGHT_ARM_EVENT = 121;
    private static final String[] MOD_IDS = {
            "touhou_little_maid",
            "touhou_little_maid_orihime",
            "touhoulittlemaid_orihime"
    };
    private static final ResourceLocation MAID_ID = ResourceLocation.fromNamespaceAndPath("touhou_little_maid", "maid");
    private static final String TAG_ANIMATION_PREFIX = "otc_touhou_anim:";
    private static final String TAG_IDLE_SIT_1 = TAG_ANIMATION_PREFIX + "idle_sit_1";
    private static Boolean loaded;

    public static boolean canUseFor(CustomerProfileLibrary.CustomerProfile profile) {
        return ConfigManager.get().touhouLittleMaidCustomerCompat
                && profile != null
                && !profile.easterEgg()
                && isLoaded()
                && BuiltInRegistries.ENTITY_TYPE.containsKey(MAID_ID);
    }

    public static LivingEntity createMaid(ServerLevel world, Component name, float yaw) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(MAID_ID);
        Entity entity = type.create(world);
        if (!(entity instanceof LivingEntity maid)) {
            return null;
        }
        maid.setYRot(yaw);
        maid.setYHeadRot(yaw);
        maid.setYBodyRot(yaw);
        maid.yRotO = yaw;
        maid.setInvulnerable(true);
        maid.setSilent(true);
        maid.setCustomName(name);
        maid.setCustomNameVisible(true);
        if (maid instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.setNoAi(false);
        }
        if (maid.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            maid.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
        }
        if (maid.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            maid.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
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
        return entity != null && MAID_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    public static void setRideableForManagedMount(LivingEntity entity, boolean rideable) {
        if (isTouhouMaid(entity)) {
            invokeBoolean(entity, "setRideable", rideable);
        }
    }

    public static void startCompletionAnimation(LivingEntity entity, boolean eating, boolean seated) {
        if (!isTouhouMaid(entity)) return;
        stopIdleAnimation(entity);
        invokeBoolean(entity, "setBegging", false);
        invokeBoolean(entity, "setSwingingArms", !eating);
        setSitting(entity, seated);
        if (eating) {
            entity.stopUsingItem();
            entity.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            entity.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    public static void tickCompletionAnimation(LivingEntity entity, boolean eating, boolean seated) {
        tickCompletionAnimation(entity, eating, seated, false);
    }

    public static void tickCompletionAnimation(LivingEntity entity, boolean eating, boolean seated, boolean leftHand) {
        if (!isTouhouMaid(entity)) return;
        maintainCompletionPose(entity, seated);
        invokeBoolean(entity, "setBegging", false);
        invokeBoolean(entity, "setSwingingArms", !eating);
        if (eating) {
            InteractionHand hand = leftHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            entity.stopUsingItem();
            entity.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            entity.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            entity.swing(hand, true);
        }
    }

    public static void maintainCompletionPose(LivingEntity entity, boolean seated) {
        if (!isTouhouMaid(entity)) return;
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
        entity.setYRot(yaw);
        entity.yRotO = yaw;
        entity.setYBodyRot(yaw);
        entity.yBodyRotO = yaw;
        entity.setYHeadRot(yaw);
        entity.yHeadRotO = yaw;
        entity.setXRot(pitch);
        entity.xRotO = pitch;
    }

    public static void stopCompletionAnimation(LivingEntity entity) {
        if (!isTouhouMaid(entity)) return;
        invokeBoolean(entity, "setBegging", false);
        invokeBoolean(entity, "setSwingingArms", false);
        entity.stopUsingItem();
        entity.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        entity.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
    }

    public static void startIdleSitAnimation(LivingEntity entity) {
        if (!isTouhouMaid(entity)) return;
        stopIdleAnimation(entity);
        entity.addTag(TAG_IDLE_SIT_1);
        setSitting(entity, true);
        invokeBoolean(entity, "setBegging", false);
        invokeBoolean(entity, "setSwingingArms", false);
    }

    public static void tickIdleSitAnimation(LivingEntity entity) {
        if (!isTouhouMaid(entity) || !entity.getTags().contains(TAG_IDLE_SIT_1)) return;
        setSitting(entity, true);
        invokeBoolean(entity, "setBegging", false);
    }

    public static void stopIdleAnimation(LivingEntity entity) {
        if (!isTouhouMaid(entity)) return;
        for (String tag : java.util.List.copyOf(entity.getTags())) {
            if (tag.startsWith(TAG_ANIMATION_PREFIX)) {
                entity.removeTag(tag);
            }
        }
        invokeBoolean(entity, "setBegging", false);
    }

    public static void initializeSpawnData(ServerLevel world, LivingEntity entity) {
        if (entity instanceof Mob mob) {
            DifficultyInstance difficulty = world.getCurrentDifficultyAt(entity.blockPosition());
            mob.finalizeSpawn(world, difficulty, MobSpawnType.MOB_SUMMONED, null);
        }
    }

    private static boolean isLoaded() {
        if (loaded != null) return loaded;
        ModList mods = ModList.get();
        for (String id : MOD_IDS) {
            if (mods.isLoaded(id)) {
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
