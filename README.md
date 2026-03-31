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

- **My-Menü** – sub-menus: Partikel, Cosmetics (auras), Köpfe, Mounts, Einstellungen, Infos
  - Slot 4: Meine Freunde (player head) – opens friends GUI
  - Slot 13: Infos (enchanted book) – help / info (previously slot 26)

- **Navigator**
  - Configurable server entries from `hmyServer.conf`
  - Slot 22: Compass "Lobby Teleports" – opens TP-point list from `lobby.yml`
  - Per-point permission check: `hmy.lobby.tp.<id>`; teleports center on the block (`.5` offset)

- **Einstellungen** (Settings)
  - Walk speed toggle (3 levels)
  - Player visibility toggle – `hmy.lobby.visibility`
  - Language selection (Deutsch / English) – moved from My-Menü to Einstellungen; language menu back button returns to Einstellungen

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

- **Heißluftballon & Fahrstuhl** (`BalloonManager`)
  - Admin legt benannte Routen mit beliebig vielen Waypoints an
  - Bis zu 20 Ballons fliegen gleichzeitig und versetzt eine Route im Kreis
  - Boarding-Zonen (grüne Partikel, Radius 5 Blöcke) nehmen Spieler automatisch auf
  - Dropoff-Zonen (blaue Partikel) setzen Spieler beim nächsten Halt ab
  - Fahrstuhl-System: Kabine fährt Ping-Pong zwischen konfigurierten Etagen
  - Alle Routen und Etagen werden persistent in `hmySettings/balloons.yml` gespeichert
  - Visuelle Strukturen: Ballon-Hülle aus Rüstungsständern (Woll-Lagen + Seile + Korb), Fahrstuhl-Kabine (Eisengitter, Türen, Kettenschacht)

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/hmy language <de\|en>` | Change language | — |
| `/lobbygame create tiktaktoe <feld-id>` | Create a TicTacToe field | `hmy.lobby.gamecreator` |
| `/lobbygame create crate` | Tag a chest as lottery crate | `hmy.lobby.gamecreator` |
| `/hmy ballon route erstellen <name>` | Create a new balloon route | `hmy.lobby.balloon.admin` |
| `/hmy ballon route waypoint <name>` | Add a waypoint at your position | `hmy.lobby.balloon.admin` |
| `/hmy ballon route boarding <name> <nr>` | Mark a waypoint as boarding zone | `hmy.lobby.balloon.admin` |
| `/hmy ballon route dropoff <name> <nr>` | Mark a waypoint as drop-off zone | `hmy.lobby.balloon.admin` |
| `/hmy ballon route list <name>` | Show all waypoints of a route | `hmy.lobby.balloon.admin` |
| `/hmy ballon route start <name> <anzahl>` | Start auto-travel with 1–20 balloons | `hmy.lobby.balloon.admin` |
| `/hmy ballon route stop <name>` | Stop auto-travel and remove balloons | `hmy.lobby.balloon.admin` |
| `/hmy ballon route info` | List all routes with status | `hmy.lobby.balloon.admin` |
| `/hmy ballon elevator create <name>` | Create an elevator | `hmy.lobby.balloon.admin` |
| `/hmy ballon elevator floor <name>` | Add a floor at your position | `hmy.lobby.balloon.admin` |
| `/hmy ballon elevator boarding <name> <nr>` | Mark a floor as boarding zone | `hmy.lobby.balloon.admin` |
| `/hmy ballon elevator dropoff <name> <nr>` | Mark a floor as drop-off zone | `hmy.lobby.balloon.admin` |
| `/hmy ballon elevator list <name>` | Show all floors of an elevator | `hmy.lobby.balloon.admin` |
| `/hmy ballon elevator start <name>` | Start the elevator | `hmy.lobby.balloon.admin` |
| `/hmy ballon elevator stop <name>` | Stop the elevator | `hmy.lobby.balloon.admin` |
| `/hmy ballon elevator info` | List all elevators with status | `hmy.lobby.balloon.admin` |

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
| `hmy.lobby.balloon.use` | `true` | Board balloons and elevators |
| `hmy.lobby.balloon.admin` | `op` | Create and manage balloon routes and elevators |
| `hmy.lobby.inventory.edit` | `false` | Allow moving hotbar items |
| `hmy.lobby.message.none` | `false` | Disable welcome message on join |
| `hmy.lobby.sound.none` | `false` | Disable join sounds |
| `hmy.lobby.particle.none` | `false` | Disable join particles |
| `hmy.lobby.bossbar.none` | `false` | Disable the BossBar |
| `hmy.agb` | `false` | Granted on AGB acceptance; required to play |

### Balloon & Elevator Setup (Kurzanleitung)

**Route mit 3 Stationen:**
```
# 1. Route erstellen
/hmy ballon route erstellen city-tour

