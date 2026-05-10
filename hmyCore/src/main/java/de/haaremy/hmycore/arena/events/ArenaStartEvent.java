package de.haaremy.hmycore.arena.events;

import de.haaremy.hmycore.arena.Arena;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ArenaStartEvent extends ArenaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final long startedAt;

    public ArenaStartEvent(@NotNull Arena arena, long startedAt) {
        super(arena);
        this.startedAt = startedAt;
    }

    public ArenaStartEvent(@NotNull Arena arena) {
        this(arena, System.currentTimeMillis());
    }

    public long getStartedAt() {
        return startedAt;
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
