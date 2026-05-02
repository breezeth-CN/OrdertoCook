package cn.breezeth.ordertocook.mixin.client.vehicle;

import cn.breezeth.ordertocook.registry.ModItems;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 与 1.21.1 一致：HEAD 模式下将不透明与半透明面分两次绘制，避免防风罩在部分视角下缺角/排序错误。
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderItem(
            ItemStack stack,
            ModelTransformationMode renderMode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            BakedModel model,
            CallbackInfo ci
    ) {
        if (stack.isOf(ModItems.HELMET) && renderMode == ModelTransformationMode.HEAD) {
            matrices.push();

            model.getTransformation().getTransformation(renderMode).apply(leftHanded, matrices);

            VertexConsumer cutoutConsumer = vertexConsumers.getBuffer(RenderLayer.getCutout());
            renderFilteredBakedItemModel(model, light, overlay, matrices, cutoutConsumer, false);

            VertexConsumer translucentConsumer = vertexConsumers.getBuffer(RenderLayer.getTranslucent());
            renderFilteredBakedItemModel(model, light, overlay, matrices, translucentConsumer, true);

            matrices.pop();
            ci.cancel();
        }
    }

    private void renderFilteredBakedItemModel(
            BakedModel model,
            int light,
            int overlay,
            MatrixStack matrices,
            VertexConsumer vertices,
            boolean isGlass
    ) {
        Random random = Random.create(42L);

        for (Direction direction : Direction.values()) {
            renderFilteredQuads(matrices, vertices, model.getQuads(null, direction, random), light, overlay, isGlass);
        }

        renderFilteredQuads(matrices, vertices, model.getQuads(null, null, random), light, overlay, isGlass);
    }

    private void renderFilteredQuads(
            MatrixStack matrices,
            VertexConsumer vertices,
            List<BakedQuad> quads,
            int light,
            int overlay,
            boolean isGlass
    ) {
        MatrixStack.Entry entry = matrices.peek();

        for (BakedQuad bakedQuad : quads) {
            Sprite sprite = bakedQuad.getSprite();
            if (sprite == null) {
                continue;
            }

            String spritePath = sprite.getContents().getId().getPath();
            boolean quadIsGlass =
                    spritePath.contains("glass")
                            || spritePath.contains("visor")
                            || spritePath.contains("translucent");

            if (quadIsGlass == isGlass) {
                // 1.20.1：quad 为 RGB 三通道，无独立 alpha 参数
                vertices.quad(entry, bakedQuad, 1.0f, 1.0f, 1.0f, light, overlay);
            }
        }
    }
}
