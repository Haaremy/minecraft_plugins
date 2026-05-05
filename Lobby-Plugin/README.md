# hmyLobby

hmyLobby is the main lobby Paper plugin. It controls hotbar items, navigation, cosmetics, friends GUI, lobby games, balloons/elevators, managed jukeboxes, AGB acceptance, server status display, and lobby-specific world rules.

## Requirements

| Requirement | Notes |
|-------------|-------|
| Paper | API version 1.19+ |
| LuckPerms | Required |
| hmyVelocity | Required for status, economy, social and routing plugin messages |
| OpenAudioMc | Optional, required for stream jukebox playback |

## Installation

1. Build with `mvn -q package`.
2. Copy the generated JAR to the lobby server's `plugins/` directory.
3. Ensure LuckPerms and hmyVelocity are active.
4. Start once so config/data files are created.
5. Configure server mappings and lobby points under `hmySettings/`.
6. Restart the lobby server.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/hmy language <de\|en>` | Change language | none |
| `/hmy ballon route ...` | Create and manage hot-air-balloon routes | `hmy.lobby.balloon.admin` |
| `/hmy ballon elevator ...` | Create and manage elevators | `hmy.lobby.balloon.admin` |
| `/server <name>` | Send player to a configured backend | `hmy.server.<name>` |
| `/lobbygame create tiktaktoe <feld-id>` | Create a TicTacToe field | `hmy.lobby.gamecreator` |
| `/lobbygame create crate` | Mark targeted chest as lottery crate | `hmy.lobby.gamecreator` |
| `/lobbygame list` | List lobby games | `hmy.lobby.gamecreator` |
| `/lobbygame delete tiktaktoe <feld-id>` | Delete a TicTacToe field | `hmy.lobby.gamecreator` |
| `/jukebox create <id>` | Register a managed jukebox | `hmy.lobby.jukebox.admin` |
| `/jukebox <id> play endless` | Loop current disc or stream | `hmy.lobby.jukebox.admin` |
| `/jukebox <id> add diskbox` | Link a chest playlist | `hmy.lobby.jukebox.admin` |
| `/jukebox <id> set stream <url>` | Play a stream via OpenAudioMc | `hmy.lobby.jukebox.admin` |
| `/jukebox sync <id1,id2,...>` | Restart multiple jukeboxes in sync | `hmy.lobby.jukebox.admin` |
| `/jukebox list` | List managed jukeboxes | `hmy.lobby.jukebox.admin` |

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `hmy.lobby.selector` | true | Show/use the navigator hotbar item |
| `hmy.lobby.speed` | true | Use speed toggle |
| `hmy.lobby.rocket` | true | Use launch rocket |
| `hmy.lobby.visibility` | true | Toggle player visibility |
| `hmy.lobby.tp.*` | true | Access all lobby teleport points |
| `hmy.lobby.tp.<id>` | custom | Access one lobby teleport point |
| `hmy.lobby.friends` | true | Show friends hotbar item |
| `hmy.lobby.gamecreator` | op | Manage TicTacToe and crates |
| `hmy.lobby.balloon.use` | true | Board balloons and elevators |
| `hmy.lobby.balloon.admin` | op | Manage balloon/elevator routes |
| `hmy.lobby.jukebox.admin` | op | Manage jukeboxes |
| `hmy.lobby.inventory.edit` | false | Move protected hotbar items |
| `hmy.lobby.message.none` | false | Disable welcome message |
| `hmy.lobby.sound.none` | false | Disable join sounds |
| `hmy.lobby.particle.none` | false | Disable join particles |
| `hmy.lobby.stats.on` | false | Enable stats sidebar toggle |
| `hmy.agb` | false | Granted after accepting AGB |

## Runtime Files

| File | Purpose |
|------|---------|
| `hmySettings/lobby.yml` | Lobby teleport points and lobby-specific settings |
| `hmySettings/lobbygames.yml` | TicTacToe fields and lottery crates |
| `hmySettings/balloons.yml` | Balloon routes and elevator floors |
| `hmySettings/jukeboxes.yml` | Managed jukebox definitions |
| `hmySettings/general.yml` | Shared language and anti-build settings |

## Balloon Setup

```bash
/hmy ballon route erstellen city-tour
/hmy ballon route waypoint city-tour
/hmy ballon route boarding city-tour 0
/hmy ballon route dropoff city-tour 0
/hmy ballon route start city-tour 3
```

## Jukebox Setup

```bash
/jukebox create lobby1
# right-click the jukebox with the golden sword
/jukebox lobby1 play endless
```

For streams:

```bash
/jukebox create radio1
# right-click the jukebox
/jukebox radio1 set stream https://example.com/stream.mp3
/jukebox radio1 play endless
```
