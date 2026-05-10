package de.haaremy.hmycore.leaderboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardManagerTest {

    @Test
    void metricFromStringAcceptsCaseInsensitiveAndTrim() {
        assertEquals(LeaderboardManager.Metric.COINS, LeaderboardManager.Metric.fromString("coins"));
        assertEquals(LeaderboardManager.Metric.COINS, LeaderboardManager.Metric.fromString("  COINS  "));
        assertEquals(LeaderboardManager.Metric.WINRATE, LeaderboardManager.Metric.fromString("winrate"));
        assertNull(LeaderboardManager.Metric.fromString("bogus"));
        assertNull(LeaderboardManager.Metric.fromString(null));
    }

    @Test
    void coinsIsTheOnlyCoinMetric() {
        assertTrue(LeaderboardManager.Metric.COINS.isCoinMetric());
        for (LeaderboardManager.Metric m : LeaderboardManager.Metric.values()) {
            if (m != LeaderboardManager.Metric.COINS) {
                assertTrue(!m.isCoinMetric(), m + " should not be a coin metric");
            }
        }
    }

    @Test
    void clampLimitBoundsToOneAndMax() {
        assertEquals(1, LeaderboardManager.clampLimit(0));
        assertEquals(1, LeaderboardManager.clampLimit(-5));
        assertEquals(10, LeaderboardManager.clampLimit(10));
        assertEquals(LeaderboardManager.MAX_LIMIT, LeaderboardManager.clampLimit(999));
        assertEquals(LeaderboardManager.MAX_LIMIT, LeaderboardManager.clampLimit(LeaderboardManager.MAX_LIMIT));
    }

    @Test
    void cacheKeyIgnoresGameTypeForCoinMetric() {
        String coinsA = LeaderboardManager.cacheKey(LeaderboardManager.Metric.COINS, null);
        String coinsB = LeaderboardManager.cacheKey(LeaderboardManager.Metric.COINS, "1v1");
        assertEquals("COINS", coinsA);
        assertEquals(coinsA, coinsB);
    }

    @Test
    void cacheKeyIncludesGameTypeForStatMetricsAndIsLowercased() {
        String a = LeaderboardManager.cacheKey(LeaderboardManager.Metric.WINS, "1v1");
        String b = LeaderboardManager.cacheKey(LeaderboardManager.Metric.WINS, "1V1");
        String c = LeaderboardManager.cacheKey(LeaderboardManager.Metric.WINS, null);
        assertEquals("WINS:1v1", a);
        assertEquals(a, b);
        assertEquals("WINS:", c);
        assertNotEquals(a, c);
    }

    @Test
    void buildSqlContainsExpectedColumnsAndPlaceholders() {
        String coins = LeaderboardManager.buildSql(LeaderboardManager.Metric.COINS);
        assertTrue(coins.contains("FROM economy"));
        assertTrue(coins.contains("ORDER BY coins DESC"));
        assertTrue(coins.endsWith("LIMIT ?"));
        assertEquals(1, countQuestionMarks(coins));

        for (LeaderboardManager.Metric m : LeaderboardManager.Metric.values()) {
            if (m == LeaderboardManager.Metric.COINS) continue;
            String sql = LeaderboardManager.buildSql(m);
            assertTrue(sql.contains("FROM stats"), m + " should query stats: " + sql);
            assertTrue(sql.contains("game_type = ?"), m + " should filter by game_type: " + sql);
            assertTrue(sql.endsWith("LIMIT ?"), m + " should end with LIMIT ?: " + sql);
            assertEquals(2, countQuestionMarks(sql), m + " should have exactly 2 placeholders");
        }
    }

    @Test
    void buildSqlForKdAndWinrateGuardsAgainstDivisionByZero() {
        String kd = LeaderboardManager.buildSql(LeaderboardManager.Metric.KD);
        assertTrue(kd.contains("(kills + deaths) > 0"), "KD must filter zero-volume rows: " + kd);
        assertTrue(kd.contains("CASE WHEN deaths = 0"), "KD must guard division: " + kd);

        String wr = LeaderboardManager.buildSql(LeaderboardManager.Metric.WINRATE);
        assertTrue(wr.contains("(wins + losses) > 0"), "WINRATE must filter zero-volume rows: " + wr);
        assertTrue(wr.contains("CAST(wins AS DOUBLE) * 100"), "WINRATE must scale to percent: " + wr);
    }

    @Test
    void leaderboardEntryStoresFieldsImmutably() {
        java.util.UUID id = java.util.UUID.randomUUID();
        LeaderboardEntry e = new LeaderboardEntry(id, "Steve", 42.0);
        assertEquals(id, e.getUuid());
        assertEquals("Steve", e.getName());
        assertEquals(42.0, e.getScore());
    }

    @Test
    void getTopCachedReturnsNullBeforeFirstRefreshAndEmptyForNullMetric() {
        LeaderboardManager mgr = new LeaderboardManager(null, 60_000L);
        assertEquals(java.util.Collections.emptyList(), mgr.getTopCached(null, null, 10));
        // Without a plugin/db, the lazy-refresh path is a no-op and the cache stays empty.
        assertNull(mgr.getTopCached(LeaderboardManager.Metric.COINS, null, 10));
        assertEquals(0, mgr.cacheSize());
    }

    @Test
    void invalidateClearsCache() {
        LeaderboardManager mgr = new LeaderboardManager(null, 60_000L);
        mgr.invalidate();
        assertEquals(0, mgr.cacheSize());
    }

    private static int countQuestionMarks(String sql) {
        int n = 0;
        for (int i = 0; i < sql.length(); i++) if (sql.charAt(i) == '?') n++;
        return n;
    }
}
