package de.haaremy.hmycore.arena.events;

import de.haaremy.hmycore.arena.Arena;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public abstract class ArenaEvent extends Event {

    private final Arena arena;

    protected ArenaEvent(@NotNull Arena arena) {
        this.arena = arena;
    }

    @NotNull
    public Arena getArena() {
        return arena;
    }
}
