package de.haaremy.hmycore.team;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HmyTeam {

    private final String name;
    private final NamedTextColor color;
    private final Set<UUID> members;
    private final int maxSize;

    public HmyTeam(String name, NamedTextColor color, int maxSize) {
        this.name = name;
        this.color = color;
        this.members = new HashSet<>();
        this.maxSize = maxSize;
    }

    public String getName() {
        return name;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getSize() {
        return members.size();
    }

    public boolean isFull() {
        return members.size() >= maxSize;
    }

    public boolean addMember(UUID uuid) {
        if (isFull()) return false;
        return members.add(uuid);
    }

    public boolean removeMember(UUID uuid) {
        return members.remove(uuid);
    }

    public boolean hasMember(UUID uuid) {
        return members.contains(uuid);
    }

    public void clear() {
        members.clear();
    }
}
