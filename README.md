# Mc.Haaremy.de Plugins
Plugins for Velocity and PaperMc Server.

[Dependencie]
LuckPerms:
https://luckperms.net/
https://github.com/LuckPerms/LuckPerms

## My Server-Setup:
```bash
├── Velocity
|   ├── server.jar
|   ├── plugins
|   |   ├── hmyVelocity.jar
|   ├── Servers
|   |   ├── Paper (Lobby)
|   |   |   ├── plugins
|   |   |   |   ├── hmyPaper.jar
|   |   ├── Paper (Sonstige)
|   |   |   ├── plugins
|   |   |   |   ├── hmyPaper.jar
```

## hmyVelocity

- Travel through Servers

| :Command: | :Description: | :Permission: |
|:---------|:-------------|------------:|
|lobby| Brings you back to Lobby, defined as "lobby" in velocity.toml.||
|hmy server [name]| Brings you to a Server, defined in velocity.toml|hmy.server.[name]|
|         |             |            |

## hmyPaper

- Chat Prefixes defined by LuckPerms
- Custom Tablist
- Spawnpoint and /spawn
    -> Name of World in SpawnWorldNames.yml allows "/spawn"


| :Command: | :Description: | :Permission: |
|:---------|:-------------|------------:|
|spawn| Brings you back to Spawn*.||
|hmy server [name]| Brings you to a Server, defined in velocity.toml|hmy.server.[name]|
|         |             |            |


## hmyLobby

- Willkommensnachricht -> hmy.lobby.message.none: deaktiviert Nachricht
- Join Sound -> hmy.lobby.sound.none: deaktiviert Nachricht
- Partikel -> hmy.lobby.particle.none: deaktiviert Nachricht
- Anti Build 