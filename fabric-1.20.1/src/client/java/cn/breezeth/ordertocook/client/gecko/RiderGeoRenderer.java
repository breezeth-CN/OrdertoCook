package cn.breezeth.ordertocook.client.gecko;

import cn.breezeth.ordertocook.client.render.MotorcycleHelmetModels;
import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * 骑乘/椅子/洗碗时替换玩家身体。父类为方块空间；实体矩阵上需轴向修正，并抵消 GeoObjectRenderer 的方块中心平移。
 * 骑摩托时在 {@code head} 骨骼上叠绘与车身颜色一致的头盔（与 fabric-1.21.1 一致）。
 */
public final class RiderGeoRenderer extends GeoObjectRenderer<RiderAnimatable> {
    /** 与 fabric-1.21.1 一致：头骨局部 Y，方块人头顶相对 pivot */
    private static final float PX = 1.0f / 16.0f;
    private static final float HELMET_OVER_HEAD_Y = 1.5f;
    private static final float HELMET_LEFT_BLOCKS = -0f * PX;
    private static final float HELMET_BACK_BLOCKS = 0f * PX;
    /** 调整头盔位置 */
    
    public RiderGeoRenderer() {
        super(new RiderModel());
        this.addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void renderForBone(MatrixStack matrices, RiderAnimatable animatable, GeoBone bone, RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                if (!"head".equals(bone.getName())) {
                    return;
                }
                MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(animatable.getPlayer().getVehicle());
                if (motorcycle == null) {
                    return;
                }
                matrices.push();
                matrices.translate(0.0f, HELMET_OVER_HEAD_Y, 0.0f);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
                matrices.translate(0.0f, 0.0f, HELMET_BACK_BLOCKS);
                matrices.translate(HELMET_LEFT_BLOCKS, 0.0f, 0.0f);
                matrices.scale(-1.0f, -1.0f, 1.0f);
                try {
                    Vec3d eye = animatable.getPlayer().getCameraPosVec(partialTick);
                    int helmetLight = WorldRenderer.getLightmapCoordinates(animatable.getPlayer().getWorld(), BlockPos.ofFloored(eye));
                    BakedModel colorModel = MotorcycleHelmetModels.getForMotorcycleColor(motorcycle.getMotorcycleColor());
                    ItemStack helmetStack = new ItemStack(ModItems.HELMET);
                    MinecraftClient.getInstance().getItemRenderer().renderItem(
                            helmetStack,
                            ModelTransformationMode.HEAD,
                            false,
                            matrices,
                            bufferSource,
                            helmetLight,
                            OverlayTexture.DEFAULT_UV,
                            colorModel
                    );
                } finally {
                    if (renderType != null) {
                        bufferSource.getBuffer(renderType);
                    }
                }
                matrices.pop();
            }
        });
    }

    @Override
    public void preRender(MatrixStack poseStack, RiderAnimatable animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0f));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.translate(-0.5f, -0.51f, -0.5f);
        poseStack.translate(0.0f, -animatable.getPlayer().getHeight() - animatable.getGeoBodyExtraDownBlocks(), 0.0f);
    }
}
