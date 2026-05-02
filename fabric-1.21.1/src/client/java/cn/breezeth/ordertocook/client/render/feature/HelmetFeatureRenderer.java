package cn.breezeth.ordertocook.client.render.feature;

import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class HelmetFeatureRenderer<T extends LivingEntity, M extends EntityModel<T> & ModelWithHead> extends FeatureRenderer<T, M> {

    public HelmetFeatureRenderer(FeatureRendererContext<T, M> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(entity.getVehicle());
        if (motorcycle != null) {
            matrices.push();

            this.getContextModel().getHead().rotate(matrices);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));

            int colorIndex = motorcycle.getMotorcycleColor();
            Identifier modelId = switch (colorIndex) {
                case 1 -> Identifier.of("ordertocook", "item/helmet_red");
                case 2 -> Identifier.of("ordertocook", "item/helmet_blue");
                case 3 -> Identifier.of("ordertocook", "item/helmet_yellow");
                case 4, 5 -> Identifier.of("ordertocook", "item/helmet_white");
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
                vertexConsumers,
                light,
                net.minecraft.client.render.OverlayTexture.DEFAULT_UV,
                colorModel
            );

            matrices.pop();
        }
    }
}

