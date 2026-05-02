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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.breezeth.ordertocook.core.NpcNames;

import cn.breezeth.ordertocook.registry.ModSounds;
import cn.breezeth.ordertocook.command.ModCommands;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import cn.breezeth.ordertocook.core.OrderNpcManager;
import cn.breezeth.ordertocook.core.OrderGenerator;
import cn.breezeth.ordertocook.core.WashingTableManager;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import cn.breezeth.ordertocook.util.DataCompat;
import cn.breezeth.ordertocook.util.MotorcycleDeliveryFeedback;
import cn.breezeth.ordertocook.item.TakeoutBagItem;
import net.minecraft.nbt.NbtCompound;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;


public class OrderToCookMod implements ModInitializer {
	public static final String MOD_ID = ModConstants.MOD_ID;
	public static final Logger LOGGER = LoggerFactory.getLogger(ModConstants.MOD_ID);

	@Override
	public void onInitialize() {
		ConfigManager.load();
		LOGGER.info("Initializing " + ModConstants.MOD_ID + " mod server-side");

		ModBlocks.registerModBlocks();
		ModEntities.registerModEntities();
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOMER, CustomerEntity.createCustomerAttributes());
		ModItems.registerModItems();
		ModItemGroups.registerItemGroups();
		ModBlockEntities.registerBlockEntities();
		ModScreenHandlers.registerScreenHandlers();
        ModSounds.registerSounds();
        ModCriteria.register();
		ModNetworking.registerPayloadTypes();
		ModNetworking.registerServerReceivers();
        NpcNames.init();
        ModCommands.register();

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClient) return ActionResult.PASS;
			if (!(world instanceof ServerWorld sw)) return ActionResult.PASS;
			if (!(entity instanceof ArmorStandEntity seat)) return ActionResult.PASS;
            if (!seat.getCommandTags().contains("otc_npc")) return ActionResult.PASS;
			ItemStack stack = player.getStackInHand(hand);
			Hand selectedHand = hand;

			if (!(stack.getItem() instanceof cn.breezeth.ordertocook.item.TakeoutBagItem)) {
				Hand otherHand = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
				ItemStack otherStack = player.getStackInHand(otherHand);
				if (otherStack.getItem() instanceof cn.breezeth.ordertocook.item.TakeoutBagItem) {
					stack = otherStack;
					selectedHand = otherHand;
				}
			}

			if (!(stack.getItem() instanceof cn.breezeth.ordertocook.item.TakeoutBagItem)) return ActionResult.PASS;
			ActionResult result = cn.breezeth.ordertocook.item.TakeoutBagItem.trySubmitDeliveryFromEntityUse(sw, player, stack, seat);
			if (ConfigManager.isDevModeEnabled()) {
				player.sendMessage(Text.literal("[OTC Dev] Delivery UseEntityCallback(seat) hand=" + hand + " used=" + selectedHand + " result=" + result).formatted(Formatting.GRAY), false);
			}
			return result;
		});

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient) return true;
            if (player.getAbilities().creativeMode && state.getBlock() instanceof cn.breezeth.ordertocook.block.OrderMachineBlock) {
                player.sendMessage(Text.translatable("message.ordertocook.creative_break_warn").formatted(Formatting.AQUA), true);
                return false;
            }
            return true;
        });

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClient) return ActionResult.PASS;
			if (!(world instanceof ServerWorld sw)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity npc)) return ActionResult.PASS;
            if (!((net.minecraft.entity.Entity)npc).getCommandTags().contains("otc_npc")) return ActionResult.PASS;

			ItemStack stack = player.getStackInHand(hand);
			Hand selectedHand = hand;

			if (!(stack.getItem() instanceof cn.breezeth.ordertocook.item.TakeoutBagItem)
                    && !(stack.getItem() instanceof cn.breezeth.ordertocook.item.FoodPlateItem)) {
				Hand otherHand = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
				ItemStack otherStack = player.getStackInHand(otherHand);
				if (otherStack.getItem() instanceof cn.breezeth.ordertocook.item.TakeoutBagItem
                        || otherStack.getItem() instanceof cn.breezeth.ordertocook.item.FoodPlateItem) {
					stack = otherStack;
					selectedHand = otherHand;
				}
			}

			if (stack.getItem() instanceof cn.breezeth.ordertocook.item.FoodPlateItem) {
				ActionResult result = cn.breezeth.ordertocook.item.TakeoutBagItem.trySubmitDineInFromEntityUse(sw, player, stack, npc, ModSounds.FOOD_PLATE_PLACE);
				if (ConfigManager.isDevModeEnabled()) {
					player.sendMessage(Text.literal("[OTC Dev] UseEntityCallback(foodPlate) hand=" + hand + " used=" + selectedHand + " result=" + result).formatted(Formatting.GRAY), false);
				}
				return result;
			}

			if (!(stack.getItem() instanceof cn.breezeth.ordertocook.item.TakeoutBagItem)) return ActionResult.PASS;

			ActionResult result = cn.breezeth.ordertocook.item.TakeoutBagItem.trySubmitDeliveryFromEntityUse(sw, player, stack, npc);
			if (result == ActionResult.PASS) {
				result = cn.breezeth.ordertocook.item.TakeoutBagItem.trySubmitDineInFromEntityUse(sw, player, stack, npc, ModSounds.FOOD_PLATE_PLACE);
			}
			if (ConfigManager.isDevModeEnabled()) {
				player.sendMessage(Text.literal("[OTC Dev] UseEntityCallback(npc) hand=" + hand + " used=" + selectedHand + " result=" + result).formatted(Formatting.GRAY), false);
			}
			return result;
		});


        // Walk-in NPC Interaction
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity npc)) return ActionResult.PASS;
            if (!npc.getCommandTags().contains(OrderNpcManager.TAG_WALKIN)) return ActionResult.PASS;

            if (npc.getCommandTags().contains("otc_walkin_interacted")) {
                return ActionResult.PASS;
            }

            npc.addCommandTag("otc_walkin_interacted");

            long spawnTick = -1;
            for (String tag : npc.getCommandTags()) {
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
                for (String tag : npc.getCommandTags()) {
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
                    spawnTick = Math.max(0L, world.getTime() - elapsedTicks);
                }
            }
            if (spawnTick == -1) spawnTick = world.getTime();

            int level = 1;
            for (String tag : npc.getCommandTags()) {
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

            BlockPos machinePos = npc.getBlockPos();
            for (String tag : npc.getCommandTags()) {
                if (tag.startsWith(OrderNpcManager.TAG_WALKIN_MACHINE_POS_PREFIX)) {
                    try {
                        long packed = Long.parseLong(tag.substring(OrderNpcManager.TAG_WALKIN_MACHINE_POS_PREFIX.length()));
                        machinePos = BlockPos.fromLong(packed);
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                }
            }

            String customerName = npc.getCustomName() != null ? npc.getCustomName().getString() : Text.translatable("keyword.ordertocook.customer").getString();
            java.util.List<net.minecraft.item.Item> menuFoods = cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity.getBoundBoardMenuFoods((ServerWorld) world, machinePos);
            ItemStack order = OrderGenerator.generateWalkInOrder((ServerWorld) world, npc.getBlockPos(), level, spawnTick, customerName, menuFoods);

            NbtCompound nbt = DataCompat.copy(order);
            if (nbt != null) {
                nbt.putLong(ModConstants.NBT_MACHINE_POS, machinePos.asLong());
                nbt.putString(ModConstants.NBT_MACHINE_DIM, ((ServerWorld) world).getRegistryKey().getValue().toString());
                int machineId = 0;
                var be = world.getBlockEntity(machinePos);
                if (be instanceof cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity omb) {
                    machineId = omb.ensureMachineId((ServerWorld) world);
                }
                if (machineId > 0) {
                    nbt.putInt(ModConstants.NBT_MACHINE_ID, machineId);
                }
                DataCompat.set(order, nbt);
                String orderId = nbt.getString(ModConstants.NBT_ORDER_ID);
                OrderNpcManager.tagNpc(player, orderId, npc);
            }

            OrderNpcManager.changeToNormalTeam((ServerWorld) world, npc);

            if (!player.getInventory().insertStack(order)) {
                player.dropItem(order, false);
            }

            world.playSound(null, npc.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);

            return ActionResult.SUCCESS;
        });

        // Walk-in NPC Despawn Tick
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % 100 == 0) {
                cn.breezeth.ordertocook.core.WalkInNpcManager.checkDespawn(world);
                cn.breezeth.ordertocook.core.OrderNpcManager.checkOrderNpcDespawn(world);
                cn.breezeth.ordertocook.core.WalkInNpcManager.cleanupChairSeats(world);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            WashingTableManager.tick(server);
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                net.minecraft.entity.Entity vehicle = player.getVehicle();
                MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(vehicle);
                if (motorcycle == null) {
                    continue;
                }

                if (player.age <= 40 && vehicle instanceof SeatEntity) {
                    player.stopRiding();
                    double yawRad = motorcycle.getYaw() * (Math.PI / 180.0);
                    double backOffsetX = Math.sin(yawRad) * 0.8;
                    double backOffsetZ = -Math.cos(yawRad) * 0.8;
                    net.minecraft.util.math.Vec3d safePos = new net.minecraft.util.math.Vec3d(
                            motorcycle.getX() - backOffsetX,
                            motorcycle.getY() + 0.1,
                            motorcycle.getZ() - backOffsetZ
                    );
                    player.teleport(player.getServerWorld(), safePos.x, safePos.y, safePos.z, player.getYaw(), player.getPitch());
                }

                if (player.age % 10 == 0) {
                    for (int slot = 0; slot < motorcycle.getCoolerInventory().size(); slot++) {
                        ItemStack stack = motorcycle.getCoolerInventory().getStack(slot);
                        TakeoutBagItem.trySpawnDeliveryWhenNearby(player.getServerWorld(), player, stack);
                    }

                    Text deliveryFeedback = MotorcycleDeliveryFeedback.build(player, motorcycle);
                    if (deliveryFeedback != null) {
                        player.sendMessage(deliveryFeedback, true);
                    }
                }
            }
        });
	}
}
