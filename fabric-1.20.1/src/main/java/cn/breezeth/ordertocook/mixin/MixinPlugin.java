package cn.breezeth.ordertocook.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;
import java.util.Set;

public final class MixinPlugin implements IMixinConfigPlugin {
    private boolean tlmLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        FabricLoader loader = FabricLoader.getInstance();
        tlmLoaded = loader.isModLoaded("touhou_little_maid")
            || loader.isModLoaded("touhou_little_maid_orihime")
            || loader.isModLoaded("touhoulittlemaid_orihime");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.equals("cn.breezeth.ordertocook.mixin.client.compat.touhoulittlemaid.EntityChairGetYawMixin")) {
            return tlmLoaded;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
