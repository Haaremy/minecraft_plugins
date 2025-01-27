# Mc.Haaremy.de Plugins
Plugins for Velocity and PaperMc Server.

[Dependencie]
LuckPerms:
[Website](https://luckperms.net/ "Offizielle Website des Plugins.")
[GitHub](https://github.com/LuckPerms/LuckPerms "Offizielle Website des Plugins.")
[Download Velocity Plugin](https://download.luckperms.net/1569/velocity/LuckPerms-Velocity-5.4.152.jar "Download des Velocity Plugins von LuckPerms.")
[Download Bukkit/Paper Plugin](https://download.luckperms.net/1569/bukkit/loader/LuckPerms-Bukkit-5.4.152.jar "Download des Bukkit Plugins von LuckPerms.")


## My Server-Setup:
```bash
├── Velocity
|   ├── server.jar
|   ├── hmyLanguages
|   |   ├── hmyLanguage_xx.properties
|   |   |   
|   ├── plugins
|   |   ├── LuckPermsVelocity.jar
|   |   ├── hmyVelocity.jar
|   |   |   ├── hmyvelocityplugin
|   |   |   |   ├── hmyVelocity.conf
|   |   |   |
|   ├── servers
|   |   ├── global_plugins (optional)
|   |   |   ├── hmyPaper.jar
|   |   |   |
|   |   ├── Paper (Lobby)
|   |   |   ├── plugins
|   |   |   |   ├── LuckPermsBukkit.jar
|   |   |   |   ├── hmyPaper.jar (optional symbolic link /server/global_plugins/hmyPaper.jar)
|   |   |   |   ├── hmyLobby.jar
|   |   |   |   ├── hmySettings
|   |   |   |   |   ├── hmyServer.conf
|   |   |   |
|   |   ├── Paper (Survival)
|   |   |   ├── plugins
|   |   |   |   ├── LuckPermsBukkit.jar
|   |   |   |   ├── hmyPaper.jar (optional symbolic link /server/global_plugins/hmyPaper.jar)
|   |   |   |   ├── hmySettings
|   |   |   |   |   ├── hmyServer.conf
|   |   |   |
|   |   ├── Paper (Kitsune Segen (Game))
|   |   |   ├── plugins
|   |   |   |   ├── LuckPermsBukkit.jar
|   |   |   |   ├── hmyPaper.jar (optional symbolic link /server/global_plugins/hmyPaper.jar)
|   |   |   |   ├── hmyKitsuneSegen.jar
|   |   |   |   ├── hmySettings
|   |   |   |   |   ├── hmyServer.conf
```

## hmyVelocity

- Travel through Servers
- Recieve Commands from Subservers

| :Command: | :Description: | :Permission: |
|:---------|:-------------|------------:|
|lobby| Brings you back to Lobby, defined as "lobby" in velocity.toml.||
|hmy server [name]| Brings you to a Server, defined in velocity.toml|hmy.server.[name]|
|hmy language [language]|Changes the players permission to its language.|hmy.language|
|broadcast [proxy, server, world] [message]|Broadcasts a message.|hmy.broadcast|
|kick [player] [reason]| Kicks a player.|hmy.kick|
|ban| not implemented properly|hmy.ban|

## hmyPaper

- Antibuild
    -> Name of World in Settings activates AntiBuild
    -> Name of Worlds in Settings define Rules (allowed Blocks to place/brake, disable damage types)
- Chat Prefixes defined in LuckPerms
- Custom Tablist
- Spawnpoint and /spawn
    -> Name of World in Settings allows "/spawn"
- World Switcher


| :Command: | :Description: | :Permission: |
|:---------|:-------------|------------:|
|world [name]| Switch to world.|hmy.world.[name]|
|spawn| Brings you back to Spawn*.|-|
|dm [player]|Direct Messages a player|hmy.dm|
|r|Answers direct message|hmy.r|
|fly [player]*|Set fly-mode|hmy.fly|
|gm [name|id]|Set gamemode|hmy.gm|
|getpos [player]|Get World and Position of Player|hmy.getpos|


## hmyLobby

- Willkommensnachricht -> hmy.lobby.message.none: deaktiviert Nachricht
- Join Sound -> hmy.lobby.sound.none: deaktiviert Sound
- Partikel -> hmy.lobby.particle.none: deaktiviert Particle
- Bossbar -> hmy.lobby.bossbar.none: deaktiviert Bossbar
- Items: -> hmy.lobby.inventory.edit: erlaubt das verschieben von Items
    - head: cosmetics Menu
    - netherstar: server navigator Menu
    - arrow: speedboost
    - globe: language selector
    - blaze rod: heap into air
- server reconnect with fields 2 blocks below player -> gras+green_wool below --> Connect to Survival Server, when step onto

## KitsuneSegen (Game)
