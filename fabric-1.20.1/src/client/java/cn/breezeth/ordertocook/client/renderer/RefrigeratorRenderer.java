package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.RefrigeratorBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

public final class RefrigeratorRenderer extends GeoBlockRenderer<RefrigeratorBlockEntity> {
    private static final int UPPER_DISPLAY_LIMIT = 8;
    private static final int LOWER_DISPLAY_LIMIT = 8;

    public RefrigeratorRenderer() {
        super(new RefrigeratorGeoModel());
        this.addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void renderForBone(MatrixStack matrices, RefrigeratorBlockEntity animatable, GeoBone bone, RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                if (!"bone".equals(bone.getName())) {
                    return;
                }
                if (animatable.isUpperOpenForDisplay()) {
                    renderDisplayItems(animatable, animatable.getDisplayItems(true, UPPER_DISPLAY_LIMIT), true, matrices, bufferSource, packedLight);
                }
                if (animatable.isLowerOpenForDisplay()) {
                    renderDisplayItems(animatable, animatable.getDisplayItems(false, LOWER_DISPLAY_LIMIT), false, matrices, bufferSource, packedLight);
                }
                if (renderType != null) {
                    bufferSource.getBuffer(renderType);
                }
            }
        });
    }

    private static void renderDisplayItems(RefrigeratorBlockEntity refrigerator, List<ItemStack> stacks, boolean upper, MatrixStack matrices, VertexConsumerProvider bufferSource, int packedLight) {
        if (stacks.isEmpty()) {
            return;
        }

        float[][] positions = upper ? upperPositions() : lowerPositions();
        int count = Math.min(stacks.size(), positions.length);
        for (int i = 0; i < count; i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            float[] itemPos = positions[i];
            matrices.push();
            matrices.translate(itemPos[0], itemPos[1], itemPos[2]);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(60.0f));
            matrices.scale(0.28f, 0.28f, 0.28f);
            MinecraftClient.getInstance().getItemRenderer().renderItem(
                    stack,
                    ModelTransformationMode.FIXED,
                    packedLight,
                    OverlayTexture.DEFAULT_UV,
                    matrices,
                    bufferSource,
                    refrigerator.getWorld(),
                    i
            );
            matrices.pop();
        }
    }

        private static float[][] upperPositions() {
        float x1 = -0.24f, x2 = 0.24f;
        return new float[][]{
                {x1, 1.625f, -0.18f},  // slot 1: 上门上 左
                {x2, 1.625f, -0.18f},  // slot 2: 上门上 右
                {x1, 1.3125f, -0.18f},  // slot 3: 上门下 左
                {x2, 1.3125f, -0.18f},  // slot 4: 上门下 右
                {x1, 1.75f, -0.18f},  // slot 5: 上门上+2px 左
                {x2, 1.75f, -0.18f},  // slot 6: 上门上+2px 右
                {x1, 1.4375f, -0.18f},  // slot 7: 上门下+2px 左
                {x2, 1.4375f, -0.18f},  // slot 8: 上门下+2px 右
        };
    }

        private static float[][] lowerPositions() {
        float x1 = -0.24f, x2 = 0.24f;
        return new float[][]{
                {x1, 0.75f, -0.18f},  // slot 1: 下门上 左
                {x2, 0.75f, -0.18f},  // slot 2: 下门上 右
                {x1, 0.1875f, -0.18f},  // slot 3: 下门下 左
                {x2, 0.1875f, -0.18f},  // slot 4: 下门下 右
                {x1, 0.9375f, -0.18f},  // slot 5: 下门上+4px 左
                {x2, 0.9375f, -0.18f},  // slot 6: 下门上+4px 右
                {x1, 0.375f, -0.18f},  // slot 7: 下门下+4px 左
                {x2, 0.375f, -0.18f},  // slot 8: 下门下+4px 右
        };
    }
}
