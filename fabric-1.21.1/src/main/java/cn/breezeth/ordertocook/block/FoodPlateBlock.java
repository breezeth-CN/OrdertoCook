package cn.breezeth.ordertocook.block;

import cn.breezeth.ordertocook.block.entity.FoodPlateBlockEntity;
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
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import cn.breezeth.ordertocook.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class FoodPlateBlock extends BlockWithEntity {
    public static final MapCodec<FoodPlateBlock> CODEC = createCodec(FoodPlateBlock::new);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final IntProperty STAGE = IntProperty.of("stage", 0, 4);
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(1, 0, 1, 15, 2, 15),
            Block.createCuboidShape(3, 2, 3, 13, 5, 13)
    );

    public FoodPlateBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(STAGE, 0));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
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
        builder.add(FACING, STAGE);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) {
            return null;
        }
        return validateTicker(type, ModBlockEntities.FOOD_PLATE_DISPLAY, FoodPlateBlockEntity::tick);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FoodPlateBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        // STAGE 0-3 仍然允许拿走，但会触发顾客差评惩罚（新逻辑）

        BlockEntity be = world.getBlockEntity(pos);
        boolean wasEating = false;
        if (be instanceof FoodPlateBlockEntity foodPlateBe) {
            ItemStack stack = foodPlateBe.getPlateStack();
            if (!stack.isEmpty()) {
                // 检查是否是正在吃东西的阶段
                if (state.contains(STAGE) && state.get(STAGE) < 4) {
                    wasEating = true;
                }

                boolean inserted = player.getInventory().insertStack(stack.copy());
                if (!inserted) {
                    ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, stack.copy());
                    entity.setVelocity(0, 0.1, 0);
                    world.spawnEntity(entity);
                }
                foodPlateBe.setPlateStack(ItemStack.EMPTY);
            }
        }
        if (state.contains(STAGE) && state.get(STAGE) == 4) {
            world.playSound(null, pos, ModSounds.PLATE_PLACE, SoundCategory.BLOCKS, 0.75f, 0.95f + world.random.nextFloat() * 0.1f);
        } else {
            world.playSound(null, pos, ModSounds.FOOD_PLATE_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }

        // 如果是正在吃东西时被收走，触发惩罚
        if (wasEating && player != null && world instanceof ServerWorld serverWorld) {
            triggerEarlyTakePenalty(serverWorld, player, pos);
        }

        world.removeBlock(pos, false);
        return ActionResult.SUCCESS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof FoodPlateBlockEntity foodPlateBe) {
                    ItemStack stack = foodPlateBe.getPlateStack();
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

    private void triggerEarlyTakePenalty(ServerWorld world, PlayerEntity player, BlockPos platePos) {
        // 在附近查找正在吃的顾客
        java.util.List<cn.breezeth.ordertocook.entity.CustomerEntity> customers = world.getEntitiesByClass(
            cn.breezeth.ordertocook.entity.CustomerEntity.class,
            new net.minecraft.util.math.Box(platePos).expand(3.0),
            c -> c.isEatingActionActive()
        );

        if (!customers.isEmpty()) {
            cn.breezeth.ordertocook.entity.CustomerEntity customer = customers.get(0);
            String name = customer.getCustomName() != null 
                ? customer.getCustomName().getString() 
                : Text.translatable("keyword.ordertocook.customer").getString();

            player.sendMessage(
                Text.translatable("message.ordertocook.plate_taken_early", name)
                    .formatted(Formatting.YELLOW), 
                false
            );

            // 打断食用动画，直接进入“从第2秒开始”的缩小消失段。
            customer.startInterruptedEatScaleAnimation();
        }
    }
}
