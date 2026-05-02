package cn.breezeth.ordertocook.mixin.client;

import cn.breezeth.ordertocook.client.gecko.RiderRenderBridge;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 在 {@link LivingEntityRenderer} 已执行 {@code setupTransforms/scale} 之后替换玩家身体渲染。
 * 若在 {@link net.minecraft.client.render.entity.PlayerEntityRenderer} 的 HEAD 取消整段渲染，矩阵栈不含实体位姿，会产生整体偏移。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Redirect(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V"
            )
    )
    /**
     * 必须为<strong>实例方法</strong>：宿主 {@code method_4054} 是实例方法；部分 Forge/信雅互联 等桥接环境会校验
     * {@code static} 与目标不一致并抛出 {@code InvalidInjectionException}。功能与原先相同。
     */
    private void ordertocook$replacePlayerBodyWithGeo(
            EntityModel<?> model,
            MatrixStack matrices,
            VertexConsumer vertexConsumer,
            int light,
            int overlay,
            float red,
            float green,
            float blue,
            float alpha,
            LivingEntity entity,
            float limbAngle,
            float limbDistance,
            MatrixStack matricesPassThrough,
            VertexConsumerProvider vertexConsumers,
            int coercedLight
    ) {
        if (entity instanceof AbstractClientPlayerEntity player && RiderRenderBridge.shouldReplaceWithGeoRider(player)) {
            RiderRenderBridge.renderGeoRiderBody(player, limbAngle, limbDistance, matrices, vertexConsumers, light, vertexConsumer, model);
            return;
        }
        model.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
    }
}
