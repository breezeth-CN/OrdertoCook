package cn.breezeth.ordertocook.client;

import cn.breezeth.ordertocook.OrderToCookMod;
import cn.breezeth.ordertocook.client.gecko.FirstPersonArmCalibration;
import cn.breezeth.ordertocook.client.animation.RideOrientationLock;
import cn.breezeth.ordertocook.client.animation.RidePerspectiveSwitch;
import cn.breezeth.ordertocook.client.gecko.RiderRenderBridge;
import cn.breezeth.ordertocook.entity.SeatEntity;
import cn.breezeth.ordertocook.client.render.MotorcycleRenderer;
import cn.breezeth.ordertocook.client.render.feature.HelmetFeatureRenderer;
import cn.breezeth.ordertocook.client.render.model.MotorcycleModel;
import cn.breezeth.ordertocook.network.ModClientNetworking;
import cn.breezeth.ordertocook.network.ModNetworking;
import cn.breezeth.ordertocook.block.WashingTableBlock;
import cn.breezeth.ordertocook.registry.ModEntities;
import cn.breezeth.ordertocook.registry.ModBlocks;
import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.Identifier;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

public final class OrderToCookClient implements ClientModInitializer {
    private enum UiHandTarget {
        BOTH,
        LEFT,
        RIGHT
    }

    private static boolean hornMouseDown = false;
    private static boolean lightMouseDown = false;
    private static UUID ridingMotorcycleId = null;
    private static UUID pendingLightToggleMotorcycleId = null;
    private static int pendingLightToggleTicks = -1;
    private static float pendingLightTogglePitch = 1.0f;
    private static boolean uiControlEnabled = false;
    private static boolean uiDecreaseMode = false;
    private static boolean uiToggleDown = false;
    private static boolean uiHandToggleDown = false;
    private static boolean uiPauseDown = false;
    private static BlockPos activeWashPos = null;
    private static long activeWashStartAge = -1L;
    private static boolean washBlockedUntilRelease = false;
    private static UiHandTarget uiHandTarget = UiHandTarget.BOTH;
    private static final int[] UI_KEYS = {
            GLFW.GLFW_KEY_KP_7,
            GLFW.GLFW_KEY_KP_8,
            GLFW.GLFW_KEY_KP_9,
            GLFW.GLFW_KEY_KP_4,
            GLFW.GLFW_KEY_KP_5,
            GLFW.GLFW_KEY_KP_6
    };
    private static final boolean[] uiKeyDown = new boolean[UI_KEYS.length];
    private static boolean inventoryKeyDown = false;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CALIBRATION_FILE_NAME = "first_person_arm_calibration.json";

