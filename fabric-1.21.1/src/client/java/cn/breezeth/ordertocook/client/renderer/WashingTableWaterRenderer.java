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
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
import org.joml.Matrix4f;

/**
 * 与 1.20.1 一致：translucent 水面单独绘制，主模型 cutout。
 */
public final class WashingTableWaterRenderer implements BlockEntityRenderer<WashingTableBlockEntity> {
    private static final SpriteIdentifier WATER_SPRITE = new SpriteIdentifier(
            PlayerScreenHandler.BLOCK_ATLAS_TEXTURE,
            Identifier.of(ModConstants.MOD_ID, "block/water"));

    private static final float X0 = 3f / 16f;
    private static final float X1 = 13f / 16f;
    private static final float Z0 = 3f / 16f;
    private static final float Z1 = 12f / 16f;
    private static final float Y = 14.98f / 16f;
    private static final float UV_TEX = 32f;

    public WashingTableWaterRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    private static float atlasU(Sprite s, float pixelU) {
        float t = pixelU / UV_TEX;
        return s.getMinU() + t * (s.getMaxU() - s.getMinU());
    }

    private static float atlasV(Sprite s, float pixelV) {
        float t = pixelV / UV_TEX;
        return s.getMinV() + t * (s.getMaxV() - s.getMinV());
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

        RenderSystem.disableCull();

        float u0 = atlasU(sprite, 0f);
        float u5 = atlasU(sprite, 5f);
        float v0 = atlasV(sprite, 0f);
        float v45 = atlasV(sprite, 4.5f);

        float nx = 0f;
        float ny = 1f;
        float nz = 0f;
        emitVertex(buffer, posMat, entry, X0, Y, Z0, u0, v0, overlay, light, nx, ny, nz);
        emitVertex(buffer, posMat, entry, X1, Y, Z0, u5, v0, overlay, light, nx, ny, nz);
        emitVertex(buffer, posMat, entry, X1, Y, Z1, u5, v45, overlay, light, nx, ny, nz);
        emitVertex(buffer, posMat, entry, X0, Y, Z1, u0, v45, overlay, light, nx, ny, nz);

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
