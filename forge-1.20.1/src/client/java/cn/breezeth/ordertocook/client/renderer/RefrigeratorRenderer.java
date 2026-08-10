package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.RefrigeratorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
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
            public void renderForBone(PoseStack poseStack, RefrigeratorBlockEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                if (!"bone".equals(bone.getName())) {
                    return;
                }
                if (animatable.isUpperOpenForDisplay()) {
                    renderDisplayItems(animatable, animatable.getDisplayItems(true, UPPER_DISPLAY_LIMIT), true, poseStack, bufferSource, packedLight);
                }
                if (animatable.isLowerOpenForDisplay()) {
                    renderDisplayItems(animatable, animatable.getDisplayItems(false, LOWER_DISPLAY_LIMIT), false, poseStack, bufferSource, packedLight);
                }
                if (renderType != null) {
                    bufferSource.getBuffer(renderType);
                }
            }
        });
    }

    private static void renderDisplayItems(RefrigeratorBlockEntity refrigerator, List<ItemStack> stacks, boolean upper, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
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
            float[] pos = positions[i];
            poseStack.pushPose();
            poseStack.translate(pos[0], pos[1], pos[2]);
            poseStack.mulPose(Axis.XP.rotationDegrees(60.0f));
            poseStack.scale(0.28f, 0.28f, 0.28f);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack,
                    ItemDisplayContext.FIXED,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    bufferSource,
                    refrigerator.getLevel(),
                    i
            );
            poseStack.popPose();
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
