package cn.breezeth.ordertocook.compat.touhoulittlemaid;

import cn.breezeth.ordertocook.block.RefrigeratorBlock;
import cn.breezeth.ordertocook.block.entity.RefrigeratorBlockEntity;
import com.github.tartaricacid.touhoulittlemaid.item.ItemWirelessIO;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

public final class RefrigeratorWirelessIOBinding {
    private RefrigeratorWirelessIOBinding() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RefrigeratorWirelessIOBinding::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(RefrigeratorWirelessIOBinding::onPlaySound);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getItemStack().getItem() instanceof ItemWirelessIO)) {
            return;
        }
        var state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof RefrigeratorBlock)) {
            return;
        }
        BlockPos bindingPos = state.getValue(RefrigeratorBlock.HALF) == DoubleBlockHalf.UPPER
                ? event.getPos().below()
                : event.getPos();
        ItemWirelessIO.setBindingPos(event.getItemStack(), bindingPos);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }

    private static void onPlaySound(PlayLevelSoundEvent.AtPosition event) {
        if (event.getSound() == null) return;
        var sound = event.getSound().value();
        if (sound != SoundEvents.CHEST_OPEN && sound != SoundEvents.CHEST_CLOSE) return;
        BlockPos pos = BlockPos.containing(event.getPosition());
        if (event.getLevel().getBlockEntity(pos) instanceof RefrigeratorBlockEntity) event.setCanceled(true);
    }
}
