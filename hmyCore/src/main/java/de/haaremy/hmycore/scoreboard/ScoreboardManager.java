package de.haaremy.hmycore.scoreboard;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.permissions.LuckPermsService;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {

    private final HmyCore plugin;
    private final Map<UUID, HmyScoreboard> scoreboards = new ConcurrentHashMap<>();

    public ScoreboardManager(HmyCore plugin) {
        this.plugin = plugin;
    }

    public HmyScoreboard createScoreboard(Player player) {
        removeScoreboard(player);
        HmyScoreboard scoreboard = new HmyScoreboard(player);
        scoreboards.put(player.getUniqueId(), scoreboard);
        return scoreboard;
    }

    public HmyScoreboard getScoreboard(Player player) {
        return scoreboards.get(player.getUniqueId());
    }

    public void removeScoreboard(Player player) {
        HmyScoreboard scoreboard = scoreboards.remove(player.getUniqueId());
        if (scoreboard != null) {
            scoreboard.remove();
        }
    }

    public boolean hasScoreboard(Player player) {
        return scoreboards.containsKey(player.getUniqueId());
    }

    public void clearAll() {
        for (HmyScoreboard scoreboard : scoreboards.values()) {
            scoreboard.remove();
        }
        scoreboards.clear();
    }

    /**
     * Komfort-Helfer fuer Spielmodi: liefert "<prefix><name>" oder nur "<name>",
     * falls LuckPerms nicht verfuegbar oder kein Prefix gesetzt.
     * Prefix kommt aus LuckPerms (legacy &-Codes oder MiniMessage), Name aus
     * dem Bukkit-Player.
     */
    public String renderRankedName(Player player) {
        if (player == null) return "";
        LuckPermsService lp = plugin.getLuckPermsService();
        String prefix = lp != null ? lp.getPrefix(player) : "";
        if (prefix == null) prefix = "";
        return prefix + player.getName();
    }
}
