package de.haaremy.hmycore.scoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;

public class HmyScoreboard {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Map<Integer, String> lines = new HashMap<>();
    private final Map<Integer, Team> teams = new HashMap<>();

    private static final String[] ENTRIES = {
        "\u00a70", "\u00a71", "\u00a72", "\u00a73", "\u00a74",
        "\u00a75", "\u00a76", "\u00a77", "\u00a78", "\u00a79",
        "\u00a7a", "\u00a7b", "\u00a7c", "\u00a7d", "\u00a7e"
    };

    public HmyScoreboard(Player player) {
        this.player = player;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("hmyboard", Criteria.DUMMY, Component.text(""));
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(scoreboard);
    }

    public void setTitle(String miniMessageTitle) {
        objective.displayName(MINI.deserialize(miniMessageTitle));
    }

    public void setLine(int line, String miniMessageText) {
        if (line < 0 || line >= ENTRIES.length) return;

        String entry = ENTRIES[line];
        String oldText = lines.get(line);

        // Nur aktualisieren, wenn sich der Text geaendert hat
        if (miniMessageText.equals(oldText)) return;

        lines.put(line, miniMessageText);

        Team team = teams.get(line);
        if (team == null) {
            team = scoreboard.registerNewTeam("line_" + line);
            team.addEntry(entry);
            teams.put(line, team);
        }

        team.prefix(MINI.deserialize(miniMessageText));
        objective.getScore(entry).setScore(14 - line);
    }

    public void removeLine(int line) {
        if (line < 0 || line >= ENTRIES.length) return;

        String entry = ENTRIES[line];
        scoreboard.resetScores(entry);
        lines.remove(line);

        Team team = teams.remove(line);
        if (team != null) {
            team.unregister();
        }
    }

    public void clear() {
        for (int i = 0; i < ENTRIES.length; i++) {
            removeLine(i);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void remove() {
        clear();
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
