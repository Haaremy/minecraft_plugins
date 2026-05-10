package de.haaremy.hmynavigator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

public class HmyNavigator extends JavaPlugin {

    private static HmyNavigator instance;
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    public static final Component COMPASS_DISPLAY_NAME = MINI.deserialize(
            "<gradient:#ff6b35:#ff8c61>Spielmodi</gradient> <gray>(Rechtsklick)</gray>"
    );

    @Override
    public void onEnable() {
        instance = this;

        // Config speichern
        saveDefaultConfig();

        // BungeeCord Plugin Messaging Channel registrieren
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Listener registrieren
        NavigatorConfig config = new NavigatorConfig(this);
        ServerConnector connector = new ServerConnector(this, config);
        NavigatorGui gui = new NavigatorGui(config);

        getServer().getPluginManager().registerEvents(new NavigatorListener(this, gui, connector, config), this);

        // Commands registrieren
        NavigatorCommand command = new NavigatorCommand(gui, connector);
        getCommand("navigator").setExecutor(command);
        getCommand("play").setExecutor(command);

        getLogger().info("hmyNavigator aktiviert!");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
        getLogger().info("hmyNavigator deaktiviert!");
    }

    public static HmyNavigator getInstance() {
        return instance;
    }
}
