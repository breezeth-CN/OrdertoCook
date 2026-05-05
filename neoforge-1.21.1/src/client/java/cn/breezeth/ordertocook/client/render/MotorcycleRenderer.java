package cn.breezeth.ordertocook.client.render;

import cn.breezeth.ordertocook.client.render.model.MotorcycleModel;
import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;

public class MotorcycleRenderer extends EntityRenderer<MotorcycleEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "motorcycle"), "main");
    private final MotorcycleModel<MotorcycleEntity> model;

    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/entity/motorcycle.png");
    private static final ResourceLocation RED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/entity/electric_motorcycle_red.png");
    private static final ResourceLocation BLUE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/entity/electric_motorcycle_blue.png");
    private static final ResourceLocation YELLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/entity/electric_motorcycle_yellow.png");
    private static final ResourceLocation LIGHT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/entity/electric_motorcycle_light.png");

    public MotorcycleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new MotorcycleModel<>(context.bakeLayer(LAYER));
    }

    @Override
    public void render(MotorcycleEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        float renderYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-renderYaw));
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0f, -1.501f, 0.0f);

        this.model.setupAnim(entity, 0.0f, 0.0f, (float) entity.tickCount + partialTick, 0.0f, 0.0f);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutout(getTextureLocation(entity)));
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        if (entity.isLightEnabled()) {
            VertexConsumer lightConsumer = bufferSource.getBuffer(RenderType.eyes(LIGHT_TEXTURE));
            this.model.renderToBuffer(poseStack, lightConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MotorcycleEntity entity) {
        return switch (entity.getMotorcycleColor()) {
            case 1 -> RED_TEXTURE;
            case 2 -> BLUE_TEXTURE;
            case 3 -> YELLOW_TEXTURE;
            default -> DEFAULT_TEXTURE;
        };
    }
}
