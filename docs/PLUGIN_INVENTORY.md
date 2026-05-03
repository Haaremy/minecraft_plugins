# Plugin Inventory

Inventory taken from the live Minecraft tree under `/root/minecraft` on 2026-05-03.

## Summary

| Category | Plugins |
| --- | --- |
| Custom with source in repo | `hmyVelocity`, `hmyPaper`, `hmyLobby`, `hmyWallpaper`, `hmyKitsuneSegen`, `HmyAI` |
| Custom deployed-only JARs archived here | `hmyCore`, `hmyDailyRewards`, `hmyNavigator`, `hmy1v1`, `hmyParkour`, `hmySpleef`, `hmySumo`, `hmyTNTRun` |
| Third-party runtime plugins | LuckPerms, Plan, WorldEdit, OpenAudioMc, ViaVersion, ViaBackwards, ViaRewind, Chunky |

## Server Plugin Matrix

| Server | Plugin | Type | Version | Repo status | Notes |
| --- | --- | --- | --- | --- | --- |
| Velocity | `hmyVelocity` | Custom | 1.2 | Source present | Proxy commands, routing, friends, economy, network tab list. |
| Velocity | `LuckPerms` | Third-party | 5.5.36 | Documented only | Permissions provider. |
| Velocity | `OpenAudioMc` | Third-party | 6.10.16 | Documented only | Proxy-side audio/voice integration. |
| Velocity | `ViaVersion` | Third-party | 5.7.2 | Documented only | Protocol compatibility for newer clients. |
| Velocity | `ViaBackwards` | Third-party | 5.7.2 | Documented only | Protocol compatibility for older clients. |
| Velocity | `ViaRewind` | Third-party | 4.0.15 | Documented only | Legacy client support. |
| Lobby | `hmyLobby` | Custom | 1.0 | Source present | Lobby navigation, hotbar, cosmetics, games, jukebox, social UI. |
| Lobby | `hmyPaper` | Custom | 1.0 | Source present | Shared Paper utilities and moderation commands. |
| Lobby | `hmyWallpaper` | Custom | 1 | Source present | Map wallpaper creation from block colors, full color, and SVG. |
| Lobby | `HmyAI` | Custom | 1 | Source present | `/ai` command and Minecraft AI bridge. |
| Lobby | `hmyCore` | Custom | 1.0 | Archived deployed JAR | Shared game-mode infrastructure, coins, stats, arena, lobby command. |
| Lobby | `hmyDailyRewards` | Custom | 1.0 | Archived deployed JAR | Daily rewards and login streaks; depends on `hmyCore`. |
| Lobby | `hmyNavigator` | Custom | 1.0 | Archived deployed JAR | Game-mode navigator; `/navigator` and `/play`. |
| Lobby | `LuckPerms` | Third-party | 5.5.36 | Documented only | Permissions provider. |
| Lobby | `Plan` | Third-party | 5.7 build 3306 | Documented only | Player analytics. |
| Lobby | `WorldEdit` | Third-party | 7.4.0+7381-3decaf0 | Documented only | World editing/admin tooling. |
| Lobby | `OpenAudioMc` | Third-party | 6.10.16 | Documented only | Proximity voice/audio. |
| Kitsune | `hmyKitsuneSegen` | Custom | 2.0 | Source present | Mythology themed combat game. |
| Kitsune | `hmyPaper` | Custom | 1.0 | Source present | Shared Paper utilities. |
| Kitsune | `hmyCore` | Custom | 1.0 | Archived deployed JAR | Shared game-mode infrastructure. |
| Kitsune | `hmyDailyRewards` | Custom | 1.0 | Archived deployed JAR | Daily rewards and streaks. |
| Kitsune | `hmy1v1` | Custom | 1.0 | Archived deployed JAR | 1v1 duel system with kit selection. |
| Kitsune | `hmyParkour` | Custom | 1.0 | Archived deployed JAR | Parkour system with checkpoints and best times. |
| Kitsune | `hmySpleef` | Custom | 1.0 | Archived deployed JAR | Spleef game mode. |
| Kitsune | `hmySumo` | Custom | 1.0 | Archived deployed JAR | Sumo 1v1 game mode. |
| Kitsune | `hmyTNTRun` | Custom | 1.0 | Archived deployed JAR | TNTRun game mode. |
| Kitsune | `LuckPerms` | Third-party | 5.5.36 | Documented only | Permissions provider. |
| Kitsune | `Plan` | Third-party | 5.7 build 3306 | Documented only | Player analytics. |
| Kitsune | `WorldEdit` | Third-party | 7.4.0+7381-3decaf0 | Documented only | World editing/admin tooling. |
| Vanilla | `hmyPaper` | Custom | 1.0 | Source present | Shared Paper utilities. |
| Vanilla | `LuckPerms` | Third-party | 5.5.36 | Documented only | Permissions provider. |
| Vanilla | `Plan` | Third-party | 5.7 build 3306 | Documented only | Player analytics. |
| Vanilla | `Chunky` | Third-party | 1.4.40 | Documented only | Chunk pre-generation. |

## Deployed-Only Custom JARs

The following custom plugins were present on live servers without matching source directories in this repository. Their deployed JARs were copied into `artifacts/deployed-custom/` so the GitHub repository has a traceable copy until source is recovered or recreated:

| JAR | Main class | Commands |
| --- | --- | --- |
| `hmyCore.jar` | `de.haaremy.hmycore.HmyCore` | `/coins`, `/stats`, `/arena`, `/lobby` |
| `hmyDailyRewards.jar` | `de.haaremy.hmydailyrewards.HmyDailyRewards` | `/daily` |
| `hmyNavigator.jar` | `de.haaremy.hmynavigator.HmyNavigator` | `/navigator`, `/play` |
| `hmy1v1.jar` | `de.haaremy.hmy1v1.Hmy1v1` | `/duel` |
| `hmyParkour.jar` | `de.haaremy.hmyparkour.HmyParkour` | `/pk`, alias `/parcour` |
| `hmySpleef.jar` | `de.haaremy.hmyspleef.HmySpleef` | `/spleef` |
| `hmySumo.jar` | `de.haaremy.hmysumo.HmySumo` | `/sumo` |
| `hmyTNTRun.jar` | `de.haaremy.hmytntrun.HmyTNTRun` | `/tntrun` |

## Notes

- `Plan/libraries/*.jar` files under the live plugin directories are runtime libraries extracted by Plan, not separate installed server plugins.
- `.paper-remapped/` JARs are generated Paper remap artifacts and are intentionally excluded from the inventory.
- `vanilla_bak` is a backup server tree and is not treated as active inventory.
