# hmyCubed Minecraft Plugins

This repository contains the custom plugins and operational documentation for the Haaremy Minecraft network (`mc.haaremy.de`).

## Repository Map

| Path | Purpose |
| --- | --- |
| `Velocity-Plugin/` | Source for the Velocity proxy plugin (`hmyVelocity`). |
| `Paper-Plugin/` | Source for the shared Paper utility plugin (`hmyPaper`). |
| `Lobby-Plugin/` | Source for the lobby plugin (`hmyLobby`). |
| `Wallpapers/` | Source for the map wallpaper plugin (`hmyWallpaper`). |
| `KitsuneSegen/` | Source for the Kitsune Segen game plugin (`hmyKitsuneSegen`). |
| `HmyAI/` | Source for the Minecraft AI bridge plugin (`HmyAI`). |
| `artifacts/deployed-custom/` | Custom plugins found on the live servers where source is not yet present in this repo. |
| `docs/PLUGIN_INVENTORY.md` | Full plugin inventory by server and ownership. |
| `docs/NETWORK.md` | Velocity backend topology, VM mapping, and the VM111-to-VM107 firewall fix. |

## Live Network

| Layer | VM | IP | Role |
| --- | --- | --- | --- |
| Main Minecraft | VM111 `hmy-minecraft` | `10.0.3.16` | Public Velocity proxy, lobby, survival, kitsune backend. |
| AI Minecraft | VM107 `hmy-minecraft-ai` | `10.0.3.62` | AI-side Minecraft stack and Paper backend ports. |

Velocity on VM111 routes `ai` to `10.0.3.62:30066`. The Proxmox firewall for VM107 must allow TCP/UDP `30066:30068` from `10.0.3.16`.

## Custom Plugins

| Plugin | Platform | Status |
| --- | --- | --- |
| `hmyVelocity` | Velocity | Source present. |
| `hmyPaper` | Paper | Source present. |
| `hmyLobby` | Paper | Source present. |
| `hmyWallpaper` | Paper | Source present. |
| `hmyKitsuneSegen` | Paper | Source present. |
| `HmyAI` | Paper | Source present. |
| `hmyCore` | Paper | Deployed JAR archived in `artifacts/deployed-custom/`; source missing. |
| `hmyDailyRewards` | Paper | Deployed JAR archived in `artifacts/deployed-custom/`; source missing. |
| `hmyNavigator` | Paper | Deployed JAR archived in `artifacts/deployed-custom/`; source missing. |
| `hmy1v1` | Paper | Deployed JAR archived in `artifacts/deployed-custom/`; source missing. |
| `hmyParkour` | Paper | Deployed JAR archived in `artifacts/deployed-custom/`; source missing. |
| `hmySpleef` | Paper | Deployed JAR archived in `artifacts/deployed-custom/`; source missing. |
| `hmySumo` | Paper | Deployed JAR archived in `artifacts/deployed-custom/`; source missing. |
| `hmyTNTRun` | Paper | Deployed JAR archived in `artifacts/deployed-custom/`; source missing. |

## Third-Party Runtime Dependencies

Third-party plugins are documented in `docs/PLUGIN_INVENTORY.md` but are not vendored here unless explicitly required. Current runtime dependencies include LuckPerms, Plan, WorldEdit, OpenAudioMc, ViaVersion, ViaBackwards, ViaRewind, and Chunky.

## Build

Each source plugin is currently its own Maven project:

```bash
cd <plugin-directory>
mvn package
```

Built JARs are produced under each module's `target/` directory. Deploy only after checking `plugin.yml` or `velocity-plugin.json` matches the live plugin name and package.
