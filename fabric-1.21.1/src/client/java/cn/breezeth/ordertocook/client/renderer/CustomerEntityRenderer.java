package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.entity.CustomerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class CustomerEntityRenderer extends GeoEntityRenderer<CustomerEntity> {
    public CustomerEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new CustomerEntityModel());
        this.shadowRadius = 0.5f;
    }
}
