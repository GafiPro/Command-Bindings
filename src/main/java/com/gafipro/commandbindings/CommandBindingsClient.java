package com.gafipro.commandbindings;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;

public final class CommandBindingsClient implements ClientModInitializer {
    private static CommandDispatcher<FabricClientCommandSource> dispatcher;

    @Override
    public void onInitializeClient() {
        BindStore.load();

        ClientCommandRegistrationCallback.EVENT.register((commandDispatcher, registryAccess) -> {
            dispatcher = commandDispatcher;
            registerManagementCommands(commandDispatcher);
            BindStore.getBinds().keySet().forEach(alias -> registerAlias(commandDispatcher, alias));
        });
    }

    private static void registerManagementCommands(CommandDispatcher<FabricClientCommandSource> commandDispatcher) {
        commandDispatcher.register(
            ClientCommandManager.literal("customcommand")
                .then(ClientCommandManager.literal("list")
                    .executes(context -> {
                        if (BindStore.getBinds().isEmpty()) {
                            context.getSource().sendFeedback(Text.literal("§7Command Bindings: §fNo aliases configured."));
                            return 0;
                        }

                        context.getSource().sendFeedback(Text.literal("§7Command Bindings:"));
                        BindStore.getBinds().forEach((alias, command) ->
                            context.getSource().sendFeedback(
                                Text.literal("§8/§b" + alias + " §7-> §f/" + command)
                            )
                        );
                        return BindStore.getBinds().size();
                    }))
                .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("alias", StringArgumentType.word())
                        .executes(context -> {
                            String alias = normalizeAlias(getString(context, "alias"));

                            if (BindStore.remove(alias)) {
                                context.getSource().sendFeedback(
                                    Text.literal("§aRemoved command alias §f/" + alias + "§a.")
                                );
                            } else {
                                context.getSource().sendFeedback(
                                    Text.literal("§cNo command alias exists for §f/" + alias + "§c.")
                                );
                            }
                            return 1;
                        })))
                .then(ClientCommandManager.argument("alias", StringArgumentType.word())
                    .then(ClientCommandManager.literal("alias")
                        .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                            .executes(context -> {
                                String alias = normalizeAlias(getString(context, "alias"));
                                String command = normalizeCommand(getString(context, "command"));

                                if (!isValidAlias(alias)) {
                                    context.getSource().sendFeedback(
                                        Text.literal("§cInvalid alias. Use letters, numbers, ".", "_" or "-".")
                                    );
                                    return 0;
                                }

                                if (command.isEmpty()) {
                                    context.getSource().sendFeedback(
                                        Text.literal("§cThe target command cannot be empty.")
                                    );
                                    return 0;
                                }

                                boolean replaced = BindStore.put(alias, command);
                                registerAlias(getDispatcher(), alias);

                                context.getSource().sendFeedback(
                                    Text.literal((replaced ? "§eUpdated" : "§aCreated")
                                        + " §f/" + alias + " §7-> §f/" + command)
                                );
                                return 1;
                            }))));

        commandDispatcher.register(
            ClientCommandManager.literal("bind")
                .then(ClientCommandManager.argument("alias", StringArgumentType.word())
                    .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                        .executes(context -> {
                            String alias = normalizeAlias(getString(context, "alias"));
                            String command = normalizeCommand(getString(context, "command"));

                            if (!isValidAlias(alias) || command.isEmpty()) {
                                context.getSource().sendFeedback(
                                    Text.literal("§cUsage: /bind /alias /command [arguments]")
                                );
                                return 0;
                            }

                            boolean replaced = BindStore.put(alias, command);
                            registerAlias(getDispatcher(), alias);

                            context.getSource().sendFeedback(
                                Text.literal((replaced ? "§eUpdated" : "§aCreated")
                                    + " §f/" + alias + " §7-> §f/" + command)
                            );
                            return 1;
                        }))));
    }

    private static CommandDispatcher<FabricClientCommandSource> getDispatcher() {
        if (dispatcher == null) {
            throw new IllegalStateException("Command dispatcher is not active yet.");
        }
        return dispatcher;
    }

    private static void registerAlias(CommandDispatcher<FabricClientCommandSource> commandDispatcher, String alias) {
        if (commandDispatcher.getRoot().getChild(alias) != null) {
            return;
        }

        commandDispatcher.register(
            ClientCommandManager.literal(alias)
                .executes(context -> executeAlias(context.getSource(), alias))
        );
    }

    private static int executeAlias(FabricClientCommandSource source, String alias) {
        String command = BindStore.get(alias);

        if (command == null || command.isBlank()) {
            source.sendFeedback(Text.literal("§cNo command is configured for §f/" + alias + "§c."));
            return 0;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            source.sendFeedback(Text.literal("§cYou are not connected to a world/server."));
            return 0;
        }

        client.getNetworkHandler().sendChatCommand(command);
        return 1;
    }

    private static boolean isValidAlias(String alias) {
        return alias.matches("[A-Za-z0-9_.-]+");
    }

    static String normalizeAlias(String alias) {
        return alias == null ? "" : alias.trim().replaceFirst("^/", "");
    }

    static String normalizeCommand(String command) {
        return command == null ? "" : command.trim().replaceFirst("^/", "");
    }
}
