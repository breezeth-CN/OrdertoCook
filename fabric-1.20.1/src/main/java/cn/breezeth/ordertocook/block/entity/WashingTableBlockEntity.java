package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public final class WashingTableBlockEntity extends BlockEntity {
    public WashingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WASHING_TABLE, pos, state);
    }
}