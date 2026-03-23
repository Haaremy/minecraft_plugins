package de.haaremy.hmylobby.balloon;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class ElevatorRoute {

    public static final double ZONE_RADIUS = 3.0;

    public final String name;
    public final List<Location> floors = new ArrayList<>();
    public final List<Integer> boardingFloors = new ArrayList<>();
    public final List<Integer> dropoffFloors = new ArrayList<>();
    public final List<ElevatorRide> activeElevators = new ArrayList<>();
    public boolean isActive = false;
    public boolean ascending = true;

    public ElevatorRoute(String name) {
        this.name = name;
    }

    public void addFloor(Location loc) {
        floors.add(loc.clone());
    }

    public void addBoardingFloor(int index) {
        if (index >= 0 && index < floors.size() && !boardingFloors.contains(index)) {
            boardingFloors.add(index);
        }
    }

    public void addDropoffFloor(int index) {
        if (index >= 0 && index < floors.size() && !dropoffFloors.contains(index)) {
            dropoffFloors.add(index);
        }
    }

    public Integer getNearestBoardingFloorIndex(Location playerLoc) {
        for (Integer index : boardingFloors) {
            if (playerLoc.distance(floors.get(index)) <= ZONE_RADIUS) {
                return index;
            }
        }
        return null;
    }

    public Integer getNearestDropoffFloorIndex(Location playerLoc) {
        for (Integer index : dropoffFloors) {
            if (playerLoc.distance(floors.get(index)) <= ZONE_RADIUS) {
                return index;
            }
        }
        return null;
    }
}
