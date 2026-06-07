package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.WashingTableBlock;
import cn.breezeth.ordertocook.block.entity.WashingTableBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
import org.joml.Matrix4f;

/**
 * 与 1.20.1 一致：translucent 水面单独绘制，主模型 cutout。
 */
public final class WashingTableWaterRenderer implements BlockEntityRenderer<WashingTableBlockEntity> {
    private static final Identifier WATER_TEXTURE =
            Identifier.of(ModConstants.MOD_ID, "textures/block/water.png");

    private static final float X0 = 3f / 16f;
    private static final float X1 = 13f / 16f;
    private static final float Z0 = 3f / 16f;
    private static final float Z1 = 12f / 16f;
    private static final float Y = 15.08f / 16f;

    public WashingTableWaterRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(
            WashingTableBlockEntity entity,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay
    ) {
        World world = entity.getWorld();
        if (world == null) {
            return;
        }
        BlockState state = entity.getCachedState();
        if (!state.contains(WashingTableBlock.FACING)) {
            return;
        }
        Direction facing = state.get(WashingTableBlock.FACING);

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(WATER_TEXTURE));

        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(switch (facing) {
            case NORTH -> 0f;
            case EAST -> 270f;
            case SOUTH -> 180f;
            case WEST -> 90f;
            default -> 0f;
        }));
        matrices.translate(-0.5, -0.5, -0.5);

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f posMat = entry.getPositionMatrix();

        RenderSystem.disableCull();

        float nx = 0f;
        float ny = 1f;
        float nz = 0f;
        emitVertex(buffer, posMat, entry, X0, Y, Z0, 0f, 0f, overlay, light, nx, ny, nz);
        emitVertex(buffer, posMat, entry, X1, Y, Z0, 1f, 0f, overlay, light, nx, ny, nz);
        emitVertex(buffer, posMat, entry, X1, Y, Z1, 1f, 1f, overlay, light, nx, ny, nz);
        emitVertex(buffer, posMat, entry, X0, Y, Z1, 0f, 1f, overlay, light, nx, ny, nz);

        matrices.pop();

        RenderSystem.enableCull();
    }

    private static void emitVertex(
            VertexConsumer buffer,
            Matrix4f posMat,
            MatrixStack.Entry pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int overlay,
            int light,
            float nx,
            float ny,
            float nz
    ) {
        buffer.vertex(posMat, x, y, z)
                .color(1f, 1f, 1f, 1f)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(pose, nx, ny, nz);
    }
}
