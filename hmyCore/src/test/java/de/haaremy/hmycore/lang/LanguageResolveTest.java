package de.haaremy.hmycore.lang;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageResolveTest {

    private static LanguageManager managerWithBundles(Properties de, Properties en) throws Exception {
        LanguageManager lm = new LanguageManager(null);
        Field f = LanguageManager.class.getDeclaredField("bundles");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Properties> bundles = (Map<String, Properties>) f.get(lm);
        bundles.clear();
        if (de != null) bundles.put("de", de);
        if (en != null) bundles.put("en", en);
        return lm;
    }

    private static Properties props(String... kv) {
        Properties p = new Properties();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            p.setProperty(kv[i], kv[i + 1]);
        }
        return p;
    }

    @Test
    void resolveReturnsLocaleSpecificValue() throws Exception {
        LanguageManager lm = managerWithBundles(
                props("core.coins.show", "<gray>Deine Coins: <gold>{coins}"),
                props("core.coins.show", "<gray>Your coins: <gold>{coins}"));
        assertEquals("<gray>Deine Coins: <gold>{coins}", lm.resolve("de", "core.coins.show"));
        assertEquals("<gray>Your coins: <gold>{coins}", lm.resolve("en", "core.coins.show"));
    }

    @Test
    void resolveFallsBackEnglishWhenLocaleMissesKey() throws Exception {
        LanguageManager lm = managerWithBundles(
                props(),
                props("core.coins.show", "<gold>{coins}"));
        assertEquals("<gold>{coins}", lm.resolve("de", "core.coins.show"));
    }

    @Test
    void resolveFallsBackGermanWhenEnglishMissesKey() throws Exception {
        LanguageManager lm = managerWithBundles(
                props("core.coins.show", "<gold>{coins} ⛁"),
                props());
        assertEquals("<gold>{coins} ⛁", lm.resolve("en", "core.coins.show"));
    }

    @Test
    void resolveReturnsKeyWhenAllBundlesMiss() throws Exception {
        LanguageManager lm = managerWithBundles(props(), props());
        assertEquals("core.unknown.key", lm.resolve("de", "core.unknown.key"));
    }

    @Test
    void resolveOnUnknownLocaleStillFallsBack() throws Exception {
        LanguageManager lm = managerWithBundles(
                props("core.player_only", "<red>nur Spieler"),
                props("core.player_only", "<red>players only"));
        assertEquals("<red>players only", lm.resolve("fr", "core.player_only"));
    }

    @Test
    void getLocaleReturnsDefaultForUnknownUuid() {
        LanguageManager lm = new LanguageManager(null);
        assertEquals(LanguageManager.DEFAULT_LOCALE,
                lm.getLocale(java.util.UUID.randomUUID()));
    }

    @Test
    void supportedLocalesContainDeAndEn() {
        assertTrue(LanguageManager.SUPPORTED.contains("de"));
        assertTrue(LanguageManager.SUPPORTED.contains("en"));
    }

    @Test
    void defaultLocaleIsDe() {
        assertEquals("de", LanguageManager.DEFAULT_LOCALE);
    }

    @Test
    void langApplyReplacesPlaceholders() {
        String out = Lang.apply("<gold>{coins} ⛁", "coins", "42");
        assertEquals("<gold>42 ⛁", out);
    }

    @Test
    void langApplyHandlesMultiplePlaceholders() {
        String out = Lang.apply("<green>{name} ({count}/{max})",
                "name", "Survival", "count", "3", "max", "12");
        assertEquals("<green>Survival (3/12)", out);
    }

    @Test
    void langApplyMissingPlaceholderLeftAsIs() {
        String out = Lang.apply("<gold>{coins}", "wrong", "42");
        assertEquals("<gold>{coins}", out);
    }

    @Test
    void langApplyEmptyPlaceholdersReturnsRaw() {
        String out = Lang.apply("<red>kein Replace");
        assertEquals("<red>kein Replace", out);
    }

    @Test
    void langApplyNullStringReturnsEmpty() {
        assertEquals("", Lang.apply(null));
    }

    @Test
    void langParseEmptyReturnsEmptyComponent() {
        assertNotNull(Lang.parse(""));
        assertEquals(net.kyori.adventure.text.Component.empty(), Lang.parse(""));
    }
}
