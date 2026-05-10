package de.haaremy.hmycore;

import de.haaremy.hmycore.arena.ArenaManager;
import de.haaremy.hmycore.commands.AnnounceCommand;
import de.haaremy.hmycore.commands.AnnounceRenderer;
import de.haaremy.hmycore.commands.ArenaCommand;
import de.haaremy.hmycore.commands.CoinsCommand;
import de.haaremy.hmycore.commands.HmyCommand;
import de.haaremy.hmycore.commands.LobbyCommand;
import de.haaremy.hmycore.commands.StatsCommand;
import de.haaremy.hmycore.commands.TopCommand;
import de.haaremy.hmycore.countdown.CountdownManager;
import de.haaremy.hmycore.economy.EconomyManager;
import de.haaremy.hmycore.gui.GuiManager;
import de.haaremy.hmycore.lang.LanguageManager;
import de.haaremy.hmycore.leaderboard.LeaderboardManager;
import de.haaremy.hmycore.lobby.LobbyConnector;
import de.haaremy.hmycore.messaging.MessagingService;
import de.haaremy.hmycore.messaging.PluginMessageMessagingService;
import de.haaremy.hmycore.permissions.ChatRankListener;
import de.haaremy.hmycore.permissions.LuckPermsService;
import de.haaremy.hmycore.permissions.TabListManager;
import de.haaremy.hmycore.placeholders.HmyCorePlaceholders;
import de.haaremy.hmycore.scoreboard.ScoreboardManager;
import de.haaremy.hmycore.stats.StatsManager;
import de.haaremy.hmycore.storage.DatabaseManager;
import de.haaremy.hmycore.storage.LegacySqliteMigrator;
import de.haaremy.hmycore.team.TeamManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

public class HmyCore extends JavaPlugin {

    private static HmyCore instance;

    private DatabaseManager databaseManager;
    private LanguageManager languageManager;
    private EconomyManager economyManager;
    private StatsManager statsManager;
    private GuiManager guiManager;
    private ScoreboardManager scoreboardManager;
    private CountdownManager countdownManager;
    private TeamManager teamManager;
    private ArenaManager arenaManager;
    private LeaderboardManager leaderboardManager;
    private LobbyConnector lobbyConnector;
    private PluginMessageMessagingService messagingService;
    private LuckPermsService luckPermsService;
    private TabListManager tabListManager;
    private HmyCorePlaceholders placeholderExpansion;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Storage zuerst initialisieren, damit Manager den Pool nutzen koennen
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.init();

        // Einmalige Migration der Legacy-SQLite-Daten (falls vorhanden)
        LegacySqliteMigrator.migrate(this);

        // Sprach-Bundles + Per-Player-Locale aus DB
        this.languageManager = new LanguageManager(this);
        this.languageManager.init();

        // Manager initialisieren
        this.economyManager = new EconomyManager(this);
        this.statsManager = new StatsManager(this);
        this.guiManager = new GuiManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.countdownManager = new CountdownManager(this);
        this.teamManager = new TeamManager();
        this.arenaManager = new ArenaManager(this);
        this.leaderboardManager = new LeaderboardManager(this);
        this.lobbyConnector = new LobbyConnector(this);

        // LuckPerms-Integration (softdep). Wrapper laeuft mit Fallbacks
        // weiter, wenn das Plugin fehlt — kein Hard-Fail.
        this.luckPermsService = new LuckPermsService(this);
        this.tabListManager = new TabListManager(this, luckPermsService);
        this.tabListManager.start();

        String serverName = getConfig().getString("messaging.server-name", "unknown");
        this.messagingService = new PluginMessageMessagingService(this, serverName);
        getLogger().info("hmy:msg Channel registriert (server=" + messagingService.localServerName() + ")");

        // Cross-Server-Ankuendigungen: rendern auf jedem Backend ausser dem Sender
        // (Velocity-Router schliesst sourceServer aus, lokaler Sender rendert direkt im Command).
        this.messagingService.subscribe(AnnounceCommand.TOPIC_PATTERN, ctx -> {
            String text = ctx.payloadAsUtf();
            getServer().getScheduler().runTask(this, () -> AnnounceRenderer.renderLocally(this, text));
        });

        // Listener registrieren
        getServer().getPluginManager().registerEvents(economyManager, this);
        getServer().getPluginManager().registerEvents(statsManager, this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(tabListManager, this);
        getServer().getPluginManager().registerEvents(new ChatRankListener(this, luckPermsService), this);

        // PlaceholderAPI-Hook (softdep). Wenn das Plugin laeuft, registrieren
        // wir die hmycore-Expansion; sonst still ueberspringen.
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                this.placeholderExpansion = new HmyCorePlaceholders(this);
                if (this.placeholderExpansion.register()) {
                    getLogger().info("PlaceholderAPI-Expansion 'hmycore' registriert.");
                } else {
                    getLogger().warning("PlaceholderAPI-Expansion 'hmycore' konnte nicht registriert werden.");
                }
            } catch (Throwable t) {
                getLogger().warning("PlaceholderAPI-Hook fehlgeschlagen: " + t.getMessage());
            }
        } else {
            getLogger().info("PlaceholderAPI nicht installiert — Expansion 'hmycore' uebersprungen.");
        }

        // Commands registrieren
        getCommand("coins").setExecutor(new CoinsCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("arena").setExecutor(new ArenaCommand(this));
        getCommand("lobby").setExecutor(new LobbyCommand(this));
        getCommand("hmyannounce").setExecutor(new AnnounceCommand(this));
        getCommand("hmy").setExecutor(new HmyCommand(this));
        TopCommand topCommand = new TopCommand(this);
        getCommand("top").setExecutor(topCommand);
        getCommand("top").setTabCompleter(topCommand);

        getLogger().info("hmyCore erfolgreich aktiviert!");
    }

    @Override
    public void onDisable() {
        if (placeholderExpansion != null) {
            try {
                placeholderExpansion.unregister();
            } catch (Throwable ignored) {
            }
        }
        if (tabListManager != null) {
            tabListManager.stop();
        }
        if (messagingService != null) {
            messagingService.shutdown();
        }
        if (economyManager != null) {
            economyManager.shutdown();
        }
        if (statsManager != null) {
            statsManager.shutdown();
        }
        if (arenaManager != null) {
            arenaManager.shutdown();
        }
        if (scoreboardManager != null) {
            scoreboardManager.clearAll();
        }
        if (countdownManager != null) {
            countdownManager.cancelAll();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        getLogger().info("hmyCore deaktiviert.");
    }

    public static HmyCore getInstance() {
        return instance;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public CountdownManager getCountdownManager() {
        return countdownManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public LobbyConnector getLobbyConnector() {
        return lobbyConnector;
    }

    public MessagingService getMessagingService() {
        return messagingService;
    }

    public LuckPermsService getLuckPermsService() {
        return luckPermsService;
    }

    public TabListManager getTabListManager() {
        return tabListManager;
    }
}
