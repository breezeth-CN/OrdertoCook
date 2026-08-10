package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.StoveBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class StoveRenderer extends GeoBlockRenderer<StoveBlockEntity> {
    public StoveRenderer() {
        super(new StoveGeoModel());
    }
}
