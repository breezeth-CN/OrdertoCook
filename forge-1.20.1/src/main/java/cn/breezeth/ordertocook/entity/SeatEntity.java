package cn.breezeth.ordertocook.entity;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SeatEntity extends Entity {
    public static final EntityDataAccessor<Integer> PARENT_ENTITY_ID = SynchedEntityData.defineId(SeatEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> TRACK_OFF_X = SynchedEntityData.defineId(SeatEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TRACK_OFF_Y = SynchedEntityData.defineId(SeatEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TRACK_OFF_Z = SynchedEntityData.defineId(SeatEntity.class, EntityDataSerializers.FLOAT);

    private UUID parentId;
    private MotorcycleEntity parent;
    private double offX;
    private double offY;
    private double offZ;

    public SeatEntity(EntityType<? extends SeatEntity> type, Level world) {
        super(type, world);
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    public void attachTo(MotorcycleEntity parent, double offX, double offY, double offZ) {
        this.parent = parent;
        this.parentId = parent.getUUID();
        this.offX = offX;
        this.offY = offY;
        this.offZ = offZ;
        this.getEntityData().set(PARENT_ENTITY_ID, parent.getId());
        this.getEntityData().set(TRACK_OFF_X, (float) offX);
        this.getEntityData().set(TRACK_OFF_Y, (float) offY);
        this.getEntityData().set(TRACK_OFF_Z, (float) offZ);
        this.moveTo(parent.getX(), parent.getY() + offY, parent.getZ(), parent.getYRot(), 0.0f);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PARENT_ENTITY_ID, -1);
        this.entityData.define(TRACK_OFF_X, 0.0f);
        this.entityData.define(TRACK_OFF_Y, 0.0f);
        this.entityData.define(TRACK_OFF_Z, 0.0f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        if (nbt.hasUUID("Parent")) {
            this.parentId = nbt.getUUID("Parent");
        }
        this.offX = nbt.getDouble("OffX");
        this.offY = nbt.getDouble("OffY");
        this.offZ = nbt.getDouble("OffZ");
        this.getEntityData().set(TRACK_OFF_X, (float) offX);
        this.getEntityData().set(TRACK_OFF_Y, (float) offY);
        this.getEntityData().set(TRACK_OFF_Z, (float) offZ);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        if (parent != null) {
            nbt.putUUID("Parent", parent.getUUID());
        } else if (parentId != null) {
            nbt.putUUID("Parent", parentId);
        }
        nbt.putDouble("OffX", offX);
        nbt.putDouble("OffY", offY);
        nbt.putDouble("OffZ", offZ);
    }

    public MotorcycleEntity resolveParent() {
        if (this.parent != null && this.parent.isAlive()) {
            return this.parent;
        }

        int parentEntityId = this.getEntityData().get(PARENT_ENTITY_ID);
        if (parentEntityId >= 0) {
            Entity trackedEntity = this.level().getEntity(parentEntityId);
            if (trackedEntity instanceof MotorcycleEntity motorcycle) {
                this.parent = motorcycle;
                return motorcycle;
            }
        }

        if (this.parentId != null) {
            for (Entity entity : this.level().getEntitiesOfClass(MotorcycleEntity.class, this.getBoundingBox().inflate(8.0), candidate -> candidate.getUUID().equals(this.parentId))) {
                this.parent = (MotorcycleEntity) entity;
                return this.parent;
            }
        }

        return null;
    }

    public void syncToParent(MotorcycleEntity motorcycle) {
        float localOffX = this.getEntityData().get(TRACK_OFF_X);
        float localOffY = this.getEntityData().get(TRACK_OFF_Y);
        float localOffZ = this.getEntityData().get(TRACK_OFF_Z);
        float yawRad = motorcycle.getYRot() * ((float) Math.PI / 180F);
        double dx = localOffX * Mth.cos(yawRad) - localOffZ * Mth.sin(yawRad);
        double dz = localOffX * Mth.sin(yawRad) + localOffZ * Mth.cos(yawRad);
        Vec3 target = new Vec3(motorcycle.getX() + dx, motorcycle.getY() + localOffY, motorcycle.getZ() + dz);
        this.moveTo(target.x, target.y, target.z, motorcycle.getYRot(), 0.0f);
    }

    private void syncPassenger(Entity passenger) {
        passenger.moveTo(this.getX(), this.getY(), this.getZ(), passenger.getYRot(), passenger.getXRot());
        passenger.setDeltaMovement(Vec3.ZERO);
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

        if (this.level().isClientSide()) {
            return;
        }

        if (motorcycle != null) {
            return;
        }

        if (this.getTags().contains(ModConstants.MOD_ID + ":motorcycle_seat")) {
            this.discard();
            return;
        }

        if (this.tickCount > 5 && this.getPassengers().isEmpty()) {
            this.discard();
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return Vec3.ZERO;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getFirstPassenger() == null && passenger instanceof Player;
    }

    @Override
    public void positionRider(Entity passenger, MoveFunction updater) {
        if (this.hasPassenger(passenger)) {
            this.syncPassenger(passenger);
            updater.accept(passenger, this.getX(), this.getY(), this.getZ());
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(net.minecraft.world.entity.LivingEntity passenger) {
        if (this.getTags().contains(ModConstants.MOD_ID + ":motorcycle_seat")) {
            MotorcycleEntity motorcycle = resolveParent();
            if (motorcycle != null) {
                double yawRad = motorcycle.getYRot() * (Math.PI / 180.0);
                double backOffsetX = Math.sin(yawRad) * 0.8;
                double backOffsetZ = -Math.cos(yawRad) * 0.8;
                return new Vec3(
                        motorcycle.getX() - backOffsetX,
                        motorcycle.getY() + 0.1,
                        motorcycle.getZ() - backOffsetZ
                );
            }
        }

        if (passenger instanceof net.minecraft.world.entity.player.Player) {
            net.minecraft.core.BlockPos pos = this.blockPosition();
            if (this.level().getBlockState(pos.above()).isAir()) {
                return new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }
}
