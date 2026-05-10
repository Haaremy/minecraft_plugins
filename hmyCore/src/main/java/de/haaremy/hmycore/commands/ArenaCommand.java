package de.haaremy.hmycore.commands;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.arena.Arena;
import de.haaremy.hmycore.lang.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArenaCommand implements CommandExecutor {

    private final HmyCore plugin;

    public ArenaCommand(HmyCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Lang.send(sender, "core.player_only");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> handleCreate(player, args);
            case "delete" -> handleDelete(player, args);
            case "list" -> handleList(player);
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player);
            case "start" -> handleStart(player, args);
            default -> sendUsage(player);
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("hmy.arena.admin")) {
            Lang.send(player, "core.no_permission");
            return;
        }
        if (args.length < 3) {
            Lang.send(player, "core.arena.create.usage");
            return;
        }

        String name = args[1];
        String gameType = args[2];
        String worldName = player.getWorld().getName();

        Arena arena = plugin.getArenaManager().createArena(name, worldName, gameType);
        arena.addSpawnPoint(player.getLocation());
        plugin.getArenaManager().saveArena(arena);

        Lang.send(player, "core.arena.create.ok",
                "name", name,
                "world", worldName,
                "gameType", gameType);
    }

    private void handleDelete(Player player, String[] args) {
        if (!player.hasPermission("hmy.arena.admin")) {
            Lang.send(player, "core.no_permission");
            return;
        }
        if (args.length < 2) {
            Lang.send(player, "core.arena.delete.usage");
            return;
        }

        String name = args[1];
        if (plugin.getArenaManager().deleteArena(name)) {
            Lang.send(player, "core.arena.delete.ok", "name", name);
        } else {
            Lang.send(player, "core.arena.notfound");
        }
    }

    private void handleList(Player player) {
        List<Arena> arenas = plugin.getArenaManager().getArenas();
        if (arenas.isEmpty()) {
            Lang.send(player, "core.arena.list.empty");
            return;
        }

        Lang.send(player, "core.arena.list.header");
        for (Arena arena : arenas) {
            Lang.send(player, "core.arena.list.entry",
                    "name", arena.getName(),
                    "gameType", arena.getGameType(),
                    "state", arena.getState().name(),
                    "count", String.valueOf(arena.getPlayerCount()),
                    "max", String.valueOf(arena.getMaxPlayers()));
        }
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            Lang.send(player, "core.arena.join.usage");
            return;
        }

        String name = args[1];
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            Lang.send(player, "core.arena.notfound");
            return;
        }

        if (plugin.getArenaManager().joinPlayer(arena, player.getUniqueId())) {
            Lang.send(player, "core.arena.join.ok",
                    "name", arena.getName(),
                    "count", String.valueOf(arena.getPlayerCount()),
                    "max", String.valueOf(arena.getMaxPlayers()));
        } else {
            Lang.send(player, "core.arena.join.fail");
        }
    }

    private void handleLeave(Player player) {
        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (plugin.getArenaManager().leavePlayer(arena, player.getUniqueId())) {
                Lang.send(player, "core.arena.leave.ok", "name", arena.getName());
                return;
            }
        }
        Lang.send(player, "core.arena.leave.notin");
    }

    private void handleStart(Player player, String[] args) {
        if (!player.hasPermission("hmy.arena.admin")) {
            Lang.send(player, "core.no_permission");
            return;
        }
        if (args.length < 2) {
            Lang.send(player, "core.arena.start.usage");
            return;
        }

        String name = args[1];
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            Lang.send(player, "core.arena.notfound");
            return;
        }

        if (arena.getState() != de.haaremy.hmycore.arena.ArenaState.WAITING) {
            Lang.send(player, "core.arena.start.notwaiting");
            return;
        }

        if (plugin.getArenaManager().changeState(arena, de.haaremy.hmycore.arena.ArenaState.STARTING)) {
            Lang.send(player, "core.arena.start.ok", "name", arena.getName());
        } else {
            Lang.send(player, "core.arena.start.rejected");
        }
    }

    private void sendUsage(Player player) {
        Lang.send(player, "core.arena.usage.header");
        Lang.send(player, "core.arena.usage.create");
        Lang.send(player, "core.arena.usage.delete");
        Lang.send(player, "core.arena.usage.list");
        Lang.send(player, "core.arena.usage.join");
        Lang.send(player, "core.arena.usage.leave");
        Lang.send(player, "core.arena.usage.start");
    }
}
