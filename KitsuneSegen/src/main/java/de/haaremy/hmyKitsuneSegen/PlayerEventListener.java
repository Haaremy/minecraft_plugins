package de.haaremy.hmykitsunesegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import de.haaremy.hmykitsunesegen.utils.PermissionUtils;
import net.kyori.adventure.bossbar.BossBar;

public class PlayerEventListener implements Listener {

    private final HmyKitsuneSegen plugin;
    private BossBar bossBar;
    private final HmyLanguageManager language;
    private boolean gameStarted = false;
    private final String gameworld;
    private final String hubworld;
    private final Map<Location, Inventory> chestInventories = new HashMap<>();
    private final Set<Player> frozenPlayers = new HashSet<>();

    public PlayerEventListener(HmyKitsuneSegen plugin, HmyLanguageManager language, String gameworld, String hubworld) {
        this.plugin = plugin;
        this.language = language;
        this.gameworld = gameworld;
        this.hubworld = hubworld;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(null); // Prevent the join message from being broadcast
        Location spawn = Bukkit.getWorld(this.hubworld).getSpawnLocation();
        player.teleport(spawn);

        // Spieler standard Items geben
        ItemStack i1 = createItem(Material.PINK_STAINED_GLASS_PANE, " ", List.of(" "));
        ItemStack i2 = createItem(Material.BLUE_STAINED_GLASS_PANE, " ", List.of(" "));
        ItemStack air = createItem(Material.AIR, " ", List.of(" "));

        int maxhealth = 45;
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxhealth);
        player.setHealth(Math.min(player.getHealth(), maxhealth));

        for(byte i = 0; i<9; i++){
            player.getInventory().setItem(i,air);
        }
        for(byte i = 9; i<36; i++){
           if(i%2==0){
            player.getInventory().setItem(i, i1);
           } else player.getInventory().setItem(i, i2); 
        }
        ItemStack pickaxe = createItem(Material.WOODEN_HOE, "§6Axt", List.of("§7Verteidige dich so lange du musst."));
        player.getInventory().setItem(0, pickaxe);
        ItemStack buildtool = createItem(Material.OAK_PLANKS, "§6Bauholz", List.of("§7Angriff? Blockade!"));
        player.getInventory().setItem(8, buildtool);
        ItemStack as0 = createItem(Material.ARROW, "Multishot", List.of("§7Cheeese!"));
        as0.setAmount(0);
        player.getInventory().setItem(22, as0);
        ItemStack as1 = createItem(Material.ARROW, "Speedshot", List.of("§7Cheeese!"));
        player.getInventory().setItem(21, as1);
        ItemStack as2 = createItem(Material.ARROW, "Distanceshot", List.of("§7Cheeese!"));
        player.getInventory().setItem(20, as2);
        ItemStack as3 = createItem(Material.ARROW, "Precisionshot", List.of("§7Cheeese!"));
        player.getInventory().setItem(19, as3);
        //#endregion

        // Prüfen, wie viele Spieler auf dem Server sind
        int playerCount = Bukkit.getOnlinePlayers().size();

        if (playerCount == 0 && gameStarted) {
            // Game Started wird zurück gesetzt
            gameStarted = false;
        }

        if (playerCount == 1 && !gameStarted) {
            replaceBlockWithChance(gameworld, plugin.getChests());
            List<Location> locations = new ArrayList<>(this.plugin.getLocations()); // Kopie der Liste an Spawnpunkten
            Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                int timer = 10; // Startwert für den Timer

                @Override
                public void run() {
                    if (timer <= 0) {
                        Bukkit.getScheduler().cancelTask(this.hashCode()); // Task stoppen
                        return;
                    }

                    // Titel für den Spieler aktualisieren
                    player.sendTitle("§6" + timer, "", 5, 20, 5);

                    timer--; // Countdown
                }
            }, 0, 20); // Start sofort, Wiederholung alle 20 Ticks (1 Sekunde)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
            //player.sendMessage("Starte Spiel.");            
                
            
                Random random = new Random();
                
                 // Spieler in der Welt filtern
                List<Player> playersInWorld = new ArrayList<>();
                for (Player playerlist : Bukkit.getOnlinePlayers()) {
                    if (playerlist.getWorld().equals(Bukkit.getWorld(hubworld))) {
                        playersInWorld.add(playerlist);
                    }
                }
                
                for(Player playerlist : playersInWorld){
                    if (locations.isEmpty()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("Es gibt keine verfügbaren Teleport-Ziele mehr.");
                        });
                        continue; // Überspringe diesen Spieler
                    };
                    int randomIndex = random.nextInt(locations.size()); // Zufälliger Index: 0 bis locations.size() - 1
                    Location location = locations.get(randomIndex);
                    player.setGameMode(GameMode.SURVIVAL);
                    player.teleport(location);
                    frozenPlayers.add(player);                    
                    locations.remove(randomIndex);
                }
                            Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                int timer = 5; // Startwert für den Spielstart-Timer

                @Override
                public void run() {
                    if (timer <= 0) {
                        Bukkit.getScheduler().cancelTask(this.hashCode()); // Task stoppen
                        for(Player playerlist : Bukkit.getOnlinePlayers()){
                            frozenPlayers.remove(player);
                        }
                        return;
                    }

                    // Titel für den Spieler aktualisieren
                    player.sendTitle("§6" + timer, "", 5, 20, 5);

                    timer--; // Countdown

                }
        }, 0, 20); // Start sofort, Wiederholung alle 20 Ticks (1 Sekunde)
            },200L); //10 Sekunden Delay vor Spielbegin

        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        frozenPlayers.remove(player);
        player.setGameMode(GameMode.SPECTATOR);
        plugin.getLogger().info(player.getName() + " hat das Spiel verlassen.");
    }




    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createArrowSlot(String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        item.setAmount(0);
        return item;
    }


