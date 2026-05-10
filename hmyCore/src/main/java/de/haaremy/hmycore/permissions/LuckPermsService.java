package de.haaremy.hmycore.permissions;

import de.haaremy.hmycore.HmyCore;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Soft-Dep-Wrapper um die LuckPerms-API. Liefert Prefix/Suffix/Group/Weight
 * fuer Spieler — wenn LuckPerms nicht installiert ist, antworten alle Methoden
 * mit Default-Werten (leerer String / 0 / "default").
 *
 * Why: LuckPerms ist softdepend, damit hmyCore auch ohne lauft (Tests, Backup).
 * How to apply: An ScoreboardManager / TabListManager / ChatListener uebergeben,
 * sodass diese unabhaengig vom Verfuegbarkeitsstatus eines LuckPerms-Plugins
 * arbeiten koennen.
 */
public final class LuckPermsService {

    private final HmyCore plugin;
    private final LuckPerms api;

    public LuckPermsService(HmyCore plugin) {
        this.plugin = plugin;
        LuckPerms detected = null;
        Plugin lp = plugin.getServer().getPluginManager().getPlugin("LuckPerms");
        if (lp != null && lp.isEnabled()) {
            try {
                detected = LuckPermsProvider.get();
                plugin.getLogger().info("LuckPerms-Integration aktiv (API verbunden).");
            } catch (IllegalStateException ex) {
                plugin.getLogger().warning("LuckPerms gefunden, aber API nicht initialisiert: " + ex.getMessage());
            }
        } else {
            plugin.getLogger().info("LuckPerms nicht installiert — Rank-Features im Fallback-Modus.");
        }
        this.api = detected;
    }

    public boolean isAvailable() {
        return api != null;
    }

    public String getPrefix(Player player) {
        if (player == null) return "";
        return getPrefix(player.getUniqueId());
    }

    public String getPrefix(UUID uuid) {
        CachedMetaData meta = metaOf(uuid);
        if (meta == null) return "";
        String prefix = meta.getPrefix();
        return prefix == null ? "" : prefix;
    }

    public String getSuffix(Player player) {
        if (player == null) return "";
        return getSuffix(player.getUniqueId());
    }

    public String getSuffix(UUID uuid) {
        CachedMetaData meta = metaOf(uuid);
        if (meta == null) return "";
        String suffix = meta.getSuffix();
        return suffix == null ? "" : suffix;
    }

    public String getPrimaryGroup(Player player) {
        if (player == null) return "default";
        return getPrimaryGroup(player.getUniqueId());
    }

    public String getPrimaryGroup(UUID uuid) {
        if (api == null) return "default";
        User user = api.getUserManager().getUser(uuid);
        if (user == null) return "default";
        String group = user.getPrimaryGroup();
        return group == null ? "default" : group;
    }

    /**
     * Gewichtung des primaeren Gruppe fuer Sortierung (TabList-Order).
     * Hoehere Werte = weiter oben. Default 0 wenn nicht gesetzt.
     */
    public int getWeight(Player player) {
        if (player == null || api == null) return 0;
        return getWeight(player.getUniqueId());
    }

    public int getWeight(UUID uuid) {
        if (api == null) return 0;
        User user = api.getUserManager().getUser(uuid);
        if (user == null) return 0;
        String group = user.getPrimaryGroup();
        if (group == null) return 0;
        var groupObj = api.getGroupManager().getGroup(group);
        if (groupObj == null) return 0;
        return groupObj.getWeight().orElse(0);
    }

    /**
     * Erzeugt einen Prefix fuer TabList-Sortierung. Format: <weightInverted>_<name>
     * — niedrige Stringwerte sortieren zuerst, daher invertieren wir die Weight.
     * Verwendet von Bukkit-Scoreboard-Teams fuer Sort-Order.
     */
    public String tablistSortKey(Player player) {
        int weight = getWeight(player);
        int inverted = Math.max(0, 9999 - weight);
        return String.format("%04d_%s", inverted, player.getName());
    }

    private CachedMetaData metaOf(UUID uuid) {
        if (api == null || uuid == null) return null;
        User user = api.getUserManager().getUser(uuid);
        if (user == null) return null;
        return user.getCachedData().getMetaData(QueryOptions.nonContextual());
    }

    /**
     * Asynchrones Laden eines Offline-Spielers fuer Prefix-Lookups (z.B. Stats-GUI).
     * Blockiert nicht den Main-Thread.
     */
    public String getPrefixOrLoad(OfflinePlayer offline) {
        if (offline == null || api == null) return "";
        UUID uuid = offline.getUniqueId();
        User user = api.getUserManager().getUser(uuid);
        if (user != null) {
            CachedMetaData meta = user.getCachedData().getMetaData(QueryOptions.nonContextual());
            String prefix = meta.getPrefix();
            return prefix == null ? "" : prefix;
        }
        return "";
    }
}
