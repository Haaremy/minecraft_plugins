package de.haaremy.hmylobby;

import de.haaremy.hmylobby.balloon.BalloonManager;
import de.haaremy.hmylobby.jukebox.ComJukebox;
import de.haaremy.hmylobby.jukebox.JukeboxListener;
import de.haaremy.hmylobby.jukebox.JukeboxManager;
import de.haaremy.hmylobby.minigames.LobbyGameListener;
import de.haaremy.hmylobby.minigames.LobbyGameManager;
import de.haaremy.hmylobby.minigames.LobbyGameSelector;
import de.haaremy.hmylobby.minigames.LobbyGamesConfig;

import java.nio.file.Path;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import net.luckperms.api.LuckPerms;

public class HmyLobby extends JavaPlugin {

    private LuckPerms luckPerms;
    private HmyLanguageManager language;
    private HmyConfigManager configManager;
    private ServerSelectorConfig serverSelectorConfig;
    private ServerInfoListener serverInfoListener;
    private CosmeticMenuListener cosmeticMenuListener;
    private EffectManager effectManager;
    private PlayerEventListener playerEventListener;
    private LobbyGameManager lobbyGameManager;
    private LotteryCrateListener lotteryCrateListener;
    private SocialListener socialListener;

    @Override
    public void onEnable() {
        getLogger().info("Haaremy: hmyLobby Plugin wird aktiviert...");

        // LuckPerms
        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
        } else {
            getLogger().severe("Haaremy: LuckPerms konnte nicht geladen werden! Lobby wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // pluginsDir = minecraftServers/subserver/plugins/
        Path pluginsDir = getDataFolder().toPath().toAbsolutePath().getParent();
        this.configManager = new HmyConfigManager(getLogger(), pluginsDir);
        this.serverSelectorConfig = new ServerSelectorConfig(configManager, getLogger());
        this.language = new HmyLanguageManager(getLogger(), pluginsDir, configManager, luckPerms);
        this.language.loadAllLanguageFiles();

        this.playerEventListener = new PlayerEventListener(this, language);
        getServer().getPluginManager().registerEvents(this.playerEventListener, this);
        getServer().getPluginManager().registerEvents(new AgbListener(this, luckPerms), this);

        CosmeticMenuListener cosmeticListener = new CosmeticMenuListener(this);
        getServer().getPluginManager().registerEvents(cosmeticListener, this);
        this.cosmeticMenuListener = cosmeticListener;
        this.effectManager = new EffectManager(this);

        this.serverInfoListener = new ServerInfoListener(this);
        getServer().getMessenger().registerIncomingPluginChannel(this, "hmy:status", serverInfoListener);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "hmy:status");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        LobbyWorldManager lobbyWorldManager = new LobbyWorldManager(this, configManager, language);
        getServer().getPluginManager().registerEvents(new DoorSignListener(this), this);
        getServer().getPluginManager().registerEvents(lobbyWorldManager, this);

        // Minigames
        Path hmySettingsDir = pluginsDir.getParent().getParent().resolve("hmySettings");
        LobbyGamesConfig gamesConfig = new LobbyGamesConfig(hmySettingsDir, getLogger());
        LobbyGameSelector selector = new LobbyGameSelector();
        this.lotteryCrateListener = new LotteryCrateListener(this);
        this.lobbyGameManager = new LobbyGameManager(this, gamesConfig, selector, lotteryCrateListener);

        getServer().getPluginManager().registerEvents(selector, this);
        getServer().getPluginManager().registerEvents(new LobbyGameListener(lobbyGameManager), this);
        getServer().getPluginManager().registerEvents(lotteryCrateListener, this);
        var lobbygameCmd = getCommand("lobbygame");
        if (lobbygameCmd != null) lobbygameCmd.setExecutor(lobbyGameManager);

        // Social / Economy channels
        this.socialListener = new SocialListener(this);
        getServer().getMessenger().registerIncomingPluginChannel(this, "hmy:social",   socialListener);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "hmy:social");
        getServer().getMessenger().registerIncomingPluginChannel(this, "hmy:economy",
                new EconomyMessageListener(this));
        getServer().getMessenger().registerOutgoingPluginChannel(this, "hmy:economy");

        // Balloon / Elevator system
        BalloonManager balloonManager = new BalloonManager(this, language, hmySettingsDir);
        getServer().getPluginManager().registerEvents(balloonManager, this);

        // Jukebox system
        JukeboxManager jukeboxManager = new JukeboxManager(this, hmySettingsDir);
        getServer().getPluginManager().registerEvents(new JukeboxListener(jukeboxManager), this);
        var jukeboxCmd = getCommand("jukebox");
        if (jukeboxCmd != null) jukeboxCmd.setExecutor(new ComJukebox(jukeboxManager));

        // Commands
        var hmyCmd = getCommand("hmy");
        if (hmyCmd != null) {
            ComHmyLanguage hmyCommand = new ComHmyLanguage(luckPerms, language);
            hmyCommand.setBalloonManager(balloonManager);
            hmyCmd.setExecutor(hmyCommand);
        }

        Bukkit.getScheduler().runTaskLater(this, this::scanForSigns, 100L);

        getLogger().info("Haaremy: Alle Lobby Funktionen wurden erfolgreich aktiviert!");
    }

    private void scanForSigns() {
        NamespacedKey key = new NamespacedKey(this, "target_server");
        int count = 0;
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (state instanceof Sign sign) {
                        if (sign.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                            serverInfoListener.registerSign(sign);
                            count++;
                        }
                    }
                }
            }
        }
        getLogger().info("Haaremy: " + count + " Server-Schilder im Speicher registriert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Haaremy: hmyLobby deaktiviert!");
    }

    public ServerInfoListener getServerInfoListener()          { return serverInfoListener; }
    public LuckPerms getLuckPerms()                            { return luckPerms; }
    public ServerSelectorConfig getServerSelectorConfig()      { return serverSelectorConfig; }
    public CosmeticMenuListener getCosmeticMenuListener()      { return cosmeticMenuListener; }
    public PlayerEventListener getPlayerEventListener()        { return playerEventListener; }
    public EffectManager getEffectManager()                    { return effectManager; }
    public HmyLanguageManager getLanguageManager()             { return language; }
    public HmyConfigManager getConfigManager()                 { return configManager; }
    public LobbyGameManager getLobbyGameManager()              { return lobbyGameManager; }
    public SocialListener getSocialListener()                  { return socialListener; }
}
