package de.haaremy.hmycore.arena.events;

import de.haaremy.hmycore.arena.Arena;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ArenaPlayerLeaveEvent extends ArenaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;

    public ArenaPlayerLeaveEvent(@NotNull Arena arena, @NotNull UUID playerUuid) {
        super(arena);
        this.playerUuid = playerUuid;
    }

    @NotNull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
