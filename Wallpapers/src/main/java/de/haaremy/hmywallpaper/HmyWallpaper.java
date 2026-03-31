package de.haaremy.hmywallpaper;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public class HmyWallpaper extends JavaPlugin {

    @Override
    public void onEnable() {
        extractSvgs();

        var cmd = getCommand("wallpaper");
        if (cmd != null) {
            cmd.setExecutor(new WallpaperCommand(this));
        }

        getLogger().info("hmyWallpaper aktiviert!");
    }

    @Override
    public void onDisable() {
        getLogger().info("hmyWallpaper deaktiviert!");
    }

    private void extractSvgs() {
        File svgDir = new File(getDataFolder(), "svgs");
        if (!svgDir.exists()) svgDir.mkdirs();

        for (String name : List.of("bubble.svg", "checkers.svg")) {
            File target = new File(svgDir, name);
            if (!target.exists()) {
                saveResource("svgs/" + name, false);
            }
        }
    }

    public File getSvgsDir() {
        return new File(getDataFolder(), "svgs");
    }
}
