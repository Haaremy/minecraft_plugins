package de.haaremy.hmykitsunesegen;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * Manages the in-game sidebar scoreboard for each player.
 *
 * Layout:
 *  §6§lKitsune Segen
 *  §7────────────────
 *  §eAm Leben: §a{count}
 *  §eKills: §a{kills}
 *  §7────────────────
 *  §bUmgebung: (5 minimap rows)
 *  §7────────────────
 *  §7mc.haaremy.de
 */
public class ScoreboardManager {

    private static final String OBJ_NAME = "ksg";

    private final HmyKitsuneSegen plugin;
    private BukkitTask updateTask;

    // Unique dummy team entries (invisible characters) for each line slot
    private static final String[] LINE_KEYS = {
        "§1§r", "§2§r", "§3§r", "§4§r", "§5§r",
        "§6§r", "§7§r", "§8§r", "§9§r", "§0§r",
        "§a§r", "§b§r", "§c§r", "§d§r", "§e§r",
        "§f§r", "§1§1", "§2§2"
    };

    public ScoreboardManager(HmyKitsuneSegen plugin) {
        this.plugin = plugin;
    }

    // ── Start / Stop ──────────────────────────────────────────────────────────

    public void startUpdating() {
        if (updateTask != null) updateTask.cancel();
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 0L, 20L);
    }

    public void stopUpdating() {
        if (updateTask != null) { updateTask.cancel(); updateTask = null; }
    }

    // ── Per-player update ─────────────────────────────────────────────────────

    private void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            World game = plugin.getServer().getWorld(plugin.getGameConfig().getGameWorld());
            if (game != null && game.equals(p.getWorld())) {
                updateScoreboard(p);
            }
        }
    }

    public void updateScoreboard(Player player) {
        Scoreboard board = getOrCreateBoard(player);
        Objective obj = board.getObjective(OBJ_NAME);
        if (obj == null) {
            obj = board.registerNewObjective(OBJ_NAME, Criteria.DUMMY,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "Kitsune Segen");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        int alive = plugin.getGameManager().getAliveCount();
        int kills = plugin.getGameManager().getKills(player);
        String[] minimap = renderMinimap(player);

        // Line ordering: high score = top, low score = bottom
        // 17 = separator top
        // 16 = "Am Leben"
        // 15 = "Kills"
        // 14 = separator mid
        // 13-9 = minimap rows (5)
        // 8 = separator bot
        // 7 = server name

        setLine(board, obj, 17, ChatColor.DARK_GRAY + "──────────────────");
        setLine(board, obj, 16, ChatColor.YELLOW + "Am Leben: " + ChatColor.GREEN + alive);
        setLine(board, obj, 15, ChatColor.YELLOW + "Kills: " + ChatColor.GREEN + kills);
        setLine(board, obj, 14, ChatColor.DARK_GRAY + "──────────────────");
        for (int i = 0; i < minimap.length; i++) {
            setLine(board, obj, 13 - i, minimap[i]);
        }
        setLine(board, obj, 8,  ChatColor.DARK_GRAY + "──────────────────");
        setLine(board, obj, 7,  ChatColor.GRAY + "mc.haaremy.de");

        player.setScoreboard(board);
    }

    public void clearScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    // ── Minimap renderer ──────────────────────────────────────────────────────
    // 7 wide × 5 tall; each cell = 8 blocks in X, 6 blocks in Z

    private static final int COLS = 7;
    private static final int ROWS = 5;
    private static final int CELL_X = 8;
    private static final int CELL_Z = 6;

    private String[] renderMinimap(Player self) {
        String[] rows = new String[ROWS];
        Location center = self.getLocation();
        World world = center.getWorld();
        int px = center.getBlockX();
        int py = center.getBlockY() - 1; // block below player
        int pz = center.getBlockZ();

        // Map (row,col) → player for nearby enemies
        Map<String, Boolean> playerCells = new HashMap<>();
        if (world != null) {
            for (Player other : world.getPlayers()) {
                if (other.equals(self)) continue;
                int col = (int) Math.round((other.getLocation().getX() - px) / (double) CELL_X);
                int row = (int) Math.round((other.getLocation().getZ() - pz) / (double) CELL_Z);
                if (col >= -(COLS / 2) && col <= (COLS / 2) && row >= -(ROWS / 2) && row <= (ROWS / 2)) {
                    boolean isAlive = plugin.getGameManager().isAlive(other);
                    playerCells.put(row + "," + col, isAlive);
                }
            }
        }

        for (int row = -(ROWS / 2); row <= (ROWS / 2); row++) {
            StringBuilder sb = new StringBuilder();
            for (int col = -(COLS / 2); col <= (COLS / 2); col++) {
                if (col == 0 && row == 0) {
                    sb.append(ChatColor.GREEN).append("@");
                } else if (playerCells.containsKey(row + "," + col)) {
                    boolean alive = playerCells.get(row + "," + col);
                    sb.append(alive ? ChatColor.RED : ChatColor.DARK_GRAY).append("●");
                } else {
                    // Sample block at this world position
                    if (world != null) {
                        int wx = px + col * CELL_X;
                        int wz = pz + row * CELL_Z;
                        Block b = world.getBlockAt(wx, py, wz);
                        if (b.getType().isSolid()) {
                            sb.append(ChatColor.DARK_GRAY).append("▪");
                        } else {
                            sb.append(ChatColor.GRAY).append("·");
                        }
                    } else {
                        sb.append(ChatColor.GRAY).append("·");
                    }
                }
            }
            rows[row + (ROWS / 2)] = sb.toString();
        }
        return rows;
    }

    // ── Scoreboard line helpers ────────────────────────────────────────────────

    private Scoreboard getOrCreateBoard(Player player) {
        Scoreboard board = player.getScoreboard();
        // If player has the main scoreboard, create a new one
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }
        return board;
    }

    private void setLine(Scoreboard board, Objective obj, int score, String text) {
        String entry = LINE_KEYS[score];

        // Remove old team for this slot if exists
        String teamName = "ksg_line_" + score;
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.addEntry(entry);
        }

        // Prefix holds the visible text (up to 64 chars)
        String prefix = text.length() > 64 ? text.substring(0, 64) : text;
        team.setPrefix(prefix);

        Score s = obj.getScore(entry);
        s.setScore(score);
    }
}
