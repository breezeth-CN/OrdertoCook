package cn.breezeth.ordertocook.client.gecko;

import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class RiderGeoRenderer extends GeoObjectRenderer<RiderAnimatable> {
    private static final float HELMET_OVER_HEAD_Y = 1.5f;

    public RiderGeoRenderer() {
        super(new RiderModel());
        this.addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void renderForBone(PoseStack matrices, RiderAnimatable animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                if (!"head".equals(bone.getName())) {
                    return;
                }

                MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(animatable.getPlayer().getVehicle());
                if (motorcycle == null) {
                    return;
                }

                matrices.pushPose();
                matrices.translate(0.0f, HELMET_OVER_HEAD_Y, 0.0f);
                matrices.mulPose(Axis.ZP.rotationDegrees(180.0f));
                matrices.scale(-1.0f, -1.0f, 1.0f);
                try {
                    Vec3 eye = animatable.getPlayer().getEyePosition(partialTick);
                    int helmetLight = LevelRenderer.getLightColor(animatable.getPlayer().level(), BlockPos.containing(eye));
                    ItemStack helmetStack = new ItemStack(ModItems.HELMET.get());
                    Minecraft.getInstance().getItemRenderer().render(
                            helmetStack,
                            ItemDisplayContext.HEAD,
                            false,
                            matrices,
                            bufferSource,
                            helmetLight,
                            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                            cn.breezeth.ordertocook.client.render.MotorcycleHelmetModels.getForMotorcycleColor(motorcycle.getMotorcycleColor())
                    );
                } finally {
                    if (renderType != null) {
                        bufferSource.getBuffer(renderType);
                    }
                }
                matrices.popPose();
            }
        });
    }

    @Override
    public void preRender(PoseStack poseStack, RiderAnimatable animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.translate(-0.5f, -0.51f, -0.5f);
        poseStack.translate(0.0f, -animatable.getPlayer().getBbHeight() - animatable.getGeoBodyExtraDownBlocks(), 0.0f);
    }
}

