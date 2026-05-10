# HmyAI

HmyAI is a Paper plugin that exposes an in-game `/ai` command. It invokes a local Claude CLI command and provides Minecraft context through a local MCP callback server.

## Requirements

| Requirement | Notes |
|-------------|-------|
| Paper | API version 1.20+ |
| Claude CLI | Must be available as configured by `claude-command` |
| Local callback port | Default `25580`, should only be reachable locally |

## Installation

1. Build the plugin with `mvn -q package`.
2. Copy the generated JAR to the target Paper server's `plugins/` directory.
3. Start the server once so `plugins/HmyAI/config.yml` is created.
4. Edit `config.yml` and verify the Claude CLI path and local callback port.
5. Grant `hmy.ai` only to trusted operators.
6. Restart the Paper server.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/ai <prompt>` | Ask Claude something or let it act on the server | `hmy.ai` |
| `/ai reset` | Clear your current conversation session | `hmy.ai` |

## Configuration

```yaml
claude-command: "claude"
http-port: 25580
timeout-seconds: 120
max-response-lines: 12
context-template: "[Server: {server} | Spieler: {player} | Welt: {world} | Pos: {x},{y},{z} | Online: {online}/{max}]"
```

| Key | Meaning |
|-----|---------|
| `claude-command` | Executable used to start Claude. Use an absolute path if the server service has a restricted PATH. |
| `http-port` | Local HTTP callback port for MCP tool calls. Keep it firewalled to localhost/server-internal traffic. |
| `timeout-seconds` | Maximum time for one AI request including tool calls. |
| `max-response-lines` | Maximum response lines sent back to the player. |
| `context-template` | Prefix added to every prompt. Supports `{player}`, `{world}`, `{x}`, `{y}`, `{z}`, `{server}`, `{online}`, `{max}`. |

## Security Notes

HmyAI can expose server actions through MCP tools such as `execute_command`, `get_server_info`, `get_players`, `get_player_info`, `get_world_info`, `get_block`, and `send_message`. Treat `hmy.ai` as an operator-level permission.
