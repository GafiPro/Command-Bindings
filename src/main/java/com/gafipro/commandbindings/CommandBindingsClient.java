package com.gafipro.commandbindings;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.text.Text;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;

public final class CommandBindingsClient implements ClientModInitializer {
    private static final String CREATE_SEPARATOR = " alias ";

    @Override
    public void onInitializeClient() {
        BindStore.load();

        ClientCommandRegistrationCallback.EVENT.register((commandDispatcher, registryAccess) ->
            registerManagementCommand(commandDispatcher)
        );

        ClientSendMessageEvents.MODIFY_COMMAND.register(CommandBindingsClient::rewriteCommand);
    }

    private static void registerManagementCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommandManager.literal("customcommand")
                .then(ClientCommandManager.argument("definition", StringArgumentType.greedyString())
                    .executes(context -> handleCustomCommand(
                        context.getSource(), getString(context, "definition"))))
        );

        dispatcher.register(
            ClientCommandManager.literal("bind")
                .then(ClientCommandManager.argument("definition", StringArgumentType.greedyString())
                    .executes(context -> handleBind(
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

        int separator = indexOfAliasSeparator(input);
        if (separator < 0) {
            source.sendFeedback(Text.literal("§cUsage: /customcommand /alias alias /command [arguments]"));
            return 0;
        }

        String alias = normalizeAlias(input.substring(0, separator));
        String command = normalizeCommand(input.substring(separator + CREATE_SEPARATOR.length()));
        return createOrUpdateAlias(source, alias, command);
    }

    private static int handleBind(FabricClientCommandSource source, String definition) {
        String input = definition.trim();
        int separator = input.indexOf(" ");

        if (separator < 0) {
            source.sendFeedback(Text.literal("§cUsage: /bind /alias /command [arguments]"));
            return 0;
        }

        String alias = normalizeAlias(input.substring(0, separator));
        String command = normalizeCommand(input.substring(separator + 1));
        return createOrUpdateAlias(source, alias, command);
    }

    private static int createOrUpdateAlias(FabricClientCommandSource source, String alias, String command) {
        if (!isValidAlias(alias)) {
            source.sendFeedback(Text.literal("§cInvalid alias. Use letters, numbers, ".", "_" or "-"."));
            return 0;
        }

        if (command.isEmpty()) {
            source.sendFeedback(Text.literal("§cThe target command cannot be empty."));
            return 0;
        }

        boolean replaced = BindStore.put(alias, command);

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

    private static String rewriteCommand(String command) {
        String input = command.trim();
        if (input.isEmpty()) {
            return command;
        }

        int firstSpace = input.indexOf(" ");
        String alias = firstSpace < 0 ? input : input.substring(0, firstSpace);
        String remainder = firstSpace < 0 ? "" : input.substring(firstSpace);

        String target = BindStore.get(normalizeAlias(alias));
        if (target == null || target.isBlank()) {
            return command;
        }

        return target + remainder;
    }

    private static int indexOfAliasSeparator(String input) {
        return input.toLowerCase(java.util.Locale.ROOT).indexOf(CREATE_SEPARATOR);
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
