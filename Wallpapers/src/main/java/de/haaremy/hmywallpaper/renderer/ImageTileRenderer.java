package de.haaremy.hmywallpaper.renderer;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class ImageTileRenderer extends MapRenderer {

    private final int[] rgbPixels; // pre-extracted 128×128 RGB values
    private boolean rendered = false;

    public ImageTileRenderer(int[] rgbPixels) {
        this.rgbPixels = rgbPixels;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (rendered) return;
        rendered = true;
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int rgb = rgbPixels[y * 128 + x];
                canvas.setPixelColor(x, y, new java.awt.Color(
                        (rgb >> 16) & 0xFF,
                        (rgb >> 8) & 0xFF,
                        rgb & 0xFF));
            }
        }
    }
}
