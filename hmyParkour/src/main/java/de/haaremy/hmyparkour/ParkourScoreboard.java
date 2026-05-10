package de.haaremy.hmyparkour;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.scoreboard.HmyScoreboard;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class ParkourScoreboard extends BukkitRunnable {

    private final HmyParkour plugin;
    private final ParkourManager manager;

    public ParkourScoreboard(HmyParkour plugin, ParkourManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void start() {
        this.runTaskTimer(plugin, 0L, 20L); // Jede Sekunde
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            ParkourSession session = manager.getSession(uuid);

            if (session == null || !session.isActive()) {
                continue;
            }

            ParkourCourse course = manager.getCourses().get(session.getCourseName());
            if (course == null) continue;

            de.haaremy.hmycore.scoreboard.ScoreboardManager sbManager = HmyCore.getInstance().getScoreboardManager();
            HmyScoreboard scoreboard = sbManager.getScoreboard(player);
            if (scoreboard == null) {
                scoreboard = sbManager.createScoreboard(player);
            }

            String diffColor = course.getDifficulty().getMiniMessageColor();

            scoreboard.setTitle("<gold><bold>Parkour</bold></gold>");
            scoreboard.setLine(0, "");
            scoreboard.setLine(1, "<gray>Kurs: <white>" + course.getName());
            scoreboard.setLine(2, "<gray>Schwierigkeit: " + diffColor + course.getDifficulty().getDisplayName());
            scoreboard.setLine(3, "");
            scoreboard.setLine(4, "<gray>Zeit: <yellow>" + session.getElapsedTimeFormatted());
            scoreboard.setLine(5, "<gray>Checkpoint: <white>" + session.getCurrentCheckpoint() + "/" + course.getCheckpoints().size());
            scoreboard.setLine(6, "");

            Long bestTime = manager.getLeaderboard().getBestTime(session.getCourseName(), uuid);
            if (bestTime != null) {
                scoreboard.setLine(7, "<gray>Bestzeit: <green>" + ParkourSession.formatTime(bestTime));
            } else {
                scoreboard.setLine(7, "<gray>Bestzeit: <white>---");
            }

            scoreboard.setLine(8, "");
            scoreboard.setLine(9, "<gold>mc.haaremy.de");
        }
    }
}