@EventHandler
public void onPlayerInteract(PlayerInteractEvent event) {
         Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Überprüfe, ob der Spieler einen Crossbow hält
        if (item != null && item.getType() == Material.CROSSBOW) {
            // Beschleunigtes Nachladen simulieren
                loadCrossbow(item);
            
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        int slot = event.getPlayer().getInventory().getHeldItemSlot();
        if (slot < 1 || slot > 7) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getCurrentItem();

    // Sicherstellen, dass das Item nicht null ist und ein ItemMeta hat
    if (item == null || !item.hasItemMeta()) {
        return; // Kein Item oder kein ItemMeta vorhanden
    }

    // Sicherstellen, dass das ItemMeta einen Displaynamen hat
        String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : "";
              // Prüfen, ob der Spieler sein eigenes Inventar bearbeitet
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !event.getView().getTopInventory().equals(clickedInventory)) {
            return; // Spieler hat nichts angeklickt oder ein anderes Inventar
        }

        // Prüfen, ob der angeklickte Slot außerhalb der Hotbar liegt
        int slot = event.getSlot();
        if (slot < 1 || slot > 7) {
            // Blockiere den Klick für alle Slots außerhalb der Hotbar
            event.setCancelled(true);
        }


        if (!PermissionUtils.hasPermission(player,"hmy.kitsunesegen.inventory.edit")) {
            event.setCancelled(true); // Verhindert, dass Items verschoben werden
        } else event.setCancelled(true); // Verhindert, dass Items verschoben werden
    }


    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Block blockUnderPlayer = player.getLocation().subtract(0, 1, 0).getBlock();

        // Prüfen, ob der Block grüne Wolle ist
        if (blockUnderPlayer.getType() == Material.OAK_TRAPDOOR) {
            // command to execute
        }

        if (frozenPlayers.contains(player)) {
            // Bewegung blockieren, indem die Position auf die vorherige gesetzt wird
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player killedPlayer = event.getEntity();
        Player killer = killedPlayer.getKiller();

        killedPlayer.setGameMode(GameMode.SPECTATOR);
        if (killer == null || !killer.isOnline()) {
            killedPlayer.teleport(killedPlayer.getWorld().getSpawnLocation());
        } else killedPlayer.setSpectatorTarget(killer);

         // Nachricht an den getöteten Spieler
            killedPlayer.sendTitle(
                ChatColor.RED + "Du bist gestorben!",
                ChatColor.YELLOW + "Zuschauen: " + killer.getName() + "oder /lobby.",
                10, // Fade-in (Ticks)
                70, // Anzeigezeit (Ticks)
                20  // Fade-out (Ticks)
            );

        World world = Bukkit.getWorld(gameworld); // Hole die Welt anhand des Namens
        if (world == null) {
            return; // Falls die Welt nicht existiert
        }

        // Filtere die Spieler in der Welt, die im Survival-Modus sind und beendet Spiel bei einer Person verbleibend
        if((int) world.getPlayers().stream()
                .filter(player -> player.getGameMode() == GameMode.SURVIVAL)
                .count() <=1 ){
            gameFinished();
        }
    }
    

    

   @EventHandler
