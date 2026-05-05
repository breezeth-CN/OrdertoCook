package cn.breezeth.ordertocook.block;

import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PlateShelfBlock extends Block {
    public static final MapCodec<PlateShelfBlock> CODEC = simpleCodec(PlateShelfBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PLATES = IntegerProperty.create("plates", 0, 18);
    private static final VoxelShape BASE_SHAPE = Shapes.or(
            Block.box(0, 0, 14, 16, 16, 16),
            Block.box(0, 0, 8, 16, 2, 14),
            Block.box(0, 8, 8, 16, 10, 14)
    );

    public PlateShelfBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PLATES, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PLATES);
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
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.isEmpty()) {
            stack = player.getItemInHand(InteractionHand.OFF_HAND);
        }
        if (!stack.is(ModItems.CLEAN_PLATE.get())) {
            return InteractionResult.PASS;
        }

        int plates = state.getValue(PLATES);
        if (plates >= 18) {
            return InteractionResult.PASS;
        }

        if (!world.isClientSide) {
            world.setBlock(pos, state.setValue(PLATES, plates + 1), Block.UPDATE_ALL);
            world.playSound(null, pos, ModSounds.PLATE_PLACE.get(), SoundSource.BLOCKS, 0.75f, 0.95f + world.random.nextFloat() * 0.1f);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock() && !world.isClientSide) {
            int plates = state.getValue(PLATES);
            if (plates > 0) {
                Block.popResource(world, pos, new ItemStack(ModItems.CLEAN_PLATE.get(), plates));
            }
        }
        super.onRemove(state, world, pos, newState, moved);
    }

    private static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};
        int times = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    buffer[1] = Shapes.or(buffer[1], Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }
        return buffer[0];
    }
}
