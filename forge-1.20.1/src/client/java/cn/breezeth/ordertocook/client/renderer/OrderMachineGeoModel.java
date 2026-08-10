package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class OrderMachineGeoModel extends GeoModel<OrderMachineBlockEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(ModConstants.MOD_ID, "geo/block/order_machine.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(ModConstants.MOD_ID, "textures/block/order_machine_geo.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(ModConstants.MOD_ID, "animations/order_machine.animation.json");

    @Override
    public ResourceLocation getModelResource(OrderMachineBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(OrderMachineBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(OrderMachineBlockEntity animatable) {
        return ANIMATION;
    }
}

