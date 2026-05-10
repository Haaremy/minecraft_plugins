package de.haaremy.hmykitsunesegen.utils;

import java.util.logging.Logger;

import org.bukkit.entity.Player;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

public class PermissionUtils {

    private static final Logger logger = Logger.getLogger(PermissionUtils.class.getName());

    public static boolean hasPermission(Player player, String permission) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            // Verwende den bereits gecachten User statt blockierendem .join()
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                logger.warning("LuckPerms-User fuer Spieler " + player.getName() + " nicht im Cache.");
                return false;
            }

            // getCachedData().getPermissionData() prueft bereits User- UND Gruppen-Berechtigungen
            return user.getCachedData()
                .getPermissionData()
                .checkPermission(permission)
                .asBoolean();

        } catch (Exception e) {
            logger.warning("Fehler beim Ueberpruefen der Berechtigung fuer Spieler " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
}
