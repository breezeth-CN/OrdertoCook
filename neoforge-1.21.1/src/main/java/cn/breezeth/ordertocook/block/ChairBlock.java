package cn.breezeth.ordertocook.block;

import cn.breezeth.ordertocook.core.OrderNpcManager;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.registry.ModEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChairBlock extends Block {
    public static final MapCodec<ChairBlock> CODEC = simpleCodec(ChairBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final double SEAT_HEIGHT_OFFSET = 9.0 / 16.0;

    private static final VoxelShape BASE_SHAPE = Shapes.or(
            Block.box(3, 6, 3, 13, 8, 13),
            Block.box(3, 0, 3, 13, 6, 13),
            Block.box(3, 0, 5, 13, 6, 11),
            Block.box(5, 0, 3, 11, 6, 13),
            Block.box(3, 8, 11, 13, 16, 13)
    );

    public ChairBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return rotateShape(Direction.NORTH, state.getValue(FACING), BASE_SHAPE);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return rotateShape(Direction.NORTH, state.getValue(FACING), BASE_SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isPassenger()) return InteractionResult.PASS;
        if (world.isClientSide) return InteractionResult.SUCCESS;

        BlockPos spawn = pos.above();
        if (!world.isEmptyBlock(spawn) || !world.isEmptyBlock(spawn.above())) return InteractionResult.PASS;

        long posLong = pos.asLong();
        String posTag = OrderNpcManager.TAG_CHAIR_SEAT_POS_PREFIX + posLong;
        AABB searchBox = new AABB(spawn).inflate(0.5, 2.0, 0.5);
        var seats = world.getEntitiesOfClass(SeatEntity.class, searchBox, s -> s.getTags().contains(OrderNpcManager.TAG_CHAIR_SEAT) && s.getTags().contains(posTag));
        for (var s : seats) {
            if (s.isVehicle()) return InteractionResult.SUCCESS;
        }

        SeatEntity seat = new SeatEntity(ModEntities.SEAT.get(), world);
        seat.setPosRaw(pos.getX() + 0.5, pos.getY() + SEAT_HEIGHT_OFFSET, pos.getZ() + 0.5);
        seat.setInvisible(true);
        seat.setNoGravity(true);
        seat.setSilent(true);
        seat.setInvulnerable(true);
        seat.addTag(OrderNpcManager.TAG_CHAIR_SEAT);
        seat.addTag(posTag);
        if (!world.addFreshEntity(seat)) return InteractionResult.PASS;

        boolean riding = player.startRiding(seat, true);
        if (!riding) {
            seat.discard();
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    private static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};
        int times = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1], Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }
        return buffer[0];
    }
}
