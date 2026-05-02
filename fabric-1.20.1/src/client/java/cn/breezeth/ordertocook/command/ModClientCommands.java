package cn.breezeth.ordertocook.command;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public final class ModClientCommands {
    private ModClientCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // Disabled due to conflict with server-side commands
            /*
            dispatcher.register(ClientCommandManager.literal("ordertocook")
                    .then(ClientCommandManager.literal("totlemoney")
                            .executes(context -> {
                                boolean sent = ModClientNetworking.sendPrestigeQuery();
                                if (!sent) {
                                    context.getSource().sendError(Text.translatable("command.ordertocook.not_connected"));
                                    return 0;
                                }
                                context.getSource().sendFeedback(Text.translatable("command.ordertocook.prestige_query_sent"));
                                return 1;
                            })));
            */
        });
    }
}
