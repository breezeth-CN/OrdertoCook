package cn.breezeth.ordertocook.block;

import cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity;
import cn.breezeth.ordertocook.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import cn.breezeth.ordertocook.util.DataCompat;
import java.util.List;
import cn.breezeth.ordertocook.config.ConfigManager;

public class OrderMachineBlock extends BaseEntityBlock {
    public static final MapCodec<OrderMachineBlock> CODEC = simpleCodec(OrderMachineBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public enum Mode implements StringRepresentable {
        IDLE("idle"), PRINTING("printing"), WAITING("waiting");
        private final String name;
        Mode(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
        @Override public String toString() { return name; }
    }
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    public static final IntegerProperty FRAME = IntegerProperty.create("frame", 0, 2);
    public static final int BEAT_TICKS = 15;

    public OrderMachineBlock(Properties settings) {
        super(settings.requiresCorrectToolForDrops().strength(3.5F));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MODE, Mode.IDLE).setValue(FRAME, 0));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Block.box(1, 0, 1, 15, 7, 15);
        return rotateShape(Direction.NORTH, state.getValue(FACING), shape);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Block.box(1, 0, 1, 15, 7, 15);
        return rotateShape(Direction.NORTH, state.getValue(FACING), shape);
    }

    private VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};
        int times = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1], Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }
        return buffer[0];
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODE, FRAME);
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
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide) {
            int maxAllowed = Math.max(1, ConfigManager.get().maxOrderMachinesWithin24);
            AABB box = new AABB(pos).inflate(24);
            boolean duplicateFound = false;
            int found = 0;
            
            for (BlockPos checkPos : BlockPos.betweenClosed(
                    (int)box.minX, (int)box.minY, (int)box.minZ,
                    (int)box.maxX, (int)box.maxY, (int)box.maxZ)) {
                
                if (checkPos.equals(pos)) continue; // Skip self

                if (world.getBlockState(checkPos).getBlock() instanceof OrderMachineBlock) {
                    found++;
                    if (found >= maxAllowed) {
                        duplicateFound = true;
                        break;
                    }
                }
            }

            if (duplicateFound) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                
                if (placer instanceof Player player) {
                    player.displayClientMessage(Component.translatable("message.ordertocook.order_machine.nearby").withStyle(ChatFormatting.RED), true);
                }

                // Refund exactly one machine preserving any NBT/components from the placed stack
                ItemStack refund = itemStack.copyWithCount(1);
                ItemEntity drop = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, refund);
                drop.setPickUpDelay(10);
                world.addFreshEntity(drop);
            } else {
                 BlockEntity be = world.getBlockEntity(pos);
                 if (be instanceof OrderMachineBlockEntity orderBe) {
                      orderBe.scheduleInitialization(20);
                      if (placer instanceof net.minecraft.server.level.ServerPlayer sp && world instanceof ServerLevel sw) {
                          // Restore ID from dropped stack if present, otherwise allocate a new one
                          int id = 0;
                          CompoundTag c = DataCompat.copy(itemStack);
                          if (c != null) {
                              orderBe.applyRestaurantDataFromItemNbt(c);
                              if (c.contains(cn.breezeth.ordertocook.core.ModConstants.NBT_MACHINE_ID)) {
                                  id = c.getInt(cn.breezeth.ordertocook.core.ModConstants.NBT_MACHINE_ID);
                              }
                          }
                          if (id <= 0) {
                              id = cn.breezeth.ordertocook.core.MachineRankingState.get(sw).allocateId();
                          }
                          orderBe.setMachineId(id);
                          orderBe.setOwnerIfEmpty(sp);
                          orderBe.setPlaced(true);
                          cn.breezeth.ordertocook.core.RestaurantRegistry.registerById(sw, id, orderBe);
                      }
                 }
            }
        }
    }

    // Remove scheduledTick as we do it in onPlaced now


    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof OrderMachineBlockEntity orderBe) {
            ItemStack stack = new ItemStack(this);
            orderBe.setPlaced(false);
            CompoundTag nbt = orderBe.saveWithId(builder.getLevel().registryAccess());
            nbt.remove("x");
            nbt.remove("y");
            nbt.remove("z");
            DataCompat.set(stack, nbt);
            return List.of(stack);
        }
        return super.getDrops(state, builder);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OrderMachineBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide) {
            MenuProvider screenHandlerFactory = state.getMenuProvider(world, pos);
            if (screenHandlerFactory != null) {
                player.openMenu(screenHandlerFactory);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            if (!world.isClientSide && world instanceof ServerLevel sw) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof OrderMachineBlockEntity omb && omb.getMachineId() > 0) {
                    cn.breezeth.ordertocook.core.RestaurantRegistry.unregisterById(sw, omb.getMachineId());
                }
            }
        }
        super.onRemove(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ORDER_MACHINE.get(), (world1, pos, state1, blockEntity) -> blockEntity.tick(world1, pos, state1));
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (state.getValue(MODE) != Mode.PRINTING) return;
        int frame = state.getValue(FRAME);
        if (frame == 0) {
            world.setBlock(pos, state.setValue(FRAME, 1), Block.UPDATE_ALL);
            world.scheduleTick(pos, this, BEAT_TICKS);
            return;
        }
        if (frame == 1) {
            world.setBlock(pos, state.setValue(FRAME, 2), Block.UPDATE_ALL);
            world.scheduleTick(pos, this, BEAT_TICKS);
            return;
        }
        world.setBlock(pos, state.setValue(MODE, Mode.WAITING), Block.UPDATE_ALL);
    }
}