# 2. An jeder Station teleportieren und Waypoint setzen
/hmy ballon route waypoint city-tour   # Waypoint #0 (Spawn)
/hmy ballon route waypoint city-tour   # Waypoint #1 (Shop)
/hmy ballon route waypoint city-tour   # Waypoint #2 (Arena)

# 3. Zonen zuweisen
/hmy ballon route boarding city-tour 0   # Einsteigen am Spawn
/hmy ballon route dropoff city-tour 1    # Aussteigen am Shop
/hmy ballon route boarding city-tour 1   # Auch Einsteigen am Shop
/hmy ballon route dropoff city-tour 2    # Aussteigen an der Arena

# 4. Starten
/hmy ballon route start city-tour 3   # 3 Ballons gleichzeitig
```

**Fahrstuhl mit 3 Etagen:**
```
/hmy ballon elevator create lift-a
# Zu jeder Etage teleportieren:
/hmy ballon elevator floor lift-a   # Etage #0 (EG)
/hmy ballon elevator floor lift-a   # Etage #1 (1. OG)
/hmy ballon elevator floor lift-a   # Etage #2 (2. OG)
# Alle Etagen als Boarding und Dropoff:
/hmy ballon elevator boarding lift-a 0
/hmy ballon elevator dropoff  lift-a 0
/hmy ballon elevator boarding lift-a 1
/hmy ballon elevator dropoff  lift-a 1
/hmy ballon elevator boarding lift-a 2
/hmy ballon elevator dropoff  lift-a 2
/hmy ballon elevator start lift-a
```

**Zonen-Übersicht:**

| Zone | Partikel | Radius | Verhalten |
|------|----------|--------|-----------|
| Boarding | Grün (`HAPPY_VILLAGER`) | 5 Blöcke | Spieler werden beim nächsten Ballon automatisch aufgenommen |
| Dropoff | Blau (`ENTITY_EFFECT`) | 5 Blöcke | Spieler werden automatisch abgesetzt, wenn der Ballon hält |
| Fahrstuhl | Grün | 3 Blöcke | Ein- und Aussteigen an jeder Etage |

**Technische Details:**

| Parameter | Wert |
|-----------|------|
| Ballon-Geschwindigkeit | 0.3 Blöcke / Tick |
| Fahrstuhl-Geschwindigkeit | 0.15 Blöcke / Tick |
| Pause pro Waypoint | 40 Ticks (2 s) |
| Pause pro Etage | 60 Ticks (3 s) |
| Update-Frequenz Ballons | alle 2 Ticks |
| Zone-Check | jeden Tick |
| Max. Boarding-Distanz zum Ballon | 30 Blöcke |
| Persistenz | `hmySettings/balloons.yml` |

---

### Jukebox System

Managed jukebox system with three playback modes, golden-sword block selection, and multi-jukebox synchronisation.

#### Features

- **Endless** – loops the disc currently placed in the jukebox forever
- **Diskbox** – links a chest; plays all music discs in the chest in slot order, then repeats
- **Stream** – plays a URL once via OpenAudioMc; live streams detected automatically via `icy-metaint`; non-live streams can be set to loop endlessly
- **Sync** – stops multiple named jukeboxes and restarts them all in the same server tick
- Managed jukeboxes are protected: non-admins cannot remove discs or break the jukebox block
- Config persisted in `hmySettings/jukeboxes.yml`; survives server restarts (mode restarts manually after reboot)

#### Commands

| Command | Description |
|---------|-------------|
| `/jukebox create <id>` | Register a new jukebox (then right-click it with a golden sword) |
| `/jukebox <id> play endless` | Loop the disc in the jukebox indefinitely; also enables repeat for stream mode |
| `/jukebox <id> stop` | Stop all playback |
| `/jukebox <id> add diskbox` | Link a chest as disc playlist (right-click it with a golden sword) |
| `/jukebox <id> set stream <url>` | Play a URL via OpenAudioMc; use `play endless` afterwards to loop it |
| `/jukebox sync <id1,id2,...>` | Restart all listed jukeboxes simultaneously in the same server tick |
| `/jukebox list` | List all registered jukeboxes with their current state |

#### Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `hmy.lobby.jukebox.admin` | `op` | Create and manage all jukebox commands |

#### Setup (Schnellstart)

**Jukebox mit Endlosschleife:**
```
1. Platziere eine Jukebox und lege eine Disk hinein.
2. /jukebox create lobby1     → Rechtsklick die Jukebox mit dem goldenen Schwert
3. /jukebox lobby1 play endless
```

**Diskbox (Playlist aus Truhe):**
```
1. Platziere eine Truhe mit Musik-Disks.
2. /jukebox create lobby2     → Rechtsklick die Jukebox
3. /jukebox lobby2 add diskbox → Rechtsklick die Truhe
   → Diskbox startet automatisch
