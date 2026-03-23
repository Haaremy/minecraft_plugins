package de.haaremy.hmylobby.balloon;

import org.bukkit.Location;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ElevatorRide {

    public final Minecart minecart;
    public final String routeName;
    public final ElevatorRoute route;
    public final List<Player> passengers = new ArrayList<>();
    public final ElevatorStructure structure;

    public Location destination;
    public int currentFloor = 0;
    public int nextFloor = 1;
    public int landingTicks = 0;

    public boolean isMoving = false;
    public int ticksMoving = 0;

    public ElevatorRide(Minecart minecart, ElevatorRoute route) {
        this.minecart = minecart;
        this.routeName = route.name;
        this.route = route;
        this.structure = new ElevatorStructure(minecart.getLocation());
    }

    public void setNextFloor(int floor) {
        if (floor >= 0 && floor < route.floors.size()) {
            this.destination = route.floors.get(floor).clone();
            this.nextFloor = floor;
            this.isMoving = true;
            this.ticksMoving = 0;
        }
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

    public void removeStructure() {
        if (structure != null) {
            structure.remove();
        }
    }
}
