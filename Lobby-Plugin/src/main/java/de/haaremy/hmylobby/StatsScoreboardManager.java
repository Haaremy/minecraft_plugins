package de.haaremy.hmylobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet das hmyCubed-Sidebar-Scoreboard (rechte HUD-Seite) pro Spieler.
 * Anzeige: Spielername, Coins, Shards, Online-Count, Ping, Server-IP.
 * Coins/Shards werden über den hmy:economy-Plugin-Channel gepusht.
 */
public class StatsScoreboardManager {

    private static final String OBJECTIVE_NAME = "hmyCubedStats";

    private final HmyLobby plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final Map<UUID, long[]> balances = new HashMap<>();

    public StatsScoreboardManager(HmyLobby plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 40L, 40L);
    }

    public void show(Player player) {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm == null) return;
        Scoreboard board = sm.getNewScoreboard();
        Component title = Component.text("hmyCubed")
                .color(TextColor.color(0xFF8C50))
                .decorate(TextDecoration.BOLD);
        Objective obj = board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        boards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
        refresh(player);
    }

    public void hide(Player player) {
        boards.remove(player.getUniqueId());
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm != null) player.setScoreboard(sm.getMainScoreboard());
    }

    public void updateBalance(UUID uuid, long coins, long shards) {
        balances.put(uuid, new long[]{coins, shards});
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && boards.containsKey(uuid)) refresh(p);
    }

    public boolean isShown(UUID uuid) {
        return boards.containsKey(uuid);
    }

    public void cleanup(UUID uuid) {
        boards.remove(uuid);
        balances.remove(uuid);
    }

    private void refreshAll() {
        for (UUID uuid : boards.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) refresh(p);
        }
    }

    private void refresh(Player player) {
        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null) return;
        Objective obj = board.getObjective(OBJECTIVE_NAME);
        if (obj == null) return;

        for (String entry : board.getEntries()) board.resetScores(entry);

        long[] bal = balances.getOrDefault(player.getUniqueId(), new long[]{0L, 0L});
        int online = Bukkit.getOnlinePlayers().size();
        int ping = player.getPing();

        int score = 10;
        putLine(obj, "§8§m━━━━━━━━━━━━━━", score--);
        putLine(obj, "§7Spieler: §e" + truncate(player.getName(), 12), score--);
        putLine(obj, "§r ", score--);
        putLine(obj, "§6⬡ §eCoins: §f" + formatNumber(bal[0]), score--);
        putLine(obj, "§b◆ §3Shards: §f" + formatNumber(bal[1]), score--);
        putLine(obj, "§r  ", score--);
        putLine(obj, "§a☺ §7Online: §f" + online, score--);
        putLine(obj, "§d⚡ §7Ping: §f" + ping + "ms", score--);
        putLine(obj, "§r   ", score--);
        putLine(obj, "§7mc.haaremy.de", score--);
    }

    private void putLine(Objective obj, String line, int score) {
        obj.getScore(line).setScore(score);
    }

    private String formatNumber(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 10_000) return String.format("%.1fk", n / 1_000.0);
        return String.valueOf(n);
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
