package cn.breezeth.ordertocook.client.renderer;

import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.core.OrderNpcVisualAccess;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;

public class OtcZombieRenderer extends ZombieEntityRenderer {
    private static final Identifier OTC_CUSTOM_ZOMBIE_TEXTURE = new Identifier(ModConstants.MOD_ID, "textures/entity/otc_zombie_custom.png");
    private static final Identifier OTC_CUSTOM_ZOMBIE_TEXTURE_2 = new Identifier(ModConstants.MOD_ID, "textures/entity/otc_zombie_custom2.png");

    public OtcZombieRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(ZombieEntity entity) {
        if (entity instanceof OrderNpcVisualAccess access) {
            int variant = access.oc$getSkinVariant();
            if (variant == 1) {
                return OTC_CUSTOM_ZOMBIE_TEXTURE;
            } else if (variant == 2) {
                return OTC_CUSTOM_ZOMBIE_TEXTURE_2;
            }
        }
        return super.getTexture(entity);
    }
}
