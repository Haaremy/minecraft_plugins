package de.haaremy.hmy1v1;

import de.haaremy.hmycore.gui.HmyGui;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

public class DuelKitGui extends HmyGui {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Hmy1v1 plugin;
    private final UUID playerUuid;
    private final Consumer<DuelKit> onKitSelected;

    public DuelKitGui(Hmy1v1 plugin, UUID playerUuid, Consumer<DuelKit> onKitSelected) {
        this.plugin = plugin;
        this.playerUuid = playerUuid;
        this.onKitSelected = onKitSelected;
    }

    @Override
    public void setup() {
        createInventory(MINI.deserialize("<dark_purple><bold>Kit auswaehlen"), 1);

        DuelKit[] kits = DuelKit.values();
        // Kits zentriert in der 9er-Reihe platzieren
        int startSlot = (9 - kits.length) / 2;

        for (int i = 0; i < kits.length; i++) {
            int slot = startSlot + i;
            DuelKit kit = kits[i];

            getInventory().setItem(slot, kit.getIconItem());

            setClickHandler(slot, event -> {
                Player player = (Player) event.getWhoClicked();
                player.closeInventory();
                if (onKitSelected != null) {
                    onKitSelected.accept(kit);
                }
            });
        }
    }

    public void open(Player player) {
        setup();
        player.openInventory(getInventory());
    }
}
