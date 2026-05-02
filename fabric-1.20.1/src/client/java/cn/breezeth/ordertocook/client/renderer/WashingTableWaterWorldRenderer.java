package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.registry.ModBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

public final class WashingTableWaterWorldRenderer {
    private WashingTableWaterWorldRenderer() {
    }

    public static void register() {
        WorldRenderEvents.LAST.register(WashingTableWaterWorldRenderer::onWorldLast);
    }

    private static void onWorldLast(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        ClientWorld world = context.world();
        Vec3d cam = context.camera().getPos();
        int viewDist = client.options.getClampedViewDistance();
        ChunkPos origin = player.getChunkPos();
        Matrix4f mat = context.matrixStack().peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buffer = tess.getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // 正式水面：仅在洗碗池位置绘制半透明水色。
        for (int dx = -viewDist; dx <= viewDist; dx++) {
            for (int dz = -viewDist; dz <= viewDist; dz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(origin.x + dx, origin.z + dz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getPos();
                    BlockState state = world.getBlockState(pos);
                    if (!state.isOf(ModBlocks.WASHINGTABLE)) {
                        continue;
                    }
                    drawFlatQuad(buffer, mat,
                            (float) pos.getX() - (float) cam.x + 3f / 16f,
                            (float) pos.getY() - (float) cam.y + 15.1f / 16f,
                            (float) pos.getZ() - (float) cam.z + 3f / 16f,
                            0.10f, 0.55f, 0.95f, 0.62f
                    );
                }
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawFlatQuad(
            BufferBuilder buffer,
            Matrix4f mat,
            float x0,
            float y,
            float z0,
            float r,
            float g,
            float b,
            float a
    ) {
        float x1 = x0 + 10f / 16f;
        float z1 = z0 + 10f / 16f;
        buffer.vertex(mat, x0, y, z0).color(r, g, b, a).next();
        buffer.vertex(mat, x1, y, z0).color(r, g, b, a).next();
        buffer.vertex(mat, x1, y, z1).color(r, g, b, a).next();
        buffer.vertex(mat, x0, y, z1).color(r, g, b, a).next();
    }
}