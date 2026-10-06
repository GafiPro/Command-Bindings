# Command Bindings

Client-side command aliases for Fabric 1.21.1.

## Create an alias

```text
/customcommand /1 alias /tp mvcraft_
```

After creating it, simply use:

```text
/1
```

The mod changes the outgoing command from `1` to `tp mvcraft_` before it is sent, so the alias does not need to exist on the server.

Arguments can be appended:

```text
/1 Steve
```

becomes:

```text
/tp mvcraft_ Steve
```

## Other commands

```text
/customcommand list
/customcommand remove /1
/bind /1 /tp mvcraft_
```

Binds are saved in `.minecraft/config/command-bindings.json`.

## Compatibility

- Minecraft 1.21.1
- Fabric
- Java 21
- Fabric API 0.116.1+1.21.1
- Fabric Loader 0.16.7+

Client-side only.