public void onMobSpawn(CreatureSpawnEvent event) {
    // Prüfen, ob der Mob eine Kreatur ist
    if (!(event.getEntity() instanceof Creature)) {
        return;
    }

    Creature mob = (Creature) event.getEntity();

    // Starte einen BukkitRunnable, um den nächsten Spieler zu verfolgen
    new BukkitRunnable() {
        @Override
        public void run() {
            Player nearestPlayer = findNearestPlayer(mob);
            if (nearestPlayer != null) {
                mob.setTarget(nearestPlayer); // Setze das Ziel auf den Spieler
            }
        }
    }.runTaskTimer(plugin, 0L, 100L); // Übergib das `plugin`-Objekt hier
}

    // Methode, um den nächsten Spieler zu finden
    private Player findNearestPlayer(LivingEntity mob) {
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : Bukkit.getOnlinePlayers()) {
            double distance = mob.getLocation().distanceSquared(player.getLocation()); // Abstand berechnen
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = player;
            }
        }

        return nearestPlayer;
    }

public void loadWorlds(String name){
    var world = Bukkit.getWorld(name);
        if (world == null) {
            Bukkit.getLogger().info("Welt " + name + " wird geladen...");
            world = Bukkit.createWorld(new WorldCreator(name));
        } else {
            Bukkit.getLogger().info("Welt " + name + " ist bereits geladen.");
        }
}



protected List<Location> findAndLogBlocks(String targetWorldName, Material material, int type) {

        var world = Bukkit.getWorld(targetWorldName);
        

        int minX = -250;
        int maxX = 250;
        int minZ = -250;
        int maxZ = 250;
        int minY = -64;
        int maxY = world.getMaxHeight();

        List<Location> locations = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y < maxY; y++) {
                    if (world.getBlockAt(x,y,z).getType() == material) {
                        double safeX = 0;
                        double safeY = 0;
                        double safeZ = 0;
                        if(type==0){ // player
                         safeX = x >= 0 ? x + 0.5 : x;
                         safeZ = z >= 0 ? z + 0.5 : z;
                         safeY = y + 1; // Spieler steht auf dem Block, nicht darin
                        } else { // block
                         safeX = x;
                         safeZ = z;
                         safeY = y + 1; // Block steht auf dem Block, nicht darin
                        }
                        Location loc = new Location(world, safeX, safeY, safeZ);
                        locations.add(loc);
                    }
                }
            }
        }

        return locations;
    }

    public Inventory getCustomChest(Location loc) {
        // Prüfen, ob die Location in der Map existiert
        if (chestInventories.containsKey(loc)) {
            return chestInventories.get(loc);
        }

        // Rückgabe von null, wenn kein Inventar gefunden wurde
        return null;
    }

    



    private final Random random = new Random();

    public void replaceBlockWithChance(String worldName, List<Location> loc) {
        // Hole die gewünschte Welt
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            Bukkit.getLogger().warning("Die Welt '" + worldName + "' existiert nicht!");
            return;
        }

        Random random = new Random();

        for (Location lokal : loc) {
            // Setze den Block auf AIR
            Block block = world.getBlockAt(lokal);
            block.setType(Material.AIR);
            
            // Erstelle ein benutzerdefiniertes Inventar
                Inventory customInventory = Bukkit.createInventory(null, 27, "KitsuneSegen");

            // Prüfe die Wahrscheinlichkeit und setze gegebenenfalls den neuen Block
            if (random.nextDouble() < 0.6) { // 40% Wahrscheinlichkeit
                Material blockType = Material.CHEST; // Standardmäßig CHEST
                

                if (random.nextDouble() < 0.2) { // Zusätzliche 10% Wahrscheinlichkeit für ENDER_CHEST
                    blockType = Material.ENDER_CHEST;
                    customInventory = plugin.getLuckItem().createSpecialChest(customInventory);
                } else customInventory = plugin.getLuckItem().createNormalChest(customInventory);

                // Setze den Blocktyp
                block.setType(blockType);

                // Speichere das Inventar (optional, wenn du es später verwenden möchtest)
                chestInventories.put(lokal, customInventory); // Map<Location, Inventory>

                // Block leuchten lassen
                block.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, block.getLocation().add(0.5, 0.5, 0.5), 10);
            }
        }
    }

    private void gameFinished() {
       gameStarted = false;
       Location spawn = Bukkit.getWorld(this.hubworld).getSpawnLocation();
       for(Player player : Bukkit.getWorld(this.gameworld).getPlayers()){
            player.teleport(spawn);
            player.setGameMode(GameMode.SPECTATOR);
       }

    }

    private void loadCrossbow(ItemStack item) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
}









