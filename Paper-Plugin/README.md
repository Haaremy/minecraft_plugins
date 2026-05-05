# hmyPaper

hmyPaper is the shared Paper utility plugin for Haaremy backend servers. It provides essentials, moderation, homes, world switching, spawn handling, anti-build rules, chat integration, and a legacy parkour system.

## Requirements

| Requirement | Notes |
|-------------|-------|
| Paper | API version 1.19+ |
| LuckPerms | Required by `plugin.yml` |
| Velocity/Bungee plugin messaging | Needed for `/lobby` and `triggervelocity` forwarding |

## Installation

1. Build with `mvn -q package`.
2. Copy the generated JAR to every Paper backend that should receive shared commands.
3. Start once so shared files are copied or loaded.
4. Configure `hmySettings/general.yml` and `hmySettings/helpBook.yml`.
5. Restart the Paper server.

## Main Commands

| Area | Commands |
|------|----------|
| Help | `/help`, `/rules` |
| Travel | `/spawn`, `/lobby`, `/world`, `/worlds`, `/back`, `/tp`, `/tphere` |
| Homes | `/sethome [1-5]`, `/home [1-5]` |
| Essentials | `/fly`, `/gm`, `/speed`, `/weather`, `/time`, `/heal`, `/feed`, `/workbench`, `/enderchest`, `/repair`, `/skull`, `/getpos`, `/lightning` |
| Moderation | `/kick`, `/kickall`, `/ban`, `/banip`, `/tempban`, `/tempbanip`, `/unban`, `/unbanip`, `/mute`, `/vanish`, `/kill`, `/invsee`, `/sudo`, `/give`, `/seen` |
| Chat | `/dm`, `/r`, `/socialspy`, `/broadcast` |
| Parkour | `/parkour create`, `/parkour delete`, `/parkour setstart`, `/parkour setgoal`, `/parkour setcheckpoint`, `/parkour quit`, `/parkour list` |

## Configuration

`hmySettings/general.yml`:

```yaml
language: de

anti-build:
  worlds:
    - lobby
    - world2
  world-settings:
    lobby:
      disabled-damage-types:
        - FALL
        - FIRE
        - LAVA
      allowed-place:
        - STONE
        - GRASS_BLOCK
        - OAK_LOG
      allowed-break:
        - DIRT
        - SAND
        - STONE
```

`hmySettings/helpBook.yml` controls the in-game help book title, author, and pages.

## Runtime Data

| File | Purpose |
|------|---------|
| `hmySettings/homes.yml` | Player homes |
| `hmySettings/parkour.yml` | Legacy parkour course points |
| `hmySettings/general.yml` | Language and anti-build rules |
| `hmySettings/helpBook.yml` | Help book content |
