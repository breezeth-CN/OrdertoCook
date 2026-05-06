package cn.breezeth.ordertocook.client.gecko;

import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public final class RiderRenderBridge {
    private static final GeoObjectRenderer<RiderAnimatable> RENDERER = new RiderGeoRenderer();
    private static final Map<UUID, RiderAnimatable> CACHE = new ConcurrentHashMap<>();

    private RiderRenderBridge() {
    }

    public static RiderAnimatable getOrCreateAnimatable(AbstractClientPlayer player) {
        RiderAnimatable animatable = CACHE.get(player.getUUID());
        if (animatable != null && animatable.getPlayer() == player) {
            return animatable;
        }

        RiderAnimatable recreated = new RiderAnimatable(player);
        CACHE.put(player.getUUID(), recreated);
        return recreated;
    }

    public static boolean triggerChairClap(AbstractClientPlayer player) {
        return getOrCreateAnimatable(player).triggerChairClap();
    }

    public static void triggerWashStart(AbstractClientPlayer player) {
        getOrCreateAnimatable(player).triggerWashStart();
    }

    public static void triggerWashStop(AbstractClientPlayer player) {
        getOrCreateAnimatable(player).triggerWashStop();
    }

    public static boolean isChairClapping(AbstractClientPlayer player) {
        return getOrCreateAnimatable(player).isChairClapActive();
    }

    public static boolean isWashing(AbstractClientPlayer player) {
        return getOrCreateAnimatable(player).isWashingActive();
    }

    public static FirstPersonArmCalibration.Profile getFirstPersonProfile(AbstractClientPlayer player) {
        return getOrCreateAnimatable(player).getFirstPersonProfile();
    }

    public static void setDebugChairClapLoop(AbstractClientPlayer player, boolean enabled) {
        getOrCreateAnimatable(player).setDebugChairClapLoop(enabled);
    }

    public static boolean isDebugChairClapLoop(AbstractClientPlayer player) {
        return getOrCreateAnimatable(player).isDebugChairClapLoop();
    }

    public static boolean toggleAnimationPaused(AbstractClientPlayer player) {
        return getOrCreateAnimatable(player).toggleAnimationPaused();
    }

    public static boolean isAnimationPaused(AbstractClientPlayer player) {
        return getOrCreateAnimatable(player).isAnimationPaused();
    }

    public static boolean shouldReplaceWithGeoRider(AbstractClientPlayer player) {
        if (MotorcycleEntity.fromVehicle(player.getVehicle()) != null) {
            return true;
        }
        if (player.getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null) {
            return true;
        }
        return getOrCreateAnimatable(player).isWashingActive();
    }

    public static void renderGeoRiderBody(
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light,
            VertexConsumer bodyConsumer,
            EntityModel<?> vanillaModel
    ) {
        RiderAnimatable animatable = getOrCreateAnimatable(player);
        RenderType layer = vanillaModel.renderType(player.getSkinTextureLocation());
        RENDERER.render(matrices, animatable, vertexConsumers, layer, bodyConsumer, light);
    }

    public static boolean renderIfAnimated(AbstractClientPlayer player, float entityYaw, PoseStack matrices, MultiBufferSource vertexConsumers, int light, float tickDelta) {
        MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
        boolean chairSeat = player.getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null;
        RiderAnimatable animatable = getOrCreateAnimatable(player);
        boolean washing = animatable.isWashingActive();
        if (motorcycle == null && !chairSeat && !washing) {
            return false;
        }

        matrices.pushPose();
        if (motorcycle != null) {
            matrices.translate(0.0, -0.68, 0.0);
            matrices.mulPose(Axis.YP.rotationDegrees(-motorcycle.getYRot()));
            matrices.translate(-0.5, 0.0, -0.55);
        } else if (washing) {
            matrices.translate(0.0, -0.58, 0.0);
            matrices.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));
            matrices.translate(-0.5, 0.0, -0.5);
        } else {
            matrices.translate(0.0, -1.125, 0.0);
            matrices.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));
            matrices.translate(-0.5, 0.0, -0.5);
        }
        RenderType layer = RenderType.entityCutout(player.getSkinTextureLocation());
        VertexConsumer consumer = vertexConsumers.getBuffer(layer);
        RENDERER.render(matrices, animatable, vertexConsumers, layer, consumer, light);
        matrices.popPose();
        return true;
    }

    public static void triggerHorn(AbstractClientPlayer player, boolean animateHorn) {
        RiderAnimatable animatable = getOrCreateAnimatable(player);
        animatable.triggerHorn(animateHorn);
    }

    public static void triggerLightToggle(AbstractClientPlayer player, boolean animateLightToggle) {
        RiderAnimatable animatable = getOrCreateAnimatable(player);
        animatable.triggerLightToggle(animateLightToggle);
    }

    public static int getLightToggleDelayTicks() {
        return RiderAnimatable.getLightToggleDelayTicks();
    }

    public static boolean isHornAnimating(AbstractClientPlayer player) {
        RiderAnimatable animatable = CACHE.get(player.getUUID());
        return animatable != null && animatable.isHornActive();
    }

    public static boolean isRepeatHornAnimating(AbstractClientPlayer player) {
        RiderAnimatable animatable = CACHE.get(player.getUUID());
        return animatable != null && animatable.isRepeatHornAnimation();
    }

    public static void clearMissingPlayer(UUID uuid) {
        CACHE.remove(uuid);
    }

    public static void clearAll() {
        CACHE.clear();
    }
}
