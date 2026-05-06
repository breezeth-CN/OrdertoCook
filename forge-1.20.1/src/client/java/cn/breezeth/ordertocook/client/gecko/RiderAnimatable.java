package cn.breezeth.ordertocook.client.gecko;

import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.network.ModClientNetworking;
import cn.breezeth.ordertocook.network.ModNetworking;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import cn.breezeth.ordertocook.registry.ModSounds;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class RiderAnimatable implements GeoAnimatable {
    private static final long LIGHT_TOGGLE_DELAY_TICKS = 6L;
    private static final long LIGHT_TOGGLE_DURATION_TICKS = 17L;
    private static final long CHAIR_CLAP_DURATION_TICKS = 60L;
    private static final long WASH_DURATION_TICKS = 80L;

    private final AbstractClientPlayer player;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long hornAnimEndAge = -1;
    private long hornSoundAge = -1;
    private long hornFirstTriggerAge = -1;
    private long lightToggleAnimStartAge = -1;
    private long lightToggleAnimEndAge = -1;
    private long chairClapEndAge = -1;
    private long chairClapFirstSoundAge = -1;
    private long chairClapSecondSoundAge = -1;
    private long chairClapThirdSoundAge = -1;
    private long washEndAge = -1;
    private long lastTickAge = -1;
    private double pausedAnimationTick = 0.0;
    private boolean hornSoundPlayed = true;
    private boolean hornAnimationRestartRequested = false;
    private boolean lightToggleAnimationRestartRequested = false;
    private boolean chairClapAnimationRestartRequested = false;
    private boolean debugChairClapLoop = false;
    private boolean animationPaused = false;
    private boolean useRepeatHornAnimation = false;

    public RiderAnimatable(AbstractClientPlayer player) {
        this.player = player;
    }

    public AbstractClientPlayer getPlayer() {
        return player;
    }

    public float getGeoBodyExtraDownBlocks() {
        return switch (getRenderMode()) {
            case WASH -> -6.0f / 16.0f;
            case DRIVE -> -4.0f / 16.0f;
            case CHAIR -> 4.0f / 16.0f;
            default -> 0.0f;
        };
    }

    public String getAnimationGroup() {
        return switch (getRenderMode()) {
            case CHAIR -> "player";
            case WASH -> "player_wash";
            default -> "drive";
        };
    }

    public FirstPersonArmCalibration.Profile getFirstPersonProfile() {
        RenderMode renderMode = getRenderMode();
        if (renderMode == RenderMode.DRIVE) {
            return FirstPersonArmCalibration.Profile.DRIVE;
        }
        if (renderMode == RenderMode.NONE) {
            return FirstPersonArmCalibration.Profile.DRIVE;
        }
        return FirstPersonArmCalibration.Profile.ACTION;
    }

    public boolean triggerChairClap() {
        if (getRenderMode() != RenderMode.CHAIR || player.tickCount < chairClapEndAge) {
            return false;
        }
        chairClapEndAge = player.tickCount + CHAIR_CLAP_DURATION_TICKS;
        chairClapFirstSoundAge = player.tickCount;
        chairClapSecondSoundAge = player.tickCount + 12L;
        chairClapThirdSoundAge = player.tickCount + 20L;
        chairClapAnimationRestartRequested = true;
        return true;
    }

    public void triggerWashStart() {
        washEndAge = player.tickCount + WASH_DURATION_TICKS;
    }

    public void triggerWashStop() {
        washEndAge = -1L;
    }

    public boolean isChairClapActive() {
        return debugChairClapLoop || player.tickCount < chairClapEndAge;
    }

    public boolean isWashingActive() {
        return player.tickCount < washEndAge;
    }

    public void setDebugChairClapLoop(boolean enabled) {
        debugChairClapLoop = enabled;
        if (enabled) {
            chairClapAnimationRestartRequested = true;
        }
    }

    public boolean isDebugChairClapLoop() {
        return debugChairClapLoop;
    }

    public boolean toggleAnimationPaused() {
        animationPaused = !animationPaused;
        if (animationPaused) {
            pausedAnimationTick = player.tickCount;
        }
        return animationPaused;
    }

    public boolean isAnimationPaused() {
        return animationPaused;
    }

    public boolean isHornActive() {
        return player.tickCount < hornAnimEndAge;
    }

    public boolean isRepeatHornAnimation() {
        return useRepeatHornAnimation;
    }

    public static int getLightToggleDelayTicks() {
        return (int)LIGHT_TOGGLE_DELAY_TICKS;
    }

    public void triggerHorn(boolean animateHorn) {
        if (animateHorn) {
            boolean replayInFastMode =
                    (this.hornAnimEndAge > player.tickCount) ||
                    (this.hornFirstTriggerAge >= 0 && (player.tickCount - this.hornFirstTriggerAge) <= 27L);

            if (replayInFastMode) {
                this.useRepeatHornAnimation = true;
                this.hornAnimEndAge = player.tickCount + 21L;
                this.hornSoundAge = player.tickCount;
                this.hornSoundPlayed = false;
                this.hornAnimationRestartRequested = true;
            } else {
                this.hornFirstTriggerAge = player.tickCount;
                this.useRepeatHornAnimation = false;
                this.hornAnimEndAge = player.tickCount + 27L;
                this.hornSoundAge = player.tickCount + 13L;
                this.hornSoundPlayed = false;
                this.hornAnimationRestartRequested = true;
            }
        } else {
            playHornSound();
            this.hornAnimEndAge = -1L;
            this.hornSoundAge = -1L;
            this.hornFirstTriggerAge = -1L;
            this.hornSoundPlayed = true;
            this.hornAnimationRestartRequested = false;
            this.useRepeatHornAnimation = false;
        }
    }

    public void triggerLightToggle(boolean animateLightToggle) {
        if (!animateLightToggle) {
            this.lightToggleAnimStartAge = -1L;
            this.lightToggleAnimEndAge = -1L;
            this.lightToggleAnimationRestartRequested = false;
            return;
        }
        this.lightToggleAnimStartAge = player.tickCount + LIGHT_TOGGLE_DELAY_TICKS;
        this.lightToggleAnimEndAge = this.lightToggleAnimStartAge + LIGHT_TOGGLE_DURATION_TICKS;
        this.lightToggleAnimationRestartRequested = true;
    }

    private void tickScheduledSounds() {
        if (lastTickAge == player.tickCount) {
            return;
        }
        lastTickAge = player.tickCount;
        if (!hornSoundPlayed && hornSoundAge >= 0 && player.tickCount >= hornSoundAge) {
            playHornSound();
            hornSoundPlayed = true;
        }
        if (chairClapFirstSoundAge >= 0 && player.tickCount >= chairClapFirstSoundAge) {
            playRandomClapSound();
            chairClapFirstSoundAge = -1L;
        }
        if (chairClapSecondSoundAge >= 0 && player.tickCount >= chairClapSecondSoundAge) {
            playRandomClapSound();
            chairClapSecondSoundAge = -1L;
        }
        if (chairClapThirdSoundAge >= 0 && player.tickCount >= chairClapThirdSoundAge) {
            playRandomClapSound();
            chairClapThirdSoundAge = -1L;
        }
    }

    private void playHornSound() {
        if (player.level() != null) {
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.HORN.get(), SoundSource.PLAYERS, 1.0f, 1.0f, false);
            ModClientNetworking.sendRiderSound(ModNetworking.RIDER_SOUND_HORN, 1.0f);
        }
    }

    private void playRandomClapSound() {
        if (player.level() == null) {
            return;
        }
        SoundEvent sound = switch (player.getRandom().nextInt(3)) {
            case 0 -> SoundEvents.VILLAGER_AMBIENT;
            case 1 -> SoundEvents.VILLAGER_TRADE;
            default -> SoundEvents.VILLAGER_CELEBRATE;
        };
        float pitch = switch (player.getRandom().nextInt(3)) {
            case 0 -> 0.82f + player.getRandom().nextFloat() * 0.08f;
            case 1 -> 1.0f + player.getRandom().nextFloat() * 0.10f;
            default -> 1.18f + player.getRandom().nextFloat() * 0.08f;
        };
        player.level().playLocalSound(
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.PLAYERS,
                0.9f,
                pitch,
                false
        );
        int soundType = ModNetworking.RIDER_SOUND_CHAIR_CELEBRATE;
        if (sound == SoundEvents.VILLAGER_AMBIENT) {
            soundType = ModNetworking.RIDER_SOUND_CHAIR_AMBIENT;
        } else if (sound == SoundEvents.VILLAGER_TRADE) {
            soundType = ModNetworking.RIDER_SOUND_CHAIR_TRADE;
        }
        ModClientNetworking.sendRiderSound(soundType, pitch);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, state -> {
            RenderMode renderMode = getRenderMode();
            if (renderMode == RenderMode.NONE) {
                return PlayState.STOP;
            }
            tickScheduledSounds();

            if (renderMode == RenderMode.CHAIR) {
                if (debugChairClapLoop) {
                    if (chairClapAnimationRestartRequested) {
                        state.getController().forceAnimationReset();
                        chairClapAnimationRestartRequested = false;
                    }
                    state.setAndContinue(RawAnimation.begin().thenLoop("player_sit&clap"));
                } else if (player.tickCount < chairClapEndAge) {
                    if (chairClapAnimationRestartRequested) {
                        state.getController().forceAnimationReset();
                        chairClapAnimationRestartRequested = false;
                    }
                    state.setAndContinue(RawAnimation.begin().thenPlay("player_sit&clap"));
                } else {
                    chairClapAnimationRestartRequested = false;
                    state.setAndContinue(RawAnimation.begin().thenLoop("player_sit"));
                }
                return PlayState.CONTINUE;
            }

            if (renderMode == RenderMode.WASH) {
                state.setAndContinue(RawAnimation.begin().thenLoop("player_wash"));
                return PlayState.CONTINUE;
            }

            MotorcycleEntity m = MotorcycleEntity.fromVehicle(player.getVehicle());
            if (m == null) {
                return PlayState.STOP;
            }
            Vec3 forwardVec = Vec3.directionFromRotation(0, m.getYRot()).normalize().scale(-1.0);
            double forwardSpeed = m.getDeltaMovement().dot(forwardVec);
            boolean lightToggleActive = player.tickCount >= lightToggleAnimStartAge && player.tickCount < lightToggleAnimEndAge;
            if (lightToggleActive && forwardSpeed >= -0.01) {
                if (lightToggleAnimationRestartRequested) {
                    state.getController().forceAnimationReset();
                    lightToggleAnimationRestartRequested = false;
                }
                useRepeatHornAnimation = false;
                state.setAndContinue(RawAnimation.begin().thenPlay("drive_didi_light"));
            } else if (player.tickCount < hornAnimEndAge && forwardSpeed >= -0.01) {
                if (hornAnimationRestartRequested) {
                    state.getController().forceAnimationReset();
                    hornAnimationRestartRequested = false;
                }
                if (useRepeatHornAnimation) {
                    state.setAndContinue(RawAnimation.begin().thenPlay("drive_didi_repeat"));
                } else {
                    state.setAndContinue(RawAnimation.begin().thenPlay("drive_didi"));
                }
            } else if (forwardSpeed < -0.01) {
                useRepeatHornAnimation = false;
                state.setAndContinue(RawAnimation.begin().thenLoop("drive_back"));
            } else {
                useRepeatHornAnimation = false;
                state.setAndContinue(RawAnimation.begin().thenLoop("drive_idle"));
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object obj) {
        return animationPaused ? pausedAnimationTick : player.tickCount;
    }

    private RenderMode getRenderMode() {
        MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
        if (motorcycle != null) {
            return RenderMode.DRIVE;
        }
        if (player.getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null) {
            return RenderMode.CHAIR;
        }
        if (isWashingActive()) {
            return RenderMode.WASH;
        }
        return RenderMode.NONE;
    }

    private enum RenderMode {
        NONE,
        DRIVE,
        CHAIR,
        WASH
    }
}
