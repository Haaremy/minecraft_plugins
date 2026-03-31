package de.haaremy.hmywallpaper;

import de.haaremy.hmywallpaper.renderer.BlockColorRenderer;
import de.haaremy.hmywallpaper.renderer.FullColorRenderer;
import de.haaremy.hmywallpaper.renderer.ImageTileRenderer;
import de.haaremy.hmywallpaper.renderer.SvgRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WallpaperCommand implements CommandExecutor {

    private static final int MAX_PARTS   = 16;
    private static final int MAX_BYTES   = 10 * 1024 * 1024; // 10 MB
    private static final int CONN_TIMEOUT = 5_000;
    private static final int READ_TIMEOUT = 15_000;

    private final HmyWallpaper plugin;

    public WallpaperCommand(HmyWallpaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return true;
        }

        if (!player.hasPermission("hmy.wallpaper.use")) {
            player.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("create")) {
            sendHelp(player);
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "full"  -> handleFull(player, args);
            case "block" -> handleBlock(player, args);
            case "svg"   -> handleSvg(player, args);
            case "image" -> handleImage(player, args);
            default      -> sendHelp(player);
        }
        return true;
    }

    // ====== FULL ======

    private void handleFull(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /wallpaper create full <#RRGGBB>");
            return;
        }
        Color color = parseHex(args[2]);
        if (color == null) {
            player.sendMessage("§cUngültige Farbe. Beispiel: §e#FF5733");
            return;
        }
        giveMap(player, new FullColorRenderer(color), null);
        player.sendMessage("§6✓ Wallpaper §e(full) §6erstellt!");
    }

    // ====== BLOCK ======

    private void handleBlock(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /wallpaper create block <Material>");
            return;
        }
        Material material = Material.matchMaterial(args[2].toUpperCase());
        if (material == null || !material.isBlock()) {
            player.sendMessage("§cUnbekannter Block: §e" + args[2]);
            return;
        }
        giveMap(player, new BlockColorRenderer(material), null);
        player.sendMessage("§6✓ Wallpaper §e(block: " + material.name().toLowerCase() + ") §6erstellt!");
    }

    // ====== SVG ======

    private void handleSvg(Player player, String[] args) {
        if (args.length < 6) {
            player.sendMessage("§cUsage: /wallpaper create svg <Name> <#c1> <#c2> <#c3>");
            return;
        }
        String name = args[2];
        if (!name.endsWith(".svg")) name += ".svg";

        File svgFile = new File(plugin.getSvgsDir(), name);
        if (!svgFile.exists()) {
            player.sendMessage("§cSVG nicht gefunden: §e" + name
                    + " §c(Ordner: plugins/hmyWallpaper/svgs/)");
            return;
        }

        Color c1 = parseHex(args[3]);
        Color c2 = parseHex(args[4]);
        Color c3 = parseHex(args[5]);

        if (c1 == null || c2 == null || c3 == null) {
            player.sendMessage("§cUngültige Farbe. Alle drei müssen gültige Hex-Werte sein. Beispiel: §e#FF0000");
            return;
        }

        giveMap(player, new SvgRenderer(svgFile, c1, c2, c3), null);
        player.sendMessage("§6✓ Wallpaper §e(svg: " + args[2] + ") §6erstellt!");
    }

    // ====== IMAGE ======

    private void handleImage(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§cUsage: /wallpaper create image <URL> <Teile (1-" + MAX_PARTS + ")>");
            return;
        }

        String urlStr = args[2];
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
            player.sendMessage("§cNur §ehttp://§c und §ehttps://§c URLs erlaubt.");
            return;
        }

        int parts;
        try {
            parts = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c<Teile> muss eine Ganzzahl sein.");
            return;
        }
        if (parts < 1 || parts > MAX_PARTS) {
            player.sendMessage("§c<Teile> muss zwischen §e1§c und §e" + MAX_PARTS + "§c liegen.");
            return;
        }

        final int finalParts = parts;
        player.sendMessage("§7⏳ Bild wird heruntergeladen...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int[] gridResult = new int[2];
                int[][] tiles = downloadAndSliceGrid(urlStr, finalParts, gridResult);

                Bukkit.getScheduler().runTask(plugin, () ->
                        giveTileMaps(player, tiles, gridResult[0], gridResult[1]));

            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage("§cFehler: §e" + msg));
            }
        });
    }

    private int[][] downloadAndSliceGrid(String urlStr, int parts, int[] gridOut) throws Exception {
        // Download
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONN_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestProperty("User-Agent", "hmyWallpaper/1");
        conn.setInstanceFollowRedirects(true);

        int status = conn.getResponseCode();
        if (status / 100 != 2) throw new IllegalArgumentException("HTTP " + status);

        long contentLen = conn.getContentLengthLong();
        if (contentLen > MAX_BYTES) throw new IllegalArgumentException("Bild zu groß (max 10 MB)");

        BufferedImage src;
        try (InputStream in = conn.getInputStream()) {
            byte[] buf = new byte[8192];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int n, total = 0;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > MAX_BYTES) throw new IllegalArgumentException("Bild zu groß (max 10 MB)");
                baos.write(buf, 0, n);
            }
            src = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
        }
        if (src == null) throw new IllegalArgumentException("Unbekanntes oder ungültiges Bildformat");

        // Determine grid
        int[] grid = calcGrid(src.getWidth(), src.getHeight(), parts);
        int cols = grid[0], rows = grid[1];
        gridOut[0] = cols;
        gridOut[1] = rows;

        // Letterbox to grid canvas
        BufferedImage canvas = letterbox(src, cols * 128, rows * 128);

        // Slice into tiles
        int[][] tiles = new int[parts][];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                BufferedImage tile = canvas.getSubimage(col * 128, row * 128, 128, 128);
                tiles[row * cols + col] = tile.getRGB(0, 0, 128, 128, null, 0, 128);
            }
        }
        return tiles;
    }

    private void giveTileMaps(Player player, int[][] tiles, int cols, int rows) {
        int total = tiles.length;
        int dropped = 0;

        for (int i = 0; i < total; i++) {
            int col = i % cols + 1;
            int row = i / cols + 1;

            MapView mapView = Bukkit.createMap(player.getWorld());
            mapView.setScale(MapView.Scale.CLOSEST);
            mapView.setTrackingPosition(false);
            mapView.setUnlimitedTracking(false);
            mapView.getRenderers().clear();
            mapView.addRenderer(new ImageTileRenderer(tiles[i]));

            ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
            MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
            mapMeta.setMapView(mapView);
            mapMeta.setDisplayName("§6Bild §e" + col + "," + row + " §7(" + (i + 1) + "/" + total + ")");
            mapItem.setItemMeta(mapMeta);

            var leftover = player.getInventory().addItem(mapItem);
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), item);
                dropped++;
            }
        }

        player.sendMessage("§6✓ §e" + total + " §6Karten erstellt §7(" + cols + "×" + rows + " Raster)"
                + (dropped > 0 ? " §e– " + dropped + " auf den Boden gelegt (Inventar voll)" : ""));
    }

    // ====== HELPERS ======

    /** Finds the factor pair (cols, rows) of parts whose aspect ratio best matches imgW:imgH. */
    private static int[] calcGrid(int imgW, int imgH, int parts) {
        double imgAspect = (imgW == 0 || imgH == 0) ? 1.0 : (double) imgW / imgH;
        int bestCols = 1, bestRows = parts;
        double bestDiff = Math.abs(imgAspect - (double) bestCols / bestRows);

        for (int cols = 1; cols <= parts; cols++) {
            if (parts % cols != 0) continue;
            int rows = parts / cols;
            double diff = Math.abs(imgAspect - (double) cols / rows);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestCols = cols;
                bestRows = rows;
            }
        }
        return new int[]{bestCols, bestRows};
    }

    /** Scales src to fill targetW×targetH with letterboxing (black bars, aspect preserved). */
    private static BufferedImage letterbox(BufferedImage src, int targetW, int targetH) {
        BufferedImage result = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = result.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, targetW, targetH);

        double scale = Math.min((double) targetW / src.getWidth(), (double) targetH / src.getHeight());
        int scaledW = (int) (src.getWidth() * scale);
        int scaledH = (int) (src.getHeight() * scale);
        int offsetX = (targetW - scaledW) / 2;
        int offsetY = (targetH - scaledH) / 2;

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(src, offsetX, offsetY, scaledW, scaledH, null);
        g2d.dispose();
        return result;
    }

    private void giveMap(Player player, MapRenderer renderer, String name) {
        MapView mapView = Bukkit.createMap(player.getWorld());
        mapView.setScale(MapView.Scale.CLOSEST);
        mapView.setTrackingPosition(false);
        mapView.setUnlimitedTracking(false);
        mapView.getRenderers().clear();
        mapView.addRenderer(renderer);

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
        mapMeta.setMapView(mapView);
        if (name != null) mapMeta.setDisplayName(name);
        mapItem.setItemMeta(mapMeta);

        player.getInventory().setItemInMainHand(mapItem);
    }

    private Color parseHex(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            if (h.length() != 6) return null;
            int rgb = Integer.parseInt(h, 16);
            return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6=== hmyWallpaper ===");
        player.sendMessage("§e/wallpaper create full <#RRGGBB> §7- Vollfarbe");
        player.sendMessage("§e/wallpaper create block <Material> §7- Block-Kartenfarbe");
        player.sendMessage("§e/wallpaper create svg <Name> <#c1> <#c2> <#c3> §7- SVG-Muster");
        player.sendMessage("§e/wallpaper create image <URL> <Teile> §7- Bild auf mehrere Karten");
    }
}
