package de.haaremy.hmypaper;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.haaremy.hmypaper.utils.PermissionUtils;

public class ComGetPos implements CommandExecutor {

   private final HmyLanguageManager language;

    public ComGetPos(HmyLanguageManager language) {
        this.language = language;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (! PermissionUtils.hasPermission(player, "hmy.pos")) {
                language.getMessage("p_no_permission", "Keine Berechtigung.");
                return false;
            }
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();

        if(args.length!=0){
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§cSpieler nicht gefunden.");
                return true;
            }
            loc = target.getLocation();
        }
        player.sendMessage(String.format("X: %.2f, Y: %.2f, Z: %.2f, Welt: %s",
                loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName()));
        return true;
    }
}
