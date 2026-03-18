package de.haaremy.hmypaper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Path;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import net.luckperms.api.LuckPerms;

import de.haaremy.hmypaper.commands.ComBack;
import de.haaremy.hmypaper.commands.ComBroadcast;
import de.haaremy.hmypaper.commands.ComDirectMessage;
import de.haaremy.hmypaper.commands.ComEnderChest;
import de.haaremy.hmypaper.commands.ComFeed;
import de.haaremy.hmypaper.commands.ComFly;
import de.haaremy.hmypaper.commands.ComGamemode;
import de.haaremy.hmypaper.commands.ComGetPos;
import de.haaremy.hmypaper.commands.ComGive;
import de.haaremy.hmypaper.commands.ComHeal;
import de.haaremy.hmypaper.commands.ComHelp;
import de.haaremy.hmypaper.commands.ComInvSee;
import de.haaremy.hmypaper.commands.ComKill;
import de.haaremy.hmypaper.commands.ComLightning;
import de.haaremy.hmypaper.commands.ComMute;
import de.haaremy.hmypaper.commands.ComReply;
import de.haaremy.hmypaper.commands.ComParkour;
import de.haaremy.hmypaper.commands.ComRepair;
import de.haaremy.hmypaper.commands.ComRules;
import de.haaremy.hmypaper.commands.ComSetHome;
import de.haaremy.hmypaper.commands.ComHome;
import de.haaremy.hmypaper.commands.ComWorlds;
import de.haaremy.hmypaper.parkour.ParkourListener;
import de.haaremy.hmypaper.parkour.ParkourManager;
import de.haaremy.hmypaper.commands.ComSkull;
import de.haaremy.hmypaper.commands.ComSocialSpy;
import de.haaremy.hmypaper.commands.ComSpawn;
import de.haaremy.hmypaper.commands.ComSpeed;
import de.haaremy.hmypaper.commands.ComSudo;
import de.haaremy.hmypaper.commands.ComTime;
import de.haaremy.hmypaper.commands.ComTp;
import de.haaremy.hmypaper.commands.ComTpHere;
import de.haaremy.hmypaper.commands.ComVanish;
import de.haaremy.hmypaper.commands.ComWeather;
import de.haaremy.hmypaper.commands.ComWorkbench;
import de.haaremy.hmypaper.commands.ComWorld;


public class HmyPaperPlugin extends JavaPlugin {

    private LuckPerms luckPerms;
    private HmyLanguageManager language;
    private HmyConfigManager configManager;
    private HomeManager homeManager;
    private ParkourManager parkourManager;
    private ParkourListener parkourListener;



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

        // pluginsDir = minecraftServers/subserver/plugins/
        // HmyConfigManager berechnet daraus: pluginsDir/../../hmySettings = minecraftServers/hmySettings/
        Path pluginsDir = getDataFolder().toPath().toAbsolutePath().getParent();
        this.configManager   = new HmyConfigManager(logger, pluginsDir);
        this.homeManager     = new HomeManager(getDataFolder(), logger);
        this.parkourManager  = new ParkourManager(getDataFolder(), logger);
        this.parkourListener = new ParkourListener(parkourManager);
        logger.info("Haaremy: Paper Config initialisiert.");
        this.language = new HmyLanguageManager(logger, pluginsDir, configManager, luckPerms);
        this.language.loadAllLanguageFiles();
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

        List<String> helpPages  = configManager.getHelpBookPages();
        String       helpTitle  = configManager.getHelpBookTitle();
        String       helpAuthor = configManager.getHelpBookAuthor();

        // Basics
        registerCommand("help", new ComHelp(helpPages, helpTitle, helpAuthor));
        registerCommand("rules", new ComRules());
        registerCommand("spawn", new ComSpawn());

        // World Commands
        registerCommand("world", new ComWorld(language));
        registerCommand("fly", new ComFly(language));
        registerCommand("weather", new ComWeather());
        registerCommand("gm", new ComGamemode());
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

        // Essentials Commands
        registerCommand("heal", new ComHeal());
        registerCommand("feed", new ComFeed());
        registerCommand("tp", new ComTp());
        registerCommand("tphere", new ComTpHere());
        ComBack comBack = new ComBack();
        registerCommand("back", comBack);
        registerCommand("workbench", new ComWorkbench());
        registerCommand("enderchest", new ComEnderChest());
        registerCommand("repair", new ComRepair());
        getServer().getPluginManager().registerEvents(comBack, this);

        // Home / Worlds / Parkour
        registerCommand("sethome", new ComSetHome(homeManager));
        registerCommand("home",    new ComHome(homeManager));
        registerCommand("worlds",  new ComWorlds());
        registerCommand("parkour", new ComParkour(parkourManager, parkourListener));
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new HmySpawn(this), this);
        getServer().getPluginManager().registerEvents(new HmyAntiBuild(this, luckPerms), this);
        getServer().getPluginManager().registerEvents(new HmyChat(luckPerms), this);
        getServer().getPluginManager().registerEvents(parkourListener, this);

        // Tab-Liste wird von hmyVelocity zentral verwaltet (VelocityTabManager)
        // HmyTab ist deaktiviert.
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            if (executor instanceof TabCompleter tc) {
                command.setTabCompleter(tc);
            }
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

    public HmyConfigManager getConfigManager()   { return configManager; }
    public HmyLanguageManager getLanguageManager() { return language; }
}
