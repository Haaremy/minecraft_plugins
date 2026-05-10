package de.haaremy.hmycore.lang;

import de.haaremy.hmycore.HmyCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class Lang {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private Lang() {}

    public static String localeOf(CommandSender sender) {
        if (sender instanceof Player p) {
            return localeOf(p.getUniqueId());
        }
        return LanguageManager.DEFAULT_LOCALE;
    }

    public static String localeOf(UUID uuid) {
        LanguageManager lm = HmyCore.getInstance().getLanguageManager();
        if (lm == null) return LanguageManager.DEFAULT_LOCALE;
        return lm.getLocale(uuid);
    }

    public static String get(CommandSender sender, String key, String... placeholders) {
        return get(localeOf(sender), key, placeholders);
    }

    public static String get(String locale, String key, String... placeholders) {
        LanguageManager lm = HmyCore.getInstance().getLanguageManager();
        String raw = lm != null ? lm.resolve(locale, key) : key;
        return apply(raw, placeholders);
    }

    public static String apply(String raw, String... placeholders) {
        if (raw == null) return "";
        if (placeholders == null || placeholders.length == 0) return raw;
        String s = raw;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            s = s.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return s;
    }

    public static Component component(CommandSender sender, String key, String... placeholders) {
        return component(localeOf(sender), key, placeholders);
    }

    public static Component component(String locale, String key, String... placeholders) {
        return parse(get(locale, key, placeholders));
    }

    public static Component parse(String s) {
        if (s == null || s.isEmpty()) return Component.empty();
        if (s.indexOf('§') >= 0) {
            return LegacyComponentSerializer.legacySection().deserialize(s);
        }
        return MINI.deserialize(s);
    }

    public static void send(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(component(sender, key, placeholders));
    }
}
