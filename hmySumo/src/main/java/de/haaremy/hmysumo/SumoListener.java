package de.haaremy.hmysumo;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class SumoListener implements Listener {

    private final HmySumo plugin;

    public SumoListener(HmySumo plugin) {
        this.plugin = plugin;
    }

    /**
     * Void Detection: Spieler faellt unter Y=0.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        SumoManager manager = plugin.getSumoManager();

        if (!manager.isInGame(uuid)) return;

        SumoGame game = manager.getGame(uuid);
        if (game == null) return;

        // Freeze waehrend Countdown
        if (game.isFrozen() && event.hasChangedPosition()) {
            event.setCancelled(true);
            return;
        }

        // Void Detection
        if (event.getTo().getY() < 0) {
            game.onPlayerFall(player);
        }
    }

    /**
     * Schaden auf 0 setzen, aber Knockback beibehalten.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();
        SumoManager manager = plugin.getSumoManager();

        if (!manager.isInGame(uuid)) return;

        // Schaden auf 0
        event.setDamage(0);
    }

    /**
     * Nur Spieler-gegen-Spieler im Sumo-Game erlauben.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        UUID victimUuid = victim.getUniqueId();
        UUID attackerUuid = attacker.getUniqueId();
        SumoManager manager = plugin.getSumoManager();

        // Wenn einer der Spieler im Sumo ist
        if (manager.isInGame(victimUuid) || manager.isInGame(attackerUuid)) {
            SumoGame victimGame = manager.getGame(victimUuid);
            SumoGame attackerGame = manager.getGame(attackerUuid);

            // Beide muessen im gleichen Spiel sein
            if (victimGame == null || attackerGame == null || victimGame != attackerGame) {
                event.setCancelled(true);
                return;
            }

            // Nicht waehrend Countdown oder Rundenende angreifen
            if (victimGame.getState() != SumoGame.GameState.FIGHTING) {
                event.setCancelled(true);
                return;
            }

            // Schaden auf 0, Knockback bleibt
            event.setDamage(0);
        }
    }

    /**
     * Spieler verlaesst den Server waehrend eines Spiels.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        SumoManager manager = plugin.getSumoManager();

        // Aus Queue entfernen
        if (manager.isInQueue(uuid)) {
            manager.leaveQueue(player);
            return;
        }

        // Aus Spiel entfernen
        if (manager.isInGame(uuid)) {
            SumoGame game = manager.getGame(uuid);
            if (game != null) {
                game.onPlayerLeave(player);
            }
        }
    }

    /**
     * Hunger deaktivieren fuer Sumo-Spieler.
     */
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (plugin.getSumoManager().isInGame(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Item-Drop verhindern fuer Sumo-Spieler.
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (plugin.getSumoManager().isInGame(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
