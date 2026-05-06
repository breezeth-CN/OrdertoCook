package cn.breezeth.ordertocook.mixin.client.vehicle;

import cn.breezeth.ordertocook.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Inject(method = {
            "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            "m_115143_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"
    }, at = @At("HEAD"), cancellable = true, remap = false)
    private void onRenderItem(ItemStack stack, ItemDisplayContext renderMode, boolean leftHanded, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, BakedModel model, CallbackInfo ci) {
        if (stack.is(ModItems.HELMET.get()) && renderMode == ItemDisplayContext.HEAD) {
            matrices.pushPose();
            
            // 闂傚倷鑳堕…鍫㈡崲閹寸偟绠惧┑鐘叉搐閺嬩焦銇勯幘璺烘灁闁崇懓绉甸妵鍕籍閸屾瀚涘┑鈩冨絻椤兘寮婚妸銉㈡婵炲棙鍩堥弳鈩冪節閳封偓閸曨厾鐓夐悗瑙勬礃缁诲牊淇婇幖浣肝ㄧ憸宥夋偩閵娾晜鈷戞繛鑼帛婵炲洭鏌涢弬璺ㄐラ柛鐕佸灦濮?JSON 婵犵數鍋為崹鍫曞箹閳哄懎鍌ㄩ柟顖嗏偓閺?"display" 闂備浇宕垫慨宕囩矆娴ｈ娅犲ù鐘差儐閸?(translation, rotation, scale)
            model.getTransforms().getTransform(renderMode).apply(leftHanded, matrices);

            // 闂傚倸鍊峰ù鍥涢崟顖涘亱闁糕剝渚楅弫瀣亜閺冨洦顥夌紒鍓佸仱閹妫冨☉娆忔殘缂佸墽铏庨崳锝夊蓟濞戙垺鏅滈柛婵嗗椤牓姊洪棃娑欏婵﹤婀辩划娆愬緞鐎ｎ剛鐦堥梺鍛婂姦閸欏骸煤椤掑嫭鈷戦柛婵嗗濠€浼存煙濮濆矈鍤欐い顏勫暣瀹曟帒鈽夊▎搴㈠攭婵犵數鍋為崹鍓佸垝閻樿缁╁ù鐘差儐閻撶喐淇婇妶鍛伌婵炲吋鍔欓弻锝夋晲閸屾稒鐝曢悗鍨緲鐎氭澘鐣烽悢纰辨晣闁绘柨鐨濋崑鎾绘倷閻戞鍙嗗┑鐐村灦閻熴劍绔熷鈧弻娑㈠Ω閵夈儺鍔夌紓浣割儏椤﹀灚淇婇崼鏇炲耿闁宠桨妞掓潻妯荤節閻㈤潧浠﹂柟鍝ヮ焾椤曪綁宕奸弴鐔蜂簵?            // 缂傚倸鍊烽悞锕傘€冭箛娑樼婵炴垶姘ㄩ崡姘舵煛婢跺鐏嶉柡瀣叄閺屽秹鎸婃径瀣缂備焦鍔栭〃鍡欐崲濞戙垹绠婚柛鎰皺妤犲洭姊虹紒妯诲碍妞ゆ垵妫涚划娆愬緞婵犲骸鎮戞繝銏ｆ硾濡绂嶆ィ鍐彄闁搞儯鍔嶉埛鎰亜椤愶絿澧垫慨濠冩そ椤㈡瑩宕崟顐€抽梻?(Cutout)
            VertexConsumer cutoutConsumer = vertexConsumers.getBuffer(RenderType.cutout());
            renderFilteredBakedItemModel(model, light, overlay, matrices, cutoutConsumer, false);

            // 缂傚倸鍊烽悞锕傘€冭箛娑樼婵炴垶淇烘慨鎶芥煃鏉炴媽顔夐柡瀣叄閺屽秹鎸婃径瀣缂備焦鍔栭〃鍡欐崲濞戙垹绠婚柛鎰皺妤犲洭姊虹紒妯诲碍妞ゆ垵顦悾鐑藉焺閸愵亞鐦堥梺鎼炲劵缁茶姤绂嶆ィ鍐彄闁搞儯鍔嶉埛鎰亜椤愶絿澧甸柡宀嬬到椤劑宕卞Δ鈧幆鐐烘⒑瑜版帗鏁遍柛銊ユ健瀵宕奸妷銉庘晠鏌曟径鍫濆姢闁?(Translucent)
            VertexConsumer translucentConsumer = vertexConsumers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
            renderFilteredBakedItemModel(model, light, overlay, matrices, translucentConsumer, true);

            matrices.popPose();
            ci.cancel();
        }
    }

    /**
     * 闂備礁鎼ˇ顐﹀疾濞戞◤娲晝閸屾氨顔呴梺闈涚墕閹冲矂鍩€椤掍礁娴€规洩绲惧鍕沪閽樺鍔樺┑鐘殿暯濡插懘宕归鍫濈劦妞ゆ巻鍋撶痪缁㈠弮閸┾偓妞ゆ帒鍊搁崢鏉戔攽閳╁啯灏︾€规洘甯掗埥澶婎潩椤掑顥旈梻?isGlass 闂傚倷绀侀幉锟犲礉閺囥垹鐤柛褎顨嗛崑鈺呮煙椤栧棔璁查崑鎾诲箳濡も偓閻顭跨捄楦垮閻㈩垼鍓熷娲川婵犲嫮顓兼繛瀛樼矋閻熝呭垝閳哄懎绠绘い鏃傛櫕閸旀悂姊洪棃娑辩劸闁稿酣浜堕幃?     */
    private void renderFilteredBakedItemModel(BakedModel model, int light, int overlay, PoseStack matrices, VertexConsumer vertices, boolean isGlass) {
        RandomSource random = RandomSource.create(42L);

        for (Direction direction : Direction.values()) {
            renderFilteredQuads(matrices, vertices, model.getQuads(null, direction, random), light, overlay, isGlass);
        }

        renderFilteredQuads(matrices, vertices, model.getQuads(null, null, random), light, overlay, isGlass);
    }

    private void renderFilteredQuads(PoseStack matrices, VertexConsumer vertices, List<BakedQuad> quads, int light, int overlay, boolean isGlass) {
        PoseStack.Pose entry = matrices.last();
        
        for (BakedQuad bakedQuad : quads) {
            TextureAtlasSprite sprite = bakedQuad.getSprite();
            if (sprite == null) continue;
            
            String spritePath = sprite.contents().name().getPath();
            boolean quadIsGlass = spritePath.contains("glass") || spritePath.contains("visor") || spritePath.contains("translucent");
            
            if (quadIsGlass == isGlass) {
                vertices.putBulkData(entry, bakedQuad, 1.0f, 1.0f, 1.0f, 1.0f, light, overlay, true);
            }
        }
    }
}

