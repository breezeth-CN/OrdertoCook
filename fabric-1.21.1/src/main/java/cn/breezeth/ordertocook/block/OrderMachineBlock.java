package cn.breezeth.ordertocook.block;

import cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity;
import cn.breezeth.ordertocook.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.entity.ItemEntity;

import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import cn.breezeth.ordertocook.util.DataCompat;
import net.minecraft.nbt.NbtCompound;
import java.util.List;
import cn.breezeth.ordertocook.config.ConfigManager;

public class OrderMachineBlock extends BlockWithEntity {
    public static final MapCodec<OrderMachineBlock> CODEC = createCodec(OrderMachineBlock::new);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public enum Mode implements StringIdentifiable {
        IDLE("idle"), PRINTING("printing"), WAITING("waiting");
        private final String name;
        Mode(String name) { this.name = name; }
        @Override public String asString() { return name; }
        @Override public String toString() { return name; }
    }
    public static final EnumProperty<Mode> MODE = EnumProperty.of("mode", Mode.class);
    public static final IntProperty FRAME = IntProperty.of("frame", 0, 2);
    public static final int BEAT_TICKS = 15;

    public OrderMachineBlock(Settings settings) {
        super(settings.requiresTool().strength(3.5F));
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(MODE, Mode.IDLE).with(FRAME, 0));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = Block.createCuboidShape(1, 0, 1, 15, 7, 15);
        return rotateShape(Direction.NORTH, state.get(FACING), shape);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = Block.createCuboidShape(1, 0, 1, 15, 7, 15);
        return rotateShape(Direction.NORTH, state.get(FACING), shape);
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

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODE, FRAME);
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
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient) {
            int maxAllowed = Math.max(1, ConfigManager.get().maxOrderMachinesWithin24);
            Box box = new Box(pos).expand(24);
            boolean duplicateFound = false;
            int found = 0;
            
            for (BlockPos checkPos : BlockPos.iterate(
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
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                
                if (placer instanceof PlayerEntity player) {
                    player.sendMessage(Text.translatable("message.ordertocook.order_machine.nearby").formatted(Formatting.RED), true);
                }

                // Refund exactly one machine preserving any NBT/components from the placed stack
                ItemStack refund = itemStack.copyWithCount(1);
                ItemEntity drop = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, refund);
                drop.setPickupDelay(10);
                world.spawnEntity(drop);
            } else {
                 BlockEntity be = world.getBlockEntity(pos);
                 if (be instanceof OrderMachineBlockEntity orderBe) {
                      orderBe.scheduleInitialization(20);
                      if (placer instanceof net.minecraft.server.network.ServerPlayerEntity sp && world instanceof ServerWorld sw) {
                          // Restore ID from dropped stack if present, otherwise allocate a new one
                          int id = 0;
                          NbtCompound c = DataCompat.copy(itemStack);
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
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        BlockEntity blockEntity = builder.getOptional(LootContextParameters.BLOCK_ENTITY);
        if (blockEntity instanceof OrderMachineBlockEntity orderBe) {
            ItemStack stack = new ItemStack(this);
            orderBe.setPlaced(false);
            NbtCompound nbt = orderBe.createNbtWithId(builder.getWorld().getRegistryManager());
            nbt.remove("x");
            nbt.remove("y");
            nbt.remove("z");
            DataCompat.set(stack, nbt);
            return List.of(stack);
        }
        return super.getDroppedStacks(state, builder);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new OrderMachineBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);
            if (screenHandlerFactory != null) {
                player.openHandledScreen(screenHandlerFactory);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            if (!world.isClient && world instanceof ServerWorld sw) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof OrderMachineBlockEntity omb && omb.getMachineId() > 0) {
                    cn.breezeth.ordertocook.core.RestaurantRegistry.unregisterById(sw, omb.getMachineId());
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlockEntities.ORDER_MACHINE, (world1, pos, state1, blockEntity) -> blockEntity.tick(world1, pos, state1));
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(MODE) != Mode.PRINTING) return;
        int frame = state.get(FRAME);
        if (frame == 0) {
            world.setBlockState(pos, state.with(FRAME, 1), Block.NOTIFY_ALL);
            world.scheduleBlockTick(pos, this, BEAT_TICKS);
            return;
        }
        if (frame == 1) {
            world.setBlockState(pos, state.with(FRAME, 2), Block.NOTIFY_ALL);
            world.scheduleBlockTick(pos, this, BEAT_TICKS);
            return;
        }
        world.setBlockState(pos, state.with(MODE, Mode.WAITING), Block.NOTIFY_ALL);
    }
}
