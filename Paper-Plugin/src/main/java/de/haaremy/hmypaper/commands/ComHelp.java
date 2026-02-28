package de.haaremy.hmypaper.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.List;

public class ComHelp implements CommandExecutor {

    private final List<String> bookPages;
    private final String bookTitle;
    private final String bookAuthor;

    public ComHelp(List<String> pages, String title, String author) {
        this.bookPages = pages;
        this.bookTitle = title != null ? title : "Hilfe";
        this.bookAuthor = author != null ? author : "Server";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Command ist nur für Spieler!");
            return true;
        }

        openHelpBook(player);
        return true;
    }

    public void openHelpBook(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (meta != null) {
            meta.setTitle(bookTitle);
            meta.setAuthor(bookAuthor);

            // Seiten hinzufügen und Farbcodes (Legacy §) unterstützen
            for (String pageContent : bookPages) {
                meta.addPages(LegacyComponentSerializer.legacySection().deserialize(pageContent));
            }

            book.setItemMeta(meta);
            player.openBook(book);
        }
    }
}