package cn.breezeth.ordertocook.entity;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.core.OrderNpcManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CustomerEntity extends ZombieEntity implements GeoEntity {
    private static final TrackedData<Integer> TEXTURE_VARIANT = DataTracker.registerData(CustomerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<String> SKIN_ACCOUNT = DataTracker.registerData(CustomerEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> SKIN_UUID = DataTracker.registerData(CustomerEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> EASTER_EGG = DataTracker.registerData(CustomerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ANIMATION_VARIANT = DataTracker.registerData(CustomerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<String> CUSTOMER_ID = DataTracker.registerData(CustomerEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Integer> ACTION_STATE = DataTracker.registerData(CustomerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final int ACTION_NONE = 0;
    private static final int ACTION_SIT_SPAWN = 1;
    private static final int ACTION_STAND_SPAWN = 2;
    private static final int ACTION_SIT_TAKE = 3;
    private static final int ACTION_STAND_TAKE = 4;
    private static final int ACTION_EAT_BOOM = 5;
    private static final int ACTION_EAT_SCALE = 6;
    private static final int ACTION_EAT_SCALE_INTERRUPT = 7;
    private static final int SIT_SPAWN_TICKS = 12;
    private static final int STAND_SPAWN_TICKS = 12;
    private static final int SIT_TAKE_TICKS = 40;
    private static final int STAND_TAKE_TICKS = 60;
    private static final int EAT_BOOM_TICKS = 120;
    private static final int EAT_SCALE_TICKS = 110;
    private static final int EAT_SCALE_INTERRUPT_TICKS = 10;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int currentCycleVariant = 0;
    private boolean currentCycleChairMode = false;
    private int currentActionTicks = 0;
    private boolean currentActionSignalTriggered = false;
    private boolean currentActionFinishTriggered = false;
    private int lastSpecialAction = ACTION_NONE;
    private boolean pendingBoomBonus = false;
    private int pendingBoomBonusCoin = 0;
    private int pendingBoomMachineId = 0;
    private boolean pendingBoomDelivery = false;
    private boolean pendingBoomLongDistance = false;
    private int pendingBoomDeliveryDistance = 0;
    private boolean pendingBoomWalkIn = false;
    private String pendingBoomRestaurantName = "";
    private String pendingBoomCustomerName = "";
    private String pendingBoomPlayerUuid = "";

    public CustomerEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
        this.setCanPickUpLoot(false);
    }

    public static DefaultAttributeContainer.Builder createCustomerAttributes() {
        return ZombieEntity.createZombieAttributes()
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(9, new LookAroundGoal(this));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(TEXTURE_VARIANT, 1);
        builder.add(SKIN_ACCOUNT, "");
        builder.add(SKIN_UUID, "");
        builder.add(EASTER_EGG, false);
        builder.add(ANIMATION_VARIANT, 1);
        builder.add(CUSTOMER_ID, "");
        builder.add(ACTION_STATE, ACTION_NONE);
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (isEatingActionActive()) {
            faceFoodDuringEat();
        }
        if (!this.getWorld().isClient()) {
            this.setTarget(null);
            tickServerAction();
        }
    }

    @Override
    public boolean burnsInDaylight() {
        return false;
    }

    @Override
    public boolean isDisallowedInPeaceful() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    public void setTextureVariant(int variant) {
        this.dataTracker.set(TEXTURE_VARIANT, Math.max(1, variant));
    }

    public int getTextureVariant() {
        return this.dataTracker.get(TEXTURE_VARIANT);
    }

    public void setSkinAccount(String account) {
        this.dataTracker.set(SKIN_ACCOUNT, account == null ? "" : account);
    }

    public String getSkinAccount() {
        return this.dataTracker.get(SKIN_ACCOUNT);
    }

    public void setSkinUuid(String skinUuid) {
        this.dataTracker.set(SKIN_UUID, skinUuid == null ? "" : skinUuid);
    }

    public String getSkinUuid() {
        return this.dataTracker.get(SKIN_UUID);
    }

    public boolean usesPlayerSkin() {
        return !getSkinAccount().isBlank();
    }

    public void setEasterEgg(boolean easterEgg) {
        this.dataTracker.set(EASTER_EGG, easterEgg);
    }

    public boolean isEasterEggCustomer() {
        return this.dataTracker.get(EASTER_EGG);
    }

    public void setAnimationVariant(int variant) {
        this.dataTracker.set(ANIMATION_VARIANT, Math.max(1, Math.min(4, variant)));
    }

    public int getAnimationVariant() {
        return this.dataTracker.get(ANIMATION_VARIANT);
    }

    public void setCustomerId(String customerId) {
        this.dataTracker.set(CUSTOMER_ID, customerId == null ? "" : customerId);
    }

    public String getCustomerId() {
        return this.dataTracker.get(CUSTOMER_ID);
    }

    public int getActionState() {
        return this.dataTracker.get(ACTION_STATE);
    }

    public boolean isTransitionAnimationActive() {
        return getActionState() != ACTION_NONE;
    }

    public void startSitSpawnAnimation() {
        startAction(ACTION_SIT_SPAWN, SIT_SPAWN_TICKS);
    }

    public void startStandSpawnAnimation() {
        startAction(ACTION_STAND_SPAWN, STAND_SPAWN_TICKS);
    }

    public void startSitTakeAnimation() {
        startAction(ACTION_SIT_TAKE, SIT_TAKE_TICKS);
    }

    public void startStandTakeAnimation() {
        startAction(ACTION_STAND_TAKE, STAND_TAKE_TICKS);
    }

    public void startEatBoomAnimation() {
        startAction(ACTION_EAT_BOOM, EAT_BOOM_TICKS);
    }

    public void startEatScaleAnimation() {
        startAction(ACTION_EAT_SCALE, EAT_SCALE_TICKS);
    }

    public void startInterruptedEatScaleAnimation() {
        startAction(ACTION_EAT_SCALE_INTERRUPT, EAT_SCALE_INTERRUPT_TICKS);
    }

    public void setPendingBoomBonus(String playerUuid, int machineId, boolean delivery, boolean longDistance, int deliveryDistance, boolean walkIn, String restaurantName, String customerName, int bonusCoin) {
        this.pendingBoomBonus = bonusCoin > 0;
        this.pendingBoomPlayerUuid = playerUuid == null ? "" : playerUuid;
        this.pendingBoomMachineId = machineId;
        this.pendingBoomDelivery = delivery;
        this.pendingBoomLongDistance = longDistance;
        this.pendingBoomDeliveryDistance = deliveryDistance;
        this.pendingBoomWalkIn = walkIn;
        this.pendingBoomRestaurantName = restaurantName == null ? "" : restaurantName;
        this.pendingBoomCustomerName = customerName == null ? "" : customerName;
        this.pendingBoomBonusCoin = Math.max(0, bonusCoin);
    }

    public void assignRandomAnimationVariant(Random random) {
        setAnimationVariant(rollWeightedAnimationVariant(random));
        this.currentCycleVariant = getAnimationVariant();
    }

    public boolean isChairCustomer() {
        return this.getVehicle() instanceof SeatEntity;
    }

    public boolean isEatingActionActive() {
        int actionState = getActionState();
        return actionState == ACTION_EAT_BOOM || actionState == ACTION_EAT_SCALE || actionState == ACTION_EAT_SCALE_INTERRUPT;
    }

    private void faceFoodDuringEat() {
        var platePos = OrderNpcManager.getCustomerPlateDisplayPos(this);
        if (platePos == null) {
            return;
        }
        double dx = platePos.getX() + 0.5D - this.getX();
        double dz = platePos.getZ() + 0.5D - this.getZ();
        if (dx * dx + dz * dz < 1.0E-6D) {
            return;
        }
        float yaw = (float) (MathHelper.atan2(dz, dx) * 57.2957763671875D) - 90.0F;
        this.setYaw(yaw);
        this.prevYaw = yaw;
        this.bodyYaw = yaw;
        this.prevBodyYaw = yaw;
        this.setHeadYaw(yaw);
        this.prevHeadYaw = yaw;
    }

    private void startAction(int actionState, int durationTicks) {
        this.dataTracker.set(ACTION_STATE, actionState);
        this.currentActionTicks = durationTicks;
        this.currentActionSignalTriggered = false;
        this.currentActionFinishTriggered = false;
    }

    private int rollWeightedAnimationVariant(Random random) {
        double roll = random.nextDouble();
        if (roll < 0.55D) {
            return 1;
        }
        if (roll < 0.70D) {
            return 2;
        }
        if (roll < 0.85D) {
            return 3;
        }
        return 4;
    }

    private void rotateAnimationCycle(boolean chairMode) {
        this.currentCycleVariant = rollWeightedAnimationVariant(this.random);
        this.currentCycleChairMode = chairMode;
    }

    private String getCurrentAnimationName(boolean chairMode) {
        if (this.currentCycleVariant <= 0 || this.currentCycleChairMode != chairMode) {
            this.currentCycleVariant = Math.max(1, getAnimationVariant());
            this.currentCycleChairMode = chairMode;
        }
        return (chairMode ? "customer_sit_" : "customer_wait_") + this.currentCycleVariant;
    }

    private String getActionAnimationName(int actionState) {
        return switch (actionState) {
            case ACTION_SIT_SPAWN -> "npc_sitspawn";
            case ACTION_STAND_SPAWN -> "npc_standspawn";
            case ACTION_SIT_TAKE -> "npc_sittake";
            case ACTION_STAND_TAKE -> "npc_standtake";
            case ACTION_EAT_BOOM -> "npc_eat&boom";
            case ACTION_EAT_SCALE -> "npc_eat&scale";
            case ACTION_EAT_SCALE_INTERRUPT -> "npc_eat&scale_interrupt";
            default -> "";
        };
    }

    private void tickServerAction() {
        int actionState = getActionState();
        if (actionState == ACTION_NONE || this.currentActionTicks <= 0) {
            return;
        }
        int totalTicks = getActionDurationTicks(actionState);
        int elapsedTicks = totalTicks - this.currentActionTicks;
        if (actionState == ACTION_EAT_BOOM || actionState == ACTION_EAT_SCALE || actionState == ACTION_EAT_SCALE_INTERRUPT) {
            tickEatEffects(actionState, elapsedTicks);
        } else if (actionState == ACTION_SIT_TAKE || actionState == ACTION_STAND_TAKE) {
            tickTeleportEffects(actionState, elapsedTicks);
        }
        this.currentActionTicks--;
        if (this.currentActionTicks > 0) {
            return;
        }
        if (actionState == ACTION_SIT_SPAWN || actionState == ACTION_STAND_SPAWN) {
            this.dataTracker.set(ACTION_STATE, ACTION_NONE);
            return;
        }
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            if (actionState == ACTION_EAT_BOOM) {
                grantPendingBoomBonus(serverWorld);
            }
            OrderNpcManager.finishAnimatedDeparture(serverWorld, this);
        }
    }

    private int getActionDurationTicks(int actionState) {
        return switch (actionState) {
            case ACTION_SIT_SPAWN -> SIT_SPAWN_TICKS;
            case ACTION_STAND_SPAWN -> STAND_SPAWN_TICKS;
            case ACTION_SIT_TAKE -> SIT_TAKE_TICKS;
            case ACTION_STAND_TAKE -> STAND_TAKE_TICKS;
            case ACTION_EAT_BOOM -> EAT_BOOM_TICKS;
            case ACTION_EAT_SCALE -> EAT_SCALE_TICKS;
            case ACTION_EAT_SCALE_INTERRUPT -> EAT_SCALE_INTERRUPT_TICKS;
            default -> 0;
        };
    }

    private void tickTeleportEffects(int actionState, int elapsedTicks) {
        int triggerTick = actionState == ACTION_SIT_TAKE ? 35 : 55;
        if (this.currentActionSignalTriggered || elapsedTicks < triggerTick || !(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        this.currentActionSignalTriggered = true;
        serverWorld.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 1.0f, 1.0f);
        serverWorld.spawnParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 0.8, this.getZ(), 40, 0.35, 0.5, 0.35, 0.2);
    }

    private void tickEatEffects(int actionState, int elapsedTicks) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        if (elapsedTicks >= 45 && elapsedTicks < 95 && elapsedTicks % 7 == 0) {
            serverWorld.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.NEUTRAL, 0.7f, 0.9f + this.random.nextFloat() * 0.2f);
        }
        if (actionState == ACTION_EAT_SCALE || actionState == ACTION_EAT_SCALE_INTERRUPT) {
            int triggerTick = actionState == ACTION_EAT_SCALE_INTERRUPT ? 5 : 105;
            if (!this.currentActionSignalTriggered && elapsedTicks >= triggerTick) {
                this.currentActionSignalTriggered = true;
                serverWorld.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 1.0f, 1.0f);
                serverWorld.spawnParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 0.8, this.getZ(), 40, 0.35, 0.5, 0.35, 0.2);
            }
            return;
        }
        if (actionState != ACTION_EAT_BOOM) {
            return;
        }
        if (!this.currentActionSignalTriggered && elapsedTicks >= 97) {
            this.currentActionSignalTriggered = true;
            serverWorld.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.NEUTRAL, 0.9f, 1.0f);
        }
        if (!this.currentActionFinishTriggered && elapsedTicks >= 118) {
            this.currentActionFinishTriggered = true;
            serverWorld.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.NEUTRAL, 1.0f, 1.0f);
            serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 0.8, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            serverWorld.spawnParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.8, this.getZ(), 18, 0.3, 0.4, 0.3, 0.05);
        }
    }

    private void grantPendingBoomBonus(ServerWorld world) {
        if (!this.pendingBoomBonus || this.pendingBoomBonusCoin <= 0 || world.getServer() == null) {
            clearPendingBoomBonus();
            return;
        }
        try {
            PlayerEntity player = this.pendingBoomPlayerUuid.isBlank() ? null : world.getServer().getPlayerManager().getPlayer(java.util.UUID.fromString(this.pendingBoomPlayerUuid));
            if (player != null) {
                cn.breezeth.ordertocook.util.CoinUtils.giveCoins(player, this.pendingBoomBonusCoin);
                cn.breezeth.ordertocook.core.PrestigeManager.addPlayerPrestige(player, this.pendingBoomBonusCoin);
                if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                    int totalEarned = cn.breezeth.ordertocook.core.PrestigeManager.getPlayerPrestige(player);
                    cn.breezeth.ordertocook.registry.ModCriteria.TOTAL_COIN.trigger(serverPlayer, totalEarned);
                }
            }
            if (this.pendingBoomMachineId > 0) {
                cn.breezeth.ordertocook.core.RestaurantRegistry.applyProfitBonusById(world, this.pendingBoomMachineId, this.pendingBoomBonusCoin, this.pendingBoomDelivery, this.pendingBoomLongDistance, this.pendingBoomDeliveryDistance, this.pendingBoomWalkIn);
            }
            String restaurantName = this.pendingBoomRestaurantName.isBlank()
                    ? Text.translatable("keyword.ordertocook.unknown").getString()
                    : this.pendingBoomRestaurantName;
            String customerName = this.pendingBoomCustomerName.isBlank()
                    ? Text.translatable("keyword.ordertocook.customer").getString()
                    : this.pendingBoomCustomerName;
            world.getServer().getPlayerManager().broadcast(Text.translatable(
                    "message.ordertocook.order_boom_bonus",
                    restaurantName,
                    customerName,
                    this.pendingBoomBonusCoin
            ).formatted(net.minecraft.util.Formatting.GOLD), false);
        } catch (IllegalArgumentException ignored) {
        } finally {
            clearPendingBoomBonus();
        }
    }

    private void clearPendingBoomBonus() {
        this.pendingBoomBonus = false;
        this.pendingBoomBonusCoin = 0;
        this.pendingBoomMachineId = 0;
        this.pendingBoomDelivery = false;
        this.pendingBoomLongDistance = false;
        this.pendingBoomDeliveryDistance = 0;
        this.pendingBoomWalkIn = false;
        this.pendingBoomRestaurantName = "";
        this.pendingBoomCustomerName = "";
        this.pendingBoomPlayerUuid = "";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, state -> {
            AnimationController<CustomerEntity> controller = state.getController();
            int specialAction = getActionState();
            if (specialAction != ACTION_NONE) {
                if (this.lastSpecialAction != specialAction) {
                    this.lastSpecialAction = specialAction;
                    controller.forceAnimationReset();
                }
                state.setAndContinue(RawAnimation.begin().thenPlay(getActionAnimationName(specialAction)));
                return PlayState.CONTINUE;
            }
            if (this.lastSpecialAction != ACTION_NONE) {
                this.lastSpecialAction = ACTION_NONE;
                controller.forceAnimationReset();
            }
            boolean chairMode = this.isChairCustomer();
            if (controller.getCurrentRawAnimation() == null || this.currentCycleChairMode != chairMode) {
                this.currentCycleVariant = Math.max(1, getAnimationVariant());
                this.currentCycleChairMode = chairMode;
                controller.forceAnimationReset();
            } else if (controller.hasAnimationFinished()) {
                rotateAnimationCycle(chairMode);
                controller.forceAnimationReset();
            }
            state.setAndContinue(RawAnimation.begin().thenPlay(getCurrentAnimationName(chairMode)));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return this.age;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt(ModConstants.NBT_CUSTOMER_TEXTURE_VARIANT, getTextureVariant());
        nbt.putString(ModConstants.NBT_CUSTOMER_SKIN_ACCOUNT, getSkinAccount());
        nbt.putString(ModConstants.NBT_CUSTOMER_SKIN_UUID, getSkinUuid());
        nbt.putBoolean(ModConstants.NBT_CUSTOMER_EASTER_EGG, isEasterEggCustomer());
        nbt.putInt("AnimationVariant", getAnimationVariant());
        nbt.putString(ModConstants.NBT_CUSTOMER_ID, getCustomerId());
        nbt.putInt("ActionState", getActionState());
        nbt.putInt("ActionTicks", this.currentActionTicks);
        nbt.putBoolean("PendingBoomBonus", this.pendingBoomBonus);
        nbt.putInt("PendingBoomBonusCoin", this.pendingBoomBonusCoin);
        nbt.putInt("PendingBoomMachineId", this.pendingBoomMachineId);
        nbt.putBoolean("PendingBoomDelivery", this.pendingBoomDelivery);
        nbt.putBoolean("PendingBoomLongDistance", this.pendingBoomLongDistance);
        nbt.putInt("PendingBoomDeliveryDistance", this.pendingBoomDeliveryDistance);
        nbt.putBoolean("PendingBoomWalkIn", this.pendingBoomWalkIn);
        nbt.putString("PendingBoomRestaurantName", this.pendingBoomRestaurantName);
        nbt.putString("PendingBoomCustomerName", this.pendingBoomCustomerName);
        nbt.putString("PendingBoomPlayerUuid", this.pendingBoomPlayerUuid);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains(ModConstants.NBT_CUSTOMER_TEXTURE_VARIANT)) {
            setTextureVariant(nbt.getInt(ModConstants.NBT_CUSTOMER_TEXTURE_VARIANT));
        }
        if (nbt.contains(ModConstants.NBT_CUSTOMER_SKIN_ACCOUNT)) {
            setSkinAccount(nbt.getString(ModConstants.NBT_CUSTOMER_SKIN_ACCOUNT));
        }
        if (nbt.contains(ModConstants.NBT_CUSTOMER_SKIN_UUID)) {
            setSkinUuid(nbt.getString(ModConstants.NBT_CUSTOMER_SKIN_UUID));
        }
        setEasterEgg(nbt.contains(ModConstants.NBT_CUSTOMER_EASTER_EGG) && nbt.getBoolean(ModConstants.NBT_CUSTOMER_EASTER_EGG));
        if (nbt.contains("AnimationVariant")) {
            setAnimationVariant(nbt.getInt("AnimationVariant"));
        }
        if (nbt.contains(ModConstants.NBT_CUSTOMER_ID)) {
            setCustomerId(nbt.getString(ModConstants.NBT_CUSTOMER_ID));
        }
        if (nbt.contains("ActionState")) {
            this.dataTracker.set(ACTION_STATE, nbt.getInt("ActionState"));
        }
        if (nbt.contains("ActionTicks")) {
            this.currentActionTicks = nbt.getInt("ActionTicks");
        }
        this.pendingBoomBonus = nbt.contains("PendingBoomBonus") && nbt.getBoolean("PendingBoomBonus");
        this.pendingBoomBonusCoin = nbt.contains("PendingBoomBonusCoin") ? nbt.getInt("PendingBoomBonusCoin") : 0;
        this.pendingBoomMachineId = nbt.contains("PendingBoomMachineId") ? nbt.getInt("PendingBoomMachineId") : 0;
        this.pendingBoomDelivery = nbt.contains("PendingBoomDelivery") && nbt.getBoolean("PendingBoomDelivery");
        this.pendingBoomLongDistance = nbt.contains("PendingBoomLongDistance") && nbt.getBoolean("PendingBoomLongDistance");
        this.pendingBoomDeliveryDistance = nbt.contains("PendingBoomDeliveryDistance") ? nbt.getInt("PendingBoomDeliveryDistance") : 0;
        this.pendingBoomWalkIn = nbt.contains("PendingBoomWalkIn") && nbt.getBoolean("PendingBoomWalkIn");
        this.pendingBoomRestaurantName = nbt.contains("PendingBoomRestaurantName") ? nbt.getString("PendingBoomRestaurantName") : "";
        this.pendingBoomCustomerName = nbt.contains("PendingBoomCustomerName") ? nbt.getString("PendingBoomCustomerName") : "";
        this.pendingBoomPlayerUuid = nbt.contains("PendingBoomPlayerUuid") ? nbt.getString("PendingBoomPlayerUuid") : "";
    }
}