```

**Stream (OpenAudioMc erforderlich):**
```
1. /jukebox create radio1     → Rechtsklick die Jukebox
2. /jukebox radio1 set stream https://example.com/stream.mp3
3. /jukebox radio1 play endless   (optional, bei nicht-Live-Streams)
```

**Sync (mehrere Jukeboxen gleichzeitig starten):**
```
/jukebox sync lobby1,lobby2,radio1
```

#### Technical Details

| Parameter | Value |
|-----------|-------|
| Disc durations | 16 hardcoded values (13 s–345 s) |
| Stream live detection | HTTP HEAD + `icy-metaint` header |
| Stream repeat interval | 6000 ticks (~5 min) for non-live endless |
| Loop restart offset | 10 ticks before song end |
| OpenAudioMc | Optional soft-dependency; resolved via reflection at runtime |
| Persistence | `hmySettings/jukeboxes.yml` |

---

## hmyWallpaper

Standalone Paper plugin that renders 128×128 Minecraft maps as wallpapers. Three render modes: solid color, block map-color tile, and SVG-pattern. The finished map item is placed directly in the player's main hand.

### Features

- **Full** – fills the entire map with any hex color
- **Block** – reads the block's native Minecraft map-color via NMS and renders a subtle 8×8 tile shading pattern across the map
- **SVG** – loads an SVG file from `plugins/hmyWallpaper/svgs/`, replaces the first 3 distinct `fill` colors with the player-specified colors, and renders all basic shapes (`<rect>`, `<circle>`, `<ellipse>`, `<polygon>`, `<g>`) to the map
- Two sample SVGs bundled and extracted on first start: `bubble.svg` (circles on background, 3 colors) and `checkers.svg` (checkerboard, 2 colors)
- Admins can add custom SVGs to `plugins/hmyWallpaper/svgs/` at any time

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/wallpaper create full <#RRGGBB>` | Solid-color map | `hmy.wallpaper.use` |
| `/wallpaper create block <Material>` | Block map-color tile | `hmy.wallpaper.use` |
| `/wallpaper create svg <Name> <#c1> <#c2> <#c3>` | SVG pattern with 3 colors | `hmy.wallpaper.use` |

### Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `hmy.wallpaper.use` | `true` | May use `/wallpaper create` |
| `hmy.wallpaper.admin` | `op` | Reserved for future admin commands |

### SVG Color Convention

SVGs use placeholder fill colors. When rendering, the plugin collects all distinct `fill` values in order of first appearance and maps them to the three user-specified colors. Example:

```xml
<!-- checkers.svg has 2 distinct fills: #111111 and #eeeeee -->
/wallpaper create svg checkers #1a1a2e #e94560 #ffffff
<!-- → #111111 becomes #1a1a2e, #eeeeee becomes #e94560 -->
```

Custom SVGs only need valid `fill="#rrggbb"` attributes on `<rect>`, `<circle>`, `<ellipse>`, and `<polygon>` elements. The `viewBox` attribute controls the coordinate space and is automatically scaled to 128×128.

---

## KitsuneSegen (Game)

