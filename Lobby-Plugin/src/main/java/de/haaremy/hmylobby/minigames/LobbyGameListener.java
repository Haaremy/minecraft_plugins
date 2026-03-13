package de.haaremy.hmylobby.minigames;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Behandelt Schilder und Block-Interaktionen für Lobby-Minispiele.
 *
 * Schilder-Format (Spieler schreibt):
 *   Zeile 0: [g: TikTakToe]
 *   Zeile 1: Anzeigename
 *   Zeile 2: Feld-ID
 *
 * Nach Registrierung:
 *   Zeile 0: §6§l[TicTacToe]
 *   Zeile 1: Anzeigename
 *   Zeile 2: Status
 */
public class LobbyGameListener implements Listener {

    private final LobbyGameManager gameManager;

    public LobbyGameListener(LobbyGameManager gameManager) {
        this.gameManager = gameManager;
    }

    // ── Schild beschreiben ────────────────────────────────────────────────────

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String line0 = event.line(0) == null ? ""
                : LegacyComponentSerializer.legacySection().serialize(event.line(0)).trim();

        if (!line0.equalsIgnoreCase("[g: TikTakToe]") && !line0.equalsIgnoreCase("[g: tiktaktoe]")) return;

        String displayName = event.line(1) == null ? "" :
                LegacyComponentSerializer.legacySection().serialize(event.line(1)).trim();
        String fieldId = event.line(2) == null ? "" :
                LegacyComponentSerializer.legacySection().serialize(event.line(2)).trim();

        TicTacToeGame game = gameManager.getGameByFieldId(fieldId);
        String status = (game != null) ? game.getStatusText() : "§aFrei";

        event.line(0, Component.text("§6§l[TicTacToe]"));
        event.line(1, Component.text("§f" + displayName));
        event.line(2, Component.text(status));
        event.line(3, Component.text("§8Feld: §7" + fieldId));

        if (event.getPlayer().hasPermission("hmy.lobby.gamecreator")) {
            event.getPlayer().sendMessage(Component.text("§aTicTacToe-Schild für Feld §e" + fieldId + " §aregistriert."));
        }
    }

    // ── Schild anklicken → Spiel beitreten ───────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();

        // Sign click – join game
        if (event.getClickedBlock().getState() instanceof Sign sign) {
            String line0 = LegacyComponentSerializer.legacySection()
                    .serialize(sign.getSide(org.bukkit.block.sign.Side.FRONT).line(0)).trim();
            if (!line0.contains("TicTacToe")) return;

            String line3 = LegacyComponentSerializer.legacySection()
                    .serialize(sign.getSide(org.bukkit.block.sign.Side.FRONT).line(3)).trim();
            // Parse field ID from "§8Feld: §7<id>"
            String fieldId = line3.replaceAll("§[0-9a-fk-or]", "").replace("Feld: ", "").trim();

            TicTacToeGame game = gameManager.getGameByFieldId(fieldId);
            if (game == null) {
                player.sendMessage(Component.text("§cDieses Spiel existiert nicht (mehr). Feld-ID: §e" + fieldId));
                return;
            }

            event.setCancelled(true);
            if (!game.join(player)) {
                player.sendMessage(Component.text("§cDu kannst diesem Spiel gerade nicht beitreten. ("
                        + game.getStatusText() + "§c)"));
            }
            updateSign(sign, game);
            return;
        }

        // Board cell click
        TicTacToeGame game = gameManager.getGameContainingCell(event.getClickedBlock().getLocation());
        if (game == null) return;

        event.setCancelled(true);
        int cellIdx = game.getCellIndex(event.getClickedBlock().getLocation());
        if (cellIdx != -1) {
            game.makeMove(player, cellIdx);
        }
    }

    // ── Mode-Selection Inventory ──────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        if (!title.contains("TicTacToe") || !title.contains("Modus")) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        if (!event.getCurrentItem().hasItemMeta()) return;

        String name = LegacyComponentSerializer.legacySection()
                .serialize(event.getCurrentItem().getItemMeta().displayName());

        // Find the game this player is in (choosing mode)
        for (TicTacToeGame game : gameManager.getAllTTTGames()) {
            if (game.getState() == TicTacToeGame.State.CHOOSING && player.equals(game.getPlayer1())) {
                player.closeInventory();
                if (name.contains("Singleplayer")) {
                    game.chooseSinglePlayer();
                } else if (name.contains("Multiplayer")) {
                    game.chooseMultiplayer();
                }
                return;
            }
        }
    }

    // ── Sign update helper ────────────────────────────────────────────────────

    private void updateSign(Sign sign, TicTacToeGame game) {
        String name = LegacyComponentSerializer.legacySection()
                .serialize(sign.getSide(org.bukkit.block.sign.Side.FRONT).line(1)).trim();
        sign.getSide(org.bukkit.block.sign.Side.FRONT).line(2, Component.text(game.getStatusText()));
        sign.update();
    }
}
