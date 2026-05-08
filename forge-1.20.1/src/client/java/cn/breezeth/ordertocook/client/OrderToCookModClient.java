package cn.breezeth.ordertocook.client;

import cn.breezeth.ordertocook.block.WashingTableBlock;
import cn.breezeth.ordertocook.client.render.MotorcycleRenderer;
import cn.breezeth.ordertocook.client.render.model.MotorcycleModel;
import cn.breezeth.ordertocook.client.animation.RideOrientationLock;
import cn.breezeth.ordertocook.client.animation.RidePerspectiveSwitch;
import cn.breezeth.ordertocook.client.gecko.RiderRenderBridge;
import cn.breezeth.ordertocook.client.renderer.CustomerEntityRenderer;
import cn.breezeth.ordertocook.client.renderer.SeatEntityRenderer;
import cn.breezeth.ordertocook.client.renderer.WashingTableWaterRenderer;
import cn.breezeth.ordertocook.client.render.feature.HelmetFeatureRenderer;
import cn.breezeth.ordertocook.core.ModConstants;
import cn.breezeth.ordertocook.core.NpcNames;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.entity.CustomerEntity;
import cn.breezeth.ordertocook.block.entity.WashingTableBlockEntity;
import cn.breezeth.ordertocook.network.ModClientNetworking;
import cn.breezeth.ordertocook.network.ModNetworking;
import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.registry.ModBlocks;
import cn.breezeth.ordertocook.registry.ModEntities;
import cn.breezeth.ordertocook.registry.ModScreenHandlers;
import cn.breezeth.ordertocook.screen.BoardScreen;
import cn.breezeth.ordertocook.screen.OrderMachineScreen;
import cn.breezeth.ordertocook.screen.TakeoutBoxScreen;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.UUID;

