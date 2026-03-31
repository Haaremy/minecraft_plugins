package de.haaremy.hmywallpaper.renderer;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class FullColorRenderer extends MapRenderer {

    private final java.awt.Color color;
    private boolean rendered = false;

    public FullColorRenderer(java.awt.Color color) {
        this.color = color;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (rendered) return;
        rendered = true;
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                canvas.setPixelColor(x, y, color);
            }
        }
    }
}
