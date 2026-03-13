package de.haaremy.hmylobby.minigames;

import de.haaremy.hmylobby.LotteryCrateListener;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Verwaltet alle aktiven TicTacToe-Spiele und Lottery-Kisten; behandelt den /lobbygame-Befehl.
 */
public class LobbyGameManager implements CommandExecutor {

    private final JavaPlugin            plugin;
    private final LobbyGamesConfig      config;
    private final LobbyGameSelector     selector;
    private final LotteryCrateListener  crateListener;
    private final List<TicTacToeGame>   activeTTTGames = new ArrayList<>();

    public LobbyGameManager(JavaPlugin plugin, LobbyGamesConfig config,
                            LobbyGameSelector selector, LotteryCrateListener crateListener) {
        this.plugin        = plugin;
        this.config        = config;
        this.selector      = selector;
        this.crateListener = crateListener;
        loadGamesFromConfig();
    }

    private void loadGamesFromConfig() {
        for (LobbyGamesConfig.TicTacToeField field : config.getTicTacToeFields()) {
            activeTTTGames.add(new TicTacToeGame(
                    field.fieldId(), field.name(), field.corner1(), field.corner2(), plugin));
        }
        plugin.getLogger().info("LobbyGameManager: " + activeTTTGames.size() + " TicTacToe-Felder geladen.");
    }

    // ── Command /lobbygame ────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("§cNur Spieler können diesen Befehl nutzen."));
            return true;
        }
        if (!player.hasPermission("hmy.lobby.gamecreator")) {
            player.sendMessage(Component.text("§cKeine Berechtigung. §8(hmy.lobby.gamecreator)"));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("create")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("tiktaktoe") && args.length >= 4) {
                createTicTacToe(player, args[2], args[3]);
                return true;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("crate")) {
                createCrate(player);
                return true;
            }
        }

        player.sendMessage(Component.text("§cVerwendung:"));
        player.sendMessage(Component.text("§e/lobbygame create tiktaktoe <name> <feld-id>"));
        player.sendMessage(Component.text("§e/lobbygame create crate §7(Truhe anvisieren)"));
        return true;
    }

    private void createCrate(Player player) {
        org.bukkit.block.Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(Component.text("§cKeine Truhe in Reichweite (max. 5 Blöcke)."));
            return;
        }
        if (crateListener.tagCrate(target)) {
            player.sendMessage(Component.text("§aTruhe erfolgreich als §6Lotterie-Kiste §amarkiert!"));
            player.sendMessage(Component.text("§7Spieler können die Kiste jetzt rechtsklicken."));
        } else {
            player.sendMessage(Component.text("§cDer anvisierte Block ist keine Truhe!"));
        }
    }

    private void createTicTacToe(Player player, String name, String fieldId) {
        Location[] sel = selector.getSelection(player);
        if (sel[0] == null || sel[1] == null) {
            player.sendMessage(Component.text("§cBitte zuerst zwei Punkte mit dem §eGoldenen Schwert §cmarkieren."));
            player.sendMessage(Component.text("§7Linksklick = Punkt 1, Rechtsklick = Punkt 2."));
            return;
        }
        if (!sel[0].getWorld().equals(sel[1].getWorld())) {
            player.sendMessage(Component.text("§cBeide Punkte müssen in der gleichen Welt sein!"));
            return;
        }

        // Validate 3x3 dimensions
        int dx = Math.abs(sel[0].getBlockX() - sel[1].getBlockX());
        int dz = Math.abs(sel[0].getBlockZ() - sel[1].getBlockZ());
        if ((dx != 2 && dx != 0) || (dz != 2 && dz != 0)) {
            player.sendMessage(Component.text("§cWarnung: §7Die markierte Fläche ist nicht genau 3x3 Blöcke groß (dx=" + dx + ", dz=" + dz + ")."));
            player.sendMessage(Component.text("§7Das Spiel wird trotzdem erstellt, aber die Zellen könnten falsch platziert sein."));
        }

        config.saveField("tiktaktoe", fieldId, name, sel[0], sel[1]);
        TicTacToeGame game = new TicTacToeGame(fieldId, name, sel[0], sel[1], plugin);
        activeTTTGames.add(game);
        selector.clearSelection(player);

        player.sendMessage(Component.text("§aTicTacToe-Feld §e" + name + " §8(ID: " + fieldId + ")§a erstellt!"));
        player.sendMessage(Component.text("§7Platziere ein Schild mit §e[g: TikTakToe] §7in der ersten Zeile zum Beitreten."));
    }

    // ── Game access ───────────────────────────────────────────────────────────

    public TicTacToeGame getGameByFieldId(String fieldId) {
        return activeTTTGames.stream()
                .filter(g -> g.getFieldId().equals(fieldId))
                .findFirst().orElse(null);
    }

    public List<TicTacToeGame> getAllTTTGames() {
        return activeTTTGames;
    }

    public TicTacToeGame getGameContainingCell(Location loc) {
        for (TicTacToeGame game : activeTTTGames) {
            if (game.getCellIndex(loc) != -1) return game;
        }
        return null;
    }
}
