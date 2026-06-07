package cn.breezeth.ordertocook.mixin.client.vehicle;

import cn.breezeth.ordertocook.client.gecko.RiderRenderBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Redirect(
            method = {
                    "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                    "m_7392_(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;m_7695_(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
                    remap = false
            ),
            require = 0,
            remap = false
    )
    private void ordertocook$replacePlayerBodyWithGeo(
            EntityModel<?> model,
            PoseStack matrices,
            VertexConsumer vertexConsumer,
            int light,
            int overlay,
            float red,
            float green,
            float blue,
            float alpha,
            LivingEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack matricesPassThrough,
            MultiBufferSource vertexConsumers,
            int packedLight
    ) {
        if (entity instanceof AbstractClientPlayer player && RiderRenderBridge.shouldReplaceWithGeoRider(player)) {
            RiderRenderBridge.renderGeoRiderBody(player, entityYaw, partialTick, matrices, vertexConsumers, light, vertexConsumer, model);
            return;
        }
        model.renderToBuffer(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
    }
}
