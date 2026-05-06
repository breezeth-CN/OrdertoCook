package cn.breezeth.ordertocook.vehicle.motorcycle;

import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.item.MotorcycleItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class MotorcycleEntity extends Entity implements MenuProvider {
    public static final float WIDTH = 1.2f; // 碰撞体积提升 1/2
    public static final float HEIGHT = 1.2f;
    private SeatEntity seatRef;
    public static final double SEAT_OFF_X = 0.0;
    public static final double SEAT_OFF_Y = 0.6;
    public static final double SEAT_OFF_Z = 0.45;

    private final SimpleContainer inventory = new SimpleContainer(54); // 大箱子容量 (6x9 = 54)

    // 使用 DataTracker 同步的关键数据
    private static final EntityDataAccessor<Float> STEERING = SynchedEntityData.defineId(MotorcycleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(MotorcycleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(MotorcycleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LIGHT_ENABLED = SynchedEntityData.defineId(MotorcycleEntity.class, EntityDataSerializers.BOOLEAN);

    private float targetSteering;
    private float handleAngle;
    private float prevHandleAngle;
    private float wheelAngle;
    private float prevWheelAngle;
    private float internalSpeed; // 本地速度变量，避免 DataTracker 同步抖动
    protected int hitCount;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;
    private int lerpSteps;

    public MotorcycleEntity(EntityType<? extends MotorcycleEntity> type, Level world) {
        super(type, world);
        this.setNoGravity(false);
        this.targetSteering = 0.0f;
        this.handleAngle = 0.0f;
        this.prevHandleAngle = 0.0f;
        this.wheelAngle = 0.0f;
        this.prevWheelAngle = 0.0f;
        this.internalSpeed = 0.0f;
        this.hitCount = 0;
        this.lerpSteps = 0;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(STEERING, 0.0f);
        this.entityData.define(SPEED, 0.0f);
        this.entityData.define(COLOR, 0); // 0: 默认, 1: 红, 2: 蓝, 3: 黄
        this.entityData.define(LIGHT_ENABLED, true);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.setSteering(nbt.getFloat("Steering"));
        this.setSpeed(nbt.getFloat("Speed"));
        this.setMotorcycleColor(nbt.getInt("MotorcycleColor"));
        this.setLightEnabled(!nbt.contains("LightEnabled") || nbt.getBoolean("LightEnabled"));
        this.inventory.fromTag(nbt.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putFloat("Steering", this.getSteering());
        nbt.putFloat("Speed", this.getSpeed());
        nbt.putInt("MotorcycleColor", this.getMotorcycleColor());
        nbt.putBoolean("LightEnabled", this.isLightEnabled());
        nbt.put("Items", this.inventory.createTag());
    }

    public void setSteering(float steering) {
        this.entityData.set(STEERING, steering);
    }

    public float getSteering() {
        return this.entityData.get(STEERING);
    }

    public void setSpeed(float speed) {
        this.entityData.set(SPEED, speed);
    }

    public float getSpeed() {
        return this.entityData.get(SPEED);
    }

    public void setMotorcycleColor(int color) {
        this.entityData.set(COLOR, color >= 1 && color <= 3 ? color : 0);
    }

    public int getMotorcycleColor() {
        return this.entityData.get(COLOR);
    }

    public void setLightEnabled(boolean enabled) {
        this.entityData.set(LIGHT_ENABLED, enabled);
    }

    public boolean isLightEnabled() {
        return this.entityData.get(LIGHT_ENABLED);
    }

    @Nullable
    public static MotorcycleEntity fromVehicle(@Nullable Entity vehicle) {
        if (vehicle instanceof MotorcycleEntity motorcycle) {
            return motorcycle;
        }
        if (vehicle instanceof SeatEntity seat) {
            return seat.resolveParent();
        }
        return null;
    }

    @Nullable
    private SeatEntity resolveSeat() {
        if (this.seatRef != null && this.seatRef.isAlive()) {
            return this.seatRef;
        }

        for (SeatEntity entity : this.level().getEntitiesOfClass(SeatEntity.class, this.getBoundingBox().inflate(8.0), seat -> true)) {
            if (entity.getEntityData().get(SeatEntity.PARENT_ENTITY_ID) == this.getId()) {
                this.seatRef = entity;
                return entity;
            }
        }

        return null;
    }

    @Override
    public void tick() {
        super.tick();
        this.tickLerp();

        if (!this.level().isClientSide && this.tickCount % 40 == 0) {
            SeatEntity seat = this.resolveSeat();
            if (seat != null) {
                boolean staleEmptySeat = !this.isVehicle() && seat.getFirstPassenger() == null;
                boolean staleBrokenLink = seat.getFirstPassenger() != null && seat.getFirstPassenger().getVehicle() != seat;
                if (staleEmptySeat || staleBrokenLink) {
                    seat.discard();
                    this.seatRef = null;
                }
            }
        }

        Entity firstPassenger = this.getFirstPassenger();
        if (firstPassenger instanceof Player player) {
            boolean locallyControlled = this.isControlledByLocalInstance();
            if (locallyControlled || !this.level().isClientSide) {
                float sideInput = player.xxa;
                float forwardInput = player.zza;

                float maxSteer = 45.0f;
                float steerSpeed = 4.5f;

                if (sideInput != 0) {
                    float steerTarget = (sideInput > 0) ? -maxSteer : maxSteer;
                    targetSteering = Mth.approach(targetSteering, steerTarget, steerSpeed);
                } else {
                    targetSteering = Mth.approach(targetSteering, 0, steerSpeed);
                }

                float maxSpeed = 0.6f;
                float accel = 0.02f;
                float friction = 0.01f;

                if (forwardInput > 0) {
                    internalSpeed = Mth.approach(internalSpeed, maxSpeed, accel);
                } else if (forwardInput < 0) {
                    internalSpeed = Mth.approach(internalSpeed, -maxSpeed / 2, accel);
                } else {
                    internalSpeed = Mth.approach(internalSpeed, 0, friction);
                }

                float steeringFactor = targetSteering / maxSteer;
                float yawChange = steeringFactor * (internalSpeed * 15.0f);
                if (locallyControlled) {
                    this.setYRot(this.getYRot() + yawChange);
                }

                Vec3 rotationVec = Vec3.directionFromRotation(0, this.getYRot());
                Vec3 forwardVec = rotationVec.normalize().scale(-1.0);
                Vec3 rightVec = new Vec3(-forwardVec.z, 0, forwardVec.x);

                double targetForwardVel = internalSpeed;
                Vec3 currentVel = this.getDeltaMovement();
                double lateralVel = currentVel.dot(rightVec);
                double lateralFriction = 0.5;
                double newLateralVel = lateralVel * lateralFriction;

                double verticalVel = this.onGround() ? 0 : currentVel.y - 0.08;

                Vec3 newVelocity = forwardVec.scale(targetForwardVel)
                        .add(rightVec.scale(newLateralVel))
                        .add(0, verticalVel, 0);

                this.setDeltaMovement(newVelocity);
                if (locallyControlled) {
                    this.move(MoverType.SELF, this.getDeltaMovement());
                }

                if (!this.level().isClientSide) {
                    this.setSteering(targetSteering);
                    this.setSpeed(internalSpeed);
                }
            }
        } else if (!this.level().isClientSide) {
            internalSpeed = Mth.approach(internalSpeed, 0, 0.005f);
            targetSteering = Mth.approach(targetSteering, 0, 2.0f);

            if (Math.abs(internalSpeed) > 0.001) {
                Vec3 forwardVec = Vec3.directionFromRotation(0, this.getYRot()).normalize().scale(-1.0);
                this.setDeltaMovement(forwardVec.scale(internalSpeed).add(0, this.getDeltaMovement().y - 0.08, 0));
                this.move(MoverType.SELF, this.getDeltaMovement());
            }

            this.setSpeed(internalSpeed);
            this.setSteering(targetSteering);
        }

        if (this.level().isClientSide) {
            if (!(firstPassenger instanceof Player p && p.isLocalPlayer())) {
                targetSteering = getSteering();
                internalSpeed = getSpeed();
            }
        }

        boolean localMainPlayerRiding = isLocalClientDriver(firstPassenger);
        float currentTargetSteering = localMainPlayerRiding ? targetSteering : getSteering();

        if (localMainPlayerRiding) {
            this.xo = this.getX();
            this.yo = this.getY();
            this.zo = this.getZ();
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
            this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
        }

        prevHandleAngle = handleAngle;
        float diff = currentTargetSteering - handleAngle;
        float handleRate = (6.0f + 8.0f * (float)Mth.clamp(Math.abs(internalSpeed) / 0.6, 0.0, 1.0));

        if (diff > 0) {
            handleAngle = Math.min(handleAngle + handleRate, currentTargetSteering);
        } else if (diff < 0) {
            handleAngle = Math.max(handleAngle - handleRate, currentTargetSteering);
        }

        // 更新车轮旋转角度（累加方式，解决松开 W 时反转的问题）
        prevWheelAngle = wheelAngle;
        if (Math.abs(internalSpeed) > 0.001) {
            wheelAngle += (internalSpeed * 25.0f);
        }
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return (other.isPushable() || other.isPickable()) && !this.isPassengerOfSameVehicle(other);
    }

    @Override
    public float maxUpStep() {
        return 1.0f;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getFirstPassenger() == null && passenger instanceof Player;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();
        return passenger instanceof LivingEntity living ? living : null;
    }

    @Override
    public void positionRider(Entity passenger, MoveFunction updater) {
        if (this.hasPassenger(passenger)) {
            double yawRad = this.getYRot() * (Math.PI / 180.0);
            double dx = SEAT_OFF_X * Math.cos(yawRad) - SEAT_OFF_Z * Math.sin(yawRad);
            double dz = SEAT_OFF_X * Math.sin(yawRad) + SEAT_OFF_Z * Math.cos(yawRad);
            updater.accept(passenger, this.getX() + dx, this.getY() + SEAT_OFF_Y, this.getZ() + dz);
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int lerpSteps, boolean teleport) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = 10;
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        if (this.isControlledByLocalInstance()) {
            return;
        }
        super.lerpMotion(x, y, z);
    }

    private void tickLerp() {
        if (this.isControlledByLocalInstance()) {
            this.lerpSteps = 0;
            this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
        }

        if (this.lerpSteps > 0) {
            double x = this.getX() + (this.lerpX - this.getX()) / this.lerpSteps;
            double y = this.getY() + (this.lerpY - this.getY()) / this.lerpSteps;
            double z = this.getZ() + (this.lerpZ - this.getZ()) / this.lerpSteps;
            float yRot = (float) (this.getYRot() + (this.lerpYRot - this.getYRot()) / this.lerpSteps);
            float xRot = (float) (this.getXRot() + (this.lerpXRot - this.getXRot()) / this.lerpSteps);
            this.setPos(x, y, z);
            this.setRot(yRot, xRot);
            this.lerpSteps--;
        }
    }

    public double lerpTargetX() {
        return this.lerpSteps > 0 ? this.lerpX : this.getX();
    }

    public double lerpTargetY() {
        return this.lerpSteps > 0 ? this.lerpY : this.getY();
    }

    public double lerpTargetZ() {
        return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
    }

    public float lerpTargetXRot() {
        return this.lerpSteps > 0 ? (float) this.lerpXRot : this.getXRot();
    }

    public float lerpTargetYRot() {
        return this.lerpSteps > 0 ? (float) this.lerpYRot : this.getYRot();
    }

    private boolean isLocalClientDriver(@Nullable Entity passenger) {
        return this.level().isClientSide && passenger instanceof Player player && player.isLocalPlayer();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        double yawRad = this.getYRot() * (Math.PI / 180.0);
        Vec3[] candidates = new Vec3[] {
                new Vec3(Math.sin(yawRad) * 0.9, 0.0, -Math.cos(yawRad) * 0.9),
                new Vec3(-Math.sin(yawRad) * 0.9, 0.0, Math.cos(yawRad) * 0.9),
                new Vec3(Math.cos(yawRad) * 0.9, 0.0, Math.sin(yawRad) * 0.9),
                new Vec3(-Math.cos(yawRad) * 0.9, 0.0, -Math.sin(yawRad) * 0.9),
                Vec3.ZERO
        };

        for (Vec3 offset : candidates) {
            net.minecraft.core.BlockPos base = net.minecraft.core.BlockPos.containing(this.getX() + offset.x, this.getY(), this.getZ() + offset.z);
            for (int dy = 0; dy <= 1; dy++) {
                net.minecraft.core.BlockPos pos = base.above(dy);
                if (isSafeDismountBlock(pos)) {
                    return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                }
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    private boolean isSafeDismountBlock(net.minecraft.core.BlockPos pos) {
        return this.level().getFluidState(pos).isEmpty()
                && this.level().getFluidState(pos.above()).isEmpty()
                && this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty()
                && this.level().getBlockState(pos.above()).getCollisionShape(this.level(), pos.above()).isEmpty()
                && !this.level().getBlockState(pos.below()).getCollisionShape(this.level(), pos.below()).isEmpty();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            // Shift + 右键：打开保温箱
            if (!this.level().isClientSide()) {
                player.openMenu(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        } else {
            // 右键：染色或骑乘
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && stack.getItem() instanceof DyeItem dyeItem) {
                DyeColor dyeColor = dyeItem.getDyeColor();
                int newColor = switch (dyeColor) {
                    case RED -> 1;
                    case BLUE -> 2;
                    case YELLOW -> 3;
                    case WHITE -> 0;
                    default -> -1;
                };

                if (newColor != -1) {
                    if (!this.level().isClientSide) {
                        this.setMotorcycleColor(newColor);
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                    }
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                }
            }

            if (!this.isVehicle()) {
                if (!this.level().isClientSide) {
                    player.startRiding(this);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (this.seatRef != null) {
            this.seatRef.discard();
            this.seatRef = null;
        }
        if (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED) {
            // 摩托车损坏时掉落保温箱物品
            Containers.dropContents(this.level(), this, this.inventory);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;
        if (!this.level().isClientSide && !this.isRemoved()) {
            spawnHitParticles();
            this.hitCount++;
            if (this.hitCount >= 5) {
                this.spawnAtLocation(MotorcycleItem.createColoredStack(this.getMotorcycleColor()));
                this.discard();
            }
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return ChestMenu.sixRows(syncId, playerInventory, this.inventory);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.ordertocook.motorcycle_cooler");
    }

    public float getHandleAngle(float tickDelta) {
        return Mth.lerp(tickDelta, prevHandleAngle, handleAngle);
    }

    public float getWheelAngle(float tickDelta) {
        return Mth.lerp(tickDelta, prevWheelAngle, wheelAngle);
    }

    public SimpleContainer getCoolerInventory() {
        return this.inventory;
    }

    private void spawnHitParticles() {
        if (!(this.level() instanceof ServerLevel serverWorld)) {
            return;
        }

        double x = this.getX();
        double y = this.getY(0.6);
        double z = this.getZ();
        serverWorld.sendParticles(
                ParticleTypes.SMOKE,
                x,
                y,
                z,
                3,
                this.getBbWidth() * 0.2,
                this.getBbHeight() * 0.1,
                this.getBbWidth() * 0.2,
                0.005
        );
        serverWorld.sendParticles(
                ParticleTypes.POOF,
                x,
                y,
                z,
                2,
                this.getBbWidth() * 0.15,
                this.getBbHeight() * 0.08,
                this.getBbWidth() * 0.15,
                0.01
        );
    }
}
