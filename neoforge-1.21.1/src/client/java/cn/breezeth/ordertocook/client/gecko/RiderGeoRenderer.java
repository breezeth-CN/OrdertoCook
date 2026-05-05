package cn.breezeth.ordertocook.client.gecko;

import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class RiderGeoRenderer extends GeoObjectRenderer<RiderAnimatable> {
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
                matrices.translate(0.0, 1.5, 0);
                ItemStack helmetStack = new ItemStack(ModItems.HELMET.get());
                Minecraft.getInstance().getItemRenderer().render(
                        helmetStack,
                        ItemDisplayContext.HEAD,
                        false,
                        matrices,
                        bufferSource,
                        packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        cn.breezeth.ordertocook.client.render.MotorcycleHelmetModels.getForMotorcycleColor(motorcycle.getMotorcycleColor())
                );
                matrices.popPose();
            }
        });
    }
}

