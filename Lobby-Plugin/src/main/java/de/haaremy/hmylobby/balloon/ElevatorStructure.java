package de.haaremy.hmylobby.balloon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ElevatorStructure {

    private final List<ArmorStand> stands = new ArrayList<>();
    private Location baseLocation;
    private static final int CABIN_HEIGHT = 3;

    public ElevatorStructure(Location loc) {
        this.baseLocation = loc.clone();
        createStructure();
    }

    private void createStructure() {
        World world = baseLocation.getWorld();
        double x = baseLocation.getX();
        double y = baseLocation.getY();
        double z = baseLocation.getZ();

        // Eckpfeiler
        createVerticalBar(world, x - 1.5, y, z - 1.5, CABIN_HEIGHT, Material.IRON_BARS);
        createVerticalBar(world, x + 1.5, y, z - 1.5, CABIN_HEIGHT, Material.IRON_BARS);
        createVerticalBar(world, x - 1.5, y, z + 1.5, CABIN_HEIGHT, Material.IRON_BARS);
        createVerticalBar(world, x + 1.5, y, z + 1.5, CABIN_HEIGHT, Material.IRON_BARS);

        // Boden
        for (double bx = x - 1.5; bx <= x + 1.5; bx += 0.5) {
            for (double bz = z - 1.5; bz <= z + 1.5; bz += 0.5) {
                spawnStand(new Location(world, bx, y, bz), Material.DARK_OAK_PLANKS);
            }
        }

        // Decke
        for (double bx = x - 1.5; bx <= x + 1.5; bx += 0.5) {
            for (double bz = z - 1.5; bz <= z + 1.5; bz += 0.5) {
                spawnStand(new Location(world, bx, y + CABIN_HEIGHT - 0.1, bz), Material.DARK_OAK_PLANKS);
            }
        }

        // Vordere Türen
        for (double ty = y + 0.5; ty < y + CABIN_HEIGHT - 0.5; ty += 0.5) {
            spawnStand(new Location(world, x - 0.5, ty, z - 1.5), Material.IRON_DOOR);
            spawnStand(new Location(world, x + 0.5, ty, z - 1.5), Material.IRON_DOOR);
        }

        // Rückwand
        for (double tx = x - 1.5; tx <= x + 1.5; tx += 0.5) {
            for (double ty = y + 0.5; ty < y + CABIN_HEIGHT - 0.5; ty += 0.5) {
                spawnStand(new Location(world, tx, ty, z + 1.5), Material.GRAY_CONCRETE);
            }
        }

        // Schachtketten oben
        createVerticalBar(world, x - 1.5, y + CABIN_HEIGHT, z - 1.5, 10, Material.CHAIN);
        createVerticalBar(world, x + 1.5, y + CABIN_HEIGHT, z - 1.5, 10, Material.CHAIN);
        createVerticalBar(world, x - 1.5, y + CABIN_HEIGHT, z + 1.5, 10, Material.CHAIN);
        createVerticalBar(world, x + 1.5, y + CABIN_HEIGHT, z + 1.5, 10, Material.CHAIN);
    }

    private void createVerticalBar(World world, double x, double y, double z, int length, Material material) {
        for (int i = 0; i < length; i++) {
            spawnStand(new Location(world, x, y + i, z), material);
        }
    }

    private void spawnStand(Location loc, Material material) {
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.getEquipment().setHelmet(new ItemStack(material));
        stands.add(stand);
    }

    public void updatePosition(Location newLoc) {
        double yOffset = newLoc.getY() - baseLocation.getY();
        for (ArmorStand stand : stands) {
            if (!stand.isValid()) continue;
            stand.teleport(stand.getLocation().clone().add(0, yOffset, 0));
        }
        baseLocation = newLoc.clone();
    }

    public void remove() {
        for (ArmorStand stand : stands) {
            if (stand.isValid()) stand.remove();
        }
        stands.clear();
    }
}
