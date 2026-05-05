package cn.breezeth.ordertocook.mixin.client.vehicle;

import cn.breezeth.ordertocook.registry.ModItems;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderItem(ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BakedModel model, CallbackInfo ci) {
        if (stack.isOf(ModItems.HELMET) && renderMode == ModelTransformationMode.HEAD) {
            matrices.push();
            
            // 鍏抽敭淇锛氭墜鍔ㄥ簲鐢ㄦā鍨?JSON 涓殑 "display" 璁剧疆 (translation, rotation, scale)
            model.getTransformation().getTransformation(renderMode).apply(leftHanded, matrices);

            // 閽堝澶寸洈鍦ㄥご涓婃覆鏌撴椂杩涜鐗规畩澶勭悊锛氬垎涓ゆ娓叉煋
            // 绗竴娆★細娓叉煋涓嶉€忔槑閮ㄥ垎 (Cutout)
            VertexConsumer cutoutConsumer = vertexConsumers.getBuffer(RenderLayer.getCutout());
            renderFilteredBakedItemModel(model, light, overlay, matrices, cutoutConsumer, false);

            // 绗簩娆★細娓叉煋鍗婇€忔槑鐜荤拑閮ㄥ垎 (Translucent)
            renderFilteredBakedItemModel(model, light, overlay, matrices, cutoutConsumer, true);

            matrices.pop();
            ci.cancel();
        }
    }

    /**
     * 杩囨护娓叉煋妯″瀷銆傛牴鎹?isGlass 鍐冲畾娓叉煋鍝竴閮ㄥ垎
     */
    private void renderFilteredBakedItemModel(BakedModel model, int light, int overlay, MatrixStack matrices, VertexConsumer vertices, boolean isGlass) {
        Random random = Random.create(42L);

        for (Direction direction : Direction.values()) {
            renderFilteredQuads(matrices, vertices, model.getQuads(null, direction, random), light, overlay, isGlass);
        }

        renderFilteredQuads(matrices, vertices, model.getQuads(null, null, random), light, overlay, isGlass);
    }

    private void renderFilteredQuads(MatrixStack matrices, VertexConsumer vertices, List<BakedQuad> quads, int light, int overlay, boolean isGlass) {
        MatrixStack.Entry entry = matrices.peek();
        
        for (BakedQuad bakedQuad : quads) {
            Sprite sprite = bakedQuad.getSprite();
            if (sprite == null) continue;
            
            String spritePath = sprite.getContents().getId().getPath();
            boolean quadIsGlass = spritePath.contains("glass") || spritePath.contains("visor") || spritePath.contains("translucent");
            
            if (quadIsGlass == isGlass) {
                vertices.quad(entry, bakedQuad, 1.0f, 1.0f, 1.0f, 1.0f, light, overlay);
            }
        }
    }
}

