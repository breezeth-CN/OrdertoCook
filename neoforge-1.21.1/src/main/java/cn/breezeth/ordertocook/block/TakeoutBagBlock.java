package cn.breezeth.ordertocook.block;

import cn.breezeth.ordertocook.block.entity.TakeoutBagBlockEntity;
import cn.breezeth.ordertocook.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class TakeoutBagBlock extends BaseEntityBlock {
    public static final MapCodec<TakeoutBagBlock> CODEC = simpleCodec(TakeoutBagBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape BASE_SHAPE = Shapes.or(
            Block.box(4, 0, 7, 12, 9, 9),
            Block.box(6.70808, 7.46677, 7.005, 11.70808, 9.46677, 8.995),
            Block.box(4.29192, 7.46677, 7.005, 9.29192, 9.46677, 8.995),
            Block.box(4.02, 0.26954, 5.64491, 11.98, 8.26954, 8.64491),
            Block.box(4.02, 0.26954, 7.35509, 11.98, 8.26954, 10.35509)
    );
    private static final VoxelShape SHAPE_NORTH = BASE_SHAPE;
    private static final VoxelShape SHAPE_EAST = rotateShape(Direction.NORTH, Direction.EAST, BASE_SHAPE);
    private static final VoxelShape SHAPE_SOUTH = rotateShape(Direction.NORTH, Direction.SOUTH, BASE_SHAPE);
    private static final VoxelShape SHAPE_WEST = rotateShape(Direction.NORTH, Direction.WEST, BASE_SHAPE);

    public TakeoutBagBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState below = ctx.getLevel().getBlockState(ctx.getClickedPos().below());
        if (below.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return this.defaultBlockState().setValue(FACING, below.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TakeoutBagBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (world.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof TakeoutBagBlockEntity bagBe) {
            ItemStack stack = bagBe.getBagStack();
            if (!stack.isEmpty()) {
                boolean inserted = player.getInventory().add(stack.copy());
                if (!inserted) {
                    ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, stack.copy());
                    entity.setDeltaMovement(0, 0.1, 0);
                    world.addFreshEntity(entity);
                }
                bagBe.setBagStack(ItemStack.EMPTY);
            }
        }
        world.playSound(null, pos, ModSounds.BAG_PICKUP.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        world.removeBlock(pos, false);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            if (!world.isClientSide) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof TakeoutBagBlockEntity bagBe) {
                    ItemStack stack = bagBe.getBagStack();
                    if (!stack.isEmpty()) {
                        ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, stack.copy());
                        entity.setDeltaMovement(0, 0.1, 0);
                        world.addFreshEntity(entity);
                    }
                }
            }
            super.onRemove(state, world, pos, newState, moved);
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction d = state.getValue(FACING);
        return switch (d) {
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction d = state.getValue(FACING);
        return switch (d) {
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    private static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};
        int times = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(
                    buffer[1],
                    Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)
            ));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }
        return buffer[0];
    }
}
