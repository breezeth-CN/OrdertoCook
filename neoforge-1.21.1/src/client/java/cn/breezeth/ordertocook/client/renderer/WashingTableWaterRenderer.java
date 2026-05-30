package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.WashingTableBlock;
import cn.breezeth.ordertocook.block.entity.WashingTableBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * 濠?1.20.1 濠电偞鍨堕幐鎾磻閹剧粯鐓犻柣銈庡灡瑜把呯磼鏉堚晜鐤剅anslucent 婵犳鍠楄摫闁圭鍟块…鍥ㄥ閺夋垹顢呴梺鍝勬川婵挳顢旈鍡欑＝濞达絽顫栭鍫濇槬婵°倕鎳忛弲顒勬倶閻愰潧甯堕柣锔界矋娣囧﹪顢涘璇蹭壕鐎规洖娲犻崑?cutout闂?
 */
public final class WashingTableWaterRenderer implements BlockEntityRenderer<WashingTableBlockEntity> {
    private static final ResourceLocation WATER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "textures/block/water.png");

    private static final float X0 = 3f / 16f;
    private static final float X1 = 13f / 16f;
    private static final float Z0 = 3f / 16f;
    private static final float Z1 = 12f / 16f;
    private static final float Y = 15.08f / 16f;

    public WashingTableWaterRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(
            WashingTableBlockEntity entity,
            float tickDelta,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light,
            int overlay
    ) {
        Level world = entity.getLevel();
        if (world == null) {
            return;
        }
        BlockState state = entity.getBlockState();
        if (!state.hasProperty(WashingTableBlock.FACING)) {
            return;
        }
        Direction facing = state.getValue(WashingTableBlock.FACING);

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderType.entityTranslucent(WATER_TEXTURE));

        matrices.pushPose();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.mulPose(Axis.YP.rotationDegrees(switch (facing) {
            case NORTH -> 0f;
            case EAST -> 270f;
            case SOUTH -> 180f;
            case WEST -> 90f;
            default -> 0f;
        }));
        matrices.translate(-0.5, -0.5, -0.5);

        PoseStack.Pose entry = matrices.last();
        Matrix4f posMat = entry.pose();

        RenderSystem.disableCull();

        float nx = 0f;
        float ny = 1f;
        float nz = 0f;
        emitVertex(buffer, posMat, entry, X0, Y, Z0, 0f, 0f, overlay, light, nx, ny, nz);
        emitVertex(buffer, posMat, entry, X1, Y, Z0, 1f, 0f, overlay, light, nx, ny, nz);
        emitVertex(buffer, posMat, entry, X1, Y, Z1, 1f, 1f, overlay, light, nx, ny, nz);
        emitVertex(buffer, posMat, entry, X0, Y, Z1, 0f, 1f, overlay, light, nx, ny, nz);

        matrices.popPose();

        RenderSystem.enableCull();
    }

    private static void emitVertex(
            VertexConsumer buffer,
            Matrix4f posMat,
            PoseStack.Pose pose,
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
        buffer.addVertex(posMat, x, y, z)
                .setColor(1f, 1f, 1f, 1f)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
