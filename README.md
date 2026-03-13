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
│           └── lobbygames.yml
│
├── Paper – Survival
│   └── plugins/
│       ├── LuckPerms-Bukkit.jar
│       ├── hmyPaper.jar
│       └── hmySettings/
│           └── hmyServer.conf
│
└── Paper – KitsuneSegen (Game)
    └── plugins/
        ├── LuckPerms-Bukkit.jar
        ├── hmyPaper.jar
        └── hmyKitsuneSegen.jar
```

---

## hmyVelocity

Proxy-layer plugin managing cross-server communication, economy, and social features.

### Features

- **Player routing** – sends players to `lobby` on join; `/lobby` command on sub-servers forwards back
- **Server status broadcasting** – pushes live player counts to lobby every 5 seconds via `hmy:status`
- **Language system** – stores language preference as a LuckPerms permission (`language.de` / `language.en`)
- **Economy** – dual currency: `hmyCoins` and `hmyShards`, stored in `data/economy.json`
- **Friends system** – add/accept/deny/remove friends; stored in `data/friends.json`
- **Follow system** – follow a friend and auto-join their server on server switches
- **Server join via friend list** – `/friend join <player>` connects you to their server
- **Plugin message channels**
  - `hmy:status` – lobby receives live player counts
  - `hmy:economy` – lobby sends coin events; proxy sends balance back
  - `hmy:social` – lobby requests friend data; proxy sends JSON friend list back
  - `hmy:trigger` – sub-servers trigger Velocity commands on behalf of a player

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/hmy language <de\|en>` | Change your language preference | — |
| `/hmy coins` | Show your hmyCoins and hmyShards balance | — |
| `/hmy coins give <player> <amount>` | Give a player hmyCoins (admin) | `hmy.coins.give` |
| `/friend add <player>` | Send a friend request (auto-accepts if mutual) | — |
| `/friend accept <player>` | Accept a pending friend request | — |
| `/friend deny <player>` | Deny a pending friend request | — |
| `/friend remove <player>` | Remove a friend | — |
| `/friend list` | List all friends (grouped online/offline by server) | — |
| `/friend join <player>` | Connect to the server a friend is on | — |
| `/friend follow <player>` | Follow a friend (auto-joins on server switch) | — |
| `/friend unfollow` | Stop following | — |
| `/broadcast <message>` | Broadcast a message to all servers | `hmy.broadcast` |

---

## hmyPaper

General-purpose Paper plugin used on all sub-servers.

### Features

