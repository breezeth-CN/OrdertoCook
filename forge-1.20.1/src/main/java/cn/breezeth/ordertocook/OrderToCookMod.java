package cn.breezeth.ordertocook;

import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.entity.CustomerEntity;
import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.registry.ModBlocks;
import cn.breezeth.ordertocook.registry.ModCriteria;
import cn.breezeth.ordertocook.registry.ModEntities;
import cn.breezeth.ordertocook.registry.ModItemGroups;
import cn.breezeth.ordertocook.registry.ModItems;
import cn.breezeth.ordertocook.registry.ModScreenHandlers;
import cn.breezeth.ordertocook.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.breezeth.ordertocook.core.NpcNames;

import cn.breezeth.ordertocook.registry.ModSounds;
import cn.breezeth.ordertocook.command.ModCommands;
import cn.breezeth.ordertocook.core.OrderNpcManager;
import cn.breezeth.ordertocook.core.OrderGenerator;
import cn.breezeth.ordertocook.core.WashingTableManager;
import cn.breezeth.ordertocook.util.DataCompat;
import cn.breezeth.ordertocook.util.MotorcycleDeliveryFeedback;
import cn.breezeth.ordertocook.item.TakeoutBagItem;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;


@Mod(ModConstants.MOD_ID)
public class OrderToCookMod {
	public static final String MOD_ID = ModConstants.MOD_ID;
	public static final Logger LOGGER = LoggerFactory.getLogger(ModConstants.MOD_ID);

	public OrderToCookMod() {
		this(FMLJavaModLoadingContext.get().getModEventBus());
	}

