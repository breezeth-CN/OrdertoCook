package cn.breezeth.ordertocook.item;

import cn.breezeth.ordertocook.block.DishwasherBlock;
import cn.breezeth.ordertocook.block.RefrigeratorBlock;
import cn.breezeth.ordertocook.block.StoveBlock;
import cn.breezeth.ordertocook.block.WashingTableBlock;
import cn.breezeth.ordertocook.registry.ModBlocks;
import cn.breezeth.ordertocook.registry.ModCriteria;
import cn.breezeth.ordertocook.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class OtcUpgradeBoxItem extends Item {
    public OtcUpgradeBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ordertocook.otc_upgrade_box.desc"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (tryUpgradeWashingTable(level, pos, state, player, stack)
                || tryUpgradeLowerBarrels(level, pos, state, player, stack)
                || tryUpgradeSmoker(level, pos, state, player, stack)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    private boolean tryUpgradeWashingTable(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack) {
        if (!state.is(ModBlocks.WASHINGTABLE.get())) {
            return false;
        }
        if (level.isClientSide) {
            return true;
        }

        int plates = state.getValue(WashingTableBlock.PLATES);
        if (plates > 0) {
            Block.popResource(level, pos, new ItemStack(ModItems.DIRTY_PLATE.get(), plates));
        }

        Direction facing = state.getValue(WashingTableBlock.FACING);
        BlockState newState = ModBlocks.DISHWASHER.get().defaultBlockState().setValue(DishwasherBlock.FACING, facing);
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        finishUpgrade((ServerLevel) level, pos, player, stack);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            ModCriteria.DISHWASHER_UPGRADE.trigger(sp);
        }
        return true;
    }

    private boolean tryUpgradeLowerBarrels(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack) {
        if (!state.is(Blocks.BARREL) || !level.getBlockState(pos.above()).is(Blocks.BARREL)) {
            return false;
        }
        if (level.getBlockState(pos.below()).is(Blocks.BARREL)) {
            return false;
        }
        if (level.isClientSide) {
            return true;
        }

        dropContainerAndClear(level, pos);
        dropContainerAndClear(level, pos.above());

        Direction facing = horizontalFacingFromState(state, player);
        BlockState lower = ModBlocks.REFRIGERATOR.get().defaultBlockState()
                .setValue(RefrigeratorBlock.FACING, facing)
                .setValue(RefrigeratorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.setValue(RefrigeratorBlock.HALF, DoubleBlockHalf.UPPER);

        level.setBlock(pos, lower, Block.UPDATE_ALL);
        level.setBlock(pos.above(), upper, Block.UPDATE_ALL);
        finishUpgrade((ServerLevel) level, pos, player, stack);
        return true;
    }

    private boolean tryUpgradeSmoker(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack) {
        if (!state.is(Blocks.SMOKER)) {
            return false;
        }
        if (level.isClientSide) {
            return true;
        }

        dropContainerAndClear(level, pos);

        Direction facing = horizontalFacingFromState(state, player);
        BlockState newState = ModBlocks.STOVE.get().defaultBlockState().setValue(StoveBlock.FACING, facing);
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        finishUpgrade((ServerLevel) level, pos, player, stack);
        return true;
    }

    private static void dropContainerAndClear(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Container container) {
            Containers.dropContents(level, pos, container);
            container.clearContent();
            blockEntity.setChanged();
        }
        level.removeBlockEntity(pos);
    }

    private static Direction horizontalFacingFromState(BlockState state, Player player) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction direction = state.getValue(BlockStateProperties.FACING);
            if (direction.getAxis().isHorizontal()) {
                return direction;
            }
        }
        return player == null ? Direction.NORTH : player.getDirection().getOpposite();
    }

    private static void finishUpgrade(ServerLevel level, BlockPos pos, Player player, ItemStack stack) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.75;
        double z = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 28, 0.35, 0.35, 0.35, 0.025);
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.55f, 1.45f);
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.45f, 1.8f);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
