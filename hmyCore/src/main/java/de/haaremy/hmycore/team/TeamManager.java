package de.haaremy.hmycore.team;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class TeamManager {

    private final List<HmyTeam> teams = new ArrayList<>();

    public HmyTeam createTeam(String name, NamedTextColor color, int maxSize) {
        HmyTeam team = new HmyTeam(name, color, maxSize);
        teams.add(team);
        return team;
    }

    public void removeTeam(String name) {
        teams.removeIf(t -> t.getName().equalsIgnoreCase(name));
    }

    public HmyTeam getTeam(String name) {
        for (HmyTeam team : teams) {
            if (team.getName().equalsIgnoreCase(name)) {
                return team;
            }
        }
        return null;
    }

    public HmyTeam getTeam(UUID playerUuid) {
        for (HmyTeam team : teams) {
            if (team.hasMember(playerUuid)) {
                return team;
            }
        }
        return null;
    }

    public boolean assignToTeam(UUID playerUuid, String teamName) {
        // Erst aus aktuellem Team entfernen
        HmyTeam current = getTeam(playerUuid);
        if (current != null) {
            current.removeMember(playerUuid);
        }

        HmyTeam target = getTeam(teamName);
        if (target == null) return false;
        return target.addMember(playerUuid);
    }

    public HmyTeam assignRandom(UUID playerUuid) {
        // Erst aus aktuellem Team entfernen
        HmyTeam current = getTeam(playerUuid);
        if (current != null) {
            current.removeMember(playerUuid);
        }

        // Team mit wenigsten Spielern zuerst (balanciert)
        List<HmyTeam> available = new ArrayList<>(teams);
        available.removeIf(HmyTeam::isFull);

        if (available.isEmpty()) return null;

        // Sortieren nach Groesse, dann zufaellig bei gleicher Groesse
        Collections.shuffle(available);
        available.sort(Comparator.comparingInt(HmyTeam::getSize));

        HmyTeam target = available.get(0);
        target.addMember(playerUuid);
        return target;
    }

    public List<HmyTeam> getTeams() {
        return Collections.unmodifiableList(teams);
    }

    public void clearAllTeams() {
        for (HmyTeam team : teams) {
            team.clear();
        }
    }

    public void removeAllTeams() {
        teams.clear();
    }
}
