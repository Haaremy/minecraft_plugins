package de.haaremy.hmy1v1;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class DuelListener implements Listener {

    private final Hmy1v1 plugin;

    public DuelListener(Hmy1v1 plugin) {
        this.plugin = plugin;
    }

    /**
     * Schaden nur zwischen Duell-Spielern im gleichen Spiel erlauben.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        UUID victimUuid = victim.getUniqueId();
        UUID attackerUuid = attacker.getUniqueId();
        DuelManager manager = plugin.getDuelManager();

        if (manager.isInGame(victimUuid) || manager.isInGame(attackerUuid)) {
            DuelGame victimGame = manager.getGame(victimUuid);
            DuelGame attackerGame = manager.getGame(attackerUuid);

            // Beide muessen im gleichen Spiel sein
            if (victimGame == null || attackerGame == null || victimGame != attackerGame) {
                event.setCancelled(true);
                return;
            }

            // Nicht waehrend Countdown angreifen
            if (victimGame.getState() != DuelGame.GameState.FIGHTING) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Spieler-Tod: Duell-Ende, kein Item-Drop, sofort Respawn.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        DuelManager manager = plugin.getDuelManager();

        if (!manager.isInGame(uuid)) return;

        DuelGame game = manager.getGame(uuid);
        if (game == null) return;

        // Kein Item-Drop
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Death-Message unterdruecken
        event.deathMessage(null);

        // Sofort respawnen (Scheduler da Respawn nicht im selben Tick moeglich)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.spigot().respawn();
        }, 1L);

        // Duell-Ende
        game.onPlayerDeath(player);
    }

    /**
     * Spieler verlaesst den Server.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        DuelManager manager = plugin.getDuelManager();

        // Aus Queue entfernen (still)
        if (manager.isInQueue(uuid)) {
            manager.leaveQueue(player);
            return;
        }

        // Zuschauer entfernen
        DuelGame spectatingGame = manager.getSpectatingGame(uuid);
        if (spectatingGame != null) {
            spectatingGame.removeSpectator(player);
            return;
        }

        // Aus Spiel entfernen
        if (manager.isInGame(uuid)) {
            DuelGame game = manager.getGame(uuid);
            if (game != null) {
                game.onPlayerQuit(player);
            }
        }
    }

    /**
     * Respawn: Teleport zum Spawn.
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Wenn der Spieler in einem Duell war, wird er per DuelGame.endGame zur Lobby gesendet
        // Hier kein extra Handling noetig
    }

    /**
     * Hunger deaktivieren fuer Duell-Spieler.
     */
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (plugin.getDuelManager().isInGame(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Soup-Kit: Rechtsklick mit Pilzsuppe heilt 6 Herzen (12 HP), Suppe wird verbraucht.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        DuelManager manager = plugin.getDuelManager();

        if (!manager.isInGame(uuid)) return;

        DuelGame game = manager.getGame(uuid);
        if (game == null || game.getKit() != DuelKit.SOUP) return;

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.MUSHROOM_STEW) return;

        event.setCancelled(true);

        // Heilen: 6 Herzen = 12 HP
        double newHealth = Math.min(player.getHealth() + 12.0, player.getMaxHealth());
        player.setHealth(newHealth);

        // Suppe verbrauchen
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.updateInventory();
    }

    /**
     * Item-Drop verhindern fuer Duell-Spieler.
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (plugin.getDuelManager().isInGame(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Bewegung waehrend Countdown unterbinden.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        DuelManager manager = plugin.getDuelManager();

        if (!manager.isInGame(uuid)) return;

        DuelGame game = manager.getGame(uuid);
        if (game == null) return;

        if (game.isFrozen() && event.hasChangedPosition()) {
            event.setCancelled(true);
        }
    }
}