	public OrderToCookMod(IEventBus modBus) {
		ConfigManager.load();
		LOGGER.info("Initializing " + ModConstants.MOD_ID + " mod server-side");

		ModBlocks.registerModBlocks(modBus);
		ModEntities.registerModEntities(modBus);
		ModItems.registerModItems(modBus);
		ModItemGroups.registerItemGroups(modBus);
		ModBlockEntities.registerBlockEntities(modBus);
		ModScreenHandlers.registerScreenHandlers(modBus);
        ModSounds.registerSounds(modBus);
        ModCriteria.register(modBus);
        modBus.addListener(this::registerAttributes);
        ModNetworking.register();
		NpcNames.init();
        MinecraftForge.EVENT_BUS.addListener(this::onEntityInteract);
        MinecraftForge.EVENT_BUS.addListener(this::onBlockBreak);
        MinecraftForge.EVENT_BUS.addListener(this::onLevelTick);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
	}

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CUSTOMER.get(), CustomerEntity.createCustomerAttributes().build());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.registerCommands(event.getDispatcher());
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        var player = event.getEntity();
        var world = player.level();
        var hand = event.getHand();
        var entity = event.getTarget();

        InteractionResult result = handleSeatDelivery(player, world, hand, entity);
        if (result == InteractionResult.PASS) {
            result = handleNpcDelivery(player, world, hand, entity);
        }
        if (result == InteractionResult.PASS) {
            result = handleWalkInNpc(player, world, hand, entity);
        }
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private InteractionResult handleSeatDelivery(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level world, InteractionHand hand, Entity entity) {
			if (world.isClientSide) return InteractionResult.PASS;
			if (!(world instanceof ServerLevel sw)) return InteractionResult.PASS;
			if (!(entity instanceof ArmorStand seat)) return InteractionResult.PASS;
			if (!seat.getTags().contains("otc_npc")) return InteractionResult.PASS;
			ItemStack stack = player.getItemInHand(hand);
			InteractionHand selectedHand = hand;

			if (!(stack.getItem() instanceof cn.breezeth.ordertocook.item.TakeoutBagItem)) {
				InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
				ItemStack otherStack = player.getItemInHand(otherHand);
				if (otherStack.getItem() instanceof cn.breezeth.ordertocook.item.TakeoutBagItem) {
					stack = otherStack;
					selectedHand = otherHand;
				}
			}

			if (!(stack.getItem() instanceof cn.breezeth.ordertocook.item.TakeoutBagItem)) return InteractionResult.PASS;
			InteractionResult result = cn.breezeth.ordertocook.item.TakeoutBagItem.trySubmitDeliveryFromEntityUse(sw, player, stack, seat);
			if (ConfigManager.isDevModeEnabled()) {
				player.displayClientMessage(Component.literal("[OTC Dev] Delivery UseEntityCallback(seat) hand=" + hand + " used=" + selectedHand + " result=" + result).withStyle(ChatFormatting.GRAY), false);
			}
			return result;
    }

    private InteractionResult handleNpcDelivery(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level world, InteractionHand hand, Entity entity) {
			if (world.isClientSide) return InteractionResult.PASS;
			if (!(world instanceof ServerLevel sw)) return InteractionResult.PASS;
			if (!(entity instanceof LivingEntity npc)) return InteractionResult.PASS;
			if (!npc.getTags().contains("otc_npc")) return InteractionResult.PASS;

			ItemStack stack = player.getItemInHand(hand);
			InteractionHand selectedHand = hand;

			if (!(stack.getItem() instanceof cn.breezeth.ordertocook.item.TakeoutBagItem)
                    && !(stack.getItem() instanceof cn.breezeth.ordertocook.item.FoodPlateItem)) {
				InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
				ItemStack otherStack = player.getItemInHand(otherHand);
				if (otherStack.getItem() instanceof cn.breezeth.ordertocook.item.TakeoutBagItem
                        || otherStack.getItem() instanceof cn.breezeth.ordertocook.item.FoodPlateItem) {
					stack = otherStack;
					selectedHand = otherHand;
				}
			}

			if (stack.getItem() instanceof cn.breezeth.ordertocook.item.FoodPlateItem) {
				InteractionResult result = cn.breezeth.ordertocook.item.TakeoutBagItem.trySubmitDineInFromEntityUse(sw, player, stack, npc, ModSounds.FOOD_PLATE_PLACE.get());
				if (ConfigManager.isDevModeEnabled()) {
					player.displayClientMessage(Component.literal("[OTC Dev] UseEntityCallback(foodPlate) hand=" + hand + " used=" + selectedHand + " result=" + result).withStyle(ChatFormatting.GRAY), false);
				}
				return result;
			}

			if (!(stack.getItem() instanceof cn.breezeth.ordertocook.item.TakeoutBagItem)) return InteractionResult.PASS;

			InteractionResult result = cn.breezeth.ordertocook.item.TakeoutBagItem.trySubmitDeliveryFromEntityUse(sw, player, stack, npc);
			if (result == InteractionResult.PASS) {
				result = cn.breezeth.ordertocook.item.TakeoutBagItem.trySubmitDineInFromEntityUse(sw, player, stack, npc);
			}
			if (ConfigManager.isDevModeEnabled()) {
				player.displayClientMessage(Component.literal("[OTC Dev] UseEntityCallback(npc) hand=" + hand + " used=" + selectedHand + " result=" + result).withStyle(ChatFormatting.GRAY), false);
			}
			return result;
    }

    private void onBlockBreak(BlockEvent.BreakEvent event) {
            var world = event.getLevel();
            var player = event.getPlayer();
            if (world.isClientSide()) return;
            if (player.getAbilities().instabuild && event.getState().getBlock() instanceof cn.breezeth.ordertocook.block.OrderMachineBlock) {
                player.displayClientMessage(Component.translatable("message.ordertocook.creative_break_warn").withStyle(ChatFormatting.AQUA), true);
                event.setCanceled(true);
            }
    }


        // Walk-in NPC Interaction
    private InteractionResult handleWalkInNpc(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level world, InteractionHand hand, Entity entity) {
            if (world.isClientSide) return InteractionResult.PASS;
            if (!(entity instanceof LivingEntity npc)) return InteractionResult.PASS;
            if (!npc.getTags().contains(OrderNpcManager.TAG_WALKIN)) return InteractionResult.PASS;
            
            if (npc.getTags().contains("otc_walkin_interacted")) {
                // Already interacted
                return InteractionResult.PASS;
            }

            // Mark as interacted immediately
            npc.addTag("otc_walkin_interacted");

            long spawnTick = -1;
            for (String tag : npc.getTags()) {
                if (tag.startsWith(OrderNpcManager.TAG_WALKIN_SPAWN_TIME)) {
                    try {
                        spawnTick = Long.parseLong(tag.substring(OrderNpcManager.TAG_WALKIN_SPAWN_TIME.length()));
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                    break;
                }
            }
            if (spawnTick == -1) {
                long spawnSys = -1;
                for (String tag : npc.getTags()) {
                    if (tag.startsWith(OrderNpcManager.TAG_WALKIN_SPAWN_SYSTEM_TIME)) {
                        try {
                            spawnSys = Long.parseLong(tag.substring(OrderNpcManager.TAG_WALKIN_SPAWN_SYSTEM_TIME.length()));
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                        break;
                    }
                }
                if (spawnSys != -1) {
                    long elapsedMs = Math.max(0L, System.currentTimeMillis() - spawnSys);
                    long elapsedTicks = (elapsedMs + 49) / 50;
                    spawnTick = Math.max(0L, world.getGameTime() - elapsedTicks);
                }
            }
            if (spawnTick == -1) spawnTick = world.getGameTime();

            // Generate Order
            
            int level = 1;
            for (String tag : npc.getTags()) {
                if (tag.startsWith("otc_level:")) {
                    try {
                        level = Integer.parseInt(tag.substring("otc_level:".length()));
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                    break;
                }
            }
            if (level <= 0) level = 1;

            BlockPos machinePos = npc.blockPosition();
            for (String tag : npc.getTags()) {
                if (tag.startsWith(cn.breezeth.ordertocook.core.OrderNpcManager.TAG_WALKIN_MACHINE_POS_PREFIX)) {
                    try {
                        long packed = Long.parseLong(tag.substring(cn.breezeth.ordertocook.core.OrderNpcManager.TAG_WALKIN_MACHINE_POS_PREFIX.length()));
                        machinePos = BlockPos.of(packed);
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                }
            }

            String customerName = npc.getCustomName() != null ? npc.getCustomName().getString() : Component.translatable("keyword.ordertocook.customer").getString();
            java.util.List<net.minecraft.world.item.Item> menuFoods = cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.getBoundBoardMenuFoods((ServerLevel) world, machinePos);
            ItemStack order = OrderGenerator.generateWalkInOrder((ServerLevel) world, npc.blockPosition(), level, spawnTick, customerName, menuFoods);
            
            // Extract Order ID to tag NPC
            CompoundTag nbt = DataCompat.copy(order);
            if (nbt != null) {
                nbt.putLong(cn.breezeth.ordertocook.core.ModConstants.NBT_MACHINE_POS, machinePos.asLong());
                nbt.putString(cn.breezeth.ordertocook.core.ModConstants.NBT_MACHINE_DIM, ((ServerLevel) world).dimension().location().toString());
                int machineId = 0;
                var be = world.getBlockEntity(machinePos);
                if (be instanceof cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity omb) {
                    machineId = omb.ensureMachineId((ServerLevel) world);
                }
                if (machineId > 0) {
                    nbt.putInt(cn.breezeth.ordertocook.core.ModConstants.NBT_MACHINE_ID, machineId);
                }
                DataCompat.set(order, nbt);
                String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
                OrderNpcManager.tagNpc(player, orderId, npc);
            }
            
            // Change outline color
            OrderNpcManager.changeToNormalTeam((ServerLevel) world, npc);
            
            // Give to player
            if (!player.getInventory().add(order)) {
                player.drop(order, false);
            }
            
            world.playSound(null, npc.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            
            return InteractionResult.SUCCESS;
    }

        // Walk-in NPC Despawn Tick
    private void onLevelTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel world)) return;
            if (world.getGameTime() % 100 == 0) {
                cn.breezeth.ordertocook.core.WalkInNpcManager.checkDespawn(world);
                cn.breezeth.ordertocook.core.OrderNpcManager.checkOrderNpcDespawn(world);
                cn.breezeth.ordertocook.core.WalkInNpcManager.cleanupChairSeats(world);
            }
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            MinecraftServer server = event.getServer();
            WashingTableManager.tick(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Entity vehicle = player.getVehicle();
                MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(vehicle);
                if (motorcycle == null) {
                    continue;
                }

                if (player.tickCount <= 40 && vehicle instanceof SeatEntity) {
                    player.stopRiding();
                    double yawRad = motorcycle.getYRot() * (Math.PI / 180.0);
                    double backOffsetX = Math.sin(yawRad) * 0.8;
                    double backOffsetZ = -Math.cos(yawRad) * 0.8;
                    Vec3 safePos = new Vec3(
                            motorcycle.getX() - backOffsetX,
                            motorcycle.getY() + 0.1,
                            motorcycle.getZ() - backOffsetZ
                    );
                    player.teleportTo(player.serverLevel(), safePos.x, safePos.y, safePos.z, player.getYRot(), player.getXRot());
                }

                if (player.tickCount % 10 == 0) {
                    for (int slot = 0; slot < motorcycle.getCoolerInventory().getContainerSize(); slot++) {
                        ItemStack stack = motorcycle.getCoolerInventory().getItem(slot);
                        TakeoutBagItem.trySpawnDeliveryWhenNearby(player.serverLevel(), player, stack);
                    }

                    Component deliveryFeedback = MotorcycleDeliveryFeedback.build(player, motorcycle);
                    if (deliveryFeedback != null) {
                        player.displayClientMessage(deliveryFeedback, true);
                    }
                }
            }
    }
}
