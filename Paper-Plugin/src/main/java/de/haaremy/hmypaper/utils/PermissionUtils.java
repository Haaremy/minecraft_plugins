package de.haaremy.hmypaper.utils;

import java.util.logging.Logger;

import org.bukkit.entity.Player;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;

public class PermissionUtils {

    private static final LuckPerms luckPerms = LuckPermsProvider.get();
    private static final Logger logger = Logger.getLogger(PermissionUtils.class.getName());

    public static boolean hasPermission(Player player, String permission) {
        try {
            // LuckPerms-User laden
            User user = luckPerms.getUserManager().loadUser(player.getUniqueId()).join();
            if (user == null) {
                logger.warning("LuckPerms-User für Spieler " + player.getName() + " konnte nicht geladen werden.");
                return false;
            }

            // Direkte Berechtigung des Spielers prüfen
            boolean hasUserPermission = user.getCachedData()
                .getPermissionData()
                .checkPermission(permission)
                .asBoolean();

            // Gruppen-Berechtigungen prüfen
            boolean hasGroupPermission = user.getNodes().stream()
                .filter(NodeType.INHERITANCE::matches) // Nur Gruppenzugehörigkeiten
                .map(node -> node.getKey()) // Schlüssel der Gruppen (z. B. "default")
                .map(groupName -> luckPerms.getGroupManager().getGroup(groupName)) // Gruppe abrufen
                .filter(group -> group != null) // Nur existierende Gruppen
                .anyMatch(group -> group.getCachedData()
                    .getPermissionData()
                    .checkPermission(permission)
                    .asBoolean());

            // Gesamtberechtigung (User oder Gruppe)
            return hasUserPermission || hasGroupPermission;

        } catch (Exception e) {
            logger.warning("Fehler beim Überprüfen der Berechtigung für Spieler " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
}
