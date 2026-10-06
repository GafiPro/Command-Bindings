package com.gafipro.commandbindings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BindStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("command-bindings.json");

    private static final Map<String, String> BINDS = new LinkedHashMap<>();

    private BindStore() {
    }

    public static void load() {
        BINDS.clear();

        if (!Files.exists(FILE)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(FILE)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }

            JsonObject binds = root.has("binds") && root.get("binds").isJsonObject()
                ? root.getAsJsonObject("binds")
                : root;

            binds.entrySet().forEach(entry -> {
                if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
                    return;
                }

                String alias = CommandBindingsClient.normalizeAlias(entry.getKey());
                String command = CommandBindingsClient.normalizeCommand(entry.getValue().getAsString());

                if (!alias.isEmpty() && !command.isEmpty() &&
                    alias.matches("[A-Za-z0-9_.-]+")) {
                    BINDS.put(alias, command);
                }
            });
        } catch (IOException | JsonParseException | IllegalStateException e) {
            System.err.println("[Command Bindings] Failed to load config: " + e.getMessage());
        }
    }

    public static boolean put(String alias, String command) {
        boolean replaced = BINDS.containsKey(alias);
        BINDS.put(alias, command);
        save();
        return replaced;
    }

    public static boolean remove(String alias) {
        boolean removed = BINDS.remove(alias) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public static String get(String alias) {
        return BINDS.get(alias);
    }

    public static Map<String, String> getBinds() {
        return Collections.unmodifiableMap(BINDS);
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());

            JsonObject root = new JsonObject();
            JsonObject binds = new JsonObject();

            BINDS.forEach(binds::addProperty);
            root.add("binds", binds);

            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            System.err.println("[Command Bindings] Failed to save config: " + e.getMessage());
        }
    }
}
