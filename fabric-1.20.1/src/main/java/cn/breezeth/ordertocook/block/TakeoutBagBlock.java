package cn.breezeth.ordertocook.block;

import cn.breezeth.ordertocook.block.entity.TakeoutBagBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.block.ShapeContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class TakeoutBagBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape BASE_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(4, 0, 7, 12, 9, 9),
            Block.createCuboidShape(6.70808, 7.46677, 7.005, 11.70808, 9.46677, 8.995),
            Block.createCuboidShape(4.29192, 7.46677, 7.005, 9.29192, 9.46677, 8.995),
            Block.createCuboidShape(4.02, 0.26954, 5.64491, 11.98, 8.26954, 8.64491),
            Block.createCuboidShape(4.02, 0.26954, 7.35509, 11.98, 8.26954, 10.35509)
    );
    private static final VoxelShape SHAPE_NORTH = BASE_SHAPE;
    private static final VoxelShape SHAPE_EAST = rotateShape(Direction.NORTH, Direction.EAST, BASE_SHAPE);
    private static final VoxelShape SHAPE_SOUTH = rotateShape(Direction.NORTH, Direction.SOUTH, BASE_SHAPE);
    private static final VoxelShape SHAPE_WEST = rotateShape(Direction.NORTH, Direction.WEST, BASE_SHAPE);

    public TakeoutBagBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState below = ctx.getWorld().getBlockState(ctx.getBlockPos().down());
        if (below.contains(Properties.HORIZONTAL_FACING)) {
            return this.getDefaultState().with(FACING, below.get(Properties.HORIZONTAL_FACING));
        }
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TakeoutBagBlockEntity(pos, state);
    }


    @Override
    public net.minecraft.util.ActionResult onUse(BlockState state, World world, BlockPos pos, net.minecraft.entity.player.PlayerEntity player, net.minecraft.util.Hand hand, net.minecraft.util.hit.BlockHitResult hit) {
        if (world.isClient) return net.minecraft.util.ActionResult.SUCCESS;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof TakeoutBagBlockEntity bagBe) {
            ItemStack stack = bagBe.getBagStack();
            if (!stack.isEmpty()) {
                boolean inserted = player.getInventory().insertStack(stack.copy());
                if (!inserted) {
                    ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, stack.copy());
                    entity.setVelocity(0, 0.1, 0);
                    world.spawnEntity(entity);
                }
                bagBe.setBagStack(ItemStack.EMPTY);
            }
        }
        world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_WOOL_BREAK, net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 1.0f);
        world.removeBlock(pos, false);
        return net.minecraft.util.ActionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof TakeoutBagBlockEntity bagBe) {
                    ItemStack stack = bagBe.getBagStack();
                    if (!stack.isEmpty()) {
                        ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, stack.copy());
                        entity.setVelocity(0, 0.1, 0);
                        world.spawnEntity(entity);
                    }
                }
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction d = state.get(FACING);
        return switch (d) {
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction d = state.get(FACING);
        return switch (d) {
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    private static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, VoxelShapes.empty()};
        int times = (to.getHorizontal() - from.getHorizontal() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = VoxelShapes.union(
                    buffer[1],
                    VoxelShapes.cuboid(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)
            ));
            buffer[0] = buffer[1];
            buffer[1] = VoxelShapes.empty();
        }
        return buffer[0];
    }
}
