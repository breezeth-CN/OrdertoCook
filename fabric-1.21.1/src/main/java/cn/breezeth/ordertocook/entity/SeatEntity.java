package cn.breezeth.ordertocook.entity;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

public class SeatEntity extends Entity {
    public static final TrackedData<Integer> PARENT_ENTITY_ID = DataTracker.registerData(SeatEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> TRACK_OFF_X = DataTracker.registerData(SeatEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> TRACK_OFF_Y = DataTracker.registerData(SeatEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> TRACK_OFF_Z = DataTracker.registerData(SeatEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private UUID parentId;
    private MotorcycleEntity parent;
    private double offX;
    private double offY;
    private double offZ;

    public SeatEntity(EntityType<? extends SeatEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    public void attachTo(MotorcycleEntity parent, double offX, double offY, double offZ) {
        this.parent = parent;
        this.parentId = parent.getUuid();
        this.offX = offX;
        this.offY = offY;
        this.offZ = offZ;
        this.getDataTracker().set(PARENT_ENTITY_ID, parent.getId());
        this.getDataTracker().set(TRACK_OFF_X, (float) offX);
        this.getDataTracker().set(TRACK_OFF_Y, (float) offY);
        this.getDataTracker().set(TRACK_OFF_Z, (float) offZ);
        this.refreshPositionAndAngles(parent.getX(), parent.getY() + offY, parent.getZ(), parent.getYaw(), 0.0f);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(PARENT_ENTITY_ID, -1);
        builder.add(TRACK_OFF_X, 0.0f);
        builder.add(TRACK_OFF_Y, 0.0f);
        builder.add(TRACK_OFF_Z, 0.0f);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.containsUuid("Parent")) {
            this.parentId = nbt.getUuid("Parent");
        }
        this.offX = nbt.getDouble("OffX");
        this.offY = nbt.getDouble("OffY");
        this.offZ = nbt.getDouble("OffZ");
        this.getDataTracker().set(TRACK_OFF_X, (float) offX);
        this.getDataTracker().set(TRACK_OFF_Y, (float) offY);
        this.getDataTracker().set(TRACK_OFF_Z, (float) offZ);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (parent != null) {
            nbt.putUuid("Parent", parent.getUuid());
        } else if (parentId != null) {
            nbt.putUuid("Parent", parentId);
        }
        nbt.putDouble("OffX", offX);
        nbt.putDouble("OffY", offY);
        nbt.putDouble("OffZ", offZ);
    }

    public MotorcycleEntity resolveParent() {
        if (this.parent != null && this.parent.isAlive()) {
            return this.parent;
        }

        int parentEntityId = this.getDataTracker().get(PARENT_ENTITY_ID);
        if (parentEntityId >= 0) {
            Entity trackedEntity = this.getWorld().getEntityById(parentEntityId);
            if (trackedEntity instanceof MotorcycleEntity motorcycle) {
                this.parent = motorcycle;
                return motorcycle;
            }
        }

        if (this.parentId != null) {
            for (Entity entity : this.getWorld().getEntitiesByClass(MotorcycleEntity.class, this.getBoundingBox().expand(8.0), candidate -> candidate.getUuid().equals(this.parentId))) {
                this.parent = (MotorcycleEntity) entity;
                return this.parent;
            }
        }

        return null;
    }

    public void syncToParent(MotorcycleEntity motorcycle) {
        float localOffX = this.getDataTracker().get(TRACK_OFF_X);
        float localOffY = this.getDataTracker().get(TRACK_OFF_Y);
        float localOffZ = this.getDataTracker().get(TRACK_OFF_Z);
        float yawRad = motorcycle.getYaw() * ((float) Math.PI / 180F);
        double dx = localOffX * MathHelper.cos(yawRad) - localOffZ * MathHelper.sin(yawRad);
        double dz = localOffX * MathHelper.sin(yawRad) + localOffZ * MathHelper.cos(yawRad);
        Vec3d target = new Vec3d(motorcycle.getX() + dx, motorcycle.getY() + localOffY, motorcycle.getZ() + dz);
        this.refreshPositionAndAngles(target.x, target.y, target.z, motorcycle.getYaw(), 0.0f);
    }

    private void syncPassenger(Entity passenger) {
        passenger.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), passenger.getYaw(), passenger.getPitch());
        passenger.setVelocity(Vec3d.ZERO);
    }

    @Override
    public void tick() {
        super.tick();

        MotorcycleEntity motorcycle = this.resolveParent();
        if (motorcycle != null) {
            this.syncToParent(motorcycle);
            if (this.getFirstPassenger() != null) {
                this.syncPassenger(this.getFirstPassenger());
            }
        }

        if (this.getWorld().isClient()) {
            return;
        }

        if (motorcycle != null) {
            return;
        }

        if (this.getCommandTags().contains(ModConstants.MOD_ID + ":motorcycle_seat")) {
            this.discard();
            return;
        }

        if (this.age > 5 && this.getPassengerList().isEmpty()) {
            this.discard();
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean collidesWith(Entity other) {
        return false;
    }

    @Override
    public boolean canHit() {
        return false;
    }

    @Override
    protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return Vec3d.ZERO;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getFirstPassenger() == null && passenger instanceof PlayerEntity;
    }

    @Override
    public void updatePassengerPosition(Entity passenger, PositionUpdater updater) {
        if (this.hasPassenger(passenger)) {
            this.syncPassenger(passenger);
            updater.accept(passenger, this.getX(), this.getY(), this.getZ());
        }
    }

    @Override
    public Vec3d updatePassengerForDismount(net.minecraft.entity.LivingEntity passenger) {
        if (this.getCommandTags().contains(ModConstants.MOD_ID + ":motorcycle_seat")) {
            MotorcycleEntity motorcycle = resolveParent();
            if (motorcycle != null) {
                double yawRad = motorcycle.getYaw() * (Math.PI / 180.0);
                double backOffsetX = Math.sin(yawRad) * 0.8;
                double backOffsetZ = -Math.cos(yawRad) * 0.8;
                return new Vec3d(
                        motorcycle.getX() - backOffsetX,
                        motorcycle.getY() + 0.1,
                        motorcycle.getZ() - backOffsetZ
                );
            }
        }

        if (passenger instanceof net.minecraft.entity.player.PlayerEntity) {
            net.minecraft.util.math.BlockPos pos = this.getBlockPos();
            if (this.getWorld().getBlockState(pos.up()).isAir()) {
                return new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            }
        }
        return super.updatePassengerForDismount(passenger);
    }
}
