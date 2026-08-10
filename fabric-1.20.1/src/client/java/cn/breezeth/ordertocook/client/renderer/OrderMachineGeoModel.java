package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity;
import cn.breezeth.ordertocook.core.ModConstants;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public final class OrderMachineGeoModel extends GeoModel<OrderMachineBlockEntity> {
    private static final Identifier MODEL = new Identifier(ModConstants.MOD_ID, "geo/block/order_machine.geo.json");
    private static final Identifier TEXTURE = new Identifier(ModConstants.MOD_ID, "textures/block/order_machine_geo.png");
    private static final Identifier ANIMATION = new Identifier(ModConstants.MOD_ID, "animations/order_machine.animation.json");

    @Override
    public Identifier getModelResource(OrderMachineBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(OrderMachineBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(OrderMachineBlockEntity animatable) {
        return ANIMATION;
    }
}
