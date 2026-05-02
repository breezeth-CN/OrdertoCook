package cn.breezeth.ordertocook.vehicle.motorcycle;

import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.item.MotorcycleItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.damage.DamageSource;

import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import org.jetbrains.annotations.Nullable;

import net.minecraft.item.DyeItem;
import net.minecraft.util.DyeColor;

public class MotorcycleEntity extends Entity implements NamedScreenHandlerFactory {
    public static final float WIDTH = 1.2f; // 碰撞体积提升 1/2
    public static final float HEIGHT = 1.2f;
    private SeatEntity seatRef;
    public static final double SEAT_OFF_X = 0.0;
    public static final double SEAT_OFF_Y = 0.6;
    public static final double SEAT_OFF_Z = 0.45;

    private final SimpleInventory inventory = new SimpleInventory(54); // 大箱子容量 (6x9 = 54)

    // 使用 DataTracker 同步的关键数据
    private static final TrackedData<Float> STEERING = DataTracker.registerData(MotorcycleEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> SPEED = DataTracker.registerData(MotorcycleEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> COLOR = DataTracker.registerData(MotorcycleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> LIGHT_ENABLED = DataTracker.registerData(MotorcycleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private float targetSteering;
    private float handleAngle;
    private float prevHandleAngle;
    private float wheelAngle;
    private float prevWheelAngle;
    private float internalSpeed; // 本地速度变量，避免 DataTracker 同步抖动
    protected int hitCount;

    public MotorcycleEntity(EntityType<? extends MotorcycleEntity> type, World world) {
        super(type, world);
        this.setNoGravity(false);
        this.targetSteering = 0.0f;
        this.handleAngle = 0.0f;
        this.prevHandleAngle = 0.0f;
        this.wheelAngle = 0.0f;
        this.prevWheelAngle = 0.0f;
        this.internalSpeed = 0.0f;
        this.hitCount = 0;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(STEERING, 0.0f);
        builder.add(SPEED, 0.0f);
        builder.add(COLOR, 0); // 0: 默认, 1: 红, 2: 蓝, 3: 黄
        builder.add(LIGHT_ENABLED, true);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setSteering(nbt.getFloat("Steering"));
        this.setSpeed(nbt.getFloat("Speed"));
        this.setMotorcycleColor(nbt.getInt("MotorcycleColor"));
        this.setLightEnabled(!nbt.contains("LightEnabled") || nbt.getBoolean("LightEnabled"));
        Inventories.readNbt(nbt, this.inventory.getHeldStacks(), this.getWorld().getRegistryManager());
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("Steering", this.getSteering());
        nbt.putFloat("Speed", this.getSpeed());
        nbt.putInt("MotorcycleColor", this.getMotorcycleColor());
        nbt.putBoolean("LightEnabled", this.isLightEnabled());
        Inventories.writeNbt(nbt, this.inventory.getHeldStacks(), this.getWorld().getRegistryManager());
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

    public void setMotorcycleColor(int color) {
        this.dataTracker.set(COLOR, color >= 1 && color <= 3 ? color : 0);
    }

    public int getMotorcycleColor() {
        return this.dataTracker.get(COLOR);
    }

    public void setLightEnabled(boolean enabled) {
        this.dataTracker.set(LIGHT_ENABLED, enabled);
    }

    public boolean isLightEnabled() {
        return this.dataTracker.get(LIGHT_ENABLED);
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

        for (SeatEntity entity : this.getWorld().getEntitiesByClass(SeatEntity.class, this.getBoundingBox().expand(8.0), seat -> true)) {
            if (entity.getDataTracker().get(SeatEntity.PARENT_ENTITY_ID) == this.getId()) {
                this.seatRef = entity;
                return entity;
            }
        }

        return null;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient && this.age % 40 == 0) {
            SeatEntity seat = this.resolveSeat();
            if (seat != null) {
                boolean staleEmptySeat = !this.hasPassengers() && seat.getFirstPassenger() == null;
                boolean staleBrokenLink = seat.getFirstPassenger() != null && seat.getFirstPassenger().getVehicle() != seat;
                if (staleEmptySeat || staleBrokenLink) {
                    seat.discard();
                    this.seatRef = null;
                }
            }
        }

        Entity firstPassenger = this.getFirstPassenger();
        if (firstPassenger instanceof PlayerEntity player) {
            if (!this.getWorld().isClient || player.isMainPlayer()) {
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

                if (!this.getWorld().isClient) {
                    this.setSteering(targetSteering);
                    this.setSpeed(internalSpeed);
                }
            }
        } else if (!this.getWorld().isClient) {
            internalSpeed = MathHelper.stepTowards(internalSpeed, 0, 0.005f);
            targetSteering = MathHelper.stepTowards(targetSteering, 0, 2.0f);

            if (Math.abs(internalSpeed) > 0.001) {
                Vec3d forwardVec = Vec3d.fromPolar(0, this.getYaw()).normalize().multiply(-1.0);
                this.setVelocity(forwardVec.multiply(internalSpeed).add(0, this.getVelocity().y - 0.08, 0));
                this.move(MovementType.SELF, this.getVelocity());
            }

            this.setSpeed(internalSpeed);
            this.setSteering(targetSteering);
        }

        if (this.getWorld().isClient) {
            if (!(firstPassenger instanceof PlayerEntity p && p.isMainPlayer())) {
                targetSteering = getSteering();
                internalSpeed = getSpeed();
            }
        }

        boolean localMainPlayerRiding = firstPassenger instanceof PlayerEntity player && player.isMainPlayer();
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
        float handleRate = (6.0f + 8.0f * (float)MathHelper.clamp(Math.abs(internalSpeed) / 0.6, 0.0, 1.0));

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
    protected boolean canAddPassenger(Entity passenger) {
        return this.getFirstPassenger() == null && passenger instanceof PlayerEntity;
    }

    @Override
    public void updatePassengerPosition(Entity passenger, PositionUpdater updater) {
        if (this.hasPassenger(passenger)) {
            double yawRad = this.getYaw() * (Math.PI / 180.0);
            double dx = SEAT_OFF_X * Math.cos(yawRad) - SEAT_OFF_Z * Math.sin(yawRad);
            double dz = SEAT_OFF_X * Math.sin(yawRad) + SEAT_OFF_Z * Math.cos(yawRad);
            updater.accept(passenger, this.getX() + dx, this.getY() + SEAT_OFF_Y, this.getZ() + dz);
        }
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (hand == Hand.OFF_HAND) return ActionResult.PASS;

        if (player.isSneaking()) {
            // Shift + 右键：打开保温箱
            if (!this.getWorld().isClient()) {
                player.openHandledScreen(this);
            }
            return ActionResult.success(this.getWorld().isClient());
        } else {
            // 右键：染色或骑乘
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
                    if (!this.getWorld().isClient) {
                        this.setMotorcycleColor(newColor);
                        if (!player.getAbilities().creativeMode) {
                            stack.decrement(1);
                        }
                    }
                    return ActionResult.success(this.getWorld().isClient);
                }
            }

            if (!this.hasPassengers()) {
                if (!this.getWorld().isClient) {
                    player.startRiding(this);
                }
                return ActionResult.success(this.getWorld().isClient);
            }
        }
        return ActionResult.PASS;
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
            ItemScatterer.spawn(this.getWorld(), this, this.inventory);
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;
        if (!this.getWorld().isClient && !this.isRemoved()) {
            spawnHitParticles();
            this.hitCount++;
            if (this.hitCount >= 5) {
                this.dropStack(MotorcycleItem.createColoredStack(this.getMotorcycleColor()));
                this.discard();
            }
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return GenericContainerScreenHandler.createGeneric9x6(syncId, playerInventory, this.inventory);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.ordertocook.motorcycle_cooler");
    }

    public float getHandleAngle(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevHandleAngle, handleAngle);
    }

    public float getWheelAngle(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevWheelAngle, wheelAngle);
    }

    public SimpleInventory getCoolerInventory() {
        return this.inventory;
    }

    private void spawnHitParticles() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        double x = this.getX();
        double y = this.getBodyY(0.6);
        double z = this.getZ();
        serverWorld.spawnParticles(
                ParticleTypes.SMOKE,
                x,
                y,
                z,
                3,
                this.getWidth() * 0.2,
                this.getHeight() * 0.1,
                this.getWidth() * 0.2,
                0.005
        );
        serverWorld.spawnParticles(
                ParticleTypes.POOF,
                x,
                y,
                z,
                2,
                this.getWidth() * 0.15,
                this.getHeight() * 0.08,
                this.getWidth() * 0.15,
                0.01
        );
    }
}
