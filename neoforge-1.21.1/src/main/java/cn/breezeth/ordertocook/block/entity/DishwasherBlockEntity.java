package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.block.DishwasherBlock;
import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class DishwasherBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final int STATE_IDLE = 0;
    private static final int STATE_EMPTY_IN = 1;
    private static final int STATE_DIRTY_IN = 2;
    private static final int STATE_WASHING = 3;
    private static final int STATE_CLEAR_OUT = 4;
    private static final int STATE_CLEAR_READY = 5;

    private static final int MAX_PLATES = 16;
    private static final int EMPTY_IN_TICKS = 14;
    private static final int DIRTY_IN_TICKS = 30;
    private static final int WASHING_TICKS = 40;
    private static final int MANY_PLATE_WASHING_TICKS = WASHING_TICKS * 2;
    private static final int CLEAR_OUT_TICKS = 24;
    private static final int DIRTY_IN_PLATE_PLACE_TICK = 14;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int state = STATE_IDLE;
    private int timer = 0;
    private int storedPlates = 0;
    private String lastAnimation = "";
    private boolean clientPreviewOpen = false;

    public DishwasherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISHWASHER.get(), pos, state);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, DishwasherBlockEntity blockEntity) {
        if (!world.isClientSide) {
            blockEntity.tickServer();
        }
    }

    public boolean canInsertDirtyPlates() {
        return state == STATE_IDLE;
    }

    public boolean canTakeCleanPlates() {
        return state == STATE_CLEAR_READY && storedPlates > 0;
    }

    public boolean isIdleForPreview() {
        return state == STATE_IDLE;
    }

    public boolean isOpenForShape() {
        return clientPreviewOpen || state == STATE_CLEAR_OUT || state == STATE_CLEAR_READY;
    }

    public boolean shouldRenderDishBone(String boneName) {
        if ("drity_dish".equals(boneName)) {
            return state == STATE_DIRTY_IN;
        }
        if ("clear_dish".equals(boneName)) {
            return state == STATE_CLEAR_OUT || state == STATE_CLEAR_READY;
        }
        return true;
    }

    public int getStoredPlates() {
        return storedPlates;
    }

    public void insertDirtyPlates(Player player) {
        if (!canInsertDirtyPlates()) {
            return;
        }
        int removed = removeDirtyPlates(player, MAX_PLATES);
        if (removed <= 0) {
            return;
        }
        this.storedPlates = removed;
        setState(STATE_DIRTY_IN, DIRTY_IN_TICKS);
    }

    public void takeCleanPlates(Player player) {
        if (!canTakeCleanPlates()) {
            return;
        }
        int count = storedPlates;
        storedPlates = 0;
        ItemStack cleanPlates = new ItemStack(ModItems.CLEAN_PLATE.get(), count);
        if (!player.getInventory().add(cleanPlates)) {
            player.drop(cleanPlates, false);
        }
        playSound(ModSounds.PLATE_PLACE.get(), 0.75f, 0.95f + player.level().random.nextFloat() * 0.1f);
        playSound(ModSounds.DISHWASHER_IN.get(), 0.85f, 1.05f);
        setState(STATE_EMPTY_IN, EMPTY_IN_TICKS);
    }

    public void setClientPreviewOpen(boolean open) {
        if (level == null || !level.isClientSide || state != STATE_IDLE || this.clientPreviewOpen == open) {
            return;
        }
        this.clientPreviewOpen = open;
        this.lastAnimation = "";
        if (open) {
            playClientPreviewOutSound();
        }
    }

    private void tickServer() {
        if (state == STATE_IDLE || state == STATE_CLEAR_READY) {
            return;
        }
        handleTimedEffects();
        if (timer > 0) {
            timer--;
            setChanged();
            return;
        }
        switch (state) {
            case STATE_EMPTY_IN -> setState(STATE_IDLE, 0);
            case STATE_DIRTY_IN -> setState(STATE_WASHING, washingTicksForStoredPlates());
            case STATE_WASHING -> setState(STATE_CLEAR_OUT, CLEAR_OUT_TICKS);
            case STATE_CLEAR_OUT -> setState(STATE_CLEAR_READY, 0);
            default -> setState(STATE_IDLE, 0);
        }
    }

    private int washingTicksForStoredPlates() {
        return storedPlates > 8 ? MANY_PLATE_WASHING_TICKS : WASHING_TICKS;
    }

    private void setState(int state, int timer) {
        this.state = state;
        this.timer = timer;
        this.clientPreviewOpen = false;
        this.lastAnimation = "";
        setChanged();
        sync();
    }

    private void sync() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    private static int removeDirtyPlates(Player player, int maxCount) {
        int remaining = maxCount;
        int removed = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(ModItems.DIRTY_PLATE.get())) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
            removed += take;
        }
        return removed;
    }

    private void handleTimedEffects() {
        if (!(level instanceof ServerLevel serverWorld)) {
            return;
        }
        int elapsed = getElapsedStateTicks();
        switch (state) {
            case STATE_EMPTY_IN -> {
                if (elapsed == 0) spawnSparkleParticles(serverWorld, 7);
            }
            case STATE_DIRTY_IN -> {
                if (elapsed == DIRTY_IN_PLATE_PLACE_TICK) {
                    playSound(ModSounds.PLATE_PLACE.get(), 0.75f, 0.95f + serverWorld.random.nextFloat() * 0.1f);
                    spawnSparkleParticles(serverWorld, 10);
                }
            }
            case STATE_WASHING -> {
                if (elapsed == 0 || (storedPlates > 8 && elapsed == WASHING_TICKS)) {
                    playSound(ModSounds.DISHWASHER_WASHING.get(), 0.9f, 1.0f);
                    spawnWhiteSparkleParticles(serverWorld, 12);
                } else if (elapsed % 10 == 0) {
                    spawnWhiteSparkleParticles(serverWorld, 4);
                }
            }
            case STATE_CLEAR_OUT -> {
                if (elapsed == 0) {
                    playSound(ModSounds.DISHWASHER_CLEAR_OUT.get(), 0.95f, 1.0f);
                    spawnDoorSmoke(serverWorld);
                    spawnSparkleParticles(serverWorld, 12);
                }
            }
            default -> {
            }
        }
    }

    private int getElapsedStateTicks() {
        return switch (state) {
            case STATE_EMPTY_IN -> EMPTY_IN_TICKS - timer;
            case STATE_DIRTY_IN -> DIRTY_IN_TICKS - timer;
            case STATE_WASHING -> washingTicksForStoredPlates() - timer;
            case STATE_CLEAR_OUT -> CLEAR_OUT_TICKS - timer;
            default -> 0;
        };
    }

    private void playClientPreviewOutSound() {
        if (level != null) {
            level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5, ModSounds.DISHWASHER_OUT.get(), SoundSource.BLOCKS, 0.75f, 1.0f, false);
        }
    }

    private void playSound(SoundEvent sound, float volume, float pitch) {
        if (level != null) {
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
        }
    }

    private void spawnSparkleParticles(ServerLevel serverWorld, int count) {
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.85;
        double z = worldPosition.getZ() + 0.5;
        serverWorld.sendParticles(ParticleTypes.END_ROD, x, y, z, count, 0.28, 0.22, 0.28, 0.015);
    }

    private void spawnWhiteSparkleParticles(ServerLevel serverWorld, int count) {
        spawnSparkleParticles(serverWorld, count);
    }

    private void spawnDoorSmoke(ServerLevel serverWorld) {
        Direction facing = getBlockState().hasProperty(DishwasherBlock.FACING) ? getBlockState().getValue(DishwasherBlock.FACING) : Direction.NORTH;
        double x = worldPosition.getX() + 0.5 + facing.getStepX() * 0.56;
        double y = worldPosition.getY() + 0.62;
        double z = worldPosition.getZ() + 0.5 + facing.getStepZ() * 0.56;
        serverWorld.sendParticles(ParticleTypes.CLOUD, x, y, z, 18, 0.18, 0.15, 0.18, 0.025);
        serverWorld.sendParticles(ParticleTypes.POOF, x, y + 0.05, z, 8, 0.12, 0.1, 0.12, 0.01);
    }

    private String currentAnimationName() {
        return switch (state) {
            case STATE_EMPTY_IN -> "empty_in";
            case STATE_DIRTY_IN -> "dirty_in";
            case STATE_WASHING -> "washing";
            case STATE_CLEAR_OUT, STATE_CLEAR_READY -> "clear_out";
            default -> clientPreviewOpen ? "empty_out" : "idle";
        };
    }

    private RawAnimation currentAnimation() {
        String animation = currentAnimationName();
        if ("idle".equals(animation)) {
            return RawAnimation.begin();
        }
        if ("washing".equals(animation)) {
            return RawAnimation.begin().thenLoop(animation);
        }
        return RawAnimation.begin().thenPlay(animation);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, animationState -> {
            String animation = currentAnimationName();
            if (!animation.equals(lastAnimation)) {
                lastAnimation = animation;
                animationState.getController().forceAnimationReset();
            }
            if ("idle".equals(animation)) {
                return PlayState.STOP;
            }
            animationState.setAndContinue(currentAnimation());
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        nbt.putInt("State", state);
        nbt.putInt("Timer", timer);
        nbt.putInt("StoredPlates", storedPlates);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        String previousAnimation = currentAnimationName();
        super.loadAdditional(nbt, registryLookup);
        this.state = nbt.getInt("State");
        this.timer = nbt.getInt("Timer");
        this.storedPlates = Math.max(0, Math.min(MAX_PLATES, nbt.getInt("StoredPlates")));
        this.clientPreviewOpen = false;
        if (!previousAnimation.equals(currentAnimationName())) {
            this.lastAnimation = "";
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return saveWithoutMetadata(registryLookup);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registryLookup) {
        loadAdditional(tag, registryLookup);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registryLookup) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, registryLookup);
        }
    }
}

