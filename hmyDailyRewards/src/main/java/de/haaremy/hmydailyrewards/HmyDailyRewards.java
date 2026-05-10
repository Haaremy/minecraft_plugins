package de.haaremy.hmydailyrewards;

import de.haaremy.hmycore.HmyCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

public class HmyDailyRewards extends JavaPlugin {

    private static HmyDailyRewards instance;

    private DailyRewardsConfig rewardsConfig;
    private DailyManager dailyManager;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        instance = this;

        // config.yml sicherstellen
        saveDefaultConfig();

        // hmyCore pruefen
        if (HmyCore.getInstance() == null) {
            getLogger().severe("hmyCore nicht gefunden! Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Manager initialisieren
        this.rewardsConfig = new DailyRewardsConfig(this);
        this.dailyManager = new DailyManager(this);

        // Listener registrieren
        getServer().getPluginManager().registerEvents(new DailyListener(this), this);

        // Command registrieren
        DailyCommand cmd = new DailyCommand(this);
        getCommand("daily").setExecutor(cmd);
        getCommand("daily").setTabCompleter(cmd);

        getLogger().info("hmyDailyRewards erfolgreich aktiviert!");
    }

    @Override
    public void onDisable() {
        if (dailyManager != null) {
            dailyManager.shutdown();
        }
        getLogger().info("hmyDailyRewards deaktiviert.");
    }

    public static HmyDailyRewards getInstance() {
        return instance;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public DailyManager getDailyManager() {
        return dailyManager;
    }

    public DailyRewardsConfig getRewardsConfig() {
        return rewardsConfig;
    }
}
