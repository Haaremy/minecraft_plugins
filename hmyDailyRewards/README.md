# hmyDailyRewards

hmyDailyRewards provides a daily claim GUI, login streaks, reward milestones, hmyCore coin rewards, optional cosmetic crate items, and temporary LuckPerms ranks.

## Requirements

| Requirement | Notes |
|-------------|-------|
| Paper | API version 1.19+ |
| hmyCore | Required for `COINS` rewards |
| LuckPerms | Optional, required for `RANK_TEMP` rewards |

## Installation

1. Build the plugin with `mvn -q package`.
2. Copy the generated JAR to the target Paper server's `plugins/` directory.
3. Ensure `hmyCore` is installed before this plugin.
4. Start the server once so `plugins/hmyDailyRewards/config.yml` is created.
5. Configure the reward milestones and `streak_timeout_hours`.
6. Restart the Paper server.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/daily` | Open the daily reward GUI | none |
| `/daily claim` | Claim the current daily reward | none |
| `/daily streak` | Show current streak and total claims | none |
| `/daily rewards` | Show the 30-day reward overview | none |

## Reward Types

| Type | Required keys | Effect |
|------|---------------|--------|
| `COINS` | `amount` | Adds coins through hmyCore EconomyManager |
| `COSMETIC_CRATE` | `amount` | Gives a cosmetic crate item with key `hmy:crate.cosmetic` |
| `RANK_TEMP` | `rank`, `duration` | Grants a temporary LuckPerms rank for `duration` seconds |
| `ITEM` | `material`, `amount` | Gives a Bukkit material item stack |

## Configuration

```yaml
rewards:
  1:
    type: COINS
    amount: 100
    description: "<yellow>100 Coins</yellow>"
    icon: GOLD_NUGGET
  7:
    type: COSMETIC_CRATE
    amount: 1
    description: "<aqua>Kosmetik-Kiste</aqua>"
    icon: CHEST
  30:
    type: RANK_TEMP
    rank: "phoenix"
    duration: 86400
    amount: 1
    description: "<gold>1 Tag Phoenix-Rang</gold>"
    icon: NETHER_STAR

streak_timeout_hours: 48
```

Rewards are configured for days 1-30. Missing intermediate coin days can be interpolated between milestones. `description` is MiniMessage text and `icon` must be a valid Bukkit material.

## Runtime Data

The plugin stores claim state in its plugin data folder. Back up `plugins/hmyDailyRewards/` together with the server's plugin data.
