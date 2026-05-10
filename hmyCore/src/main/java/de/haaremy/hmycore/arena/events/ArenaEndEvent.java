package de.haaremy.hmycore.arena.events;

import de.haaremy.hmycore.arena.Arena;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ArenaEndEvent extends ArenaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final long endedAt;

    public ArenaEndEvent(@NotNull Arena arena, long endedAt) {
        super(arena);
        this.endedAt = endedAt;
    }

    public ArenaEndEvent(@NotNull Arena arena) {
        this(arena, System.currentTimeMillis());
    }

    public long getEndedAt() {
        return endedAt;
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
