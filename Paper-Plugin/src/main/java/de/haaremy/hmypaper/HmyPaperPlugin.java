package de.haaremy.hmypaper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Path;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import net.luckperms.api.LuckPerms;

import de.haaremy.hmypaper.commands.ComBroadcast;
import de.haaremy.hmypaper.commands.ComDirectMessage;
import de.haaremy.hmypaper.commands.ComFly;
import de.haaremy.hmypaper.commands.ComGamemode;
import de.haaremy.hmypaper.commands.ComGetPos;
import de.haaremy.hmypaper.commands.ComGive;
import de.haaremy.hmypaper.commands.ComHelp;
import de.haaremy.hmypaper.commands.ComInvSee;
import de.haaremy.hmypaper.commands.ComKill;
import de.haaremy.hmypaper.commands.ComLightning;
import de.haaremy.hmypaper.commands.ComMute;
import de.haaremy.hmypaper.commands.ComReply;
import de.haaremy.hmypaper.commands.ComRules;
import de.haaremy.hmypaper.commands.ComSkull;
import de.haaremy.hmypaper.commands.ComSocialSpy;
import de.haaremy.hmypaper.commands.ComSpawn;
import de.haaremy.hmypaper.commands.ComSpeed;
import de.haaremy.hmypaper.commands.ComSudo;
import de.haaremy.hmypaper.commands.ComTime;
import de.haaremy.hmypaper.commands.ComVanish;
import de.haaremy.hmypaper.commands.ComWeather;
import de.haaremy.hmypaper.commands.ComWorld;


public class HmyPaperPlugin extends JavaPlugin {

    private LuckPerms luckPerms;
    private HmyLanguageManager language;
    private HmyConfigManager configManager;



    @Override
    public void onEnable() {
        getLogger().info("hmyPaper Plugin wird aktiviert...");

        if (!setupLuckPerms()) {
            getLogger().severe("LuckPerms konnte nicht geladen werden! Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        var logger = getLogger();
        logger.info("Haaremy: hmyPaper Plugin wird aktiviert...");

        // Datenverzeichnis und Konfigurationsmanager initialisieren
        Path dataDirectory = getDataFolder().toPath().getParent();
        this.configManager = new HmyConfigManager(logger,dataDirectory);
        logger.info("Haaremy: Paper Config mit initialisiert.");
        this.language = new HmyLanguageManager(logger, dataDirectory, configManager, luckPerms);
        logger.info("Haaremy: Paper Sprachen initialisiert.");

        // PluginChannel registrieren
        getServer().getMessenger().registerOutgoingPluginChannel(this, "hmy:trigger");

        // Befehle und Events registrieren
        registerCommands();
        registerEvents();

        getLogger().info("Haaremy: Alle Paper Funktionen wurden erfolgreich aktiviert!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Haaremy: hmyPaper Plugin wird deaktiviert!");
    }

    private boolean setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
            getLogger().info("Haaremy: LuckPerms erfolgreich eingebunden!");
            return true;
        }
        return false;
    }

    private void registerCommands() {
        registerCommand("triggervelocity", this);
        
        List<String> helpPages = loadHelpPagesFromConfig();
        String helpTitle = "§6Server Hilfe"; // Standardwerte, falls Config leer
        String helpAuthor = "Haaremy";

        // Basics
        registerCommand("help", new ComHelp(helpPages, helpTitle, helpAuthor));
        registerCommand("rules", new ComRules());
        registerCommand("spawn", new ComSpawn());

        // World Commands
        registerCommand("world", new ComWorld(language));
        registerCommand("fly", new ComFly(language));
        registerCommand("weather", new ComWeather());
        registerCommand("gm", new ComGamemode(language));
        registerCommand("time", new ComTime());
        registerCommand("lightning", new ComLightning());
        registerCommand("speed", new ComSpeed());
        registerCommand("skull", new ComSkull());
        registerCommand("getpos", new ComGetPos(language));
        registerCommand("kill", new ComKill());
        registerCommand("invsee", new ComInvSee());
        registerCommand("vanish", new ComVanish());
        registerCommand("sudo", new ComSudo());
        registerCommand("give", new ComGive());

        // Chat Commands
        registerCommand("mute", new ComMute());
        registerCommand("dm", new ComDirectMessage(language));
        registerCommand("broadcast", new ComBroadcast(language));
        registerCommand("r", new ComReply(language));
        registerCommand("socialspy", new ComSocialSpy());
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new HmySpawn(this), this);
        getServer().getPluginManager().registerEvents(new HmyAntiBuild(this, luckPerms), this);
        getServer().getPluginManager().registerEvents(new HmyChat(luckPerms), this);

        HmyTab hmyTab = new HmyTab(luckPerms);
        getServer().getPluginManager().registerEvents(hmyTab, this);
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("triggervelocity")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cNur Spieler können diesen Befehl ausführen!");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage("§eVerwendung: /triggervelocity <Befehl> <Argumente>");
                return true;
            }

            Player player = (Player) sender;
            String velocityCommand = String.join(" ", args);

            try {
                ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteArray);

                out.writeUTF(player.getName());
                out.writeUTF(velocityCommand);

                player.sendPluginMessage(this, "hmy:trigger", byteArray.toByteArray());
                player.sendMessage("§aNachricht an Velocity gesendet!");

            } catch (Exception e) {
                player.sendMessage("§cFehler beim Senden der Nachricht!");
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
    
    
 // Hilfsmethode zum Laden der Buchseiten
    private List<String> loadHelpPagesFromConfig() {
        // Hier greifen wir auf die Datei zu, die du auch in HmyAntiBuild nutzt
        java.io.File configFile = new java.io.File(getDataFolder().getParentFile(), "hmySettings/hmyServer.conf");
        if (configFile.exists()) {
            try {
                String data = String.join("", java.nio.file.Files.readAllLines(configFile.toPath())).trim();
                // Wir nutzen dein bestehendes extractGlobalSection System (angepasst auf helpbook_pages)
                // In der Config: helpbook_pages = {"Seite 1", "Seite 2"}
                return extractList(data, "helpbook_pages");
            } catch (Exception e) {
                getLogger().warning("Konnte Hilfe-Seiten nicht laden: " + e.getMessage());
            }
        }
        return List.of("§cKeine Hilfe-Seiten konfiguriert.");
    }

    // Kleine Utility zum Parsen einfacher Listen
    private List<String> extractList(String data, String section) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b" + section + "\\s*=\\s*\\{([^}]*)}");
        java.util.regex.Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            if (!value.isEmpty()) {
                return java.util.Arrays.asList(value.replace("\"", "").split("\\s*,\\s*"));
            }
        }
        return List.of();
    }
}
