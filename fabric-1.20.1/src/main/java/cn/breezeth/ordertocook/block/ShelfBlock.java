package cn.breezeth.ordertocook.block;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.state.property.Properties;

public class ShelfBlock extends Block {
    public static final int MAX_PAPERS = 10;
    public static final IntProperty PAPERS = IntProperty.of("papers", 0, MAX_PAPERS);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 14, 16, 16, 16),
            Block.createCuboidShape(0, 0, 8, 16, 2, 14),
            Block.createCuboidShape(0, 8, 8, 16, 10, 14)
    );

    public ShelfBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(PAPERS, 0).with(FACING, Direction.NORTH));
    }

    // 1.20.1 无需 MapCodec/getCodec

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(PAPERS, FACING);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return rotateShape(Direction.NORTH, state.get(FACING), SHAPE);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return rotateShape(Direction.NORTH, state.get(FACING), SHAPE);
    }

    @Override
    public BlockState getPlacementState(net.minecraft.item.ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    private VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, VoxelShapes.empty()};
        int times = (to.getHorizontal() - from.getHorizontal() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = VoxelShapes.union(buffer[1], VoxelShapes.cuboid(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = VoxelShapes.empty();
        }
        return buffer[0];
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, net.minecraft.util.Hand hand, BlockHitResult hit) {
        ItemStack stack = player.getMainHandStack();
        if (stack.getItem() != Items.LEATHER) {
            stack = player.getOffHandStack();
        }
        if (stack.getItem() != Items.LEATHER) return ActionResult.PASS;

        int papers = state.get(PAPERS);
        if (papers >= MAX_PAPERS) return ActionResult.SUCCESS;
        if (world.isClient) return ActionResult.SUCCESS;

        world.setBlockState(pos, state.with(PAPERS, papers + 1), Block.NOTIFY_ALL);
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock() && !world.isClient) {
            int papers = state.get(PAPERS);
            if (papers > 0) {
                ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.LEATHER, papers));
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
