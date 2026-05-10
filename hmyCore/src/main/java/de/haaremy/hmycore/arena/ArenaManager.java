package de.haaremy.hmycore.arena;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.arena.events.ArenaEndEvent;
import de.haaremy.hmycore.arena.events.ArenaPlayerJoinEvent;
import de.haaremy.hmycore.arena.events.ArenaPlayerLeaveEvent;
import de.haaremy.hmycore.arena.events.ArenaStartEvent;
import de.haaremy.hmycore.arena.events.ArenaStateChangeEvent;
import de.haaremy.hmycore.arena.events.ArenaWinnerEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArenaManager {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final HmyCore plugin;
    private final Map<String, Arena> arenas = new HashMap<>();
    private final File arenaFolder;

    public ArenaManager(HmyCore plugin) {
        this.plugin = plugin;
        this.arenaFolder = new File(plugin.getDataFolder(), "arenas");
        if (!arenaFolder.exists()) {
            arenaFolder.mkdirs();
        }
        loadArenas();
    }

    private void loadArenas() {
        File[] files = arenaFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String arenaName = config.getString("name", file.getName().replace(".yml", ""));

            Arena arena = new Arena(arenaName);
            arena.setWorldName(config.getString("world", "world"));
            arena.setGameType(config.getString("gameType", "default"));
            arena.setMinPlayers(config.getInt("minPlayers", 2));
            arena.setMaxPlayers(config.getInt("maxPlayers", 16));
            arena.setTemplateWorldName(config.getString("templateWorld", null));

            // Spawnpunkte laden
            ConfigurationSection spawns = config.getConfigurationSection("spawnPoints");
            if (spawns != null) {
                for (String key : spawns.getKeys(false)) {
                    ConfigurationSection sp = spawns.getConfigurationSection(key);
                    if (sp != null) {
                        World world = Bukkit.getWorld(arena.getWorldName());
                        if (world != null) {
                            Location loc = new Location(
                                    world,
                                    sp.getDouble("x"),
                                    sp.getDouble("y"),
                                    sp.getDouble("z"),
                                    (float) sp.getDouble("yaw", 0),
                                    (float) sp.getDouble("pitch", 0)
                            );
                            arena.addSpawnPoint(loc);
                        }
                    }
                }
            }

            arenas.put(arenaName.toLowerCase(), arena);
            plugin.getLogger().info("Arena geladen: " + arenaName);
        }
    }

    public Arena createArena(String name, String worldName, String gameType) {
        Arena arena = new Arena(name);
        arena.setWorldName(worldName);
        arena.setGameType(gameType);
        arenas.put(name.toLowerCase(), arena);
        saveArena(arena);
        return arena;
    }

    public boolean deleteArena(String name) {
        Arena arena = arenas.remove(name.toLowerCase());
        if (arena == null) return false;

        File file = new File(arenaFolder, name.toLowerCase() + ".yml");
        if (file.exists()) {
            file.delete();
        }
        return true;
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Arena getAvailableArena(String gameType) {
        for (Arena arena : arenas.values()) {
            if (arena.getGameType().equalsIgnoreCase(gameType)
                    && arena.getState() == ArenaState.WAITING
                    && !arena.isFull()) {
                return arena;
            }
        }
        return null;
    }

    public List<Arena> getArenas() {
        return Collections.unmodifiableList(new ArrayList<>(arenas.values()));
    }

    public void saveArena(Arena arena) {
        File file = new File(arenaFolder, arena.getName().toLowerCase() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("name", arena.getName());
        config.set("world", arena.getWorldName());
        config.set("gameType", arena.getGameType());
        config.set("minPlayers", arena.getMinPlayers());
        config.set("maxPlayers", arena.getMaxPlayers());
        config.set("templateWorld", arena.getTemplateWorldName());

        List<Location> spawns = arena.getSpawnPoints();
        for (int i = 0; i < spawns.size(); i++) {
            Location loc = spawns.get(i);
            String path = "spawnPoints." + i;
            config.set(path + ".x", loc.getX());
            config.set(path + ".y", loc.getY());
            config.set(path + ".z", loc.getZ());
            config.set(path + ".yaw", loc.getYaw());
            config.set(path + ".pitch", loc.getPitch());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Fehler beim Speichern der Arena " + arena.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Setzt die Welt einer Arena zurueck:
     * 1. Welt entladen
     * 2. Ordner loeschen
     * 3. Template-Ordner kopieren
     * 4. Welt neu laden
     */
    public void resetArenaWorld(Arena arena, Runnable onComplete) {
        String worldName = arena.getWorldName();
        String templateName = arena.getTemplateWorldName();

        if (templateName == null || templateName.isEmpty()) {
            plugin.getLogger().warning("Kein Template fuer Arena " + arena.getName() + " definiert.");
            if (onComplete != null) onComplete.run();
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            // Alle Spieler aus der Welt entfernen
            World fallback = Bukkit.getWorlds().get(0);
            world.getPlayers().forEach(p -> p.teleport(fallback.getSpawnLocation()));
            Bukkit.unloadWorld(world, false);
        }

        // Async: Welt-Ordner loeschen und Template kopieren
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Path worldPath = Bukkit.getWorldContainer().toPath().resolve(worldName);
                Path templatePath = Bukkit.getWorldContainer().toPath().resolve(templateName);

                // Welt-Ordner loeschen
                if (Files.exists(worldPath)) {
                    deleteDirectory(worldPath);
                }

                // Template kopieren
                if (Files.exists(templatePath)) {
                    copyDirectory(templatePath, worldPath);
                }

                // Zurueck auf Main-Thread: Welt laden
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    WorldCreator creator = new WorldCreator(worldName);
                    Bukkit.createWorld(creator);
                    arena.reset();
                    plugin.getLogger().info("Arena-Welt " + worldName + " zurueckgesetzt.");
                    if (onComplete != null) onComplete.run();
                });

            } catch (IOException e) {
                plugin.getLogger().severe("Fehler beim Zuruecksetzen der Arena-Welt: " + e.getMessage());
            }
        });
    }

    private void deleteDirectory(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void shutdown() {
        for (Arena arena : arenas.values()) {
            saveArena(arena);
        }
    }

    /**
     * Beigetreten + Event feuernd. PreEvent ist cancellable; bei Cancel oder
     * Capacity-Limit kommt false zurueck.
     */
    public boolean joinPlayer(Arena arena, UUID playerUuid) {
        if (arena.isFull() || arena.getState() != ArenaState.WAITING) {
            return false;
        }
        if (arena.hasPlayer(playerUuid)) {
            return false;
        }
        ArenaPlayerJoinEvent event = new ArenaPlayerJoinEvent(arena, playerUuid);
        callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        return arena.addPlayer(playerUuid);
    }

    /**
     * Verlassen + Event feuernd. Event wird nur gefeuert wenn ein Spieler tatsaechlich
     * entfernt wurde.
     */
    public boolean leavePlayer(Arena arena, UUID playerUuid) {
        boolean removed = arena.removePlayer(playerUuid);
        if (removed) {
            callEvent(new ArenaPlayerLeaveEvent(arena, playerUuid));
        }
        return removed;
    }

    /**
     * State-Wechsel + Events feuernd. ArenaStateChangeEvent ist cancellable.
     * Falls Ziel-State RUNNING/ENDING ist, wird zusaetzlich
     * ArenaStartEvent / ArenaEndEvent gefeuert.
     * Identitaets-Aufrufe (from == to) werden ignoriert.
     */
    public boolean changeState(Arena arena, ArenaState newState) {
        ArenaState oldState = arena.getState();
        if (oldState == newState) {
            return false;
        }
        ArenaStateChangeEvent event = new ArenaStateChangeEvent(arena, oldState, newState);
        callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        arena.setState(newState);
        if (newState == ArenaState.RUNNING) {
            callEvent(new ArenaStartEvent(arena));
        } else if (newState == ArenaState.ENDING) {
            callEvent(new ArenaEndEvent(arena));
        }
        return true;
    }

    /**
     * Setzt den Gewinner einer Arena und feuert ArenaWinnerEvent.
     */
    public void setWinner(Arena arena, UUID winnerUuid) {
        arena.setWinner(winnerUuid);
        callEvent(new ArenaWinnerEvent(arena, winnerUuid));
    }

    private void callEvent(Event event) {
        // Defensiv: in Tests/CI ohne aktiven Server ist Bukkit.getPluginManager() null.
        if (Bukkit.getServer() == null) {
            return;
        }
        try {
            Bukkit.getPluginManager().callEvent(event);
        } catch (IllegalStateException e) {
            // Async/Plugin-Zustand inkonsistent - nur loggen, nicht crashen.
            plugin.getLogger().warning("Event-Dispatch fehlgeschlagen: " + e.getMessage());
        }
    }
}
