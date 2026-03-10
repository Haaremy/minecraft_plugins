package de.haaremy.hmylobby;

import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

public class LobbyWorldManager implements Listener {

    private final HmyLobby plugin;
    private final HmyConfigManager config;
    private final HmyLanguageManager language;
    private final String lobbyWorldName;

    public LobbyWorldManager(HmyLobby plugin, HmyConfigManager config, HmyLanguageManager language) {
        this.plugin = plugin;
        this.config = config;
        this.language = language;
        this.lobbyWorldName = config.getLobbyWorld();
        applyGameRules();
    }

    private void applyGameRules() {
        World world = plugin.getServer().getWorld(lobbyWorldName);
        if (world == null) return;

        world.setGameRule(GameRule.DO_WEATHER_CYCLE,  config.getLobbyRule("weather",        false));
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, config.getLobbyRule("daylight-cycle", false));
        world.setGameRule(GameRule.DO_MOB_SPAWNING,   config.getLobbyRule("mob-spawning",   false));
        world.setGameRule(GameRule.DO_FIRE_TICK,       config.getLobbyRule("fire-tick",      false));
        world.setGameRule(GameRule.FALL_DAMAGE,        config.getLobbyRule("fall-damage",    false));

        //world.setDifficulty(Difficulty.PEACEFUL);
        world.setPVP(config.getLobbyRule("pvp", false));

        if (!config.getLobbyRule("daylight-cycle", false)) {
            world.setTime(6000);
        }
    }

    // ── Hunger ───────────────────────────────────────────────────────────────

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isLobbyWorld(player)) return;
        if (!config.getLobbyRule("hunger", false)) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    // ── Damage ───────────────────────────────────────────────────────────────

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isLobbyWorld(player)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();

        if (cause == EntityDamageEvent.DamageCause.FALL) {
            if (!config.getLobbyRule("fall-damage", false)) event.setCancelled(true);
            return;
        }
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            if (!config.getLobbyRule("pvp", false)) event.setCancelled(true);
            return;
        }
        if (!config.getLobbyRule("all-damage", false)) event.setCancelled(true);
    }

    // ── Weather ──────────────────────────────────────────────────────────────

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!event.getWorld().getName().equals(lobbyWorldName)) return;
        if (!config.getLobbyRule("weather", false) && event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    // ── Block Protection ─────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isLobbyWorld(event.getBlock().getWorld())) return;
        if (!config.getLobbyRule("block-protection", true)) return;
        if (event.getPlayer().hasPermission("hmy.world.edit")) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isLobbyWorld(event.getBlock().getWorld())) return;
        if (!config.getLobbyRule("block-protection", true)) return;
        if (event.getPlayer().hasPermission("hmy.world.edit")) return;
        event.setCancelled(true);
    }

    // ── Void Protection ──────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isLobbyWorld(event.getPlayer().getWorld())) return;
        if (!config.getLobbyRule("void-protection", true)) return;
        if (event.getTo() == null) return;

        if (event.getTo().getY() < -60) {
            Player player = event.getPlayer();
            player.teleport(player.getWorld().getSpawnLocation());
            player.sendMessage(language.getMessage(player, "void_rescued", "§aDu wurdest aus dem Void gerettet!"));
            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
        }
    }

    // ── Fire Spread ──────────────────────────────────────────────────────────

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!isLobbyWorld(event.getBlock().getWorld())) return;
        if (!config.getLobbyRule("fire-spread", true)) return;
        if (event.getSource().getType() == Material.FIRE
                || event.getNewState().getType() == Material.FIRE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!isLobbyWorld(event.getBlock().getWorld())) return;
        if (!config.getLobbyRule("fire-spread", true)) return;
        if (event.getCause() != BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) {
            event.setCancelled(true);
        }
    }

    // ── Leaf Decay ───────────────────────────────────────────────────────────

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!isLobbyWorld(event.getBlock().getWorld())) return;
        if (!config.getLobbyRule("leaf-decay", false)) event.setCancelled(true);
    }

    // ── Item Drop / Pickup ───────────────────────────────────────────────────

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!isLobbyWorld(event.getPlayer().getWorld())) return;
        if (!config.getLobbyRule("item-drop", false)) event.setCancelled(true);
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isLobbyWorld(player.getWorld())) return;
        if (!config.getLobbyRule("item-pickup", false)) event.setCancelled(true);
    }

    // ── Interaction Block ────────────────────────────────────────────────────

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (!isLobbyWorld(event.getPlayer().getWorld())) return;
        if (!config.getLobbyRule("interaction-block", false)) return;
        if (event.getPlayer().hasPermission("hmy.lobby.bypass.interact")) return;
        if (event.getClickedBlock() == null) return;

        Material type = event.getClickedBlock().getType();
        boolean isContainer = event.getClickedBlock().getState() instanceof Container;
        boolean isDoor = Tag.DOORS.isTagged(type) || Tag.TRAPDOORS.isTagged(type)
                || Tag.FENCE_GATES.isTagged(type);

        if (isContainer || isDoor) event.setCancelled(true);
    }

    // ── Explosion Protection ──────────────────────────────────────────────────

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!isLobbyWorld(event.getLocation().getWorld())) return;
        if (config.getLobbyRule("explosion-protection", true)) event.blockList().clear();
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!isLobbyWorld(event.getBlock().getWorld())) return;
        if (config.getLobbyRule("explosion-protection", true)) event.blockList().clear();
    }

    // ── Crop Trampling ───────────────────────────────────────────────────────

    @EventHandler
    public void onCropTrample(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.PHYSICAL) return;
        if (!isLobbyWorld(event.getPlayer().getWorld())) return;
        if (!config.getLobbyRule("crop-trampling", true)) return;
        Block b = event.getClickedBlock();
        if (b != null && b.getType() == Material.FARMLAND) event.setCancelled(true);
    }

    // ── Anti-WDL ─────────────────────────────────────────────────────────────

    @EventHandler
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        if (!config.getLobbyRule("anti-wdl", true)) return;
        String channel = event.getChannel().toLowerCase();
        if (channel.startsWith("wdl:") || channel.equals("wdl")) {
            Player player = event.getPlayer();
            player.kickPlayer(language.getMessage(player, "anti_wdl_kick", "§cWorld Downloader ist auf diesem Server nicht erlaubt."));
            plugin.getLogger().warning("Anti-WDL: " + player.getName()
                    + " wurde gekickt (Channel: " + event.getChannel() + ")");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isLobbyWorld(Player player) {
        return player.getWorld().getName().equals(lobbyWorldName);
    }

    private boolean isLobbyWorld(World world) {
        return world != null && world.getName().equals(lobbyWorldName);
    }
}
