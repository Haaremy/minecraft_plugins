package de.haaremy.hmyparkour;

import de.haaremy.hmycore.HmyCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ParkourManager {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String PREFIX = "<gold>[Parkour]</gold> ";

    private final HmyParkour plugin;
    private final Map<String, ParkourCourse> courses = new LinkedHashMap<>();
    private final Map<UUID, ParkourSession> activeSessions = new ConcurrentHashMap<>();
    private final ParkourLeaderboard leaderboard;
    private final File coursesFile;

    public ParkourManager(HmyParkour plugin) {
        this.plugin = plugin;
        this.coursesFile = new File(plugin.getDataFolder(), "courses.yml");
        this.leaderboard = new ParkourLeaderboard(plugin.getDataFolder());
        loadCourses();
    }

    private void loadCourses() {
        if (!coursesFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(coursesFile);
        ConfigurationSection coursesSection = config.getConfigurationSection("courses");
        if (coursesSection == null) return;

        for (String name : coursesSection.getKeys(false)) {
            ParkourCourse course = ParkourCourse.loadFromConfig(name, coursesSection.getConfigurationSection(name));
            if (course != null) {
                courses.put(name.toLowerCase(), course);
            }
        }
        plugin.getLogger().info(courses.size() + " Parkour-Kurse geladen.");
    }

    public void saveCourses() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, ParkourCourse> entry : courses.entrySet()) {
            entry.getValue().saveToConfig(config.createSection("courses." + entry.getKey()));
        }
        try {
            config.save(coursesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Speichern der Kurse", e);
        }
    }

    public boolean startCourse(Player player, String courseName) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            sendMessage(player, "<red>Du bist bereits in einem Parkour-Kurs! Nutze <white>/parkour quit</white> zum Verlassen.");
            return false;
        }

        ParkourCourse course = courses.get(courseName.toLowerCase());
        if (course == null) {
            sendMessage(player, "<red>Kurs <white>" + courseName + "</white> nicht gefunden!");
            return false;
        }

        if (!course.isComplete()) {
            sendMessage(player, "<red>Dieser Kurs ist noch nicht fertig eingerichtet!");
            return false;
        }

        ParkourSession session = new ParkourSession(player.getUniqueId(), courseName.toLowerCase(), course.getStartLocation());
        activeSessions.put(player.getUniqueId(), session);

        player.teleport(course.getStartLocation());

        String diffColor = course.getDifficulty().getMiniMessageColor();
        sendMessage(player, "<green>Kurs <white>" + course.getName() + "</white> " +
                diffColor + "(" + course.getDifficulty().getDisplayName() + ")</green> gestartet!");
        sendMessage(player, "<gray>Checkpoints: <white>" + course.getCheckpoints().size() +
                "</white> | Belohnung: <gold>" + course.getCoinReward() + " Coins</gold>");

        return true;
    }

    public void quitCourse(Player player) {
        ParkourSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) {
            sendMessage(player, "<red>Du bist in keinem Parkour-Kurs!");
            return;
        }
        session.setActive(false);

        // Scoreboard entfernen
        HmyCore.getInstance().getScoreboardManager().removeScoreboard(player);

        ParkourCourse course = courses.get(session.getCourseName());
        if (course != null) {
            player.teleport(course.getStartLocation());
        }

        sendMessage(player, "<yellow>Du hast den Parkour-Kurs verlassen.");
    }

    public void onCheckpoint(Player player, int checkpointIndex) {
        ParkourSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        ParkourCourse course = courses.get(session.getCourseName());
        if (course == null) return;

        if (checkpointIndex < 0 || checkpointIndex >= course.getCheckpoints().size()) return;

        // Nur vorwaerts zaehlen
        if (checkpointIndex <= session.getCurrentCheckpoint() - 1) return;

        session.setCurrentCheckpoint(checkpointIndex + 1);
        session.setLastCheckpointLocation(course.getCheckpoints().get(checkpointIndex));

        int total = course.getCheckpoints().size();
        Component actionBar = MINI.deserialize(
                PREFIX + "<green>Checkpoint <white>" + (checkpointIndex + 1) + "/" + total + "</white></green>");
        player.sendActionBar(actionBar);
    }

    public void onFinish(Player player) {
        ParkourSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;
        session.setActive(false);

        ParkourCourse course = courses.get(session.getCourseName());
        if (course == null) return;

        // Pruefen ob alle Checkpoints durchlaufen wurden
        if (session.getCurrentCheckpoint() < course.getCheckpoints().size()) {
            sendMessage(player, "<red>Du hast noch nicht alle Checkpoints erreicht! (" +
                    session.getCurrentCheckpoint() + "/" + course.getCheckpoints().size() + ")");
            activeSessions.put(player.getUniqueId(), session);
            session.setActive(true);
            return;
        }

        long timeMs = session.getElapsedTimeMs();
        String formattedTime = ParkourSession.formatTime(timeMs);
        UUID uuid = player.getUniqueId();

        // Bestzeit pruefen
        Long previousBest = leaderboard.getBestTime(session.getCourseName(), uuid);
        boolean isNewBest = previousBest == null || timeMs < previousBest;

        // Zeit speichern
        leaderboard.saveTime(session.getCourseName(), uuid, timeMs);

        // Coins berechnen
        int coins = course.getCoinReward();
        if (isNewBest) {
            coins = course.getBonusCoinReward();
        }

        // Coins vergeben
        HmyCore.getInstance().getEconomyManager().addCoins(uuid, coins);

        // Scoreboard entfernen
        HmyCore.getInstance().getScoreboardManager().removeScoreboard(player);

        // Nachricht senden
        sendMessage(player, "<green>Kurs <white>" + course.getName() + "</white> abgeschlossen!");
        sendMessage(player, "<gray>Zeit: <white>" + formattedTime + "</white>");

        if (isNewBest) {
            if (previousBest != null) {
                sendMessage(player, "<gold>Neue Bestzeit! <gray>(Vorher: " + ParkourSession.formatTime(previousBest) + ")");
            } else {
                sendMessage(player, "<gold>Erste Wertung eingetragen!");
            }
            sendMessage(player, "<gold>+" + coins + " Coins <gray>(Bestzeit-Bonus!)");
        } else {
            sendMessage(player, "<gray>Bestzeit: <white>" + ParkourSession.formatTime(previousBest) + "</white>");
            sendMessage(player, "<gold>+" + coins + " Coins");
        }
    }

    public void teleportToCheckpoint(Player player) {
        ParkourSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            sendMessage(player, "<red>Du bist in keinem Parkour-Kurs!");
            return;
        }
        player.teleport(session.getLastCheckpointLocation());
        sendMessage(player, "<gray>Zum letzten Checkpoint teleportiert.");
    }

    // ---- Admin-Methoden ----

    public boolean createCourse(String name, ParkourDifficulty difficulty, org.bukkit.Location startLocation) {
        if (courses.containsKey(name.toLowerCase())) return false;

        ParkourCourse course = new ParkourCourse(name, difficulty, startLocation);
        courses.put(name.toLowerCase(), course);
        saveCourses();
        return true;
    }

    public boolean setEnd(String name, org.bukkit.Location endLocation) {
        ParkourCourse course = courses.get(name.toLowerCase());
        if (course == null) return false;

        course.setEndLocation(endLocation);
        saveCourses();
        return true;
    }

    public boolean addCheckpoint(String name, org.bukkit.Location location) {
        ParkourCourse course = courses.get(name.toLowerCase());
        if (course == null) return false;

        course.addCheckpoint(location);
        saveCourses();
        return true;
    }

    public boolean deleteCourse(String name) {
        if (courses.remove(name.toLowerCase()) == null) return false;
        saveCourses();
        return true;
    }

    // ---- Getter ----

    public Map<String, ParkourCourse> getCourses() {
        return Collections.unmodifiableMap(courses);
    }

    public ParkourSession getSession(UUID uuid) {
        return activeSessions.get(uuid);
    }

    public boolean hasActiveSession(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    public ParkourLeaderboard getLeaderboard() {
        return leaderboard;
    }

    public void shutdown() {
        // Alle aktiven Sessions beenden
        for (UUID uuid : new ArrayList<>(activeSessions.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                quitCourse(player);
            }
        }
        activeSessions.clear();
        leaderboard.shutdown();
    }

    private void sendMessage(Player player, String miniMessageText) {
        player.sendMessage(MINI.deserialize(PREFIX + miniMessageText));
    }
}
