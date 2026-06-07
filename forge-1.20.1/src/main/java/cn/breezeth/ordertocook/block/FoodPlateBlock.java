package cn.breezeth.ordertocook.block;

import cn.breezeth.ordertocook.block.entity.FoodPlateBlockEntity;
import cn.breezeth.ordertocook.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import cn.breezeth.ordertocook.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class FoodPlateBlock extends BaseEntityBlock {    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final int MAX_STAGE = 40;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, MAX_STAGE);

    public static boolean isEatingStage(int stage) {
        return stage > 0 && !isDirtyStage(stage);
    }

    public static boolean isDirtyStage(int stage) {
        return stage > 0 && ((stage - 1) % 4) == 3;
    }
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 2, 15),
            Block.box(3, 2, 3, 13, 5, 13)
    );

    public FoodPlateBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STAGE, 0));
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
        builder.add(FACING, STAGE);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.FOOD_PLATE_DISPLAY.get(), FoodPlateBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoodPlateBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // STAGE 0-3 仍然允许拿走，但会触发顾客差评惩罚（新逻辑）

        BlockEntity be = world.getBlockEntity(pos);
        boolean wasEating = false;
        if (be instanceof FoodPlateBlockEntity foodPlateBe) {
            ItemStack stack = foodPlateBe.getPlateStack();
            if (!stack.isEmpty()) {
                // 检查是否是正在吃东西的阶段
                if (state.hasProperty(STAGE) && isEatingStage(state.getValue(STAGE))) {
                    wasEating = true;
                }

                boolean inserted = player.getInventory().add(stack.copy());
                if (!inserted) {
                    ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, stack.copy());
                    entity.setDeltaMovement(0, 0.1, 0);
                    world.addFreshEntity(entity);
                }
                foodPlateBe.setPlateStack(ItemStack.EMPTY);
            }
        }
        if (state.hasProperty(STAGE) && isDirtyStage(state.getValue(STAGE))) {
            world.playSound(null, pos, ModSounds.PLATE_PLACE.get(), SoundSource.BLOCKS, 0.75f, 0.95f + world.random.nextFloat() * 0.1f);
        } else {
            world.playSound(null, pos, ModSounds.FOOD_PLATE_PLACE.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        // 如果是正在吃东西时被收走，触发惩罚
        if (wasEating && player != null && world instanceof ServerLevel serverWorld) {
            triggerEarlyTakePenalty(serverWorld, player, pos);
        }

        world.removeBlock(pos, false);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            if (!world.isClientSide) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof FoodPlateBlockEntity foodPlateBe) {
                    ItemStack stack = foodPlateBe.getPlateStack();
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

    private void triggerEarlyTakePenalty(ServerLevel world, Player player, BlockPos platePos) {
        // 在附近查找正在吃的顾客
        java.util.List<cn.breezeth.ordertocook.entity.CustomerEntity> customers = world.getEntitiesOfClass(
            cn.breezeth.ordertocook.entity.CustomerEntity.class,
            new net.minecraft.world.phys.AABB(platePos).inflate(3.0),
            c -> c.isEatingActionActive()
        );

        if (!customers.isEmpty()) {
            cn.breezeth.ordertocook.entity.CustomerEntity customer = customers.get(0);
            String name = customer.getCustomName() != null 
                ? customer.getCustomName().getString() 
                : Component.translatable("keyword.ordertocook.customer").getString();

            player.displayClientMessage(
                Component.translatable("message.ordertocook.plate_taken_early", name)
                    .withStyle(ChatFormatting.YELLOW), 
                false
            );

            // 打断食用动画，直接进入“从第2秒开始”的缩小消失段。
            customer.startInterruptedEatScaleAnimation();
        }
    }
}
