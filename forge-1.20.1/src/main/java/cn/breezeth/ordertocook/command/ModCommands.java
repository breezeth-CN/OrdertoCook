package cn.breezeth.ordertocook.command;

import cn.breezeth.ordertocook.block.entity.OrderMachineBlockEntity;
import cn.breezeth.ordertocook.config.ConfigManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
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
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                literal("ordertocook")
                        .executes(ctx -> {
                            ctx.getSource().sendFailure(Component.translatable("message.ordertocook.cmd.usage"));
                            return 0;
                        })
                        .then(literal("totlemoney")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                        ctx.getSource().sendFailure(Component.translatable("message.ordertocook.cmd.player_only"));
                                        return 0;
                                    }
                                    int prestige = PrestigeManager.getPlayerPrestige(player);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.ordertocook.prestige_result", prestige), false);
                                    return 1;
                                }))
                        .then(literal("dev")
                                .executes(ctx -> {
                                    ctx.getSource().sendFailure(Component.translatable("message.ordertocook.cmd.usage_dev"));
                                    return 0;
                                })
                                .then(argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            if (!hasDevPermission(ctx.getSource())) {
                                                ctx.getSource().sendFailure(Component.translatable("message.ordertocook.cmd.no_permission"));
                                                return 0;
                                            }
                                            boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                            ConfigManager.setDevModeEnabled(enabled);
                                            ctx.getSource().sendSuccess(() -> Component.translatable(enabled
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

    private static int executeCustomMenuHand(CommandSourceStack src, int hunger) {
        if (!src.hasPermission(2)) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.no_permission"));
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.player_only"));
            return 0;
        }
        var stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            src.sendFailure(Component.literal("Main hand is empty."));
            return 0;
        }
        boolean ok = ConfigManager.upsertCustomMenuItem(stack, hunger);
        if (!ok) {
            src.sendFailure(Component.literal("Failed to add custom menu item."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Added custom menu item: " + stack.getHoverName().getString() + " (hunger=" + hunger + ")"), false);
        return 1;
    }

    private static int executeCustomMenuInventory(CommandSourceStack src, int hunger) {
        if (!src.hasPermission(2)) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.no_permission"));
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.player_only"));
            return 0;
        }
        int added = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (ConfigManager.upsertCustomMenuItem(stack, hunger)) {
                added++;
            }
        }
        if (added <= 0) {
            src.sendFailure(Component.literal("No valid items in inventory."));
            return 0;
        }
        final int addedFinal = added;
        src.sendSuccess(() -> Component.literal("Added/updated custom menu items from inventory: " + addedFinal + " entries (hunger=" + hunger + ")"), false);
        return added;
    }

    private static int executeRemoveNpc(CommandSourceStack src) {
        if (!hasDevPermission(src)) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.no_permission"));
            return 0;
        }
        if (!ConfigManager.isDevModeEnabled()) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.neworder_dev_required"));
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.player_only"));
            return 0;
        }

        ServerLevel world = player.serverLevel();
        int removed = OrderNpcManager.despawnAllNpcNear(world, player.blockPosition(), 12);
        if (removed <= 0) {
            src.sendSuccess(() -> Component.translatable("message.ordertocook.cmd.no_npc_nearby"), false);
            return 0;
        }
        src.sendSuccess(() -> Component.translatable("message.ordertocook.cmd.removed_npcs", removed), false);
        return removed;
    }

    private static int executeNewNpc(CommandSourceStack src, boolean forceEasterEgg) {
        if (!hasDevPermission(src)) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.no_permission"));
            return 0;
        }
        if (!ConfigManager.isDevModeEnabled()) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.neworder_dev_required"));
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.player_only"));
            return 0;
        }

        ServerLevel world = player.serverLevel();
        BlockPos origin = player.blockPosition();
        int radius = 64;
        OrderMachineBlockEntity target = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -8; dy <= 8; dy++) {
                    m.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    OrderMachineBlockEntity be = world.getBlockEntity(m) instanceof OrderMachineBlockEntity om ? om : null;
                    if (be == null) continue;
                    double d = player.blockPosition().distSqr(be.getBlockPos());
                    if (d < bestDist) {
                        bestDist = d;
                        target = be;
                    }
                }
            }
        }
        if (target == null) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.no_machine"));
            return 0;
        }
        
        final OrderMachineBlockEntity finalTarget = target;
        boolean spawned = forceEasterEgg
                ? WalkInNpcManager.spawn(world, target.getBlockPos(), target.getRestaurantLevel(), CustomerProfileLibrary.createGuaranteedEasterEggProfile(world))
                : OrderNpcManager.spawnWalkIn(world, target.getBlockPos(), target.getRestaurantLevel());
        if (!spawned) {
            src.sendFailure(Component.literal("未找到可用椅子或椅子生成位被占用"));
            return 0;
        }
        src.sendSuccess(() -> Component.translatable("message.ordertocook.cmd.npc_spawned_at", finalTarget.getBlockPos().toShortString()), false);
        return 1;
    }

    private static int executeNewOrder(CommandSourceStack src) {
        if (!hasDevPermission(src)) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.no_permission"));
            return 0;
        }
        if (!ConfigManager.isDevModeEnabled()) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.neworder_dev_required"));
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.player_only"));
            return 0;
        }

        ServerLevel world = player.serverLevel();
        BlockPos origin = player.blockPosition();
        int radius = 64;
        OrderMachineBlockEntity target = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -8; dy <= 8; dy++) {
                    m.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    OrderMachineBlockEntity be = world.getBlockEntity(m) instanceof OrderMachineBlockEntity om ? om : null;
                    if (be == null) continue;
                    double d = player.blockPosition().distSqr(be.getBlockPos());
                    if (d < bestDist) {
                        bestDist = d;
                        target = be;
                    }
                }
            }
        }
        if (target == null) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.no_machine"));
            return 0;
        }
        target.forceRefresh();
        src.sendSuccess(() -> Component.translatable("message.ordertocook.cmd.machine_refreshed"), false);
        return 1;
    }

    private static boolean hasDevPermission(CommandSourceStack src) {
        return src.hasPermission(4);
    }

    private static int executeRank(CommandSourceStack src) {
        if (!hasDevPermission(src)) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.no_permission"));
            return 0;
        }
        if (!ConfigManager.isDevModeEnabled()) {
            src.sendFailure(Component.translatable("message.ordertocook.cmd.neworder_dev_required"));
            return 0;
        }
        ServerLevel world = src.getLevel();
        MachineRankingState state = MachineRankingState.get(world);
        Map<Integer, MachineRankingState.Stats> all = state.all();
        if (all.isEmpty()) {
            src.sendSuccess(() -> Component.translatable("message.ordertocook.cmd.stats_empty"), false);
            return 1;
        }
        var list = new java.util.ArrayList<Map.Entry<Integer, MachineRankingState.Stats>>(all.entrySet());
        list.sort(Comparator.<Map.Entry<Integer, MachineRankingState.Stats>>comparingInt(e -> e.getValue().totalProfit).reversed()
                .thenComparing(Comparator.comparingInt(e -> e.getValue().accepted)).reversed());
        int max = Math.min(30, list.size());
        src.sendSuccess(() -> Component.translatable("message.ordertocook.cmd.ranking_title", max, list.size()), false);
        for (int i = 0; i < max; i++) {
            int rank = i + 1;
            var en = list.get(i);
            int id = en.getKey();
            var s = en.getValue();
            src.sendSuccess(() -> Component.translatable(
                    "message.ordertocook.cmd.ranking_row",
                    rank, id, s.name, s.owner, s.level, s.accepted, s.delivery, s.longDistance, s.totalProfit, s.deliveryProfit, s.maxDeliveryDist, s.walkIn
            ), false);
        }
        return 1;
    }

    private ModCommands() {}
}
