package de.haaremy.hmycore.arena.events;

import de.haaremy.hmycore.arena.Arena;
import de.haaremy.hmycore.arena.ArenaState;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ArenaStateChangeEvent extends ArenaEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ArenaState from;
    private final ArenaState to;
    private boolean cancelled;

    public ArenaStateChangeEvent(@NotNull Arena arena, @NotNull ArenaState from, @NotNull ArenaState to) {
        super(arena);
        this.from = from;
        this.to = to;
    }

    @NotNull
    public ArenaState getFrom() {
        return from;
    }

    @NotNull
    public ArenaState getTo() {
        return to;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
