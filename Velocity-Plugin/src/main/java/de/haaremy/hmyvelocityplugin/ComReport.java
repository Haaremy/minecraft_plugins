package de.haaremy.hmyvelocityplugin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * /report <player> <reason...>
 * Saves to data/reports.log and notifies online admins (hmy.report.admin).
 */
public class ComReport implements SimpleCommand {

    private static final String PREFIX = "§8[§cReport§8] ";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ProxyServer server;
    private final Logger      logger;
    private final Path        reportsFile;

    public ComReport(ProxyServer server, Logger logger, Path dataDirectory) {
        this.server      = server;
        this.logger      = logger;
        this.reportsFile = dataDirectory.resolve("reports.log");
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player reporter)) {
            source.sendMessage(Component.text("§cNur Spieler können melden."));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 2) {
            reporter.sendMessage(Component.text("§cVerwendung: §e/report <Spieler> <Grund>"));
            return;
        }

        String targetName = args[0];
        Optional<Player> target = server.getPlayer(targetName);

        if (target.isEmpty()) {
            reporter.sendMessage(Component.text(PREFIX + "§c" + targetName + " §7ist nicht online."));
            return;
        }
        if (target.get().getUniqueId().equals(reporter.getUniqueId())) {
            reporter.sendMessage(Component.text(PREFIX + "§cDu kannst dich nicht selbst melden."));
            return;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        String server  = target.get().getCurrentServer()
                .map(s -> s.getServerInfo().getName()).orElse("?");
        String timestamp = LocalDateTime.now().format(FMT);

        String logLine = "[" + timestamp + "] " + reporter.getUsername()
                + " meldete " + targetName + " auf " + server + ": " + reason;

        // Persist to file
        try {
            Files.writeString(reportsFile, logLine + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("Report konnte nicht gespeichert werden: ", e);
        }

        // Notify reporter
        reporter.sendMessage(Component.text(PREFIX + "§aDu hast §e" + targetName
                + " §awegen §f\"" + reason + "\" §agemeldet."));

        // Notify admins
        Component adminMsg = Component.text(PREFIX + "§e" + reporter.getUsername()
                + " §7meldet §c" + targetName + " §7[§b" + server + "§7]: §f" + reason);
        for (Player p : this.server.getAllPlayers()) {
            if (p.hasPermission("hmy.report.admin")) {
                p.sendMessage(adminMsg);
            }
        }

        logger.info(logLine);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 1) {
            String prefix = invocation.arguments()[0].toLowerCase();
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(n -> n.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
