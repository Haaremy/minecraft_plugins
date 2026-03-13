package de.haaremy.hmyvelocityplugin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.haaremy.hmyvelocityplugin.economy.ComCoins;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Einheitlicher /hmy-Befehl. Routet Unterkommandos:
 *   /hmy language <lang>
 *   /hmy coins [give <player> <amount>]
 */
public class ComHmy implements SimpleCommand {

    private final ComHmyLanguage languageCmd;
    private final ComCoins        coinsCmd;

    public ComHmy(ComHmyLanguage languageCmd, ComCoins coinsCmd) {
        this.languageCmd = languageCmd;
        this.coinsCmd    = coinsCmd;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            source.sendMessage(Component.text("§cVerwendung: §e/hmy <language|coins> [...]"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "language" -> languageCmd.execute(buildSubInvocation(invocation, args));
            case "coins"    -> coinsCmd.execute(buildSubInvocation(invocation, args));
            default -> source.sendMessage(Component.text("§cUnbekanntes Unterkommando. Verfügbar: §elanguage§c, §ecoins"));
        }
    }

    /** Builds a forwarded Invocation with args[1..] so sub-commands see their own args. */
    private Invocation buildSubInvocation(Invocation parent, String[] args) {
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return new Invocation() {
            @Override public CommandSource source()    { return parent.source(); }
            @Override public String alias()            { return parent.alias(); }
            @Override public String[] arguments()      { return subArgs; }
        };
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) return List.of("language", "coins");
        if (args[0].equalsIgnoreCase("language")) return List.of("de", "en");
        if (args[0].equalsIgnoreCase("coins"))    return List.of("give");
        return List.of();
    }
}
