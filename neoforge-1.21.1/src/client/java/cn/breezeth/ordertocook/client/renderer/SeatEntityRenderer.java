package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.entity.SeatEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
public class SeatEntityRenderer extends EntityRenderer<SeatEntity> {
    public SeatEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SeatEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stone.png");
    }

    @Override
    public boolean shouldRender(SeatEntity entity, Frustum frustum, double x, double y, double z) {
        return false; // Don't render
    }
}

