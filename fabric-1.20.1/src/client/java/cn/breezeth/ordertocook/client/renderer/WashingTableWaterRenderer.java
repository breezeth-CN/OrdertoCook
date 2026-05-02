package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.WashingTableBlock;
import cn.breezeth.ordertocook.block.entity.WashingTableBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class WashingTableWaterRenderer {
    private static final boolean DEBUG_HIGHLIGHT = true;

    private static final SpriteIdentifier WATER_SPRITE = new SpriteIdentifier(
            PlayerScreenHandler.BLOCK_ATLAS_TEXTURE,
            new Identifier("minecraft", "block/water_still")
    );

    private static final float X0 = 3f / 16f;
    private static final float X1 = 13f / 16f;
    private static final float Z0 = 3f / 16f;
    private static final float Z1 = 12f / 16f;
    private static final float Y = (DEBUG_HIGHLIGHT ? 15.1f : 14.98f) / 16f;

    private WashingTableWaterRenderer() {
    }

    public static void drawForEntity(
            WashingTableBlockEntity entity,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay
    ) {
        drawForState(entity.getCachedState(), matrices, vertexConsumers, light, overlay);
    }

    public static void drawForState(
            BlockState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay
    ) {
        if (!state.contains(WashingTableBlock.FACING)) {
            return;
        }

        Direction facing = state.get(WashingTableBlock.FACING);
        Sprite sprite = WATER_SPRITE.getSprite();
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getTranslucent());

        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(switch (facing) {
            case NORTH -> 0f;
            case EAST -> 90f;
            case SOUTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        }));
        matrices.translate(-0.5, -0.5, -0.5);

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f posMat = entry.getPositionMatrix();
        Matrix3f normMat = entry.getNormalMatrix();

        RenderSystem.disableCull();
        emitUpQuad(buffer, posMat, normMat, overlay, light, sprite);
        RenderSystem.enableCull();

        matrices.pop();
    }

    private static void emitUpQuad(
            VertexConsumer buffer,
            Matrix4f posMat,
            Matrix3f normMat,
            int overlay,
            int light,
            Sprite sprite
    ) {
        float nx = 0f;
        float ny = 1f;
        float nz = 0f;

        float r = DEBUG_HIGHLIGHT ? 0.0f : 1.0f;
        float g = DEBUG_HIGHLIGHT ? 1.0f : 1.0f;
        float b = DEBUG_HIGHLIGHT ? 1.0f : 1.0f;
        float a = DEBUG_HIGHLIGHT ? 1.0f : 0.82f;

        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();

        quadVertex(buffer, posMat, normMat, X0, Y, Z0, u0, v0, r, g, b, a, overlay, light, nx, ny, nz);
        quadVertex(buffer, posMat, normMat, X1, Y, Z0, u1, v0, r, g, b, a, overlay, light, nx, ny, nz);
        quadVertex(buffer, posMat, normMat, X1, Y, Z1, u1, v1, r, g, b, a, overlay, light, nx, ny, nz);
        quadVertex(buffer, posMat, normMat, X0, Y, Z1, u0, v1, r, g, b, a, overlay, light, nx, ny, nz);
    }

    private static void quadVertex(
            VertexConsumer buffer,
            Matrix4f posMat,
            Matrix3f normMat,
            float x,
            float y,
            float z,
            float u,
            float v,
            float r,
            float g,
            float b,
            float a,
            int overlay,
            int light,
            float nx,
            float ny,
            float nz
    ) {
        buffer.vertex(posMat, x, y, z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(normMat, nx, ny, nz)
                .next();
    }
}