package de.haaremy.hmyvelocityplugin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;

public class BroadcastC implements SimpleCommand {

    private final ProxyServer proxy;

    public BroadcastC(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            source.sendMessage(Component.text("§cBenutzung: /broadcast [proxy|server|world] <Nachricht>"));
            return;
        }

        String target = args[0].toLowerCase();
        String message = String.join(" ", args).substring(target.length()).trim();

        switch (target) {
            case "proxy":
                proxy.getAllPlayers().forEach(player -> player.sendMessage(Component.text("§e[Broadcast] §r" + message)));
                proxy.getConsoleCommandSource().sendMessage(Component.text("§e[Broadcast] §r" + message));
                break;

            case "server":
                if (source instanceof Player) {
                    Player player = (Player) source;
                    player.getCurrentServer().ifPresent(server -> server.getServer().sendMessage(Component.text("§e[Broadcast] §r" + message)));
                } else {
                    source.sendMessage(Component.text("§cDieser Befehl kann nur von einem Spieler ausgeführt werden."));
                }
                break;


            default:
                source.sendMessage(Component.text("§cUngültiges Ziel. Benutze proxy, server oder world."));
                break;
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("hmy.broadcast");
    }
}
