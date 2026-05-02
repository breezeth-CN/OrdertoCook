package cn.breezeth.ordertocook.block;

import cn.breezeth.ordertocook.core.OrderNpcManager;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.registry.ModEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class ChairBlock extends Block {
    public static final MapCodec<ChairBlock> CODEC = createCodec(ChairBlock::new);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    private static final double SEAT_HEIGHT_OFFSET = 9.0 / 16.0;

    private static final VoxelShape BASE_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(3, 6, 3, 13, 8, 13),
            Block.createCuboidShape(3, 0, 3, 13, 6, 13),
            Block.createCuboidShape(3, 0, 5, 13, 6, 11),
            Block.createCuboidShape(5, 0, 3, 11, 6, 13),
            Block.createCuboidShape(3, 8, 11, 13, 16, 13)
    );

    public ChairBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }
    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
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
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player.hasVehicle()) return ActionResult.PASS;
        if (world.isClient) return ActionResult.SUCCESS;

        BlockPos spawn = pos.up();
        if (!world.isAir(spawn) || !world.isAir(spawn.up())) return ActionResult.PASS;

        long posLong = pos.asLong();
        String posTag = OrderNpcManager.TAG_CHAIR_SEAT_POS_PREFIX + posLong;
        Box searchBox = new Box(spawn).expand(0.5, 2.0, 0.5);
        var seats = world.getEntitiesByClass(SeatEntity.class, searchBox, s -> s.getCommandTags().contains(OrderNpcManager.TAG_CHAIR_SEAT) && s.getCommandTags().contains(posTag));
        for (var s : seats) {
            if (s.hasPassengers()) return ActionResult.SUCCESS;
        }

        SeatEntity seat = new SeatEntity(ModEntities.SEAT, world);
        seat.setPos(pos.getX() + 0.5, pos.getY() + SEAT_HEIGHT_OFFSET, pos.getZ() + 0.5);
        seat.setInvisible(true);
        seat.setNoGravity(true);
        seat.setSilent(true);
        seat.setInvulnerable(true);
        seat.addCommandTag(OrderNpcManager.TAG_CHAIR_SEAT);
        seat.addCommandTag(posTag);
        if (!world.spawnEntity(seat)) return ActionResult.PASS;

        boolean riding = player.startRiding(seat, true);
        if (!riding) {
            seat.discard();
            return ActionResult.PASS;
        }
        return ActionResult.SUCCESS;
    }

    private static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, VoxelShapes.empty()};
        int times = (to.getHorizontal() - from.getHorizontal() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = VoxelShapes.union(buffer[1], VoxelShapes.cuboid(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = VoxelShapes.empty();
        }
        return buffer[0];
    }
}
