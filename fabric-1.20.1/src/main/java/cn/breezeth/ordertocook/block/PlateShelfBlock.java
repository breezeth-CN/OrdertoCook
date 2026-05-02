package cn.breezeth.ordertocook.block;

import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.registry.ModSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * 与 1.21.1 一致：无方块实体，仅用 {@link #PLATES} 状态计数；操作台从邻近盘子架扣减。
 */
public class PlateShelfBlock extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final IntProperty PLATES = IntProperty.of("plates", 0, 18);
    private static final VoxelShape BASE_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 14, 16, 16, 16),
            Block.createCuboidShape(0, 0, 8, 16, 2, 14),
            Block.createCuboidShape(0, 8, 8, 16, 10, 14)
    );

    public PlateShelfBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(PLATES, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, PLATES);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return rotateShape(Direction.NORTH, state.get(FACING), BASE_SHAPE);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return rotateShape(Direction.NORTH, state.get(FACING), BASE_SHAPE);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
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

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack stack = player.getStackInHand(Hand.MAIN_HAND);
        if (stack.isEmpty()) {
            stack = player.getStackInHand(Hand.OFF_HAND);
        }
        if (!stack.isOf(ModItems.CLEAN_PLATE)) {
            return ActionResult.PASS;
        }

        int plates = state.get(PLATES);
        if (plates >= 18) {
            return ActionResult.PASS;
        }

        if (!world.isClient) {
            world.setBlockState(pos, state.with(PLATES, plates + 1), Block.NOTIFY_ALL);
            world.playSound(null, pos, ModSounds.PLATE_PLACE, SoundCategory.BLOCKS, 0.75f, 0.95f + world.random.nextFloat() * 0.1f);
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }
        return ActionResult.success(world.isClient);
    }

    private static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, VoxelShapes.empty()};
        int times = (to.getHorizontal() - from.getHorizontal() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forEachBox((minX, minY, minZ, maxX, maxY, maxZ) ->
                    buffer[1] = VoxelShapes.union(buffer[1], VoxelShapes.cuboid(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = VoxelShapes.empty();
        }
        return buffer[0];
    }
}
