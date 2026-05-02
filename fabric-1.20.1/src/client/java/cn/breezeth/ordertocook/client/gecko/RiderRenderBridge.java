package cn.breezeth.ordertocook.client.gecko;

import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 身体替换经 {@link cn.breezeth.ordertocook.mixin.client.LivingEntityRendererMixin} 在 setupTransforms 之后调用，
 * 与实体在世界中的位置一致。
 */
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

    public static boolean shouldReplaceWithGeoRider(AbstractClientPlayerEntity player) {
        if (MotorcycleEntity.fromVehicle(player.getVehicle()) != null) {
            return true;
        }
        if (player.getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null) {
            return true;
        }
        return getOrCreateAnimatable(player).isWashingActive();
    }

    public static void renderGeoRiderBody(
            AbstractClientPlayerEntity player,
            float limbAngle,
            float limbDistance,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            VertexConsumer bodyConsumer,
            EntityModel<?> vanillaModel
    ) {
        RiderAnimatable animatable = getOrCreateAnimatable(player);
        RenderLayer layer = vanillaModel instanceof PlayerEntityModel<?> pm
                ? pm.getLayer(player.getSkinTexture())
                : RenderLayer.getEntityCutoutNoCull(player.getSkinTexture());

        // 仅使用 LivingEntityRenderer 已应用的矩阵；不在此做平移/旋转补偿（会与碰撞箱错位）。
        RENDERER.render(matrices, animatable, vertexConsumers, layer, bodyConsumer, light);
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
