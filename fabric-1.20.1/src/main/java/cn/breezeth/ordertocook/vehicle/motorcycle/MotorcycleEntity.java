package cn.breezeth.ordertocook.vehicle.motorcycle;

import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.item.MotorcycleItem;
import cn.breezeth.ordertocook.registry.ModEntities;
import cn.breezeth.ordertocook.util.ImplementedInventory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MotorcycleEntity extends Entity implements NamedScreenHandlerFactory, ImplementedInventory, GeoEntity {
    public static final float WIDTH = 1.2f;
    public static final float HEIGHT = 1.2f;
    public static final double SEAT_OFF_X = 0.0;
    public static final double SEAT_OFF_Y = 0.6;
    public static final double SEAT_OFF_Z = 0.45;

    private static final int INVENTORY_SIZE = 54;

    private static final TrackedData<Boolean> LIGHT_ENABLED = DataTracker.registerData(MotorcycleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> MOTORCYCLE_COLOR = DataTracker.registerData(MotorcycleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> STEERING = DataTracker.registerData(MotorcycleEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> SPEED = DataTracker.registerData(MotorcycleEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);

    private float targetSteering;
    private float handleAngle;
    private float prevHandleAngle;
    private float wheelAngle;
    private float prevWheelAngle;
    private float internalSpeed;
    private int hitCount;
    private int driveSoundCooldown;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MotorcycleEntity(EntityType<? extends Entity> type, World world) {
        super(type, world);
        this.targetSteering = 0.0f;
        this.handleAngle = 0.0f;
        this.prevHandleAngle = 0.0f;
        this.wheelAngle = 0.0f;
        this.prevWheelAngle = 0.0f;
        this.internalSpeed = 0.0f;
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(LIGHT_ENABLED, true);
        this.dataTracker.startTracking(MOTORCYCLE_COLOR, 0);
        this.dataTracker.startTracking(STEERING, 0.0f);
        this.dataTracker.startTracking(SPEED, 0.0f);
    }

    public static MotorcycleEntity create(World world, double x, double y, double z) {
        MotorcycleEntity motorcycle = new MotorcycleEntity(ModEntities.MOTORCYCLE, world);
        motorcycle.setPosition(x, y, z);
        motorcycle.setVelocity(Vec3d.ZERO);
        motorcycle.prevX = x;
        motorcycle.prevY = y;
        motorcycle.prevZ = z;
        return motorcycle;
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return !player.isRemoved() && player.squaredDistanceTo(this) < 8.0 * 8.0;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("MotorcycleColor", getMotorcycleColor());
        nbt.putInt("Color", getColor());
        nbt.putBoolean("HeadlightOn", isHeadlightOn());
        nbt.putBoolean("LightEnabled", isHeadlightOn());
        nbt.putFloat("Steering", getSteering());
        nbt.putFloat("Speed", getSpeed());
        NbtCompound inventoryNbt = new NbtCompound();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty()) {
                inventoryNbt.put("Slot" + i, stack.writeNbt(new NbtCompound()));
            }
        }
        nbt.put("Inventory", inventoryNbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("MotorcycleColor")) {
            setMotorcycleColor(nbt.getInt("MotorcycleColor"));
        } else if (nbt.contains("Color")) {
            setMotorcycleColor(MotorcycleItem.semanticFromLegacyDyeId(nbt.getInt("Color")));
        } else {
            setMotorcycleColor(0);
        }
        if (nbt.contains("LightEnabled")) {
            setHeadlightOn(nbt.getBoolean("LightEnabled"));
        } else if (nbt.contains("HeadlightOn")) {
            setHeadlightOn(nbt.getBoolean("HeadlightOn"));
        } else {
            setHeadlightOn(true);
        }
        if (nbt.contains("Steering")) {
            setSteering(nbt.getFloat("Steering"));
        }
        if (nbt.contains("Speed")) {
            setSpeed(nbt.getFloat("Speed"));
        }
        if (nbt.contains("Inventory")) {
            NbtCompound inventoryNbt = nbt.getCompound("Inventory");
            for (int i = 0; i < inventory.size(); i++) {
                if (inventoryNbt.contains("Slot" + i)) {
                    inventory.set(i, ItemStack.fromNbt(inventoryNbt.getCompound("Slot" + i)));
                } else {
                    inventory.set(i, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.ordertocook.motorcycle_cooler");
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (hand == Hand.OFF_HAND) {
            return ActionResult.PASS;
        }

        if (player.isSneaking()) {
            if (!getWorld().isClient) {
                player.openHandledScreen(this);
            }
            return ActionResult.success(getWorld().isClient);
        }

        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof DyeItem dyeItem) {
            DyeColor dyeColor = dyeItem.getColor();
            int newColor = switch (dyeColor) {
                case RED -> 1;
                case BLUE -> 2;
                case YELLOW -> 3;
                case WHITE -> 0;
                default -> -1;
            };
            if (newColor != -1) {
                if (!getWorld().isClient) {
                    setMotorcycleColor(newColor);
                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                }
                return ActionResult.success(getWorld().isClient);
            }
        }

        if (!hasPassengers()) {
            if (!getWorld().isClient) {
                player.startRiding(this);
            }
            return ActionResult.success(getWorld().isClient);
        }

        return ActionResult.PASS;
    }

    @Override
    public void onPassengerLookAround(Entity passenger) {
        if (passenger instanceof PlayerEntity) {
            // 乘客视角锁定
        }
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return GenericContainerScreenHandler.createGeneric9x6(syncId, playerInventory, this);
    }

    @Override
    public boolean canHit() {
        return !this.isRemoved();
    }

    @Override
    public boolean collidesWith(Entity other) {
        return (other.isPushable() || other.canHit()) && !this.isConnectedThroughVehicle(other);
    }

    @Override
    public float getStepHeight() {
        return 1.0f;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        if (!getWorld().isClient && !isRemoved()) {
            spawnHitParticles();
            hitCount++;
            if (hitCount >= 5) {
                dropStack(MotorcycleItem.createColoredStack(getMotorcycleColor()));
                discard();
            }
            return true;
        }
        return false;
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!getWorld().isClient && (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED)) {
            for (ItemStack stack : inventory) {
                if (!stack.isEmpty()) {
                    dropStack(stack);
                }
            }
        }
        super.remove(reason);
    }

    private void spawnHitParticles() {
        if (!(getWorld() instanceof ServerWorld sw)) {
            return;
        }
        double x = getX();
        double y = getY() + getHeight() * 0.6;
        double z = getZ();
        double w = getWidth();
        double h = getHeight();
        sw.spawnParticles(ParticleTypes.SMOKE, x, y, z, 3, w * 0.2, h * 0.1, w * 0.2, 0.005);
        sw.spawnParticles(ParticleTypes.POOF, x, y, z, 2, w * 0.15, h * 0.08, w * 0.15, 0.01);
    }

    public void setSteering(float steering) {
        this.dataTracker.set(STEERING, steering);
    }

    public float getSteering() {
        return this.dataTracker.get(STEERING);
    }

    public void setSpeed(float speed) {
        this.dataTracker.set(SPEED, speed);
    }

    public float getSpeed() {
        return this.dataTracker.get(SPEED);
    }

    public int getMotorcycleColor() {
        int c = this.dataTracker.get(MOTORCYCLE_COLOR);
        return c >= 0 && c <= 3 ? c : 0;
    }

    public void setMotorcycleColor(int color) {
        int v = color >= 0 && color <= 3 ? color : 0;
        this.dataTracker.set(MOTORCYCLE_COLOR, v);
    }

    public int getColor() {
        return switch (getMotorcycleColor()) {
            case 1 -> DyeColor.RED.getId();
            case 2 -> DyeColor.BLUE.getId();
            case 3 -> DyeColor.YELLOW.getId();
            default -> DyeColor.WHITE.getId();
        };
    }

    public boolean isHeadlightOn() {
        return this.dataTracker.get(LIGHT_ENABLED);
    }

    public boolean isLightEnabled() {
        return isHeadlightOn();
    }

    public void setHeadlightOn(boolean on) {
        this.dataTracker.set(LIGHT_ENABLED, on);
    }

    public void setLightEnabled(boolean enabled) {
        setHeadlightOn(enabled);
    }

    public void toggleHeadlight() {
        setHeadlightOn(!isHeadlightOn());
    }

    public void honk() {
        this.playSound(SoundEvents.ENTITY_HORSE_ANGRY, 1.0f, 1.0f);
    }

    public float getHandleAngle(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevHandleAngle, handleAngle);
    }

    public float getWheelAngle(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevWheelAngle, wheelAngle);
    }

    public static MotorcycleEntity fromVehicle(Entity vehicle) {
        if (vehicle instanceof MotorcycleEntity motorcycle) {
            return motorcycle;
        }
        if (vehicle instanceof SeatEntity seat) {
            return seat.resolveParent();
        }
        return null;
    }

    public boolean controlsThisMotorcycle(PlayerEntity player) {
        if (this.hasPassenger(player)) {
            return true;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof SeatEntity seat) {
            MotorcycleEntity p = seat.resolveParent();
            return p == this;
        }
        return false;
    }

    /**
     * 1.20.1 已用 {@link Entity#updatePassengerPosition(Entity, PositionUpdater)} 驱动乘客位置；旧版 {@code positionPassenger} 不再被引擎调用，
     * 与 fabric-1.21.1 MotorcycleEntity 使用同一套偏移逻辑，减轻骑行橡皮感与贴图错位。
     */
    @Override
    protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
        if (passenger instanceof PlayerEntity) {
            double yawRad = this.getYaw() * (Math.PI / 180.0);
            double dx = SEAT_OFF_X * Math.cos(yawRad) - SEAT_OFF_Z * Math.sin(yawRad);
            double dz = SEAT_OFF_X * Math.sin(yawRad) + SEAT_OFF_Z * Math.cos(yawRad);
            positionUpdater.accept(passenger, this.getX() + dx, this.getY() + SEAT_OFF_Y, this.getZ() + dz);
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengerList().size() < 1 && passenger instanceof PlayerEntity;
    }

    @Override
    public double getMountedHeightOffset() {
        return 1.0;
    }

    @Override
    public void tick() {
        super.tick();

        Entity firstPassenger = this.getFirstPassenger();

        // 与 fabric-1.21.1 一致：本地主玩家骑乘时在客户端也跑位移，避免仅靠服务端回传产生橡皮感。
        if (firstPassenger instanceof PlayerEntity player) {
            if (!getWorld().isClient || player.isMainPlayer()) {
                float sideInput = player.sidewaysSpeed;
                float forwardInput = player.forwardSpeed;

                float maxSteer = 45.0f;
                float steerSpeed = 4.5f;

                if (sideInput != 0) {
                    float steerTarget = (sideInput > 0) ? -maxSteer : maxSteer;
                    targetSteering = MathHelper.stepTowards(targetSteering, steerTarget, steerSpeed);
                } else {
                    targetSteering = MathHelper.stepTowards(targetSteering, 0, steerSpeed);
                }

                float maxSpeed = 0.6f;
                float accel = 0.02f;
                float friction = 0.01f;

                if (forwardInput > 0) {
                    internalSpeed = MathHelper.stepTowards(internalSpeed, maxSpeed, accel);
                } else if (forwardInput < 0) {
                    internalSpeed = MathHelper.stepTowards(internalSpeed, -maxSpeed / 2, accel);
                } else {
                    internalSpeed = MathHelper.stepTowards(internalSpeed, 0, friction);
                }

                float steeringFactor = targetSteering / maxSteer;
                float yawChange = steeringFactor * (internalSpeed * 15.0f);
                this.setYaw(this.getYaw() + yawChange);

                Vec3d rotationVec = Vec3d.fromPolar(0, this.getYaw());
                Vec3d forwardVec = rotationVec.normalize().multiply(-1.0);
                Vec3d rightVec = new Vec3d(-forwardVec.z, 0, forwardVec.x);

                double targetForwardVel = internalSpeed;
                Vec3d currentVel = this.getVelocity();
                double lateralVel = currentVel.dotProduct(rightVec);
                double lateralFriction = 0.5;
                double newLateralVel = lateralVel * lateralFriction;

                double verticalVel = this.isOnGround() ? 0 : currentVel.y - 0.08;

                Vec3d newVelocity = forwardVec.multiply(targetForwardVel)
                        .add(rightVec.multiply(newLateralVel))
                        .add(0, verticalVel, 0);

                this.setVelocity(newVelocity);
                this.move(MovementType.SELF, this.getVelocity());

                if (!getWorld().isClient) {
                    setSteering(targetSteering);
                    setSpeed(internalSpeed);
                }
            }
        } else if (!getWorld().isClient) {
            internalSpeed = MathHelper.stepTowards(internalSpeed, 0, 0.005f);
            targetSteering = MathHelper.stepTowards(targetSteering, 0, 2.0f);

            if (Math.abs(internalSpeed) > 0.001) {
                Vec3d forwardVec = Vec3d.fromPolar(0, this.getYaw()).normalize().multiply(-1.0);
                this.setVelocity(forwardVec.multiply(internalSpeed).add(0, this.getVelocity().y - 0.08, 0));
                this.move(MovementType.SELF, this.getVelocity());
            }

            setSpeed(internalSpeed);
            setSteering(targetSteering);
        }

        if (getWorld().isClient) {
            if (!(firstPassenger instanceof PlayerEntity p && p.isMainPlayer())) {
                targetSteering = getSteering();
                internalSpeed = getSpeed();
            }
        }

        boolean localMainPlayerRiding = firstPassenger instanceof PlayerEntity p && p.isMainPlayer();
        float currentTargetSteering = localMainPlayerRiding ? targetSteering : getSteering();

        if (localMainPlayerRiding) {
            this.prevX = this.getX();
            this.prevY = this.getY();
            this.prevZ = this.getZ();
            this.prevYaw = this.getYaw();
            this.prevPitch = this.getPitch();
        }

        prevHandleAngle = handleAngle;
        float diff = currentTargetSteering - handleAngle;
        float handleRate = (6.0f + 8.0f * (float) MathHelper.clamp(Math.abs(internalSpeed) / 0.6, 0.0, 1.0));

        if (diff > 0) {
            handleAngle = Math.min(handleAngle + handleRate, currentTargetSteering);
        } else if (diff < 0) {
            handleAngle = Math.max(handleAngle - handleRate, currentTargetSteering);
        }

        prevWheelAngle = wheelAngle;
        if (Math.abs(internalSpeed) > 0.001) {
            wheelAngle += internalSpeed * 25.0f;
        }

        if (!getWorld().isClient && Math.abs(internalSpeed) > 0.05f) {
            if (driveSoundCooldown > 0) {
                driveSoundCooldown--;
            } else {
                this.playSound(SoundEvents.ENTITY_MINECART_RIDING, 0.08f, 0.75f + this.random.nextFloat() * 0.25f);
                driveSoundCooldown = 12;
            }
        } else if (!getWorld().isClient) {
            driveSoundCooldown = 0;
        }
    }

    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_HORSE_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_HORSE_DEATH;
    }

    @Override
    public void onBlockCollision(BlockState state) {
        super.onBlockCollision(state);
    }

    public boolean canSpawn(SpawnReason spawnReason) {
        return true;
    }

    @Override
    public BlockPos getBlockPos() {
        return new BlockPos((int) this.getX(), (int) this.getY(), (int) this.getZ());
    }

    public void openCooler(PlayerEntity player) {
        if (!getWorld().isClient && player instanceof ServerPlayerEntity) {
            player.openHandledScreen(this);
        }
    }

    public ImplementedInventory getCoolerInventory() {
        return this;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, state -> {
            state.setAndContinue(RawAnimation.begin().thenPlay("motorcycle_idle"));
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
}