    @Override
    public void onInitializeClient() {
        loadCalibration();
        EntityRendererRegistry.register(ModEntities.MOTORCYCLE, MotorcycleRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(MotorcycleRenderer.LAYER, MotorcycleModel::getTexturedModelData);

        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.addModels(
                Identifier.of("ordertocook", "item/helmet_red"),
                Identifier.of("ordertocook", "item/helmet_blue"),
                Identifier.of("ordertocook", "item/helmet_yellow"),
                Identifier.of("ordertocook", "item/helmet_white")
            );
        });

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                registrationHelper.register(new HelmetFeatureRenderer<>(playerRenderer));
            }
        });

        RidePerspectiveSwitch.init();
        RideOrientationLock.init();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(createUiClientCommand("otcclient"));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            resetRidingClientState();
            RiderRenderBridge.clearAll();
        });
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> renderUiControlHud(drawContext));

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of("ordertocook", "reset_custom_skin_count");
                    }

                    @Override
                    public void reload(net.minecraft.resource.ResourceManager manager) {
                        cn.breezeth.ordertocook.client.renderer.CustomerEntityModel.resetCustomSkinCount();
                    }
                }
        );

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || client.options == null) {
                resetRidingClientState();
                inventoryKeyDown = false;
                return;
            }

            MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(client.player.getVehicle());
            boolean onMotorcycle = motorcycle != null;

            if (onMotorcycle) {
                int inventoryKeyCode = client.options.inventoryKey.getDefaultKey().getCode();
                boolean inventoryKeyPressed = GLFW.glfwGetKey(client.getWindow().getHandle(), inventoryKeyCode) == GLFW.GLFW_PRESS;
                if (inventoryKeyPressed && !inventoryKeyDown) {
                    if (client.currentScreen == null) {
                        ClientPlayNetworking.send(ModNetworking.OpenMotorcycleCoolerPayload.INSTANCE);
                    }
                }
                inventoryKeyDown = inventoryKeyPressed;
            } else {
                inventoryKeyDown = false;
            }

            if (activeWashPos != null) {
                client.options.forwardKey.setPressed(false);
                client.options.backKey.setPressed(false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                client.options.sneakKey.setPressed(false);
            }

            if (onMotorcycle || isChairSeat(client.player)) {
                client.options.attackKey.setPressed(false);
                if (onMotorcycle) {
                    client.options.useKey.setPressed(false);
                    client.options.inventoryKey.setPressed(false);
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.options == null) {
                stopWashing(client, true);
                resetRidingClientState();
                return;
            }

            if (client.currentScreen != null) {
                stopWashing(client, true);
                return;
            }
            if (uiControlEnabled) {
                processUiControlHotkeys(client);
            } else {
                resetUiKeyStates();
            }

            processWashingInput(client);

            MotorcycleEntity motorcycle = MotorcycleEntity.fromVehicle(client.player.getVehicle());
            boolean chairSeat = isChairSeat(client.player);
            if (motorcycle == null) {
                if (!chairSeat) {
                    hornMouseDown = false;
                    lightMouseDown = false;
                    ridingMotorcycleId = null;
                    pendingLightToggleMotorcycleId = null;
                    pendingLightToggleTicks = -1;
                    return;
                }

                boolean leftDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
                boolean clicked = leftDown && !hornMouseDown;
                hornMouseDown = leftDown;
                lightMouseDown = false;
                ridingMotorcycleId = null;
                pendingLightToggleMotorcycleId = null;
                pendingLightToggleTicks = -1;
                if (clicked) {
                    RiderRenderBridge.triggerChairClap(client.player);
                    ModClientNetworking.sendRiderAnimation(ModNetworking.RIDER_ANIM_CHAIR_CLAP, true);
                }
                return;
            }

            UUID currentMotorcycleId = motorcycle.getUuid();
            if (!currentMotorcycleId.equals(ridingMotorcycleId)) {
                ridingMotorcycleId = currentMotorcycleId;
                lightMouseDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
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
                        ClientPlayNetworking.send(ModNetworking.ToggleMotorcycleLightPayload.INSTANCE);
                        client.player.playSound(SoundEvents.BLOCK_LEVER_CLICK, 0.8f, pendingLightTogglePitch);
                        pendingLightToggleMotorcycleId = null;
                        pendingLightToggleTicks = -1;
                    }
                }
            }

            boolean rightDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            boolean rightClicked = rightDown && !lightMouseDown;
            lightMouseDown = rightDown;
            if (rightClicked) {
                Vec3d forwardVec = Vec3d.fromPolar(0, motorcycle.getYaw()).normalize().multiply(-1.0);
                double forwardSpeed = motorcycle.getVelocity().dotProduct(forwardVec);
                boolean reversing = forwardSpeed < -0.01;
                boolean enabled = !motorcycle.isLightEnabled();
                pendingLightToggleMotorcycleId = currentMotorcycleId;
                pendingLightToggleTicks = RiderRenderBridge.getLightToggleDelayTicks();
                pendingLightTogglePitch = enabled ? 1.1f : 0.9f;
                boolean animateLight = !reversing;
                RiderRenderBridge.triggerLightToggle(client.player, animateLight);
                ModClientNetworking.sendRiderAnimation(ModNetworking.RIDER_ANIM_LIGHT_TOGGLE, animateLight);
            }

            boolean leftDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean clicked = leftDown && !hornMouseDown;
            hornMouseDown = leftDown;
            if (!clicked) return;

            Vec3d forwardVec = Vec3d.fromPolar(0, motorcycle.getYaw()).normalize().multiply(-1.0);
            double forwardSpeed = motorcycle.getVelocity().dotProduct(forwardVec);
            boolean reversing = forwardSpeed < -0.01;
            boolean animateHorn = !reversing;
            RiderRenderBridge.triggerHorn(client.player, animateHorn);
            ModClientNetworking.sendRiderAnimation(ModNetworking.RIDER_ANIM_HORN, animateHorn);
        });
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

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> createUiClientCommand(String rootLiteral) {
        return ClientCommandManager.literal(rootLiteral)
                .then(ClientCommandManager.literal("uicontrol")
                        .then(ClientCommandManager.literal("clap")
                                .then(ClientCommandManager.argument("mode", StringArgumentType.word())
                                        .executes(context -> {
                                            String mode = StringArgumentType.getString(context, "mode");
                                            MinecraftClient client = MinecraftClient.getInstance();
                                            if (client.player == null) {
                                                return 0;
                                            }
                                            boolean enabled = "continue".equalsIgnoreCase(mode);
                                            RiderRenderBridge.setDebugChairClapLoop(client.player, enabled);
                                            client.player.sendMessage(Text.literal("clap loop: " + (enabled ? "continue" : "stop")), false);
                                            return 1;
                                        })))
                .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            uiControlEnabled = BoolArgumentType.getBool(context, "enabled");
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client.player != null) {
                                client.player.sendMessage(
                                        Text.literal("uicontrol: " + (uiControlEnabled ? "true" : "false")),
                                        false
                                );
                            }
                            return 1;
                        })))
                .then(ClientCommandManager.literal("calibration")
                        .then(ClientCommandManager.literal("export")
                                .executes(context -> exportCalibrationCommand())));
    }

    private static int exportCalibrationCommand() {
        saveCalibration();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("Calibration exported: " + calibrationPath()), false);
        }
        return 1;
    }

    private static boolean isChairSeat(net.minecraft.client.network.ClientPlayerEntity player) {
        return player != null && player.getVehicle() instanceof SeatEntity seat && seat.resolveParent() == null;
    }

    private static void processWashingInput(MinecraftClient client) {
        if (client.player == null || client.options == null) {
            return;
        }
        if (MotorcycleEntity.fromVehicle(client.player.getVehicle()) != null || isChairSeat(client.player)) {
            stopWashing(client, true);
            return;
        }

        boolean usePressed = client.options.useKey.isPressed();
        if (!usePressed) {
            washBlockedUntilRelease = false;
            stopWashing(client, true);
            return;
        }

        if (activeWashPos != null) {
            if (client.player.age - activeWashStartAge >= 80L) {
                BlockPos washPos = activeWashPos;
                if (canContinueWashing(client, washPos)) {
                    beginWashing(client, washPos);
                } else {
                    stopWashing(client, false);
                }
            } else if (!canContinueWashing(client, activeWashPos)) {
                stopWashing(client, true);
            }
            return;
        }

        if (washBlockedUntilRelease || !isEmptyHanded(client)) {
            return;
        }

        BlockPos washPos = findWashTarget(client);
        if (washPos == null) {
            return;
        }

        beginWashing(client, washPos);
    }

    private static void beginWashing(MinecraftClient client, BlockPos washPos) {
        if (client.player == null) {
            return;
        }
        activeWashPos = washPos;
        activeWashStartAge = client.player.age;
        RiderRenderBridge.triggerWashStart(client.player);
        ModClientNetworking.sendRiderAnimation(ModNetworking.RIDER_ANIM_WASH_START, true);
        ClientPlayNetworking.send(new ModNetworking.StartWashingPayload(washPos));
    }

    private static boolean canContinueWashing(MinecraftClient client, BlockPos pos) {
        return client.player != null
                && isEmptyHanded(client)
                && client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) <= 16.0
                && client.world != null
                && client.world.getBlockState(pos).getBlock() == ModBlocks.WASHINGTABLE
                && client.world.getBlockState(pos).getOrEmpty(WashingTableBlock.PLATES).orElse(0) > 0;
    }

    private static BlockPos findWashTarget(MinecraftClient client) {
        if (!isEmptyHanded(client) || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK || client.world == null) {
            return null;
        }

        BlockHitResult hitResult = (BlockHitResult) client.crosshairTarget;
        BlockPos pos = hitResult.getBlockPos();
        if (client.world.getBlockState(pos).getBlock() != ModBlocks.WASHINGTABLE) {
            return null;
        }
        if (client.world.getBlockState(pos).getOrEmpty(WashingTableBlock.PLATES).orElse(0) <= 0) {
            return null;
        }
        return pos;
    }

    private static boolean isEmptyHanded(MinecraftClient client) {
        return client.player != null && client.player.getMainHandStack().isEmpty() && client.player.getOffHandStack().isEmpty();
    }

    private static void stopWashing(MinecraftClient client, boolean notifyServer) {
        if (activeWashPos == null || client.player == null) {
            activeWashPos = null;
            activeWashStartAge = -1L;
            return;
        }
        if (notifyServer) {
            ClientPlayNetworking.send(ModNetworking.StopWashingPayload.INSTANCE);
        }
        RiderRenderBridge.triggerWashStop(client.player);
        ModClientNetworking.sendRiderAnimation(ModNetworking.RIDER_ANIM_WASH_STOP, false);
        activeWashPos = null;
        activeWashStartAge = -1L;
    }

    private static void renderUiControlHud(DrawContext drawContext) {
        if (!uiControlEnabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.textRenderer == null) {
            return;
        }

        String profile = RiderRenderBridge.getFirstPersonProfile(client.player).name().toLowerCase(Locale.ROOT);
        String hand = uiHandTarget.name().toLowerCase(Locale.ROOT);
        String mode = uiDecreaseMode ? "-1" : "+1";
        String paused = RiderRenderBridge.isAnimationPaused(client.player) ? "paused" : "running";

        drawContext.drawTextWithShadow(client.textRenderer, "uicontrol profile: " + profile, 8, 8, 0xFFFFFF);
        drawContext.drawTextWithShadow(client.textRenderer, "uicontrol hand: " + hand, 8, 20, 0xD0D0D0);
        drawContext.drawTextWithShadow(client.textRenderer, "uicontrol mode: " + mode, 8, 32, 0xD0D0D0);
        drawContext.drawTextWithShadow(client.textRenderer, "uicontrol animation: " + paused, 8, 44, 0xD0D0D0);
    }

    private static void processUiControlHotkeys(MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        boolean toggleDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_KP_0) == GLFW.GLFW_PRESS;
        if (toggleDown && !uiToggleDown) {
            uiDecreaseMode = !uiDecreaseMode;
            if (client.player != null) {
                client.player.sendMessage(Text.literal("uicontrol mode: " + (uiDecreaseMode ? "-1" : "+1")), true);
            }
        }
        uiToggleDown = toggleDown;

        boolean handToggleDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_KP_DECIMAL) == GLFW.GLFW_PRESS;
        if (handToggleDown && !uiHandToggleDown) {
            uiHandTarget = switch (uiHandTarget) {
                case BOTH -> UiHandTarget.LEFT;
                case LEFT -> UiHandTarget.RIGHT;
                case RIGHT -> UiHandTarget.BOTH;
            };
            if (client.player != null) {
                client.player.sendMessage(Text.literal("uicontrol hand: " + uiHandTarget.name().toLowerCase(Locale.ROOT)), true);
            }
        }
        uiHandToggleDown = handToggleDown;

        boolean pauseDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_KP_MULTIPLY) == GLFW.GLFW_PRESS;
        if (pauseDown && !uiPauseDown && client.player != null) {
            boolean paused = RiderRenderBridge.toggleAnimationPaused(client.player);
            client.player.sendMessage(Text.literal("animation paused: " + paused), true);
        }
        uiPauseDown = pauseDown;
        float direction = uiDecreaseMode ? -1.0f : 1.0f;

        for (int i = 0; i < UI_KEYS.length; i++) {
            boolean isDown = GLFW.glfwGetKey(handle, UI_KEYS[i]) == GLFW.GLFW_PRESS;
            if (isDown && !uiKeyDown[i]) {
                applyUiAdjustment(client, i, direction);
            }
            uiKeyDown[i] = isDown;
        }
    }

    private static void applyUiAdjustment(MinecraftClient client, int index, float direction) {
        FirstPersonArmCalibration.HandTarget target = switch (uiHandTarget) {
            case BOTH -> FirstPersonArmCalibration.HandTarget.BOTH;
            case LEFT -> FirstPersonArmCalibration.HandTarget.LEFT;
            case RIGHT -> FirstPersonArmCalibration.HandTarget.RIGHT;
        };
        FirstPersonArmCalibration.Profile profile = currentUiProfile(client);
        switch (index) {
            case 0 -> {
                FirstPersonArmCalibration.adjustBaseX(profile, FirstPersonArmCalibration.PIXEL * direction, target);
                sendUiValue(client, profile, "baseX", target);
            }
            case 1 -> {
                FirstPersonArmCalibration.adjustBaseY(profile, FirstPersonArmCalibration.PIXEL * direction, target);
                sendUiValue(client, profile, "baseY", target);
            }
            case 2 -> {
                FirstPersonArmCalibration.adjustBaseZ(profile, FirstPersonArmCalibration.PIXEL * direction, target);
                sendUiValue(client, profile, "baseZ", target);
            }
            case 3 -> {
                FirstPersonArmCalibration.adjustBaseXRot(profile, direction, target);
                sendUiValue(client, profile, "baseXRot", target);
            }
            case 4 -> {
                FirstPersonArmCalibration.adjustBaseYRot(profile, direction, target);
                sendUiValue(client, profile, "baseYRot", target);
            }
            case 5 -> {
                FirstPersonArmCalibration.adjustBaseZRot(profile, direction, target);
                sendUiValue(client, profile, "baseZRot", target);
            }
        }
    }

    private static FirstPersonArmCalibration.Profile currentUiProfile(MinecraftClient client) {
        if (client.player == null) {
            return FirstPersonArmCalibration.Profile.DRIVE;
        }
        return RiderRenderBridge.getFirstPersonProfile(client.player);
    }

    private static void sendUiValue(MinecraftClient client, FirstPersonArmCalibration.Profile profile, String key, FirstPersonArmCalibration.HandTarget target) {
        if (client.player == null) {
            return;
        }
        String valueText = switch (target) {
            case BOTH -> String.format(Locale.ROOT, "R%.3f L%.3f", valueForKey(profile, key, true), valueForKey(profile, key, false));
            case LEFT -> String.format(Locale.ROOT, "%.3f", valueForKey(profile, key, false));
            case RIGHT -> String.format(Locale.ROOT, "%.3f", valueForKey(profile, key, true));
        };
        client.player.sendMessage(Text.literal(profile.name().toLowerCase(Locale.ROOT) + "." + key + "=" + valueText), true);
    }

    private static float valueForKey(FirstPersonArmCalibration.Profile profile, String key, boolean right) {
        return switch (key) {
            case "baseX" -> right ? FirstPersonArmCalibration.mainX(profile) : FirstPersonArmCalibration.offX(profile);
            case "baseY" -> right ? FirstPersonArmCalibration.mainY(profile) : FirstPersonArmCalibration.offY(profile);
            case "baseZ" -> right ? FirstPersonArmCalibration.mainZ(profile) : FirstPersonArmCalibration.offZ(profile);
            case "baseXRot" -> right ? FirstPersonArmCalibration.mainXRot(profile) : FirstPersonArmCalibration.offXRot(profile);
            case "baseYRot" -> right ? FirstPersonArmCalibration.mainYRot(profile) : FirstPersonArmCalibration.offYRot(profile);
            case "baseZRot" -> right ? FirstPersonArmCalibration.mainZRot(profile) : FirstPersonArmCalibration.offZRot(profile);
            default -> 0.0f;
        };
    }

    private static void resetUiKeyStates() {
        uiToggleDown = false;
        uiHandToggleDown = false;
        uiPauseDown = false;
        for (int i = 0; i < uiKeyDown.length; i++) {
            uiKeyDown[i] = false;
        }
    }

    private static Path calibrationPath() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.runDirectory.toPath().resolve("config").resolve("ordertocook").resolve(CALIBRATION_FILE_NAME);
    }

    private static void loadCalibration() {
        try {
            Path path = calibrationPath();
            if (!Files.exists(path)) {
                return;
            }
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();

            float legacyBaseX = readFloat(root, "BASE_X", -6f * FirstPersonArmCalibration.PIXEL);
            float legacyBaseY = readFloat(root, "BASE_Y", -8f * FirstPersonArmCalibration.PIXEL);
            float legacyBaseZ = readFloat(root, "BASE_Z", -10f * FirstPersonArmCalibration.PIXEL);
            float legacyBaseXRot = readFloat(root, "BASE_X_ROT", 78.0f);
            float legacyBaseYRotMain = readFloat(root, "BASE_Y_ROT_MAIN", -12.0f);
            float legacyBaseYRotOff = readFloat(root, "BASE_Y_ROT_OFF", 12.0f);
            float legacyBaseZRotMain = readFloat(root, "BASE_Z_ROT_MAIN", -45.0f);
            float legacyBaseZRotOff = readFloat(root, "BASE_Z_ROT_OFF", -45.0f);

            FirstPersonArmCalibration.MAIN_X = readFloat(root, "MAIN_X",
                    legacyBaseX + readFloat(root, "RIGHT_X_OFFSET", 0.0f));
            FirstPersonArmCalibration.MAIN_Y = readFloat(root, "MAIN_Y",
                    legacyBaseY + readFloat(root, "RIGHT_Y_OFFSET", 0.0f));
            FirstPersonArmCalibration.MAIN_Z = readFloat(root, "MAIN_Z",
                    legacyBaseZ + readFloat(root, "RIGHT_Z_OFFSET", 0.0f));
            FirstPersonArmCalibration.MAIN_X_ROT = readFloat(root, "MAIN_X_ROT",
                    legacyBaseXRot + readFloat(root, "RIGHT_X_ROT_OFFSET", 0.0f));
            FirstPersonArmCalibration.MAIN_Y_ROT = readFloat(root, "MAIN_Y_ROT",
                    legacyBaseYRotMain + readFloat(root, "RIGHT_Y_ROT_OFFSET", 0.0f));
            FirstPersonArmCalibration.MAIN_Z_ROT = readFloat(root, "MAIN_Z_ROT",
                    legacyBaseZRotMain + readFloat(root, "RIGHT_Z_ROT_OFFSET", 0.0f));

            FirstPersonArmCalibration.OFF_X = readFloat(root, "OFF_X",
                    -(legacyBaseX + readFloat(root, "LEFT_X_OFFSET", 0.0f)));
            FirstPersonArmCalibration.OFF_Y = readFloat(root, "OFF_Y",
                    legacyBaseY + readFloat(root, "LEFT_Y_OFFSET", 0.0f));
            FirstPersonArmCalibration.OFF_Z = readFloat(root, "OFF_Z",
                    legacyBaseZ + readFloat(root, "LEFT_Z_OFFSET", 0.0f));
            FirstPersonArmCalibration.OFF_X_ROT = readFloat(root, "OFF_X_ROT",
                    legacyBaseXRot + readFloat(root, "LEFT_X_ROT_OFFSET", 0.0f));
            FirstPersonArmCalibration.OFF_Y_ROT = readFloat(root, "OFF_Y_ROT",
                    legacyBaseYRotOff + readFloat(root, "LEFT_Y_ROT_OFFSET", 0.0f));
            FirstPersonArmCalibration.OFF_Z_ROT = readFloat(root, "OFF_Z_ROT",
                    legacyBaseZRotOff + readFloat(root, "LEFT_Z_ROT_OFFSET", 0.0f));

            FirstPersonArmCalibration.ACTION_MAIN_X = readFloat(root, "ACTION_MAIN_X", FirstPersonArmCalibration.MAIN_X);
            FirstPersonArmCalibration.ACTION_MAIN_Y = readFloat(root, "ACTION_MAIN_Y", FirstPersonArmCalibration.MAIN_Y);
            FirstPersonArmCalibration.ACTION_MAIN_Z = readFloat(root, "ACTION_MAIN_Z", FirstPersonArmCalibration.MAIN_Z);
            FirstPersonArmCalibration.ACTION_MAIN_X_ROT = readFloat(root, "ACTION_MAIN_X_ROT", FirstPersonArmCalibration.MAIN_X_ROT);
            FirstPersonArmCalibration.ACTION_MAIN_Y_ROT = readFloat(root, "ACTION_MAIN_Y_ROT", FirstPersonArmCalibration.MAIN_Y_ROT);
            FirstPersonArmCalibration.ACTION_MAIN_Z_ROT = readFloat(root, "ACTION_MAIN_Z_ROT", FirstPersonArmCalibration.MAIN_Z_ROT);

            FirstPersonArmCalibration.ACTION_OFF_X = readFloat(root, "ACTION_OFF_X", FirstPersonArmCalibration.OFF_X);
            FirstPersonArmCalibration.ACTION_OFF_Y = readFloat(root, "ACTION_OFF_Y", FirstPersonArmCalibration.OFF_Y);
            FirstPersonArmCalibration.ACTION_OFF_Z = readFloat(root, "ACTION_OFF_Z", FirstPersonArmCalibration.OFF_Z);
            FirstPersonArmCalibration.ACTION_OFF_X_ROT = readFloat(root, "ACTION_OFF_X_ROT", FirstPersonArmCalibration.OFF_X_ROT);
            FirstPersonArmCalibration.ACTION_OFF_Y_ROT = readFloat(root, "ACTION_OFF_Y_ROT", FirstPersonArmCalibration.OFF_Y_ROT);
            FirstPersonArmCalibration.ACTION_OFF_Z_ROT = readFloat(root, "ACTION_OFF_Z_ROT", FirstPersonArmCalibration.OFF_Z_ROT);

        } catch (Exception e) {
            OrderToCookMod.LOGGER.warn("Failed to load first person arm calibration", e);
        }
    }

    private static void saveCalibration() {
        try {
            Path path = calibrationPath();
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();

            root.addProperty("MAIN_X", FirstPersonArmCalibration.MAIN_X);
            root.addProperty("MAIN_Y", FirstPersonArmCalibration.MAIN_Y);
            root.addProperty("MAIN_Z", FirstPersonArmCalibration.MAIN_Z);
            root.addProperty("MAIN_X_ROT", FirstPersonArmCalibration.MAIN_X_ROT);
            root.addProperty("MAIN_Y_ROT", FirstPersonArmCalibration.MAIN_Y_ROT);
            root.addProperty("MAIN_Z_ROT", FirstPersonArmCalibration.MAIN_Z_ROT);

            root.addProperty("OFF_X", FirstPersonArmCalibration.OFF_X);
            root.addProperty("OFF_Y", FirstPersonArmCalibration.OFF_Y);
            root.addProperty("OFF_Z", FirstPersonArmCalibration.OFF_Z);
            root.addProperty("OFF_X_ROT", FirstPersonArmCalibration.OFF_X_ROT);
            root.addProperty("OFF_Y_ROT", FirstPersonArmCalibration.OFF_Y_ROT);
            root.addProperty("OFF_Z_ROT", FirstPersonArmCalibration.OFF_Z_ROT);

            root.addProperty("ACTION_MAIN_X", FirstPersonArmCalibration.ACTION_MAIN_X);
            root.addProperty("ACTION_MAIN_Y", FirstPersonArmCalibration.ACTION_MAIN_Y);
            root.addProperty("ACTION_MAIN_Z", FirstPersonArmCalibration.ACTION_MAIN_Z);
            root.addProperty("ACTION_MAIN_X_ROT", FirstPersonArmCalibration.ACTION_MAIN_X_ROT);
            root.addProperty("ACTION_MAIN_Y_ROT", FirstPersonArmCalibration.ACTION_MAIN_Y_ROT);
            root.addProperty("ACTION_MAIN_Z_ROT", FirstPersonArmCalibration.ACTION_MAIN_Z_ROT);

            root.addProperty("ACTION_OFF_X", FirstPersonArmCalibration.ACTION_OFF_X);
            root.addProperty("ACTION_OFF_Y", FirstPersonArmCalibration.ACTION_OFF_Y);
            root.addProperty("ACTION_OFF_Z", FirstPersonArmCalibration.ACTION_OFF_Z);
            root.addProperty("ACTION_OFF_X_ROT", FirstPersonArmCalibration.ACTION_OFF_X_ROT);
            root.addProperty("ACTION_OFF_Y_ROT", FirstPersonArmCalibration.ACTION_OFF_Y_ROT);
            root.addProperty("ACTION_OFF_Z_ROT", FirstPersonArmCalibration.ACTION_OFF_Z_ROT);

            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            OrderToCookMod.LOGGER.warn("Failed to save first person arm calibration", e);
        }
    }

    private static float readFloat(JsonObject root, String key, float fallback) {
        if (!root.has(key) || !root.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return root.get(key).getAsFloat();
        } catch (Exception ignored) {
            return fallback;
        }
    }
}