- **AntiBuild** – world names in config activate build protection; per-world allowed-block rules
- **Chat prefixes** – pulled from LuckPerms groups
- **Custom tab list** – shows player list with LuckPerms prefix/suffix
- **Spawn system** – `/spawn` teleports to world spawn (disabled when world name starts with `survival`)
- **World switcher** – `/world <name>` moves players between worlds
- **Moderation** – ban, tempban, IP-ban, kick, mute, vanish, invsee, sudo
- **Utilities** – fly, gamemode, speed, weather, time, lightning, skull, getpos, socialspy

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/help [page]` | List available commands | `hmy.help` |
| `/rules [page]` | Show server rules | `hmy.rules` |
| `/spawn` | Teleport to world spawn | — |
| `/lobby` | Return to lobby server | — |
| `/fly [player]` | Toggle fly mode | `hmy.fly` |
| `/gm <name\|id> [player]` | Set game mode | `hmy.gm` |
| `/speed [player] <walk\|fly> <value>` | Set walk/fly speed | `hmy.speed` |
| `/weather <sonne\|regen\|sturm> [duration]` | Change weather | `hmy.weather` |
| `/time [value]` | Show or set world time | `hmy.time` |
| `/world <name>` | Switch to another world | `hmy.world` |
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
| `/triggervelocity` | Trigger a Velocity command | `hmy.triggervelocity` |

---

## hmyLobby

Lobby-server plugin with hotbar navigation, cosmetics, minigames, economy display, and social features.

### Features

- **Hotbar layout**
  - Slot 0: My-Menü (player head) – opens cosmetics/profile menu
  - Slot 1: Infos (enchanted book) – always visible
  - Slot 2: Freunde (paper) – opens friends GUI (fetched from Velocity)
  - Slot 4: Navigator (nether star) – server selector + TP-points (`hmy.lobby.selector`)
  - Slot 7: Geschwindigkeit (feather) – cycle walk speed (`hmy.lobby.speed`)
  - Slot 8: Sprung Boost (rocket) – launch into the air (`hmy.lobby.rocket`)

- **My-Menü** – sub-menus: Partikel, Cosmetics (auras), Sprache, Köpfe, Mounts, Einstellungen

- **Navigator** – configurable server entries from `hmyServer.conf`; center slot is a compass for lobby TP-points defined in `lobby.yml` under `teleport-points`

- **Einstellungen** (Settings)
  - Walk speed toggle (3 levels)
  - Player visibility toggle (hide/show all players) – `hmy.lobby.visibility`

- **Cosmetics** (auras/effects via CosmeticMenuListener)

- **BossBar** – pink bar showing `mc.haaremy.de | <N> Spieler online`, updates every 5 s

- **XP bar animation** – sine-wave pulsing XP bar while in lobby

- **Lobby world rules** – difficulty EASY, mob AI disabled, no natural mob spawns, lava causes custom damage + teleport to spawn

- **Friends GUI** – right-click the Freunde item → requests friend data from Velocity → 54-slot GUI showing online friends (green, with server) and offline friends (gray); click an online friend to join their server

- **Economy display** – action bar shows `hmyCoins | hmyShards` balance after crate prizes (received from Velocity via `hmy:economy`)

- **Lottery Crate** (`LotteryCrateListener`)
  - Admin marks a chest as lottery crate with `/lobbygame create crate`
  - Players right-click the chest to spin a prize reel animation
  - Prizes: Common · Uncommon · Rare · Epic · Legendary (hmyCoins reward)
  - Win triggers fireworks, title, server broadcast, and sends `ADD_COINS` to Velocity

- **TicTacToe Minigame**
  - Admin marks a 3×3 field with golden sword (left-click = corner 1, right-click = corner 2)
  - Creates the game with `/lobbygame create tiktaktoe <feld-id>`
  - Players place a sign with the format below to join
  - Single-player (vs simple AI: win → block → center → random) or two-player mode
  - Win → fireworks → 5 s auto-reset

  **Sign format** (write on sign, plugin reformats automatically):
  ```
  Line 1: [g: tiktaktoe]
  Line 2: <Anzeigename>
  Line 3: <feld-id>
  ```

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/hmy language <de\|en>` | Change language | — |
| `/lobbygame create tiktaktoe <feld-id>` | Create a TicTacToe field from golden-sword selection | `hmy.lobby.gamecreator` |
| `/lobbygame create crate` | Tag the targeted chest as a lottery crate | `hmy.lobby.gamecreator` |

### Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `hmy.lobby.selector` | `true` | Show Navigator in hotbar |
| `hmy.lobby.speed` | `true` | Show speed toggle in hotbar |
| `hmy.lobby.rocket` | `true` | Show Sprung Boost in hotbar |
| `hmy.lobby.visibility` | `true` | Toggle player visibility in Settings |
| `hmy.lobby.gamecreator` | `op` | Create TicTacToe fields and lottery crates; use golden-sword selector |
| `hmy.lobby.inventory.edit` | `false` | Allow moving hotbar items |
| `hmy.lobby.message.none` | `false` | Disable welcome message on join |
| `hmy.lobby.sound.none` | `false` | Disable join sounds |
| `hmy.lobby.particle.none` | `false` | Disable join particles |
| `hmy.lobby.bossbar.none` | `false` | Disable the BossBar |

---

## KitsuneSegen (Game)

*(Documentation pending)*
