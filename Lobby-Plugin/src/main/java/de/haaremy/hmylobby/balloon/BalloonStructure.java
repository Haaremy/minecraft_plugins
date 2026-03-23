package de.haaremy.hmylobby.balloon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class BalloonStructure {

    private final List<ArmorStand> stands = new ArrayList<>();
    private Location baseLocation;

    public BalloonStructure(Location loc) {
        this.baseLocation = loc.clone();
        createStructure();
    }

    private void createStructure() {
        World world = baseLocation.getWorld();
        // Ballon-Hülle (Kuppel)
        createBalloonLayer(world, 0, 4, Material.RED_WOOL);
        createBalloonLayer(world, 1, 6, Material.RED_WOOL);
        createBalloonLayer(world, 2, 8, Material.ORANGE_WOOL);
        createBalloonLayer(world, 3, 6, Material.RED_WOOL);
        createBalloonLayer(world, 4, 4, Material.RED_WOOL);
        // Seile & Korb
        createRopes(world);
        createBasket(world);
    }

    private void createBalloonLayer(World world, int height, int radius, Material material) {
        double cx = baseLocation.getX();
        double cy = baseLocation.getY() + height;
        double cz = baseLocation.getZ();
        int points = (int) (Math.PI * radius);
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            spawnStand(new Location(world, cx + radius * Math.cos(angle), cy, cz + radius * Math.sin(angle)), material);
        }
    }

    private void createRopes(World world) {
        double balloonBottomY = baseLocation.getY() + 4;
        double basketTopY = baseLocation.getY() - 2;
        double[] offsets = {2, -2};
        for (double xOff : offsets) {
            for (double zOff : offsets) {
                for (double y = balloonBottomY; y > basketTopY; y -= 0.5) {
                    spawnStand(new Location(world,
                            baseLocation.getX() + xOff, y, baseLocation.getZ() + zOff),
                            Material.CHAIN);
                }
            }
        }
    }

    private void createBasket(World world) {
        double by = baseLocation.getY() - 2;
        double bx = baseLocation.getX();
        double bz = baseLocation.getZ();
        for (double x = bx - 1.5; x <= bx + 1.5; x += 0.5) {
            for (double z = bz - 1.5; z <= bz + 1.5; z += 0.5) {
                if (Math.abs(x - bx) >= 1.4 || Math.abs(z - bz) >= 1.4) {
                    spawnStand(new Location(world, x, by, z), Material.OAK_WOOD);
                }
            }
        }
        for (double x = bx - 1; x < bx + 1; x += 0.4) {
            for (double z = bz - 1; z < bz + 1; z += 0.4) {
                spawnStand(new Location(world, x, by, z), Material.BROWN_WOOL);
            }
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

    public void updatePosition(Location newLoc, float yaw, float pitch) {
        for (ArmorStand stand : stands) {
            if (!stand.isValid()) continue;
            Vector relativePos = stand.getLocation().toVector().subtract(baseLocation.toVector());
            Vector rotated = rotateVector(relativePos, yaw, pitch);
            stand.teleport(newLoc.clone().add(rotated));
        }
        baseLocation = newLoc.clone();
    }

    private Vector rotateVector(Vector v, float yawDeg, float pitchDeg) {
        float y = (float) Math.toRadians(yawDeg);
        float p = (float) Math.toRadians(pitchDeg);
        double x = v.getX(), vy = v.getY(), z = v.getZ();
        double x1 = x * Math.cos(y) - z * Math.sin(y);
        double z1 = x * Math.sin(y) + z * Math.cos(y);
        double y2 = vy * Math.cos(p) - z1 * Math.sin(p);
        double z2 = vy * Math.sin(p) + z1 * Math.cos(p);
        return new Vector(x1, y2, z2);
    }

    public void remove() {
        for (ArmorStand stand : stands) {
            if (stand.isValid()) stand.remove();
        }
        stands.clear();
    }
}
