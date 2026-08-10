package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.DishwasherBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.util.Set;

public final class DishwasherRenderer extends GeoBlockRenderer<DishwasherBlockEntity> {
    private static final Set<String> DISH_BONES = Set.of("drity_dish", "clear_dish");

    public DishwasherRenderer() {
        super(new DishwasherGeoModel());
    }

    @Override
    public void renderRecursively(MatrixStack poseStack, DishwasherBlockEntity animatable, GeoBone bone, RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int renderColor) {
        if (DISH_BONES.contains(bone.getName()) && !animatable.shouldRenderDishBone(bone.getName())) {
            return;
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, renderColor);
    }

    @Override
    public void renderCubesOfBone(MatrixStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, int renderColor) {
        if (!DISH_BONES.contains(bone.getName())) {
            super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, renderColor);
            return;
        }
        if (bone.isHidden()) {
            return;
        }
        int cubeIndex = 1;
        for (GeoCube cube : bone.getCubes()) {
            if (shouldRenderDishCube(cubeIndex, getAnimatable().getStoredPlates())) {
                poseStack.push();
                renderCube(poseStack, cube, buffer, packedLight, packedOverlay, renderColor);
                poseStack.pop();
            }
            cubeIndex++;
        }
    }

    private static boolean shouldRenderDishCube(int cubeIndex, int plateCount) {
        if (plateCount <= 0) {
            return false;
        }
        if (plateCount == 1) {
            return cubeIndex == 2;
        }
        if (plateCount == 2) {
            return cubeIndex == 2 || cubeIndex == 5;
        }
        return true;
    }
}
