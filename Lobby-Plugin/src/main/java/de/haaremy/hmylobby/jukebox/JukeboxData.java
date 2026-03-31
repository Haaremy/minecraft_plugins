package de.haaremy.hmylobby.jukebox;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitTask;

public class JukeboxData {

    public final String id;
    public Location jukeboxLoc;
    public Location chestLoc;          // set when DISKBOX mode is used

    public JukeboxMode mode = JukeboxMode.STOPPED;
    public Material currentDisc = Material.AIR;   // cached disc for ENDLESS/DISKBOX
    public int diskboxIndex = 0;                  // next disc index in chest

    public String streamUrl = null;
    public boolean streamEndless = false;
    public boolean streamLive = false;

    public BukkitTask currentTask = null;          // never persisted

    public JukeboxData(String id, Location jukeboxLoc) {
        this.id = id;
        this.jukeboxLoc = jukeboxLoc;
    }
}
