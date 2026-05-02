package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.entity.SeatEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class SeatEntityRenderer extends EntityRenderer<SeatEntity> {
    public SeatEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(SeatEntity entity) {
        return Identifier.of("minecraft", "textures/block/stone.png");
    }

    @Override
    public boolean shouldRender(SeatEntity entity, Frustum frustum, double x, double y, double z) {
        return false; // Don't render
    }
}

