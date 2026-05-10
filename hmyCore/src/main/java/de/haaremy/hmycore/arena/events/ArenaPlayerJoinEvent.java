package de.haaremy.hmycore.arena.events;

import de.haaremy.hmycore.arena.Arena;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ArenaPlayerJoinEvent extends ArenaEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private boolean cancelled;

    public ArenaPlayerJoinEvent(@NotNull Arena arena, @NotNull UUID playerUuid) {
        super(arena);
        this.playerUuid = playerUuid;
    }

    @NotNull
    public UUID getPlayerUuid() {
        return playerUuid;
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
