package de.haaremy.hmypaper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.luckperms.api.LuckPerms;

public class HmyPaperPlugin extends JavaPlugin {

    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        getLogger().info("hmyPaper Plugin wird aktiviert...");

        // LuckPerms-Integration
        if (!setupLuckPerms()) {
            getLogger().severe("LuckPerms konnte nicht geladen werden! Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Befehle und Events registrieren
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "hmy:trigger");
        registerCommands();
        registerEvents();

        getLogger().info("Alle Funktionen wurden erfolgreich aktiviert!");
    }

    @Override
    public void onDisable() {
        getLogger().info("hmyPaper Plugin wird deaktiviert!");
    }

    private boolean setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
            getLogger().info("LuckPerms erfolgreich eingebunden!");
            return true;
        }
        return false;
    }

    private void registerCommands() {

        registerCommand("triggervelocity", this);

        //lobby & /hmy server sind in hmyVelocity registriert
        // Basics
        registerCommand("help", new ComHelp());
        registerCommand("rules", new ComRules());
        registerCommand("spawn", new ComSpawn());

        // World Commands
        registerCommand("fly", new ComFly());
        registerCommand("weather", new ComWeather());
        registerCommand("gm", new ComGamemode());
        registerCommand("time", new ComTime());
        registerCommand("lightning", new ComLightning());
        registerCommand("speed", new ComSpeed());
        registerCommand("skull", new ComSkull());
        registerCommand("getpos", new ComGetPos());
        registerCommand("kill", new ComKill());
        registerCommand("invsee", new ComInvSee());
        registerCommand("vanish", new ComVanish());
        registerCommand("sudo", new ComSudo());
        registerCommand("give", new ComGive());

        // Chat Commands
        registerCommand("mute", new ComMute());
        registerCommand("dm", new ComDirectMessage());
        registerCommand("broadcast", new ComBroadcast());
        registerCommand("r", new ComReply());
        registerCommand("socialspy", new ComSocialSpy());


        

    }

    private void registerEvents() {
        // Spieler-Events
        getServer().getPluginManager().registerEvents(new HmySpawn(), this);

        // Chat-Events
        getServer().getPluginManager().registerEvents(new HmyChat(luckPerms), this);

        // Tab-Events
        HmyTab hmyTab = new HmyTab(luckPerms);
        getServer().getPluginManager().registerEvents(hmyTab, this);

        // Tab-Liste regelmäßig aktualisieren
        hmyTab.runTaskTimer(this, 0, 20);
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().severe("Fehler beim Registrieren des Befehls: " + name);
        }
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

     @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("triggervelocity")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Nur Spieler können diesen Befehl ausführen!");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage("Verwendung: /triggervelocity <Befehl> <Argumente>");
                return true;
            }

            Player player = (Player) sender;
            String velocityCommand = String.join(" ", args);

            // Nachricht an Velocity senden
            try {
                ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteArray);

                out.writeUTF(player.getName()); // Spielername
                out.writeUTF(velocityCommand);  // Der auszuführende Befehl

                player.sendPluginMessage(this, "hmy:trigger", byteArray.toByteArray());

            } catch (Exception e) {
                player.sendMessage("Fehler beim Senden der Nachricht!");
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
}