Fortnite-style battle-royale game on its own Paper server. Players join a safe hub world and are dropped into a game world to fight until one remains.

### Game Flow

1. Players join → teleported to `hub` world (Adventure mode, no damage)
2. Countdown starts automatically when `min-players` is reached (configurable)
3. Additional players can join until `max-players` or the game starts
4. On game start the server is locked; latecomers are sent to the lobby
5. Players spawn via the configured spawn mode, then fight
6. Last player alive wins; results are broadcast and everyone returns to hub
7. The game world is reset from a backup after each round

### Spawn Modes

| Mode | Behaviour |
|------|-----------|
| `random` | Players are distributed across Obsidian marker blocks scanned at startup |
| `flight` | All players teleport to world spawn at `elytra-height` Y with an Elytra equipped; Elytra is removed on first ground contact |

### In-Game Rules

- **Slot 0 (Axt)** – given at spawn, cannot be moved or dropped
- **Block protection** – only blocks listed in `breakable-blocks` (config whitelist) can be broken or placed
- **No hunger** – food level stays full; no hunger damage; no natural health regen
- **Health** – restored only by specific items (healing potions)
- **Mob spawning** – disabled in the game world
- **No public chat** – hub chat is suppressed; DM hint shown instead

### Chests

- Marker blocks (`chest-spawn-block`) are scanned at startup; 60 % spawn as chests on game start
- 20 % chance of an Ender Chest (special loot) vs normal Chest
- Right-clicking a chest shows a bossbar loading animation; on completion items drop naturally and the block is removed

### Weapons & Items

Four weapon categories, each as a Crossbow with hidden enchantments driving its behaviour:

| Category | Behaviour |
|----------|-----------|
| Multishot | Short range, many arrows |
| Speedshot | Single shots, fast reload, lower damage |
| Distanceshot | Single shots, medium range, higher damage |
| Precisionshot | Single shot, very slow reload, long range, very high damage |

Each weapon has five rarities (Common → Legendary) scaling damage / speed. Arrows are category-specific and must match the weapon type.

Special items: splash healing potions, splash damage potions, slimeballs (jump booster).

### Death & Spectator

- Death message suppressed; placement shown as title (`Platz X / Y`)
- Inventory cleared; two items given:
  - Slot 0: **Red Dye** "Verlassen" – sends player to lobby
  - Slot 8: **Book** "Report" – shows `/report` instruction
- Player enters Spectator mode and is hidden from alive players
- Spectators can see each other; dead spectators remain until the round ends

### Scoreboard (Sidebar)

| Line | Content |
|------|---------|
| Title | `Kitsune Segen` |
| Am Leben | Remaining alive players |
| Kills | This player's kill count |
| Minimap | 7×5 text minimap (8-block cells); `@` = self, `●` = enemy, `▪` = solid, `·` = open; updates every second |

### World Reset

After each game, the game world folder is deleted and replaced with the backup from `world-backup-path`, then reloaded. If no backup exists the world is simply reloaded without a reset.

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/game start` | Force-start the game immediately | `hmy.kitsunesegen.admin` |
| `/game countdown` | Start the countdown manually | `hmy.kitsunesegen.admin` |
| `/game stop` | Force-end the game and return everyone to hub | `hmy.kitsunesegen.admin` |
| `/game info` | Show game state and alive player list | `hmy.kitsunesegen.admin` |
| `/game kick <player>` | Eliminate a player from the current game | `hmy.kitsunesegen.admin` |
| `/game reset` | Reset state without world restart | `hmy.kitsunesegen.admin` |

### Config (`config.yml`)

```yaml
hub-world:          "hub"
game-world:         "game"
lobby-server:       "lobby"
world-backup-path:  "world_backups/game"
min-players:        2
max-players:        20
max-health:         40.0
countdown-seconds:  60
spawn-mode:         "random"   # or "flight"
elytra-height:      100
spawn-block:        "OBSIDIAN"
chest-spawn-block:  "OAK_PLANKS"
breakable-blocks:
  - "OAK_PLANKS"
  - "COBBLESTONE"
  # …
```

### Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `hmy.kitsunesegen.admin` | `op` | Full access to all `/game` sub-commands |
| `hmy.kitsunesegen.play` | `true` | Allowed to participate in the game |
