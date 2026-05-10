# hmyVelocity

hmyVelocity is the Velocity proxy plugin for the Haaremy Minecraft network. It handles server routing, network tab list, economy, friends, cross-server DMs, reports, language metadata, and plugin message channels for Paper backends.

## Requirements

| Requirement | Notes |
|-------------|-------|
| Velocity | Proxy platform |
| LuckPerms | Required dependency |
| Paper backends | Must have BungeeCord/Velocity plugin messaging enabled for cross-server features |

## Installation

1. Build with `mvn -q package`.
2. Copy the generated JAR to the Velocity `plugins/` directory as `hmyVelocity.jar` or equivalent.
3. Ensure LuckPerms Velocity is installed.
4. Start the proxy once so `plugins/hmyvelocityplugin/` data files are created.
5. Verify backend names match the Velocity config, especially `lobby`, `survival`, and `kitsune`.
6. Restart the proxy.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/lobby` | Send player to the lobby backend | none |
| `/server <name>` | Send player to a backend server | backend permission if configured |
| `/hmy language <de\|en>` | Set language preference in LuckPerms meta | none |
| `/hmy coins` | Show hmyCoins and hmyShards | none |
| `/hmy coins give <player> <amount>` | Give hmyCoins | `hmy.coins.give` |
| `/friend add <player>` | Send a friend request | none |
| `/friend accept <player>` | Accept a friend request | none |
| `/friend deny <player>` | Deny a friend request | none |
| `/friend remove <player>` | Remove a friend | none |
| `/friend list` | List friends by online state | none |
| `/friend join <player>` | Join a friend's current server | none |
| `/friend follow <player>` | Follow a friend across server switches | none |
| `/friend unfollow` | Stop following | none |
| `/dm <player> <message>` | Cross-server private message | none |
| `/r <message>` | Reply to the last DM | none |
| `/report <player> <reason>` | Report a player | none |
| `/broadcast <message>` | Broadcast network-wide | `hmy.broadcast` |

## Permissions

| Permission | Description |
|------------|-------------|
| `hmy.coins.give` | Admin coin grants |
| `hmy.broadcast` | Network broadcast |
| `hmy.socialspy` | See SocialSpy DM copies |
| `hmy.report.admin` | Receive report notifications |

## Runtime Data

| File | Purpose |
|------|---------|
| `plugins/hmyvelocityplugin/economy.json` | hmyCoins and hmyShards balances |
| `plugins/hmyvelocityplugin/friends.json` | Accepted friendships |
| `plugins/hmyvelocityplugin/friend_requests.json` | Pending friend requests |
| `plugins/hmyvelocityplugin/reports.log` | Player reports |
| `hmyLanguages/hmyLanguage_<lang>.properties` | Language strings |

## Plugin Message Channels

| Channel | Purpose |
|---------|---------|
| `hmy:status` | Backend player counts to lobby |
| `hmy:economy` | Coin/shard updates and balance requests |
| `hmy:social` | Friend data and friend join requests |
| `hmy:trigger` | Backend-triggered Velocity commands |
