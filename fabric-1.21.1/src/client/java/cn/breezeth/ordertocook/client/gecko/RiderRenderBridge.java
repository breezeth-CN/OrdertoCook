package cn.breezeth.ordertocook.client.gecko;

import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RiderRenderBridge {
    private static final GeoObjectRenderer<RiderAnimatable> RENDERER = new RiderGeoRenderer();
    private static final Map<UUID, RiderAnimatable> CACHE = new ConcurrentHashMap<>();

    private RiderRenderBridge() {
    }

    public static RiderAnimatable getOrCreateAnimatable(AbstractClientPlayerEntity player) {
        RiderAnimatable animatable = CACHE.get(player.getUuid());
        if (animatable != null && animatable.getPlayer() == player) {
            return animatable;
        }

        RiderAnimatable recreated = new RiderAnimatable(player);
        CACHE.put(player.getUuid(), recreated);
        return recreated;
    }

    public static boolean triggerChairClap(AbstractClientPlayerEntity player) {
        return getOrCreateAnimatable(player).triggerChairClap();
    }

    public static void triggerWashStart(AbstractClientPlayerEntity player) {
        getOrCreateAnimatable(player).triggerWashStart();
    }

    public static void triggerWashStop(AbstractClientPlayerEntity player) {
        getOrCreateAnimatable(player).triggerWashStop();
    }

    public static boolean isChairClapping(AbstractClientPlayerEntity player) {
        return getOrCreateAnimatable(player).isChairClapActive();
    }

    public static boolean isWashing(AbstractClientPlayerEntity player) {
        return getOrCreateAnimatable(player).isWashingActive();
    }

    public static FirstPersonArmCalibration.Profile getFirstPersonProfile(AbstractClientPlayerEntity player) {
        return getOrCreateAnimatable(player).getFirstPersonProfile();
    }

    public static void setDebugChairClapLoop(AbstractClientPlayerEntity player, boolean enabled) {
        getOrCreateAnimatable(player).setDebugChairClapLoop(enabled);
    }

    public static boolean isDebugChairClapLoop(AbstractClientPlayerEntity player) {
        return getOrCreateAnimatable(player).isDebugChairClapLoop();
    }

    public static boolean toggleAnimationPaused(AbstractClientPlayerEntity player) {
        return getOrCreateAnimatable(player).toggleAnimationPaused();
    }

    public static boolean isAnimationPaused(AbstractClientPlayerEntity player) {
        return getOrCreateAnimatable(player).isAnimationPaused();
    }

    public static boolean renderIfAnimated(AbstractClientPlayerEntity player, float entityYaw, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta) {
        MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
        boolean chairSeat = player.getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null;
        RiderAnimatable animatable = getOrCreateAnimatable(player);
        boolean washing = animatable.isWashingActive();
        if (motorcycle == null && !chairSeat && !washing) {
            return false;
        }

        matrices.push();
        if (motorcycle != null) {
            matrices.translate(0.0, -0.68, 0.0);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-motorcycle.getYaw()));
            matrices.translate(-0.5, 0.0, -0.55);
        } else if (washing) {
            matrices.translate(0.0, -0.58, 0.0);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - entityYaw));
            matrices.translate(-0.5, 0.0, -0.5);
        } else {
            matrices.translate(0.0, -1.125, 0.0);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - entityYaw));
            matrices.translate(-0.5, 0.0, -0.5);
        }
        RenderLayer layer = RenderLayer.getEntityCutout(player.getSkinTextures().texture());
        VertexConsumer consumer = vertexConsumers.getBuffer(layer);
        RENDERER.render(matrices, animatable, vertexConsumers, layer, consumer, light, tickDelta);
        matrices.pop();
        return true;
    }

    public static void triggerHorn(AbstractClientPlayerEntity player, boolean animateHorn) {
        RiderAnimatable animatable = getOrCreateAnimatable(player);
        animatable.triggerHorn(animateHorn);
    }

    public static void triggerLightToggle(AbstractClientPlayerEntity player, boolean animateLightToggle) {
        RiderAnimatable animatable = getOrCreateAnimatable(player);
        animatable.triggerLightToggle(animateLightToggle);
    }

    public static int getLightToggleDelayTicks() {
        return RiderAnimatable.getLightToggleDelayTicks();
    }

    public static boolean isHornAnimating(AbstractClientPlayerEntity player) {
        RiderAnimatable animatable = CACHE.get(player.getUuid());
        return animatable != null && animatable.isHornActive();
    }

    public static boolean isRepeatHornAnimating(AbstractClientPlayerEntity player) {
        RiderAnimatable animatable = CACHE.get(player.getUuid());
        return animatable != null && animatable.isRepeatHornAnimation();
    }

    public static void clearMissingPlayer(UUID uuid) {
        CACHE.remove(uuid);
    }

    public static void clearAll() {
        CACHE.clear();
    }
}
