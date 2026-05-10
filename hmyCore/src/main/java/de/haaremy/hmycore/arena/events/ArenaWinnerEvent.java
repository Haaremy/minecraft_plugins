package de.haaremy.hmycore.arena.events;

import de.haaremy.hmycore.arena.Arena;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ArenaWinnerEvent extends ArenaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID winnerUuid;

    public ArenaWinnerEvent(@NotNull Arena arena, @NotNull UUID winnerUuid) {
        super(arena);
        this.winnerUuid = winnerUuid;
    }

    @NotNull
    public UUID getWinnerUuid() {
        return winnerUuid;
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
