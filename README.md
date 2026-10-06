# Command Bindings

A lightweight **client-side Fabric mod for Minecraft 1.21.1** that creates real command aliases.

## Main syntax

```text
/customcommand /home alias /spawn
```

Now typing:

```text
/home
```

sends:

```text
/spawn
```

The slash is optional in both places:

```text
/customcommand home alias spawn
```

Arguments are supported:

```text
/customcommand /rtp alias /rtp world
```

Then `/rtp` runs `/rtp world`.

## Management

```text
/customcommand list
/customcommand remove /home
```

There is also a shorter form:

```text
/bind /home /spawn
```

All aliases are stored in `.minecraft/config/command-bindings.json`.

The mod is client-side only and does not bypass server permissions; the target command is sent normally to the connected server.

## Compatibility

- Minecraft 1.21.1
- Fabric
- Java 21
- Fabric API 0.116.1+1.21.1
- Fabric Loader 0.16.7+
