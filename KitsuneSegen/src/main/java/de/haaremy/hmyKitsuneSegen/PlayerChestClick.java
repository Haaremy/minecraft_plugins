package de.haaremy.hmykitsunesegen;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


public class PlayerChestClick implements Listener {

    private final HmyKitsuneSegen plugin;
    private BossBar bossBar;
    private final HmyLanguageManager language;
    private final String lang;
    private boolean gameStarted = false;
    private String gameworld;
    private String hubworld;

    public PlayerChestClick(HmyKitsuneSegen plugin, HmyLanguageManager language) {
        this.plugin = plugin;
        this.language = language;
        lang = language.getMessage("language","Sprache");
    }

     @EventHandler
    public void onChestClick(PlayerInteractEvent event) {
        // Überprüfen, ob der Spieler rechtsklickt
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Überprüfen, ob der angeklickte Block existiert und eine Truhe ist
        if (event.getClickedBlock() == null || (event.getClickedBlock().getType() != Material.CHEST && event.getClickedBlock().getType() != Material.ENDER_CHEST)) return;
        event.setCancelled(true);
        // Den Spieler ermitteln
        Player player = event.getPlayer();
        Location clickedLocation = event.getClickedBlock().getLocation();
        Inventory customInventory = plugin.getPlayerEventListener().getCustomChest(clickedLocation);


// BossBar erstellen
        BossBar bossBar = Bukkit.createBossBar("Öffne Truhe...", BarColor.BLUE, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBar.setProgress(0.0);

            // Ladeanimation (BossBar aktualisieren)
    Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
        double progress = 0.0;

        @Override
        public void run() {
            progress += 0.1;
            if (progress >= 1.0) {
                // Ladeanimation abgeschlossen, BossBar entfernen
                bossBar.removePlayer(player);
                bossBar.setVisible(false);

                // Truhe öffnen (benutzerdefiniertes Inventar)
                if (customInventory != null) {
                    for (ItemStack item : customInventory.getContents()) {
                        if (item != null) {
                            clickedLocation.getWorld().dropItemNaturally(clickedLocation, item);
                        }
                    }
                    customInventory.clear(); // Inhalte entfernen
                    clickedLocation.getBlock().setType(Material.AIR); // Truhe entfernen
                }

                // Scheduler stoppen
                Bukkit.getScheduler().cancelTask(this.hashCode());
            } else {
                bossBar.setProgress(Math.min(progress, 1.0)); // Sicherstellen, dass progress <= 1.0

                // Partikel hinzufügen
                clickedLocation.getWorld().spawnParticle(
                    org.bukkit.Particle.END_ROD,
                    clickedLocation.clone().add(0.5, 1.0, 0.5), 5
                );
            }
        }
    }, 0L, 5L); // Start sofort, alle 5 Ticks (0,25 Sekunden) aktualisieren
}
}