package de.haaremy.hmylobby.balloon;

import org.bukkit.Location;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BalloonRide {

    public final Minecart minecart;
    public final String routeName;
    public final UUID owner;
    public final List<Player> passengers = new ArrayList<>();

    public Location destination;
    public BalloonStructure structure;
    public BalloonRoute route;
    public boolean isAutoTravel;

    public int currentWaypointIndex = 0;
    public int nextWaypointIndex = 1;
    public int landingTicks = 0;

    public boolean isMoving = false;
    public int ticksMoving = 0;
    public int lastWaypointIndex;

    public float yaw = 0;
    public float pitch = 0;

    public BalloonRide(Minecart minecart, String routeName, UUID owner) {
        this(minecart, routeName, owner, false);
    }

    public BalloonRide(Minecart minecart, String routeName, UUID owner, boolean isAutoTravel) {
        this.minecart = minecart;
        this.routeName = routeName;
        this.owner = owner;
        this.isAutoTravel = isAutoTravel;
        this.structure = new BalloonStructure(minecart.getLocation());
    }

    public void setDestination(Location dest, int waypointIndex) {
        this.destination = dest;
        this.lastWaypointIndex = waypointIndex;
        this.isMoving = true;
        this.ticksMoving = 0;
    }

    public void addPassenger(Player player) {
        if (!passengers.contains(player)) {
            passengers.add(player);
            minecart.addPassenger(player);
        }
    }

    public void removePassenger(Player player) {
        passengers.remove(player);
        if (minecart.isValid()) {
            minecart.removePassenger(player);
        }
    }

    public List<Player> getPassengers() {
        return new ArrayList<>(passengers);
    }

    public void updateStructure(Location newLoc, Vector direction) {
        if (structure == null) return;
        if (direction.length() > 0.01) {
            yaw = (float) Math.atan2(-direction.getX(), direction.getZ()) * 180 / (float) Math.PI;
            double horizontalDist = Math.sqrt(
                    direction.getX() * direction.getX() + direction.getZ() * direction.getZ());
            pitch = (float) Math.atan2(direction.getY(), horizontalDist) * 180 / (float) Math.PI;
        }
        structure.updatePosition(newLoc, yaw, pitch);
    }

    public void removeStructure() {
        if (structure != null) {
            structure.remove();
        }
    }
}
