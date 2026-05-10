package de.haaremy.hmy1v1;

import de.haaremy.hmycore.HmyCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

public class Hmy1v1 extends JavaPlugin {

    private static Hmy1v1 instance;

    private DuelManager duelManager;
    private DuelElo duelElo;
    private DuelArena duelArena;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        instance = this;

        // hmyCore pruefen
        if (HmyCore.getInstance() == null) {
            getLogger().severe("hmyCore nicht gefunden! Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Manager initialisieren
        this.duelElo = new DuelElo(this);
        this.duelArena = new DuelArena(this);
        this.duelManager = new DuelManager(this);

        // Listener registrieren
        getServer().getPluginManager().registerEvents(new DuelListener(this), this);

        // Command registrieren
        DuelCommand cmd = new DuelCommand(this);
        getCommand("duel").setExecutor(cmd);
        getCommand("duel").setTabCompleter(cmd);

        getLogger().info("hmy1v1 erfolgreich aktiviert!");
    }

    @Override
    public void onDisable() {
        if (duelManager != null) {
            duelManager.shutdown();
        }
        if (duelElo != null) {
            duelElo.shutdown();
        }
        getLogger().info("hmy1v1 deaktiviert.");
    }

    public static Hmy1v1 getInstance() {
        return instance;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public DuelElo getDuelElo() {
        return duelElo;
    }

    public DuelArena getDuelArena() {
        return duelArena;
    }
}
