package de.haaremy.hmykitsunesegen;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.io.*;
import java.util.*;

/**
 * Verwaltet AGB-Zustimmungen der Spieler.
 * Speichert UUIDs persistent in plugins/hmyKitsuneSegen/agb_accepted.txt
 */
public class AgbManager {

    private final HmyKitsuneSegen plugin;
    private final Set<UUID> accepted = new HashSet<>();
    private final File file;

    public AgbManager(HmyKitsuneSegen plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "agb_accepted.txt");
        load();
    }

    public boolean hasAccepted(UUID uuid) {
        return accepted.contains(uuid);
    }

    public void accept(UUID uuid) {
        accepted.add(uuid);
        save();
    }

    // ── Persistenz ────────────────────────────────────────────────────────────

    private void load() {
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try { accepted.add(UUID.fromString(line)); }
                    catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("AGB-Datei konnte nicht geladen werden: " + e.getMessage());
        }
    }

    private void save() {
        plugin.getDataFolder().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (UUID uuid : accepted) {
                writer.write(uuid.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().warning("AGB-Datei konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    // ── Buch-Erstellung ───────────────────────────────────────────────────────

    public ItemStack createBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("AGB – mc.haaremy.de");
        meta.setAuthor("Haaremy");

        // Seite 1: Titelseite
        meta.addPages(Component.text()
                .append(Component.text("Allgemeine\nGeschäfts-\nbedingungen\n\n")
                        .color(NamedTextColor.DARK_PURPLE)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text("mc.haaremy.de\n\n")
                        .color(NamedTextColor.GRAY))
                .append(Component.text("Bitte lies alle\nSeiten sorgfältig\ndurch.")
                        .color(NamedTextColor.DARK_GRAY))
                .build());

        // Seite 2: Regeln Teil 1
        meta.addPages(Component.text()
                .append(Component.text("§ Regeln\n\n")
                        .color(NamedTextColor.DARK_RED)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text("1. ")
                        .color(NamedTextColor.DARK_RED))
                .append(Component.text("Kein Cheaten, Hacking oder Exploiting.\n\n")
                        .color(NamedTextColor.BLACK))
                .append(Component.text("2. ")
                        .color(NamedTextColor.DARK_RED))
                .append(Component.text("Respektvoller Umgang mit allen Spielern.\n\n")
                        .color(NamedTextColor.BLACK))
                .append(Component.text("3. ")
                        .color(NamedTextColor.DARK_RED))
                .append(Component.text("Keine Beleidigungen oder Hassrede.")
                        .color(NamedTextColor.BLACK))
                .build());

        // Seite 3: Regeln Teil 2
        meta.addPages(Component.text()
                .append(Component.text("§ Regeln\n\n")
                        .color(NamedTextColor.DARK_RED)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text("4. ")
                        .color(NamedTextColor.DARK_RED))
                .append(Component.text("Keine Werbung für andere Server.\n\n")
                        .color(NamedTextColor.BLACK))
                .append(Component.text("5. ")
                        .color(NamedTextColor.DARK_RED))
                .append(Component.text("Bugs müssen gemeldet, nicht ausgenutzt werden.\n\n")
                        .color(NamedTextColor.BLACK))
                .append(Component.text("Verstöße können zum permanenten Bann führen.")
                        .color(NamedTextColor.DARK_GRAY))
                .build());

        // Seite 4: Zustimmung
        meta.addPages(Component.text()
                .append(Component.text("Zustimmung\n\n")
                        .color(NamedTextColor.DARK_GREEN)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text("Mit dem Klicken auf Akzeptieren bestätigst du, dass du die AGB gelesen hast.\n\n")
                        .color(NamedTextColor.BLACK))
                .append(Component.text("► Akzeptieren ◄")
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/agb accept"))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("Klicke um den AGB zuzustimmen")
                                        .color(NamedTextColor.GREEN))))
                .build());

        book.setItemMeta(meta);
        return book;
    }
}
