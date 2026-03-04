package de.haaremy.hmylobby;

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

    @Override
    public void onEnable() {
        getLogger().info("Haaremy: hmyLobby Plugin wird aktiviert...");
        saveDefaultConfig();

        // LuckPerms
        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
        } else {
            getLogger().severe("Haaremy: LuckPerms konnte nicht geladen werden! Lobby wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Manager
        var logger = getLogger();
        Path dataDirectory = getDataFolder().toPath().getParent();
        this.configManager = new HmyConfigManager(logger, dataDirectory);
        this.serverSelectorConfig = new ServerSelectorConfig(this);
        this.language = new HmyLanguageManager(logger, dataDirectory, configManager, luckPerms);
        
        this.playerEventListener = new PlayerEventListener(this, language);
        getServer().getPluginManager().registerEvents(this.playerEventListener, this);
        
        CosmeticMenuListener cosmeticListener = new CosmeticMenuListener(this);
        getServer().getPluginManager().registerEvents(cosmeticListener, this);
        this.cosmeticMenuListener = cosmeticListener; // Getter erstellen!
        this.effectManager = new EffectManager(this);

        // --- NEU: Server Status Logik ---
        this.serverInfoListener = new ServerInfoListener(this);
        getServer().getMessenger().registerIncomingPluginChannel(this, "hmy:status", serverInfoListener);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "hmy:status");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // LobbyWorldManager
        LobbyWorldManager lobbyWorldManager = new LobbyWorldManager(this);

        
        getServer().getPluginManager().registerEvents(new DoorSignListener(this), this);
        getServer().getPluginManager().registerEvents(lobbyWorldManager, this);
        
       
        

        // --- NEU: Scan für bestehende Schilder nach 5 Sekunden ---
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

    public ServerInfoListener getServerInfoListener() { return serverInfoListener; }
    public LuckPerms getLuckPerms() { return luckPerms; }
    public ServerSelectorConfig getServerSelectorConfig() { return serverSelectorConfig; }

	public CosmeticMenuListener getCosmeticMenuListener() {
		return cosmeticMenuListener;
	}
	
	public PlayerEventListener getPlayerEventListener() {
		
		return playerEventListener;
	}
	
	public EffectManager getEffectManager() { return effectManager; }
}