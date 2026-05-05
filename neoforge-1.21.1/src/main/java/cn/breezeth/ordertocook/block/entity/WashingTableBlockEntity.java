package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class WashingTableBlockEntity extends BlockEntity {
    public WashingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WASHING_TABLE.get(), pos, state);
    }
}
