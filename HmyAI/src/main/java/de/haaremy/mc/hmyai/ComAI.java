package de.haaremy.mc.hmyai;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /ai <prompt>  – Ask Claude something or let it act on the server.
 * /ai reset     – Clear the conversation session for this player.
 *
 * Runs Claude asynchronously; blocks duplicate requests per player.
 */
public class ComAI implements CommandExecutor {

    private final HmyAI plugin;
    // Players currently waiting for a Claude response
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    public ComAI(HmyAI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler können /ai nutzen.");
            return true;
        }

        if (!player.hasPermission("hmy.ai")) {
            player.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§eUsage: §f/ai <Frage oder Aufgabe> §8| §f/ai reset");
            return true;
        }

        // ── /ai reset ────────────────────────────────────────────────────────
        if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
            plugin.getSessionManager().clearSession(player.getUniqueId());
            player.sendMessage("§aSession zurückgesetzt. Neues Gespräch beginnt beim nächsten /ai.");
            return true;
        }

        // ── Cooldown: one request at a time per player ────────────────────────
        if (pending.contains(player.getUniqueId())) {
            player.sendMessage("§eIch bin noch dabei, deine letzte Anfrage zu bearbeiten...");
            return true;
        }

        String userPrompt = String.join(" ", args);
        pending.add(player.getUniqueId());
        player.sendMessage("§7[AI] §eDenke nach...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // ── Build context prefix ──────────────────────────────────────
                String context = buildContext(player);
                String fullPrompt = context + "\n" + userPrompt;

                // ── Retrieve existing session ─────────────────────────────────
                String sessionId = plugin.getSessionManager().getSession(player.getUniqueId());

                // ── Call Claude ───────────────────────────────────────────────
                ClaudeProcess.ClaudeResult result =
                        plugin.getClaudeProcess().run(fullPrompt, sessionId);

                // ── Persist new session ID ────────────────────────────────────
                if (result.sessionId() != null) {
                    plugin.getSessionManager().setSession(player.getUniqueId(), result.sessionId());
                    plugin.getSessionManager().save();
                }

                // ── Send response to player ───────────────────────────────────
                sendResponse(player, result.text());

            } catch (Exception e) {
                player.sendMessage("§c[AI] Fehler: " + e.getMessage());
                plugin.getLogger().warning("Claude-Fehler für " + player.getName() + ": " + e.getMessage());
            } finally {
                pending.remove(player.getUniqueId());
            }
        });

        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildContext(Player player) {
        String template = plugin.getConfig().getString(
                "context-template",
                "[Server: {server} | Spieler: {player} | Welt: {world} | Pos: {x},{y},{z} | Online: {online}/{max}]");

        return template
                .replace("{player}", player.getName())
                .replace("{world}",  player.getWorld().getName())
                .replace("{x}",      String.valueOf((int) player.getLocation().getX()))
                .replace("{y}",      String.valueOf((int) player.getLocation().getY()))
                .replace("{z}",      String.valueOf((int) player.getLocation().getZ()))
                .replace("{server}", plugin.getServer().getName())
                .replace("{online}", String.valueOf(plugin.getServer().getOnlinePlayers().size()))
                .replace("{max}",    String.valueOf(plugin.getServer().getMaxPlayers()));
    }

    private void sendResponse(Player player, String text) {
        if (!player.isOnline()) return;

        int maxLines = plugin.getConfig().getInt("max-response-lines", 12);
        String[] lines = text.split("\n");

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.sendMessage("§7[AI] §f──────────────────");
            int shown = 0;
            for (String line : lines) {
                if (shown >= maxLines) {
                    player.sendMessage("§8[... " + (lines.length - shown) + " weitere Zeile(n) gekürzt]");
                    break;
                }
                // Colour codes work in chat
                player.sendMessage("§f" + line);
                shown++;
            }
        });
    }
}
