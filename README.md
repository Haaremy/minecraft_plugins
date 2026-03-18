# mc.haaremy.de – Plugins

Custom plugins for a Velocity + PaperMC network.

---

## Dependencies

| Plugin | Platform | Links |
|--------|----------|-------|
| **LuckPerms** | Velocity + Bukkit | [Website](https://luckperms.net/) · [GitHub](https://github.com/LuckPerms/LuckPerms) · [Velocity JAR](https://download.luckperms.net/1569/velocity/LuckPerms-Velocity-5.4.152.jar) · [Bukkit JAR](https://download.luckperms.net/1569/bukkit/loader/LuckPerms-Bukkit-5.4.152.jar) |

---

## Server Structure

```
├── Velocity
│   ├── plugins/
│   │   ├── LuckPerms-Velocity.jar
│   │   └── hmyVelocity.jar
│   │       └── data/
│   │           ├── economy.json
│   │           ├── friends.json
│   │           └── friend_requests.json
│   └── hmyLanguages/
│       └── hmyLanguage_<lang>.properties
│
├── Paper – Lobby
│   └── plugins/
│       ├── LuckPerms-Bukkit.jar
│       ├── hmyPaper.jar
│       ├── hmyLobby.jar
│       └── hmySettings/
│           ├── hmyServer.conf
│           ├── lobbygames.yml
│           └── parkour.yml
│
├── Paper – Survival / Game
│   └── plugins/
│       ├── LuckPerms-Bukkit.jar
│       ├── hmyPaper.jar
│       └── hmySettings/
│           ├── hmyServer.conf
│           ├── homes.yml
│           └── parkour.yml
│
└── Paper – KitsuneSegen (Game)
    └── plugins/
        ├── LuckPerms-Bukkit.jar
        ├── hmyPaper.jar
        └── hmyKitsuneSegen.jar
```

---

## hmyVelocity

Proxy-layer plugin managing cross-server communication, economy, social features, and the network-wide tab list.

### Features

- **Player routing** – sends players to `lobby` on join; `/lobby` on sub-servers forwards back
- **Network-wide tab list** (`VelocityTabManager`)
  - Lobby: all players grouped by server ([Lobby], [Survival], …), friends always shown at the bottom in cyan
  - Other servers: only players on the same server
  - Max 80 entries; friend slots are always reserved regardless of the limit
  - Updates every 2 seconds; instant rebuild on server switch or disconnect
  - Header shows server name and player count; footer shows ping
- **Server status broadcasting** – pushes live player counts to lobby every 5 s via `hmy:status`
- **Language system** – stores preference as a LuckPerms meta node (`language.de` / `language.en`)
- **Economy** – dual currency: `hmyCoins` and `hmyShards`; stored in `data/economy.json`
- **Friends system** – add / accept / deny / remove friends; stored in `data/friends.json`
- **Follow system** – auto-join a friend's server on their server switches
- **Direct messages** – cross-server `/dm` and `/r` with SocialSpy support
- **Reports** – `/report <player> <reason>` logs to `data/reports.log` and notifies admins
- **Join / leave messages** – vanilla messages suppressed on all servers; lobby shows formatted join and server-switch messages with clickable player names
- **Plugin message channels**
  - `hmy:status` – live player counts to lobby
  - `hmy:economy` – coin events from lobby; balance responses back
  - `hmy:social` – friend data requests / friend join requests
  - `hmy:trigger` – sub-servers trigger Velocity commands on behalf of a player

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/hmy language <de\|en>` | Change your language preference | — |
| `/hmy coins` | Show your hmyCoins and hmyShards balance | — |
| `/hmy coins give <player> <amount>` | Give a player hmyCoins (admin) | `hmy.coins.give` |
| `/friend add <player>` | Send a friend request | — |
| `/friend accept <player>` | Accept a pending friend request | — |
| `/friend deny <player>` | Deny a pending friend request | — |
| `/friend remove <player>` | Remove a friend | — |
| `/friend list` | List all friends grouped by online/offline | — |
| `/friend join <player>` | Connect to a friend's server | — |
| `/friend follow <player>` | Follow a friend across server switches | — |
| `/friend unfollow` | Stop following | — |
| `/dm <player> <message>` | Send a cross-server private message | — |
| `/r <message>` | Reply to the last private message | — |
| `/report <player> <reason>` | Report a player | — |
| `/broadcast <message>` | Broadcast to all servers | `hmy.broadcast` |

### Permissions

| Permission | Description |
|-----------|-------------|
| `hmy.coins.give` | Give hmyCoins to other players |
| `hmy.broadcast` | Broadcast network-wide messages |
| `hmy.socialspy` | Receive SocialSpy notifications for DMs |
| `hmy.report.admin` | Receive report notifications |

---

## hmyPaper

General-purpose Paper plugin used on all sub-servers.

### Features

- **AntiBuild** – per-world build protection with allowed-block overrides
- **Chat prefixes** – pulled from LuckPerms groups
- **Spawn system** – `/spawn` teleports to world spawn (disabled on worlds starting with `survival`)
- **World switcher** – `/world <name>` moves players between worlds; automatically loads unloaded worlds from disk on demand
- **Home system** – up to 5 home slots per player, each with its own permission; only usable in worlds starting with `survival_`; stored in `hmySettings/homes.yml`
- **Parkour system** – create start / checkpoint / goal blocks; tracks time, checkpoints, and auto-resets on finish; fall detection: if the player drops 10 blocks below the last checkpoint they are teleported back; stored in `hmySettings/parkour.yml`
- **Moderation** – ban, tempban, IP-ban, kick, mute, vanish, invsee, sudo
- **Essentials** – heal, feed, tp, tphere, back, workbench, enderchest, repair
- **Utilities** – fly, gamemode, speed, weather, time (`day`/`noon`/`night`/`midnight`), lightning, skull, getpos, socialspy, worlds

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/help [page]` | List available commands | `hmy.help` |
| `/rules [page]` | Show server rules | `hmy.rules` |
| `/spawn` | Teleport to world spawn | — |
| `/lobby` | Return to lobby server | — |
| `/world <name>` | Switch to another world; loads it from disk if not yet loaded | `hmy.world.<name>` |
| `/worlds` | List all worlds (loaded with details + unloaded from disk) | `hmy.worlds` |
| `/fly [player]` | Toggle fly mode | `hmy.fly` |
| `/gm <name\|id> [player]` | Set game mode (optionally for another player) | `hmy.gm` / `hmy.gm.other` |
| `/speed [player] <walk\|fly> <value>` | Set walk/fly speed | `hmy.speed` |
| `/weather <sonne\|regen\|sturm> [duration]` | Change weather | `hmy.weather` |
| `/time <day\|noon\|night\|midnight\|number>` | Set world time | `hmy.time` |
| `/heal [player]` | Fully heal a player | `hmy.heal` / `hmy.heal.other` |
| `/feed [player]` | Fully feed a player | `hmy.feed` / `hmy.feed.other` |
| `/tp <player> [target]` | Teleport to or as another player | `hmy.tp` / `hmy.tp.other` |
| `/tphere <player>` | Teleport a player to you | `hmy.tphere` |
| `/back` | Return to your last location | `hmy.back` |
| `/workbench` | Open a virtual crafting table | `hmy.workbench` |
| `/enderchest [player]` | Open your or another player's ender chest | `hmy.enderchest` / `hmy.enderchest.other` |
| `/repair [all]` | Repair held item or entire inventory | `hmy.repair` / `hmy.repair.all` |
| `/sethome [1-5]` | Set a home at your position (default slot 1) | `hmy.home.<slot>` |
| `/home [1-5]` | Teleport to a saved home (default slot 1) | `hmy.home.<slot>` |
| `/parkour create <name>` | Create a parkour | `hmy.parkour.admin` |
| `/parkour delete <name>` | Delete a parkour | `hmy.parkour.admin` |
| `/parkour setstart <name>` | Set the start block at your position | `hmy.parkour.admin` |
| `/parkour setgoal <name>` | Set the goal block at your position | `hmy.parkour.admin` |
| `/parkour setcheckpoint <name> <id>` | Set a checkpoint at your position | `hmy.parkour.admin` |
| `/parkour quit` | Abort the current parkour | — |
| `/parkour list` | List all parkours | — |
| `/dm <player> <message>` | Send a private message | `hmy.dm` |
| `/r <message>` | Reply to last private message | `hmy.r` |
| `/socialspy [on\|off]` | Toggle SocialSpy | `hmy.socialspy` |
| `/broadcast <message>` | Broadcast to server/proxy | `hmy.broadcast` |
| `/kick <player> [reason]` | Kick a player | `hmy.kick` |
| `/kickall [reason]` | Kick all players | `hmy.kickall` |
| `/ban <player> [reason]` | Permanently ban a player | `hmy.ban` |
| `/banip <ip> [reason]` | Permanently ban an IP | `hmy.banip` |
| `/tempban <player> <duration> [reason]` | Temporarily ban a player | `hmy.tempban` |
| `/tempbanip <player> <duration> [reason]` | Temporarily ban an IP | `hmy.tempbanip` |
| `/unban <player>` | Unban a player | `hmy.unban` |
| `/unbanip <ip>` | Unban an IP | `hmy.unbanip` |
| `/mute <player>` | Mute a player | `hmy.mute` |
| `/vanish` | Toggle vanish | `hmy.vanish` |
| `/kill <player\|entity>` | Kill a player or entity | `hmy.kill` |
| `/invsee <player>` | View a player's inventory | `hmy.invsee` |
| `/sudo <player> <command>` | Execute a command as a player | `hmy.sudo` |
| `/give <player> <item> [amount]` | Give an item | `hmy.give` |
| `/skull [player]` | Get a player's head | `hmy.skull` |
| `/seen <player>` | Show last logout time | `hmy.seen` |
| `/getpos [player]` | Show world and position | `hmy.getpos` |
| `/lightning [player]` | Strike lightning | `hmy.lightning` |

### Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `hmy.home.1` | `true` | Home slot 1 |
| `hmy.home.2` | `false` | Home slot 2 |
| `hmy.home.3` | `false` | Home slot 3 |
| `hmy.home.4` | `false` | Home slot 4 |
| `hmy.home.5` | `false` | Home slot 5 |
| `hmy.parkour` | `true` | Use `/parkour quit` and `/parkour list` |
| `hmy.parkour.admin` | `op` | Create and configure parkours |
| `hmy.worlds` | `op` | List available worlds |
| `hmy.gm.other` | `op` | Change another player's game mode |
| `hmy.heal` / `hmy.heal.other` | `op` | Heal self / others |
| `hmy.feed` / `hmy.feed.other` | `op` | Feed self / others |
| `hmy.tp` / `hmy.tp.other` | `op` | Teleport self / others |
| `hmy.tphere` | `op` | Teleport others to self |
| `hmy.back` | `op` | Return to last location |
| `hmy.workbench` | `op` | Open virtual crafting table |
| `hmy.enderchest` / `hmy.enderchest.other` | `op` | Open own / another player's ender chest |
| `hmy.repair` / `hmy.repair.all` | `op` | Repair held item / full inventory |

---

## hmyLobby

Lobby-server plugin with hotbar navigation, cosmetics, minigames, economy display, social features, and an AGB (Terms of Service) system.

### Features

- **Hotbar layout**
  - Slot 0: My-Menü (player head) – opens profile/cosmetics menu
  - Slot 4: Navigator (nether star) – server selector + lobby TP-points (`hmy.lobby.selector`)
  - Slot 7: Geschwindigkeit (feather) – cycle walk speed (`hmy.lobby.speed`)
  - Slot 8: Sprung Boost (rocket) – launch into the air (`hmy.lobby.rocket`)

- **My-Menü** – sub-menus: Partikel, Cosmetics (auras), Sprache, Köpfe, Mounts, Einstellungen
  - Slot 4: Meine Freunde (player head) – opens friends GUI
  - Slot 26: Infos (enchanted book) – help / info

- **Navigator**
  - Configurable server entries from `hmyServer.conf`
  - Slot 22: Compass "Lobby Teleports" – opens TP-point list from `lobby.yml`
  - Per-point permission check: `hmy.lobby.tp.<id>`; teleports center on the block (`.5` offset)

- **Einstellungen** (Settings)
  - Walk speed toggle (3 levels)
  - Player visibility toggle – `hmy.lobby.visibility`

- **Mounts** – player-controlled mounts (ground and flying); AI disabled via pathfinder clearing each tick; full 3D steering for flying mobs

- **Friends GUI** – 54-slot GUI; online friends shown with server label; SHIFT+CLICK → suggest `/dm`; LEFT+CLICK → request server join via `hmy:social`

- **BossBar** – shows `mc.haaremy.de | <N> Spieler online`, updates every 5 s

- **XP bar animation** – sine-wave pulsing while in lobby

- **Lobby world rules** – difficulty EASY, mob AI off, no natural spawns, lava causes damage + teleport to spawn

- **Economy display** – action bar shows `hmyCoins | hmyShards` after crate wins

- **Lottery Crate** (`LotteryCrateListener`)
  - Admin marks a chest as lottery crate with `/lobbygame create crate`
  - Players right-click to spin an animated prize reel
  - Prizes: Common · Uncommon · Rare · Epic · Legendary (hmyCoins)
  - Win triggers fireworks, title, broadcast, and `ADD_COINS` to Velocity

- **TicTacToe Minigame**
  - Admin marks a 3×3 field with golden sword, then runs `/lobbygame create tiktaktoe <feld-id>`
  - Players join by placing a sign (format below)
  - Modes: single-player (vs AI) or two-player
  - Win → fireworks → 5 s auto-reset

  **Sign format:**
  ```
  Line 1: [g: tiktaktoe]
  Line 2: <Anzeigename>
  Line 3: <feld-id>
  ```

- **AGB / Terms of Service**
  - First-time players see an inventory with the Terms of Service on join
  - Accept → grants `hmy.agb` via LuckPerms; player continues normally
  - Decline → player is kicked with a friendly message

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/hmy language <de\|en>` | Change language | — |
| `/lobbygame create tiktaktoe <feld-id>` | Create a TicTacToe field | `hmy.lobby.gamecreator` |
| `/lobbygame create crate` | Tag a chest as lottery crate | `hmy.lobby.gamecreator` |

### Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `hmy.lobby.selector` | `true` | Show Navigator in hotbar |
| `hmy.lobby.speed` | `true` | Show speed toggle in hotbar |
| `hmy.lobby.rocket` | `true` | Show Sprung Boost in hotbar |
| `hmy.lobby.visibility` | `true` | Toggle player visibility in Settings |
| `hmy.lobby.tp.*` | `true` | Access all lobby TP-points |
| `hmy.lobby.tp.<id>` | — | Access a specific TP-point |
| `hmy.lobby.gamecreator` | `op` | Create TicTacToe fields and lottery crates |
| `hmy.lobby.inventory.edit` | `false` | Allow moving hotbar items |
| `hmy.lobby.message.none` | `false` | Disable welcome message on join |
| `hmy.lobby.sound.none` | `false` | Disable join sounds |
| `hmy.lobby.particle.none` | `false` | Disable join particles |
| `hmy.lobby.bossbar.none` | `false` | Disable the BossBar |
| `hmy.agb` | `false` | Granted on AGB acceptance; required to play |

---

## KitsuneSegen (Game)

*(Documentation pending)*
