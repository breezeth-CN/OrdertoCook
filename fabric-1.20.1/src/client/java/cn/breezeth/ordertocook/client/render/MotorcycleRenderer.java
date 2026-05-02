package cn.breezeth.ordertocook.client.render;

import cn.breezeth.ordertocook.client.render.model.MotorcycleModel;
import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class MotorcycleRenderer extends EntityRenderer<MotorcycleEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(new Identifier(ModConstants.MOD_ID, "motorcycle"), "main");

    private static final Identifier DEFAULT_TEXTURE = new Identifier(ModConstants.MOD_ID, "textures/entity/motorcycle.png");
    private static final Identifier RED_TEXTURE = new Identifier(ModConstants.MOD_ID, "textures/entity/electric_motorcycle_red.png");
    private static final Identifier BLUE_TEXTURE = new Identifier(ModConstants.MOD_ID, "textures/entity/electric_motorcycle_blue.png");
    private static final Identifier YELLOW_TEXTURE = new Identifier(ModConstants.MOD_ID, "textures/entity/electric_motorcycle_yellow.png");
    private static final Identifier LIGHT_TEXTURE = new Identifier(ModConstants.MOD_ID, "textures/entity/electric_motorcycle_light.png");

    private final MotorcycleModel model;

    public MotorcycleRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.model = new MotorcycleModel(ctx.getPart(LAYER));
    }

    @Override
    public void render(MotorcycleEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        MinecraftClient client = MinecraftClient.getInstance();
        boolean isLocalRidden = client.player != null && MotorcycleEntity.fromVehicle(client.player.getVehicle()) == entity;
        if (isLocalRidden) {
            double renderX = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
            double renderY = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
            double renderZ = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
            matrices.translate(entity.getX() - renderX, entity.getY() - renderY, entity.getZ() - renderZ);
        }

        float renderYaw = isLocalRidden ? entity.getYaw() : MathHelper.lerpAngleDegrees(tickDelta, entity.prevYaw, entity.getYaw());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-renderYaw));

        matrices.scale(-1.0f, -1.0f, 1.0f);
        matrices.translate(0.0f, -1.501f, 0.0f);

        this.model.setAngles(entity, 0, 0, (float) entity.age + tickDelta, 0, 0);

        Identifier texture = this.getTexture(entity);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(texture));
        this.model.renderWithColor(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
        if (entity.isLightEnabled()) {
            VertexConsumer lightConsumer = vertexConsumers.getBuffer(RenderLayer.getEyes(LIGHT_TEXTURE));
            this.model.renderWithColor(matrices, lightConsumer, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
        }

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(MotorcycleEntity entity) {
        int color = entity.getMotorcycleColor();
        return switch (color) {
            case 1 -> RED_TEXTURE;
            case 2 -> BLUE_TEXTURE;
            case 3 -> YELLOW_TEXTURE;
            default -> DEFAULT_TEXTURE;
        };
    }
}