@EventBusSubscriber(modid = ModConstants.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class OrderToCookModClient {
    private static boolean hornMouseDown = false;
    private static boolean lightMouseDown = false;
    private static boolean inventoryKeyDown = false;
    private static UUID ridingMotorcycleId = null;
    private static UUID pendingLightToggleMotorcycleId = null;
    private static int pendingLightToggleTicks = -1;
    private static float pendingLightTogglePitch = 1.0f;
    private static BlockPos activeWashPos = null;
    private static long activeWashStartAge = -1L;
    private static boolean washBlockedUntilRelease = false;
    private static boolean uiControlEnabled = false;

    private OrderToCookModClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            RideOrientationLock.register();
            RidePerspectiveSwitch.register();
            registerScreens();
            MinecraftForge.EVENT_BUS.addListener(OrderToCookModClient::onClientTickPre);
            MinecraftForge.EVENT_BUS.addListener(OrderToCookModClient::onClientTickPost);
            MinecraftForge.EVENT_BUS.addListener(OrderToCookModClient::onClientLogout);
            MinecraftForge.EVENT_BUS.addListener(OrderToCookModClient::onRenderGui);
            MinecraftForge.EVENT_BUS.addListener(OrderToCookModClient::onRegisterClientCommands);
        });
    }

    public static void registerScreens() {
        MenuType<? extends cn.breezeth.ordertocook.screen.OrderMachineScreenHandler> orderMachine =
                Objects.requireNonNull(ModScreenHandlers.ORDER_MACHINE_SCREEN_HANDLER.get());
        MenuType<? extends cn.breezeth.ordertocook.screen.TakeoutBoxScreenHandler> countertop =
                Objects.requireNonNull(ModScreenHandlers.COUNTERTOP_SCREEN_HANDLER.get());
        MenuType<? extends cn.breezeth.ordertocook.screen.BoardScreenHandler> board =
                Objects.requireNonNull(ModScreenHandlers.BOARD_SCREEN_HANDLER.get());
        MenuScreens.register(orderMachine, OrderMachineScreen::new);
        MenuScreens.register(countertop, TakeoutBoxScreen::new);
        MenuScreens.register(board, BoardScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        EntityType<? extends MotorcycleEntity> motorcycle = Objects.requireNonNull(ModEntities.MOTORCYCLE.get());
        EntityType<? extends SeatEntity> seat = Objects.requireNonNull(ModEntities.SEAT.get());
        EntityType<? extends CustomerEntity> customer = Objects.requireNonNull(ModEntities.CUSTOMER.get());
        BlockEntityType<? extends WashingTableBlockEntity> washingTable =
                Objects.requireNonNull(ModBlockEntities.WASHING_TABLE.get());
        event.registerEntityRenderer(motorcycle, MotorcycleRenderer::new);
        event.registerEntityRenderer(seat, SeatEntityRenderer::new);
        event.registerEntityRenderer(customer, CustomerEntityRenderer::new);
        event.registerBlockEntityRenderer(washingTable, WashingTableWaterRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(Objects.requireNonNull(MotorcycleRenderer.LAYER), MotorcycleModel::getTexturedModelData);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(model("helmet_red"));
        event.register(model("helmet_blue"));
        event.register(model("helmet_yellow"));
        event.register(model("helmet_white"));
    }

    @SubscribeEvent
    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) NpcNames::reloadFromClientResources);
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addRendererLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new HelmetFeatureRenderer(renderer));
            }
        }
    }

    private static void onClientTickPre(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.options == null) {
            resetRidingClientState();
            inventoryKeyDown = false;
            return;
        }

        MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
        boolean onMotorcycle = motorcycle != null;
        if (onMotorcycle) {
            int inventoryKeyCode = client.options.keyInventory.getDefaultKey().getValue();
            boolean inventoryKeyPressed = GLFW.glfwGetKey(client.getWindow().getWindow(), inventoryKeyCode) == GLFW.GLFW_PRESS;
            if (inventoryKeyPressed && !inventoryKeyDown && client.screen == null) {
                sendToServer(ModNetworking.OpenMotorcycleCoolerPayload.INSTANCE);
            }
            inventoryKeyDown = inventoryKeyPressed;
        } else {
            inventoryKeyDown = false;
        }

        if (activeWashPos != null) {
            client.options.keyUp.setDown(false);
            client.options.keyDown.setDown(false);
            client.options.keyLeft.setDown(false);
            client.options.keyRight.setDown(false);
            client.options.keyJump.setDown(false);
            client.options.keyShift.setDown(false);
        }

        if (onMotorcycle || isChairSeat(player)) {
            client.options.keyAttack.setDown(false);
            if (onMotorcycle) {
                client.options.keyUse.setDown(false);
                client.options.keyInventory.setDown(false);
            }
        }
    }

    private static void onClientTickPost(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.options == null) {
            stopWashing(client, true);
            resetRidingClientState();
            return;
        }
        if (client.screen != null) {
            stopWashing(client, true);
            return;
        }

        processWashingInput(client);

        MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(player.getVehicle());
        boolean chairSeat = isChairSeat(player);
        if (motorcycle == null) {
            if (!chairSeat) {
                hornMouseDown = false;
                lightMouseDown = false;
                ridingMotorcycleId = null;
                pendingLightToggleMotorcycleId = null;
                pendingLightToggleTicks = -1;
                return;
            }

            boolean leftDown = GLFW.glfwGetMouseButton(client.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean clicked = leftDown && !hornMouseDown;
            hornMouseDown = leftDown;
            lightMouseDown = false;
            ridingMotorcycleId = null;
            pendingLightToggleMotorcycleId = null;
            pendingLightToggleTicks = -1;
            if (clicked) {
                RiderRenderBridge.triggerChairClap(player);
                ModClientNetworking.sendRiderAnimation(ModNetworking.RIDER_ANIM_CHAIR_CLAP, true);
            }
            return;
        }

        UUID currentMotorcycleId = motorcycle.getUUID();
        if (!currentMotorcycleId.equals(ridingMotorcycleId)) {
            ridingMotorcycleId = currentMotorcycleId;
            lightMouseDown = GLFW.glfwGetMouseButton(client.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            pendingLightToggleMotorcycleId = null;
            pendingLightToggleTicks = -1;
        }

        if (pendingLightToggleTicks >= 0) {
            if (!currentMotorcycleId.equals(pendingLightToggleMotorcycleId)) {
                pendingLightToggleMotorcycleId = null;
                pendingLightToggleTicks = -1;
            } else {
                if (pendingLightToggleTicks > 0) {
                    pendingLightToggleTicks--;
                }
                if (pendingLightToggleTicks == 0) {
                    sendToServer(ModNetworking.ToggleMotorcycleLightPayload.INSTANCE);
                    SoundEvent leverClick = Objects.requireNonNull(SoundEvents.LEVER_CLICK);
                    player.playSound(leverClick, 0.8f, pendingLightTogglePitch);
                    pendingLightToggleMotorcycleId = null;
                    pendingLightToggleTicks = -1;
                }
            }
        }

        boolean rightDown = GLFW.glfwGetMouseButton(client.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean rightClicked = rightDown && !lightMouseDown;
        lightMouseDown = rightDown;
        if (rightClicked) {
            boolean reversing = isReversing(motorcycle);
            pendingLightToggleMotorcycleId = currentMotorcycleId;
            pendingLightToggleTicks = RiderRenderBridge.getLightToggleDelayTicks();
            pendingLightTogglePitch = motorcycle.isLightEnabled() ? 0.9f : 1.1f;
            boolean animateLight = !reversing;
            RiderRenderBridge.triggerLightToggle(player, animateLight);
            ModClientNetworking.sendRiderAnimation(ModNetworking.RIDER_ANIM_LIGHT_TOGGLE, animateLight);
        }

        boolean leftDown = GLFW.glfwGetMouseButton(client.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean clicked = leftDown && !hornMouseDown;
        hornMouseDown = leftDown;
        if (clicked) {
            boolean animateHorn = !isReversing(motorcycle);
            RiderRenderBridge.triggerHorn(player, animateHorn);
            ModClientNetworking.sendRiderAnimation(ModNetworking.RIDER_ANIM_HORN, animateHorn);
            ModClientNetworking.sendRiderSound(ModNetworking.RIDER_SOUND_HORN, 1.0f);
        }
    }

    private static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        resetRidingClientState();
        RiderRenderBridge.clearAll();
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        if (!uiControlEnabled) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.font == null) {
            return;
        }
        Font font = Objects.requireNonNull(client.font);
        event.getGuiGraphics().drawString(font, "uicontrol: enabled", 8, 8, 0xFFFFFF, true);
    }

    private static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        ArgumentType<Boolean> boolArgument = Objects.requireNonNull(BoolArgumentType.bool());
        event.getDispatcher().register(Commands.literal("otcclient")
                .then(Commands.literal("uicontrol")
                        .then(Commands.argument("enabled", boolArgument)
                                .executes(context -> {
                                    uiControlEnabled = BoolArgumentType.getBool(context, "enabled");
                                    Minecraft client = Minecraft.getInstance();
                                    if (client.player != null) {
                                        Component message = Objects.requireNonNull(Component.literal("uicontrol: " + uiControlEnabled));
                                        client.player.sendSystemMessage(message);
                                    }
                                    return 1;
                                }))));
    }

    private static boolean isChairSeat(LocalPlayer player) {
        return player != null && player.getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null;
    }

    private static boolean isReversing(MotorcycleEntity motorcycle) {
        Vec3 forwardVec = Vec3.directionFromRotation(0.0f, motorcycle.getYRot()).normalize().scale(-1.0);
        return motorcycle.getDeltaMovement().dot(forwardVec) < -0.01;
    }

    private static void processWashingInput(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.options == null) {
            return;
        }
        if (MotorcycleEntity.fromVehicle(player.getVehicle()) != null || isChairSeat(player)) {
            stopWashing(client, true);
            return;
        }

        boolean usePressed = client.options.keyUse.isDown();
        if (!usePressed) {
            washBlockedUntilRelease = false;
            stopWashing(client, true);
            return;
        }

        if (activeWashPos != null) {
            if (player.tickCount - activeWashStartAge >= 80L) {
                BlockPos washPos = activeWashPos;
                if (canContinueWashing(client, washPos)) {
                    beginWashing(player, washPos);
                } else {
                    stopWashing(client, false);
                }
            } else if (!canContinueWashing(client, activeWashPos)) {
                stopWashing(client, true);
            }
            return;
        }

        if (washBlockedUntilRelease || !isEmptyHanded(player)) {
            return;
        }

        BlockPos washPos = findWashTarget(client);
        if (washPos == null) {
            return;
        }

        beginWashing(player, washPos);
    }

    private static void beginWashing(LocalPlayer player, BlockPos washPos) {
        activeWashPos = washPos;
        activeWashStartAge = player.tickCount;
        RiderRenderBridge.triggerWashStart(player);
        ModClientNetworking.sendRiderAnimation(ModNetworking.RIDER_ANIM_WASH_START, true);
        sendToServer(new ModNetworking.StartWashingPayload(Objects.requireNonNull(washPos)));
    }

    private static boolean canContinueWashing(Minecraft client, BlockPos pos) {
        LocalPlayer player = client.player;
        Level level = client.level;
        if (player == null || level == null) {
            return false;
        }
        BlockPos safePos = Objects.requireNonNull(pos);
        Vec3 center = Objects.requireNonNull(Vec3.atCenterOf(safePos));
        BlockState state = level.getBlockState(safePos);
        IntegerProperty plates = Objects.requireNonNull(WashingTableBlock.PLATES);
        Block washingTable = Objects.requireNonNull(ModBlocks.WASHINGTABLE.get());
        return isEmptyHanded(player)
                && player.distanceToSqr(center) <= 16.0
                && state.getBlock() == washingTable
                && state.getValue(plates) > 0;
    }

    private static BlockPos findWashTarget(Minecraft client) {
        LocalPlayer player = client.player;
        HitResult hitResult = client.hitResult;
        Level level = client.level;
        if (player == null || !isEmptyHanded(player) || hitResult == null
                || hitResult.getType() != HitResult.Type.BLOCK || level == null) {
            return null;
        }

        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
        BlockPos pos = Objects.requireNonNull(blockHitResult.getBlockPos());
        BlockState state = level.getBlockState(pos);
        Block washingTable = Objects.requireNonNull(ModBlocks.WASHINGTABLE.get());
        if (state.getBlock() != washingTable) {
            return null;
        }
        IntegerProperty plates = Objects.requireNonNull(WashingTableBlock.PLATES);
        if (state.getValue(plates) <= 0) {
            return null;
        }
        return pos;
    }

    private static boolean isEmptyHanded(LocalPlayer player) {
        return player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
    }

    private static void stopWashing(Minecraft client, boolean notifyServer) {
        if (activeWashPos == null || client.player == null) {
            activeWashPos = null;
            activeWashStartAge = -1L;
            return;
        }
        if (notifyServer) {
            sendToServer(ModNetworking.StopWashingPayload.INSTANCE);
        }
        RiderRenderBridge.triggerWashStop(client.player);
        ModClientNetworking.sendRiderAnimation(ModNetworking.RIDER_ANIM_WASH_STOP, false);
        activeWashPos = null;
        activeWashStartAge = -1L;
    }

    private static void resetRidingClientState() {
        hornMouseDown = false;
        lightMouseDown = false;
        ridingMotorcycleId = null;
        pendingLightToggleMotorcycleId = null;
        pendingLightToggleTicks = -1;
        pendingLightTogglePitch = 1.0f;
        activeWashPos = null;
        activeWashStartAge = -1L;
        washBlockedUntilRelease = false;
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, path));
    }

    private static ModelResourceLocation model(String path) {
        return Objects.requireNonNull(new ModelResourceLocation(id(path), "inventory"));
    }

    private static void sendToServer(Object payload) {
        ModNetworking.sendToServer(Objects.requireNonNull(payload));
    }
}
