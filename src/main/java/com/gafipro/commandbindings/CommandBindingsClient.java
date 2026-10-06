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
                .then(ClientCommandManager.argument("definition", StringArgumentType.greedyString())
                    .executes(context -> handleCustomCommand(
                        context.getSource(), getString(context, "definition"))))
        );

        commandDispatcher.register(
            ClientCommandManager.literal("bind")
                .then(ClientCommandManager.argument("definition", StringArgumentType.greedyString())
                    .executes(context -> handleShortBind(
                        context.getSource(), getString(context, "definition"))))
        );
    }

    private static int handleCustomCommand(FabricClientCommandSource source, String definition) {
        String input = definition.trim();

        if (input.equalsIgnoreCase("list")) {
            return listAliases(source);
        }

        if (input.regionMatches(true, 0, "remove ", 0, 7)) {
            return removeAlias(source, input.substring(7).trim());
        }

        int separator = input.toLowerCase().indexOf(" alias ");
        if (separator < 0) {
            source.sendFeedback(Text.literal("§cUsage: /customcommand /alias alias /command [arguments]"));
            return 0;
        }

        String alias = normalizeAlias(input.substring(0, separator).trim());
        String command = normalizeCommand(input.substring(separator + 7).trim());
        return createOrUpdateAlias(source, alias, command);
    }

    private static int handleShortBind(FabricClientCommandSource source, String definition) {
        String input = definition.trim();
        int separator = input.indexOf(" ");

        if (separator < 0) {
            source.sendFeedback(Text.literal("§cUsage: /bind /alias /command [arguments]"));
            return 0;
        }

        String alias = normalizeAlias(input.substring(0, separator).trim());
        String command = normalizeCommand(input.substring(separator + 1).trim());
        return createOrUpdateAlias(source, alias, command);
    }

    private static int createOrUpdateAlias(FabricClientCommandSource source, String alias, String command) {
        if (!isValidAlias(alias)) {
            source.sendFeedback(
                Text.literal("§cInvalid alias. Use letters, numbers, ".", "_" or "-".")
            );
            return 0;
        }

        if (command.isEmpty()) {
            source.sendFeedback(Text.literal("§cThe target command cannot be empty."));
            return 0;
        }

        boolean replaced = BindStore.put(alias, command);
        registerAlias(getDispatcher(), alias);

        source.sendFeedback(
            Text.literal((replaced ? "§eUpdated" : "§aCreated")
                + " §f/" + alias + " §7-> §f/" + command)
        );
        return 1;
    }

    private static int listAliases(FabricClientCommandSource source) {
        if (BindStore.getBinds().isEmpty()) {
            source.sendFeedback(Text.literal("§7Command Bindings: §fNo aliases configured."));
            return 0;
        }

        source.sendFeedback(Text.literal("§7Command Bindings:"));
        BindStore.getBinds().forEach((alias, command) ->
            source.sendFeedback(Text.literal("§8/§b" + alias + " §7-> §f/" + command))
        );
        return BindStore.getBinds().size();
    }

    private static int removeAlias(FabricClientCommandSource source, String rawAlias) {
        String alias = normalizeAlias(rawAlias);
        if (!isValidAlias(alias)) {
            source.sendFeedback(Text.literal("§cUsage: /customcommand remove /alias"));
            return 0;
        }

        if (BindStore.remove(alias)) {
            source.sendFeedback(Text.literal("§aRemoved command alias §f/" + alias + "§a."));
        } else {
            source.sendFeedback(Text.literal("§cNo command alias exists for §f/" + alias + "§c."));
        }
        return 1;
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
