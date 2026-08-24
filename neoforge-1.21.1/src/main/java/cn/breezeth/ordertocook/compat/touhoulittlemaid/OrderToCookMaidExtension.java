package cn.breezeth.ordertocook.compat.touhoulittlemaid;

import cn.breezeth.ordertocook.block.entity.RefrigeratorBlockEntity;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.api.bauble.IChestType;
import com.github.tartaricacid.touhoulittlemaid.inventory.chest.ChestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

@LittleMaidExtension
public final class OrderToCookMaidExtension implements ILittleMaid {
    @Override
    public void addChestType(ChestManager manager) {
        manager.add(new RefrigeratorChestType());
    }

    private static final class RefrigeratorChestType implements IChestType {
        @Override
        public boolean isChest(BlockEntity chest) {
            return chest instanceof RefrigeratorBlockEntity;
        }

        @Override
        public boolean canOpenByPlayer(BlockEntity chest, Player player) {
            return chest instanceof RefrigeratorBlockEntity refrigerator
                    && refrigerator.canOpenByPlayer(player);
        }

        @Override
        public int getOpenCount(BlockGetter level, BlockPos pos, BlockEntity chest) {
            return chest instanceof RefrigeratorBlockEntity refrigerator && !refrigerator.hasViewers()
                    ? ALLOW_COUNT
                    : DENY_COUNT;
        }
    }
}
