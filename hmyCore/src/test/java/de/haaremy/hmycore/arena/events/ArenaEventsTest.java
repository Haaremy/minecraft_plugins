package de.haaremy.hmycore.arena.events;

import de.haaremy.hmycore.arena.Arena;
import de.haaremy.hmycore.arena.ArenaState;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaEventsTest {

    private static Arena makeArena() {
        return new Arena("unit-test-arena");
    }

    // === ArenaPlayerJoinEvent ===

    @Test
    void joinEventCarriesArenaAndPlayer() {
        Arena arena = makeArena();
        UUID p = UUID.randomUUID();
        ArenaPlayerJoinEvent e = new ArenaPlayerJoinEvent(arena, p);
        assertSame(arena, e.getArena());
        assertEquals(p, e.getPlayerUuid());
    }

    @Test
    void joinEventDefaultsToNotCancelled() {
        ArenaPlayerJoinEvent e = new ArenaPlayerJoinEvent(makeArena(), UUID.randomUUID());
        assertFalse(e.isCancelled());
    }

    @Test
    void joinEventCancellableRoundtrip() {
        ArenaPlayerJoinEvent e = new ArenaPlayerJoinEvent(makeArena(), UUID.randomUUID());
        e.setCancelled(true);
        assertTrue(e.isCancelled());
        e.setCancelled(false);
        assertFalse(e.isCancelled());
    }

    @Test
    void joinEventHandlerListIsSingleton() {
        HandlerList a = ArenaPlayerJoinEvent.getHandlerList();
        ArenaPlayerJoinEvent e = new ArenaPlayerJoinEvent(makeArena(), UUID.randomUUID());
        assertNotNull(a);
        assertSame(a, e.getHandlers());
    }

    // === ArenaPlayerLeaveEvent ===

    @Test
    void leaveEventCarriesArenaAndPlayer() {
        Arena arena = makeArena();
        UUID p = UUID.randomUUID();
        ArenaPlayerLeaveEvent e = new ArenaPlayerLeaveEvent(arena, p);
        assertSame(arena, e.getArena());
        assertEquals(p, e.getPlayerUuid());
    }

    @Test
    void leaveEventHandlerListIsSingleton() {
        assertSame(ArenaPlayerLeaveEvent.getHandlerList(),
                new ArenaPlayerLeaveEvent(makeArena(), UUID.randomUUID()).getHandlers());
    }

    // === ArenaStateChangeEvent ===

    @Test
    void stateChangeEventCarriesFromAndTo() {
        Arena arena = makeArena();
        ArenaStateChangeEvent e =
                new ArenaStateChangeEvent(arena, ArenaState.WAITING, ArenaState.RUNNING);
        assertSame(arena, e.getArena());
        assertEquals(ArenaState.WAITING, e.getFrom());
        assertEquals(ArenaState.RUNNING, e.getTo());
    }

    @Test
    void stateChangeEventCancellableRoundtrip() {
        ArenaStateChangeEvent e =
                new ArenaStateChangeEvent(makeArena(), ArenaState.WAITING, ArenaState.RUNNING);
        assertFalse(e.isCancelled());
        e.setCancelled(true);
        assertTrue(e.isCancelled());
    }

    @Test
    void stateChangeEventHandlerListIsSingleton() {
        assertSame(ArenaStateChangeEvent.getHandlerList(),
                new ArenaStateChangeEvent(makeArena(), ArenaState.WAITING, ArenaState.ENDING)
                        .getHandlers());
    }

    // === ArenaStartEvent ===

    @Test
    void startEventDefaultTimestampIsRecent() {
        long before = System.currentTimeMillis();
        ArenaStartEvent e = new ArenaStartEvent(makeArena());
        long after = System.currentTimeMillis();
        assertTrue(e.getStartedAt() >= before && e.getStartedAt() <= after,
                "Default startedAt should be inside [before, after]");
    }

    @Test
    void startEventExplicitTimestampIsPreserved() {
        ArenaStartEvent e = new ArenaStartEvent(makeArena(), 1735689600000L);
        assertEquals(1735689600000L, e.getStartedAt());
    }

    @Test
    void startEventHandlerListIsSingleton() {
        assertSame(ArenaStartEvent.getHandlerList(), new ArenaStartEvent(makeArena()).getHandlers());
    }

    // === ArenaEndEvent ===

    @Test
    void endEventDefaultTimestampIsRecent() {
        long before = System.currentTimeMillis();
        ArenaEndEvent e = new ArenaEndEvent(makeArena());
        long after = System.currentTimeMillis();
        assertTrue(e.getEndedAt() >= before && e.getEndedAt() <= after);
    }

    @Test
    void endEventExplicitTimestampIsPreserved() {
        ArenaEndEvent e = new ArenaEndEvent(makeArena(), 1735689600000L);
        assertEquals(1735689600000L, e.getEndedAt());
    }

    @Test
    void endEventHandlerListIsSingleton() {
        assertSame(ArenaEndEvent.getHandlerList(), new ArenaEndEvent(makeArena()).getHandlers());
    }

    // === ArenaWinnerEvent ===

    @Test
    void winnerEventCarriesArenaAndUuid() {
        Arena arena = makeArena();
        UUID winner = UUID.randomUUID();
        ArenaWinnerEvent e = new ArenaWinnerEvent(arena, winner);
        assertSame(arena, e.getArena());
        assertEquals(winner, e.getWinnerUuid());
    }

    @Test
    void winnerEventHandlerListIsSingleton() {
        assertSame(ArenaWinnerEvent.getHandlerList(),
                new ArenaWinnerEvent(makeArena(), UUID.randomUUID()).getHandlers());
    }
}
