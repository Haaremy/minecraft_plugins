package de.haaremy.hmykitsunesegen;

import java.nio.file.Path;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.luckperms.api.LuckPerms;

public class HmyKitsuneSegen extends JavaPlugin {

    private LuckPerms luckPerms;
    private HmyConfigManager configManager;
    private HmyLanguageManager language;
    private PlayerEventListener playerEventListener;
    private List<Location> locations;
    private List<Location> chests;
    private PlayerChestClick playerChestClick;
    private LuckyItem luckyItem;

    @Override
public void onEnable() {
    getLogger().info("Haaremy: hmyKitsuneSegen Plugin wird aktiviert...");

    RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
    if (provider != null) {
        this.luckPerms = provider.getProvider();
    } else {
        getLogger().severe("Haaremy: LuckPerms konnte nicht geladen werden! KistuneSegen wird deaktiviert.");
        getServer().getPluginManager().disablePlugin(this);
        return;
    }

    //saveDefaultConfig();
    //gameWorldManager = new GameWorldManager(getDataFolder());

     // Datenverzeichnis und Konfigurationsmanager initialisieren
        var logger = getLogger();
        Path dataDirectory = getDataFolder().toPath().getParent();
        this.configManager = new HmyConfigManager(logger,dataDirectory);
        logger.info("Haaremy: Paper Config wird initialisiert.");
        this.language = new HmyLanguageManager(logger, dataDirectory, configManager, luckPerms);
        logger.info("Haaremy: Paper Sprachen initialisiert.");

    // Event-Listener registrieren
    String gameworld = "game";
    String hubworld = "hub";
    playerEventListener = new PlayerEventListener(this, language, gameworld, hubworld);
    playerChestClick = new PlayerChestClick(this, language);
    luckyItem = new LuckyItem(this);
    getServer().getPluginManager().registerEvents(playerEventListener, this);
    getServer().getPluginManager().registerEvents(playerChestClick, this);
    playerEventListener.loadWorlds(gameworld);
    //Bukkit.getScheduler().runTask(this, () -> {
    locations = playerEventListener.findAndLogBlocks(gameworld, Material.OBSIDIAN, 0);
    chests = playerEventListener.findAndLogBlocks(gameworld, Material.OAK_PLANKS, 1);
    
    //});

    getLogger().info("Haaremy: KitsuneSegen wurde erfolgreich aktiviert!");
}
    @Override
    public void onDisable() {
        getLogger().info("Haaremy: KitsuneSegen deaktiviert!");
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public List<Location> getLocations(){
        return locations;
    }

    public List<Location> getChests(){
        return chests;
    }

    public PlayerEventListener getPlayerEventListener(){
        return playerEventListener;
    }

    public LuckyItem getLuckItem(){
        return luckyItem;
    }



    
}
