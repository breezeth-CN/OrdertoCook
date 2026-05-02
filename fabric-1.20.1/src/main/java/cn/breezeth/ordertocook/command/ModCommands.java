package cn.breezeth.ordertocook.command;

import cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity;
import cn.breezeth.ordertocook.config.ConfigManager;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.text.Text;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import cn.breezeth.ordertocook.core.OrderNpcManager;
import cn.breezeth.ordertocook.core.CustomerProfileLibrary;
import cn.breezeth.ordertocook.core.WalkInNpcManager;

import cn.breezeth.ordertocook.core.PrestigeManager;
import cn.breezeth.ordertocook.core.MachineRankingState;
import java.util.Comparator;
import java.util.Map;

public final class ModCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });
    }

    static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("ordertocook")
                        .executes(ctx -> {
                            ctx.getSource().sendError(Text.translatable("message.ordertocook.cmd.usage"));
                            return 0;
                        })
                        .then(literal("totlemoney")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                                        ctx.getSource().sendError(Text.translatable("message.ordertocook.cmd.player_only"));
                                        return 0;
                                    }
                                    int prestige = PrestigeManager.getPlayerPrestige(player);
                                    ctx.getSource().sendFeedback(() -> Text.translatable("command.ordertocook.prestige_result", prestige), false);
                                    return 1;
                                }))
                        .then(literal("dev")
                                .executes(ctx -> {
                                    ctx.getSource().sendError(Text.translatable("message.ordertocook.cmd.usage_dev"));
                                    return 0;
                                })
                                .then(argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            if (!hasDevPermission(ctx.getSource())) {
                                                ctx.getSource().sendError(Text.translatable("message.ordertocook.cmd.no_permission"));
                                                return 0;
                                            }
                                            boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                            ConfigManager.setDevModeEnabled(enabled);
                                            ctx.getSource().sendFeedback(() -> Text.translatable(enabled
                                                    ? "message.ordertocook.cmd.dev_enabled"
                                                    : "message.ordertocook.cmd.dev_disabled"), false);
                                            return 1;
                                        })))
                        .then(literal("neworder")
                                .executes(ctx -> executeNewOrder(ctx.getSource())))
                        .then(literal("newnpc")
                                .executes(ctx -> executeNewNpc(ctx.getSource(), false))
                                .then(literal("caidan")
                                        .executes(ctx -> executeNewNpc(ctx.getSource(), true))))
                        .then(literal("removenpc")
                                .executes(ctx -> executeRemoveNpc(ctx.getSource())))
                        .then(literal("rank")
                                .executes(ctx -> executeRank(ctx.getSource())))
                        .then(literal("custommenu")
                                .then(literal("hand")
                                        .then(argument("x", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeCustomMenuHand(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "x")))))
                                .then(literal("inventory")
                                        .then(argument("x", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeCustomMenuInventory(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "x")))))
                                )
        );
    }

    private static int executeCustomMenuHand(ServerCommandSource src, int hunger) {
        if (!src.hasPermissionLevel(2)) {
            src.sendError(Text.translatable("message.ordertocook.cmd.no_permission"));
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            src.sendError(Text.translatable("message.ordertocook.cmd.player_only"));
            return 0;
        }
        var stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            src.sendError(Text.literal("Main hand is empty."));
            return 0;
        }
        boolean ok = ConfigManager.upsertCustomMenuItem(stack, hunger);
        if (!ok) {
            src.sendError(Text.literal("Failed to add custom menu item."));
            return 0;
        }
        src.sendFeedback(() -> Text.literal("Added custom menu item: " + stack.getName().getString() + " (hunger=" + hunger + ")"), false);
        return 1;
    }

    private static int executeCustomMenuInventory(ServerCommandSource src, int hunger) {
        if (!src.hasPermissionLevel(2)) {
            src.sendError(Text.translatable("message.ordertocook.cmd.no_permission"));
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            src.sendError(Text.translatable("message.ordertocook.cmd.player_only"));
            return 0;
        }
        int added = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            var stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (ConfigManager.upsertCustomMenuItem(stack, hunger)) {
                added++;
            }
        }
        if (added <= 0) {
            src.sendError(Text.literal("No valid items in inventory."));
            return 0;
        }
        final int addedFinal = added;
        src.sendFeedback(() -> Text.literal("Added/updated custom menu items from inventory: " + addedFinal + " entries (hunger=" + hunger + ")"), false);
        return added;
    }

    private static int executeRemoveNpc(ServerCommandSource src) {
        if (!hasDevPermission(src)) {
            src.sendError(Text.translatable("message.ordertocook.cmd.no_permission"));
            return 0;
        }
        if (!ConfigManager.isDevModeEnabled()) {
            src.sendError(Text.translatable("message.ordertocook.cmd.neworder_dev_required"));
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            src.sendError(Text.translatable("message.ordertocook.cmd.player_only"));
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        int removed = OrderNpcManager.despawnAllNpcNear(world, player.getBlockPos(), 12);
        if (removed <= 0) {
            src.sendFeedback(() -> Text.translatable("message.ordertocook.cmd.no_npc_nearby"), false);
            return 0;
        }
        src.sendFeedback(() -> Text.translatable("message.ordertocook.cmd.removed_npcs", removed), false);
        return removed;
    }

    private static int executeNewNpc(ServerCommandSource src, boolean forceEasterEgg) {
        if (!hasDevPermission(src)) {
            src.sendError(Text.translatable("message.ordertocook.cmd.no_permission"));
            return 0;
        }
        if (!ConfigManager.isDevModeEnabled()) {
            src.sendError(Text.translatable("message.ordertocook.cmd.neworder_dev_required"));
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            src.sendError(Text.translatable("message.ordertocook.cmd.player_only"));
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos origin = player.getBlockPos();
        int radius = 64;
        OrderMachineBlockEntity target = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -8; dy <= 8; dy++) {
                    m.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    OrderMachineBlockEntity be = world.getBlockEntity(m) instanceof OrderMachineBlockEntity om ? om : null;
                    if (be == null) continue;
                    double d = player.getBlockPos().getSquaredDistance(be.getPos());
                    if (d < bestDist) {
                        bestDist = d;
                        target = be;
                    }
                }
            }
        }
        if (target == null) {
            src.sendError(Text.translatable("message.ordertocook.cmd.no_machine"));
            return 0;
        }

        final OrderMachineBlockEntity finalTarget = target;
        boolean spawned = forceEasterEgg
                ? WalkInNpcManager.spawn(world, target.getPos(), target.getLevel(), CustomerProfileLibrary.createGuaranteedEasterEggProfile(world))
                : OrderNpcManager.spawnWalkIn(world, target.getPos(), target.getLevel());
        if (!spawned) {
            src.sendError(Text.literal("未找到可用椅子或椅子生成位被占用"));
            return 0;
        }
        src.sendFeedback(() -> Text.translatable("message.ordertocook.cmd.npc_spawned_at", finalTarget.getPos().toShortString()), false);
        return 1;
    }

    private static int executeNewOrder(ServerCommandSource src) {
        if (!hasDevPermission(src)) {
            src.sendError(Text.translatable("message.ordertocook.cmd.no_permission"));
            return 0;
        }
        if (!ConfigManager.isDevModeEnabled()) {
            src.sendError(Text.translatable("message.ordertocook.cmd.neworder_dev_required"));
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            src.sendError(Text.translatable("message.ordertocook.cmd.player_only"));
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos origin = player.getBlockPos();
        int radius = 64;
        OrderMachineBlockEntity target = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -8; dy <= 8; dy++) {
                    m.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    OrderMachineBlockEntity be = world.getBlockEntity(m) instanceof OrderMachineBlockEntity om ? om : null;
                    if (be == null) continue;
                    double d = player.getBlockPos().getSquaredDistance(be.getPos());
                    if (d < bestDist) {
                        bestDist = d;
                        target = be;
                    }
                }
            }
        }
        if (target == null) {
            src.sendError(Text.translatable("message.ordertocook.cmd.no_machine"));
            return 0;
        }
        target.forceRefresh();
        src.sendFeedback(() -> Text.translatable("message.ordertocook.cmd.machine_refreshed"), false);
        return 1;
    }

    private static boolean hasDevPermission(ServerCommandSource src) {
        return src.hasPermissionLevel(4);
    }

    private static int executeRank(ServerCommandSource src) {
        if (!hasDevPermission(src)) {
            src.sendError(Text.translatable("message.ordertocook.cmd.no_permission"));
            return 0;
        }
        if (!ConfigManager.isDevModeEnabled()) {
            src.sendError(Text.translatable("message.ordertocook.cmd.neworder_dev_required"));
            return 0;
        }
        ServerWorld world = src.getWorld();
        MachineRankingState state = MachineRankingState.get(world);
        Map<Integer, MachineRankingState.Stats> all = state.all();
        if (all.isEmpty()) {
            src.sendFeedback(() -> Text.translatable("message.ordertocook.cmd.stats_empty"), false);
            return 1;
        }
        var list = new java.util.ArrayList<Map.Entry<Integer, MachineRankingState.Stats>>(all.entrySet());
        list.sort(Comparator.<Map.Entry<Integer, MachineRankingState.Stats>>comparingInt(e -> e.getValue().totalProfit).reversed()
                .thenComparing(Comparator.comparingInt(e -> e.getValue().accepted)).reversed());
        int max = Math.min(30, list.size());
        src.sendFeedback(() -> Text.translatable("message.ordertocook.cmd.ranking_title", max, list.size()), false);
        for (int i = 0; i < max; i++) {
            int rank = i + 1;
            var en = list.get(i);
            int id = en.getKey();
            var s = en.getValue();
            src.sendFeedback(() -> Text.translatable(
                    "message.ordertocook.cmd.ranking_row",
                    rank, id, s.name, s.owner, s.level, s.accepted, s.delivery, s.longDistance, s.totalProfit, s.deliveryProfit, s.maxDeliveryDist, s.walkIn
            ), false);
        }
        return 1;
    }

    private ModCommands() {}
}
