package de.haaremy.hmywallpaper.renderer;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SvgRenderer extends MapRenderer {

    private final File svgFile;
    private final java.awt.Color[] userColors;

    private boolean rendered = false;
    private int[] rgbPixels = null;

    public SvgRenderer(File svgFile, java.awt.Color color1, java.awt.Color color2, java.awt.Color color3) {
        this.svgFile = svgFile;
        this.userColors = new java.awt.Color[]{color1, color2, color3};
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (!rendered) {
            rendered = true;
            try {
                BufferedImage img = renderSvg();
                rgbPixels = new int[128 * 128];
                for (int y = 0; y < 128; y++) {
                    for (int x = 0; x < 128; x++) {
                        rgbPixels[y * 128 + x] = img.getRGB(x, y);
                    }
                }
            } catch (Exception e) {
                rgbPixels = new int[128 * 128];
                java.util.Arrays.fill(rgbPixels, 0xFF00FF);
            }
        }

        if (rgbPixels != null) {
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

    private BufferedImage renderSvg() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = factory.newDocumentBuilder().parse(svgFile);
        Element root = doc.getDocumentElement();

        double[] viewBox = parseViewBox(root);

        List<String> distinctColors = new ArrayList<>();
        collectColors(root, distinctColors);

        Map<String, java.awt.Color> colorMap = new HashMap<>();
        for (int i = 0; i < Math.min(distinctColors.size(), userColors.length); i++) {
            if (userColors[i] != null) {
                colorMap.put(distinctColors.get(i),
                        new java.awt.Color(userColors[i].getRed(), userColors[i].getGreen(), userColors[i].getBlue()));
            }
        }

        BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        g2d.setColor(java.awt.Color.BLACK);
        g2d.fillRect(0, 0, 128, 128);

        double scaleX = 128.0 / viewBox[2];
        double scaleY = 128.0 / viewBox[3];
        g2d.scale(scaleX, scaleY);
        g2d.translate(-viewBox[0], -viewBox[1]);

        renderNode(root, g2d, colorMap);
        g2d.dispose();
        return img;
    }

    private double[] parseViewBox(Element root) {
        double[] vb = {0, 0, 128, 128};
        String attr = root.getAttribute("viewBox");
        if (attr.isEmpty()) {
            String w = root.getAttribute("width");
            String h = root.getAttribute("height");
            if (!w.isEmpty()) vb[2] = parseDouble(w, 128);
            if (!h.isEmpty()) vb[3] = parseDouble(h, 128);
            return vb;
        }
        String[] parts = attr.trim().split("[\\s,]+");
        if (parts.length >= 4) {
            vb[0] = parseDouble(parts[0], 0);
            vb[1] = parseDouble(parts[1], 0);
            vb[2] = parseDouble(parts[2], 128);
            vb[3] = parseDouble(parts[3], 128);
        }
        return vb;
    }

    private void collectColors(Element element, List<String> colors) {
        String fill = element.getAttribute("fill").toLowerCase().trim();
        if (!fill.isEmpty() && !fill.equals("none") && !colors.contains(fill)) {
            colors.add(fill);
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child) {
                collectColors(child, colors);
            }
        }
    }

    private void renderNode(Element element, Graphics2D g2d, Map<String, java.awt.Color> colorMap) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element el)) continue;

            String tag = el.getTagName().replaceFirst("^.*:", "").toLowerCase();

            switch (tag) {
                case "defs", "style", "title", "desc" -> { /* skip non-visual */ }
                case "rect"    -> renderRect(el, g2d, colorMap);
                case "circle"  -> renderCircle(el, g2d, colorMap);
                case "ellipse" -> renderEllipse(el, g2d, colorMap);
                case "polygon" -> renderPolygon(el, g2d, colorMap);
                case "g" -> {
                    var saved = g2d.getTransform();
                    applyTransform(el.getAttribute("transform"), g2d);
                    renderNode(el, g2d, colorMap);
                    g2d.setTransform(saved);
                }
                default -> renderNode(el, g2d, colorMap);
            }
        }
    }

    private void renderRect(Element el, Graphics2D g2d, Map<String, java.awt.Color> colorMap) {
        java.awt.Color fill = resolveFill(el, colorMap);
        if (fill == null) return;
        double x  = parseDouble(el.getAttribute("x"), 0);
        double y  = parseDouble(el.getAttribute("y"), 0);
        double w  = parseDouble(el.getAttribute("width"), 0);
        double h  = parseDouble(el.getAttribute("height"), 0);
        double rx = parseDouble(el.getAttribute("rx"), 0);
        double ry = parseDouble(el.getAttribute("ry"), 0);
        g2d.setColor(fill);
        if (rx > 0 || ry > 0) {
            g2d.fill(new RoundRectangle2D.Double(x, y, w, h, rx * 2, ry * 2));
        } else {
            g2d.fill(new Rectangle2D.Double(x, y, w, h));
        }
    }

    private void renderCircle(Element el, Graphics2D g2d, Map<String, java.awt.Color> colorMap) {
        java.awt.Color fill = resolveFill(el, colorMap);
        if (fill == null) return;
        double cx = parseDouble(el.getAttribute("cx"), 0);
        double cy = parseDouble(el.getAttribute("cy"), 0);
        double r  = parseDouble(el.getAttribute("r"), 0);
        g2d.setColor(fill);
        g2d.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
    }

    private void renderEllipse(Element el, Graphics2D g2d, Map<String, java.awt.Color> colorMap) {
        java.awt.Color fill = resolveFill(el, colorMap);
        if (fill == null) return;
        double cx = parseDouble(el.getAttribute("cx"), 0);
        double cy = parseDouble(el.getAttribute("cy"), 0);
        double rx = parseDouble(el.getAttribute("rx"), 0);
        double ry = parseDouble(el.getAttribute("ry"), 0);
        g2d.setColor(fill);
        g2d.fill(new Ellipse2D.Double(cx - rx, cy - ry, 2 * rx, 2 * ry));
    }

    private void renderPolygon(Element el, Graphics2D g2d, Map<String, java.awt.Color> colorMap) {
        java.awt.Color fill = resolveFill(el, colorMap);
        if (fill == null) return;
        String pointsStr = el.getAttribute("points").trim();
        if (pointsStr.isEmpty()) return;
        String[] pts = pointsStr.split("[\\s,]+");
        int n = pts.length / 2;
        if (n < 3) return;
        int[] xp = new int[n];
        int[] yp = new int[n];
        for (int k = 0; k < n; k++) {
            xp[k] = (int) Double.parseDouble(pts[k * 2]);
            yp[k] = (int) Double.parseDouble(pts[k * 2 + 1]);
        }
        g2d.setColor(fill);
        g2d.fillPolygon(xp, yp, n);
    }

    private java.awt.Color resolveFill(Element el, Map<String, java.awt.Color> colorMap) {
        String fillAttr = el.getAttribute("fill").toLowerCase().trim();
        if (fillAttr.equals("none")) return null;
        java.awt.Color mapped = colorMap.get(fillAttr);
        if (mapped != null) return mapped;
        // Try to parse as hex directly (unmapped colors)
        return parseHexColor(fillAttr);
    }

    private void applyTransform(String transform, Graphics2D g2d) {
        if (transform == null || transform.isEmpty()) return;
        if (transform.startsWith("translate(")) {
            String inner = transform.substring(10);
            int end = inner.indexOf(')');
            if (end < 0) return;
            String[] parts = inner.substring(0, end).trim().split("[\\s,]+");
            double tx = parseDouble(parts[0], 0);
            double ty = parts.length > 1 ? parseDouble(parts[1], 0) : 0;
            g2d.translate(tx, ty);
        }
    }

    private java.awt.Color parseHexColor(String hex) {
        try {
            if (hex.startsWith("#")) {
                String h = hex.substring(1);
                if (h.length() == 6) {
                    return new java.awt.Color(Integer.parseInt(h, 16));
                }
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }

    private double parseDouble(String val, double def) {
        if (val == null || val.isEmpty()) return def;
        try { return Double.parseDouble(val.replaceAll("[^0-9.\\-]", "")); }
        catch (NumberFormatException e) { return def; }
    }
}
