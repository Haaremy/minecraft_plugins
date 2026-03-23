package de.haaremy.hmylobby.balloon;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class BalloonRoute {

    public static final double ZONE_RADIUS = 5.0;

    public final String name;
    public final List<Location> waypoints = new ArrayList<>();
    public final List<Integer> boardingWaypoints = new ArrayList<>();
    public final List<Integer> dropoffWaypoints = new ArrayList<>();
    public final List<BalloonRide> activeBalloons = new ArrayList<>();
    public boolean isActive = false;

    public BalloonRoute(String name) {
        this.name = name;
    }

    public void addWaypoint(Location loc) {
        waypoints.add(loc.clone());
    }

    public void addBoardingZone(int index) {
        if (index >= 0 && index < waypoints.size() && !boardingWaypoints.contains(index)) {
            boardingWaypoints.add(index);
        }
    }

    public void addDropoffZone(int index) {
        if (index >= 0 && index < waypoints.size() && !dropoffWaypoints.contains(index)) {
            dropoffWaypoints.add(index);
        }
    }

    public Integer getNearestBoardingZoneIndex(Location playerLoc) {
        for (Integer index : boardingWaypoints) {
            if (playerLoc.distance(waypoints.get(index)) <= ZONE_RADIUS) {
                return index;
            }
        }
        return null;
    }

    public Integer getNearestDropoffZoneIndex(Location playerLoc) {
        for (Integer index : dropoffWaypoints) {
            if (playerLoc.distance(waypoints.get(index)) <= ZONE_RADIUS) {
                return index;
            }
        }
        return null;
    }
}
