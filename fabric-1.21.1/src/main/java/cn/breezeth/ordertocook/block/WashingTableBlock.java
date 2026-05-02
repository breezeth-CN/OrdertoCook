package cn.breezeth.ordertocook.block;

import cn.breezeth.ordertocook.block.entity.WashingTableBlockEntity;
import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
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
import org.jetbrains.annotations.Nullable;

public class WashingTableBlock extends BlockWithEntity {
    public static final MapCodec<WashingTableBlock> CODEC = createCodec(WashingTableBlock::new);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final IntProperty PLATES = IntProperty.of("plates", 0, 10);
    private static final VoxelShape BASE_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 2, 16, 16, 16),
            Block.createCuboidShape(0, 12, 0, 16, 16, 2),
            Block.createCuboidShape(1, 1, 1, 15, 11, 2)
    );

    public WashingTableBlock(Settings settings) {
        super(settings.strength(2.5F));
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(PLATES, 0));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WashingTableBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, PLATES);
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
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player.getMainHandStack().isEmpty() && player.getOffHandStack().isEmpty() && state.get(PLATES) > 0) {
            return ActionResult.success(world.isClient);
        }

        ItemStack stack = player.getStackInHand(Hand.MAIN_HAND);
        if (stack.isEmpty()) {
            stack = player.getStackInHand(Hand.OFF_HAND);
        }

        if (!stack.isOf(ModItems.DIRTY_PLATE)) {
            return ActionResult.PASS;
        }

        int plates = state.get(PLATES);
        if (plates >= 10) {
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

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return rotateShape(Direction.NORTH, state.get(FACING), BASE_SHAPE);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return rotateShape(Direction.NORTH, state.get(FACING), BASE_SHAPE);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
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
