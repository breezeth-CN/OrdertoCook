package cn.breezeth.ordertocook.client.gecko;

import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class RiderGeoRenderer extends GeoObjectRenderer<RiderAnimatable> {
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
                matrices.translate(0.0, 1.5, 0);
                int colorIndex = motorcycle.getMotorcycleColor();
                Identifier modelId = switch (colorIndex) {
                    case 1 -> Identifier.of("ordertocook", "item/helmet_red");
                    case 2 -> Identifier.of("ordertocook", "item/helmet_blue");
                    case 3 -> Identifier.of("ordertocook", "item/helmet_yellow");
                    case 0 -> Identifier.of("ordertocook", "item/helmet_white");
                    default -> Identifier.of("ordertocook", "item/helmet");
                };

                BakedModel colorModel = MinecraftClient.getInstance().getBakedModelManager().getModel(modelId);
                ItemStack helmetStack = new ItemStack(ModItems.HELMET);
                MinecraftClient.getInstance().getItemRenderer().renderItem(
                        helmetStack,
                        ModelTransformationMode.HEAD,
                        false,
                        matrices,
                        bufferSource,
                        packedLight,
                        net.minecraft.client.render.OverlayTexture.DEFAULT_UV,
                        colorModel
                );
                matrices.pop();
            }
        });
    }
}

