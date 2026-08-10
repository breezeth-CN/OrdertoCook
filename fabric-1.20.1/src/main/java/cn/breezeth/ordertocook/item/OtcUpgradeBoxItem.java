package cn.breezeth.ordertocook.item;

import cn.breezeth.ordertocook.block.DishwasherBlock;
import cn.breezeth.ordertocook.block.RefrigeratorBlock;
import cn.breezeth.ordertocook.block.StoveBlock;
import cn.breezeth.ordertocook.block.WashingTableBlock;
import cn.breezeth.ordertocook.registry.ModBlocks;
import cn.breezeth.ordertocook.registry.ModCriteria;
import cn.breezeth.ordertocook.registry.ModItems;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

public class OtcUpgradeBoxItem extends Item {
    public OtcUpgradeBoxItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<net.minecraft.text.Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(net.minecraft.text.Text.translatable("item.ordertocook.otc_upgrade_box.desc"));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        if (tryUpgradeWashingTable(world, pos, state, player, stack)
                || tryUpgradeLowerBarrels(world, pos, state, player, stack)
                || tryUpgradeSmoker(world, pos, state, player, stack)) {
            return ActionResult.success(world.isClient);
        }

        return ActionResult.PASS;
    }

    private boolean tryUpgradeWashingTable(World world, BlockPos pos, BlockState state, PlayerEntity player, ItemStack stack) {
        if (!state.isOf(ModBlocks.WASHINGTABLE)) {
            return false;
        }
        if (world.isClient) {
            return true;
        }

        int plates = state.get(WashingTableBlock.PLATES);
        if (plates > 0) {
            Block.dropStack(world, pos, new ItemStack(ModItems.DIRTY_PLATE, plates));
        }

        Direction facing = state.get(WashingTableBlock.FACING);
        BlockState newState = ModBlocks.DISHWASHER.getDefaultState().with(DishwasherBlock.FACING, facing);
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);
        finishUpgrade((ServerWorld) world, pos, player, stack);
        if (player instanceof ServerPlayerEntity sp) {
            ModCriteria.DISHWASHER_UPGRADE.trigger(sp);
        }
        return true;
    }

    private boolean tryUpgradeLowerBarrels(World world, BlockPos pos, BlockState state, PlayerEntity player, ItemStack stack) {
        if (!state.isOf(Blocks.BARREL) || !world.getBlockState(pos.up()).isOf(Blocks.BARREL)) {
            return false;
        }
        if (world.getBlockState(pos.down()).isOf(Blocks.BARREL)) {
            return false;
        }
        if (world.isClient) {
            return true;
        }

        dropContainerAndClear(world, pos);
        dropContainerAndClear(world, pos.up());

        Direction facing = horizontalFacingFromState(state, player);
        BlockState lower = ModBlocks.REFRIGERATOR.getDefaultState()
                .with(RefrigeratorBlock.FACING, facing)
                .with(RefrigeratorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.with(RefrigeratorBlock.HALF, DoubleBlockHalf.UPPER);

        world.setBlockState(pos, lower, Block.NOTIFY_ALL);
        world.setBlockState(pos.up(), upper, Block.NOTIFY_ALL);
        finishUpgrade((ServerWorld) world, pos, player, stack);
        return true;
    }

    private boolean tryUpgradeSmoker(World world, BlockPos pos, BlockState state, PlayerEntity player, ItemStack stack) {
        if (!state.isOf(Blocks.SMOKER)) {
            return false;
        }
        if (world.isClient) {
            return true;
        }

        dropContainerAndClear(world, pos);

        Direction facing = horizontalFacingFromState(state, player);
        BlockState newState = ModBlocks.STOVE.getDefaultState().with(StoveBlock.FACING, facing);
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);
        finishUpgrade((ServerWorld) world, pos, player, stack);
        return true;
    }

    private static void dropContainerAndClear(World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof Inventory inventory) {
            ItemScatterer.spawn(world, pos, inventory);
            inventory.clear();
            blockEntity.markDirty();
        }
        world.removeBlockEntity(pos);
    }

    private static Direction horizontalFacingFromState(BlockState state, PlayerEntity player) {
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            return state.get(Properties.HORIZONTAL_FACING);
        }
        if (state.contains(BarrelBlock.FACING)) {
            Direction direction = state.get(BarrelBlock.FACING);
            if (direction.getAxis().isHorizontal()) {
                return direction;
            }
        }
        return player == null ? Direction.NORTH : player.getHorizontalFacing().getOpposite();
    }

    private static void finishUpgrade(ServerWorld world, BlockPos pos, PlayerEntity player, ItemStack stack) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.75;
        double z = pos.getZ() + 0.5;
        world.spawnParticles(ParticleTypes.CLOUD, x, y, z, 28, 0.35, 0.35, 0.35, 0.025);
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.55f, 1.45f);
        world.playSound(null, pos, SoundEvents.BLOCK_SMITHING_TABLE_USE, SoundCategory.BLOCKS, 0.8f, 1.2f);
        world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 0.45f, 1.8f);
        if (player == null || !player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
    }
}
