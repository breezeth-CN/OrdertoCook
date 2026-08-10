package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.block.entity.TakeoutBagBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class TakeoutBagPlacedRenderer extends GeoBlockRenderer<TakeoutBagBlockEntity> {
    public TakeoutBagPlacedRenderer() {
        super(new TakeoutBagPlacedGeoModel());
    }
}

