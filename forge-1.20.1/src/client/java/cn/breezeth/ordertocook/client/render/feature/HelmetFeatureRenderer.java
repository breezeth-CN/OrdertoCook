package cn.breezeth.ordertocook.client.render.feature;

import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import cn.breezeth.ordertocook.client.gecko.RiderRenderBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class HelmetFeatureRenderer<T extends LivingEntity, M extends EntityModel<T> & HeadedModel> extends RenderLayer<T, M> {

    public HelmetFeatureRenderer(RenderLayerParent<T, M> context) {
        super(context);
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(entity.getVehicle());
        if (motorcycle != null) {
            if (entity instanceof AbstractClientPlayer player && RiderRenderBridge.shouldReplaceWithGeoRider(player)) {
                return;
            }
            matrices.pushPose();

            this.getParentModel().getHead().translateAndRotate(matrices);
            matrices.mulPose(Axis.YP.rotationDegrees(180));
            matrices.mulPose(Axis.XP.rotationDegrees(180));

            ItemStack helmetStack = new ItemStack(ModItems.HELMET.get());

            Minecraft.getInstance().getItemRenderer().render(
                helmetStack,
                ItemDisplayContext.HEAD,
                false,
                matrices,
                vertexConsumers,
                light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                cn.breezeth.ordertocook.client.render.MotorcycleHelmetModels.getForMotorcycleColor(motorcycle.getMotorcycleColor())
            );

            matrices.popPose();
        }
    }
}

