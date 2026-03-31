package de.haaremy.hmywallpaper.renderer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class BlockColorRenderer extends MapRenderer {

    private final Material material;
    private boolean rendered = false;

    public BlockColorRenderer(Material material) {
        this.material = material;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (rendered) return;
        rendered = true;

        java.awt.Color base = getBlockColor(material);

        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int tileX = x / 8;
                int tileY = y / 8;
                int shade = (tileX + tileY) % 3;

                int r = base.getRed();
                int g = base.getGreen();
                int b = base.getBlue();

                if (shade == 0) {
                    r = Math.min(255, r + 30);
                    g = Math.min(255, g + 30);
                    b = Math.min(255, b + 30);
                } else if (shade == 2) {
                    r = Math.max(0, r - 25);
                    g = Math.max(0, g - 25);
                    b = Math.max(0, b - 25);
                }

                canvas.setPixelColor(x, y, new java.awt.Color(r, g, b));
            }
        }
    }

    private static java.awt.Color getBlockColor(Material material) {
        return switch (material) {
            // Terrain
            case GRASS_BLOCK                                              -> new java.awt.Color(127, 178,  56);
            case DIRT, COARSE_DIRT, ROOTED_DIRT                          -> new java.awt.Color(135,  96,  71);
            case STONE, COBBLESTONE, STONE_BRICKS, GRAVEL                -> new java.awt.Color(112, 112, 112);
            case SAND, SANDSTONE, SMOOTH_SANDSTONE, CUT_SANDSTONE        -> new java.awt.Color(247, 233, 163);
            case RED_SAND, RED_SANDSTONE, SMOOTH_RED_SANDSTONE           -> new java.awt.Color(190,  87,  57);
            case BEDROCK                                                  -> new java.awt.Color( 85,  85,  85);
            case DIRT_PATH                                                -> new java.awt.Color(215, 192, 120);
            case CLAY                                                     -> new java.awt.Color(164, 168, 184);
            case PODZOL                                                   -> new java.awt.Color(129,  77,  40);
            case MYCELIUM                                                 -> new java.awt.Color(111,  68,  79);
            case SNOW_BLOCK, POWDER_SNOW                                  -> new java.awt.Color(255, 255, 255);
            case ICE, PACKED_ICE, FROSTED_ICE, BLUE_ICE                  -> new java.awt.Color(160, 160, 255);
            case NETHERRACK                                               -> new java.awt.Color(112,   2,   0);
            case SOUL_SAND, SOUL_SOIL                                     -> new java.awt.Color( 84,  61,  43);
            case NETHER_BRICKS                                            -> new java.awt.Color( 79,   1,  10);
            case BASALT, SMOOTH_BASALT, POLISHED_BASALT                  -> new java.awt.Color( 66,  73,  81);
            case BLACKSTONE                                               -> new java.awt.Color( 40,  28,  36);
            case END_STONE, END_STONE_BRICKS                             -> new java.awt.Color(220, 220, 125);
            case PURPUR_BLOCK, PURPUR_PILLAR, PURPUR_SLAB                -> new java.awt.Color(169, 121, 169);
            case OBSIDIAN, CRYING_OBSIDIAN                               -> new java.awt.Color( 25,  25,  40);
            case MAGMA_BLOCK                                              -> new java.awt.Color(180,  90,   0);
            case DEEPSLATE, COBBLED_DEEPSLATE, POLISHED_DEEPSLATE,
                 DEEPSLATE_BRICKS, DEEPSLATE_TILES, CHISELED_DEEPSLATE  -> new java.awt.Color( 59,  59,  68);
            case TUFF                                                     -> new java.awt.Color(105, 104,  93);
            case CALCITE                                                  -> new java.awt.Color(221, 219, 216);
            case DRIPSTONE_BLOCK                                          -> new java.awt.Color(131,  97,  77);
            case MUD, MUDDY_MANGROVE_ROOTS                               -> new java.awt.Color( 55,  45,  50);

            // Water / Lava
            case WATER                                                    -> new java.awt.Color( 64,  64, 255);
            case LAVA                                                     -> new java.awt.Color(255,  90,   0);

            // Wood (Planks / Logs)
            case OAK_PLANKS, OAK_LOG, STRIPPED_OAK_LOG                  -> new java.awt.Color(197, 179,  95);
            case SPRUCE_PLANKS, SPRUCE_LOG, STRIPPED_SPRUCE_LOG         -> new java.awt.Color(111,  79,  41);
            case BIRCH_PLANKS, BIRCH_LOG, STRIPPED_BIRCH_LOG            -> new java.awt.Color(213, 201, 140);
            case JUNGLE_PLANKS, JUNGLE_LOG, STRIPPED_JUNGLE_LOG         -> new java.awt.Color(149, 108,  76);
            case ACACIA_PLANKS, ACACIA_LOG, STRIPPED_ACACIA_LOG         -> new java.awt.Color(168,  98,  45);
            case DARK_OAK_PLANKS, DARK_OAK_LOG, STRIPPED_DARK_OAK_LOG  -> new java.awt.Color( 66,  45,  21);
            case MANGROVE_PLANKS, MANGROVE_LOG, STRIPPED_MANGROVE_LOG   -> new java.awt.Color(107,  36,  37);
            case CHERRY_PLANKS, CHERRY_LOG, STRIPPED_CHERRY_LOG         -> new java.awt.Color(223, 173, 167);
            case BAMBOO_PLANKS, BAMBOO_BLOCK                             -> new java.awt.Color(205, 177, 100);
            case CRIMSON_PLANKS, CRIMSON_STEM, STRIPPED_CRIMSON_STEM    -> new java.awt.Color(149,  29,  29);
            case WARPED_PLANKS, WARPED_STEM, STRIPPED_WARPED_STEM       -> new java.awt.Color( 58, 142, 140);

            // Leaves / Plants
            case OAK_LEAVES, JUNGLE_LEAVES, ACACIA_LEAVES               -> new java.awt.Color(102, 127,  51);
            case SPRUCE_LEAVES                                            -> new java.awt.Color( 96, 116,  63);
            case BIRCH_LEAVES                                             -> new java.awt.Color(128, 167,  85);
            case DARK_OAK_LEAVES, MANGROVE_LEAVES                       -> new java.awt.Color( 61,  96,  31);
            case CHERRY_LEAVES                                            -> new java.awt.Color(235, 155, 180);
            case AZALEA_LEAVES, FLOWERING_AZALEA_LEAVES                 -> new java.awt.Color( 98, 139,  56);

            // Ore / Minerals
            case GOLD_BLOCK, RAW_GOLD_BLOCK                              -> new java.awt.Color(255, 215,  25);
            case IRON_BLOCK, RAW_IRON_BLOCK                              -> new java.awt.Color(192, 192, 192);
            case DIAMOND_BLOCK                                            -> new java.awt.Color( 91, 216, 210);
            case LAPIS_BLOCK                                              -> new java.awt.Color( 74, 128, 255);
            case EMERALD_BLOCK, RAW_COPPER_BLOCK                         -> new java.awt.Color(  0, 192,   0);
            case COPPER_BLOCK, EXPOSED_COPPER, WAXED_COPPER_BLOCK        -> new java.awt.Color(192, 115,  65);
            case OXIDIZED_COPPER, WAXED_OXIDIZED_COPPER                 -> new java.awt.Color( 82, 182, 135);
            case AMETHYST_BLOCK                                           -> new java.awt.Color(131,  98, 194);
            case NETHERITE_BLOCK                                          -> new java.awt.Color( 64,  58,  64);
            case COAL_BLOCK                                               -> new java.awt.Color( 25,  25,  25);
            case REDSTONE_BLOCK                                           -> new java.awt.Color(210,   0,   0);
            case QUARTZ_BLOCK, SMOOTH_QUARTZ, QUARTZ_BRICKS             -> new java.awt.Color(235, 227, 219);

            // Wool / Concrete / Terracotta
            case WHITE_WOOL,   WHITE_CONCRETE,   WHITE_TERRACOTTA        -> new java.awt.Color(235, 235, 235);
            case ORANGE_WOOL,  ORANGE_CONCRETE,  ORANGE_TERRACOTTA       -> new java.awt.Color(234, 126,  54);
            case MAGENTA_WOOL, MAGENTA_CONCRETE, MAGENTA_TERRACOTTA      -> new java.awt.Color(199,  78, 189);
            case LIGHT_BLUE_WOOL, LIGHT_BLUE_CONCRETE, LIGHT_BLUE_TERRACOTTA -> new java.awt.Color(102, 153, 216);
            case YELLOW_WOOL,  YELLOW_CONCRETE,  YELLOW_TERRACOTTA       -> new java.awt.Color(229, 229,  51);
            case LIME_WOOL,    LIME_CONCRETE,    LIME_TERRACOTTA          -> new java.awt.Color(127, 204,  25);
            case PINK_WOOL,    PINK_CONCRETE,    PINK_TERRACOTTA          -> new java.awt.Color(242, 127, 165);
            case GRAY_WOOL,    GRAY_CONCRETE,    GRAY_TERRACOTTA          -> new java.awt.Color( 76,  76,  76);
            case LIGHT_GRAY_WOOL, LIGHT_GRAY_CONCRETE, LIGHT_GRAY_TERRACOTTA -> new java.awt.Color(153, 153, 153);
            case CYAN_WOOL,    CYAN_CONCRETE,    CYAN_TERRACOTTA          -> new java.awt.Color( 22, 156, 156);
            case PURPLE_WOOL,  PURPLE_CONCRETE,  PURPLE_TERRACOTTA        -> new java.awt.Color(178,  76, 216);
            case BLUE_WOOL,    BLUE_CONCRETE,    BLUE_TERRACOTTA          -> new java.awt.Color( 51,  76, 178);
            case BROWN_WOOL,   BROWN_CONCRETE,   BROWN_TERRACOTTA         -> new java.awt.Color(102,  76,  51);
            case GREEN_WOOL,   GREEN_CONCRETE,   GREEN_TERRACOTTA         -> new java.awt.Color(102, 127,  51);
            case RED_WOOL,     RED_CONCRETE,     RED_TERRACOTTA           -> new java.awt.Color(153,  51,  51);
            case BLACK_WOOL,   BLACK_CONCRETE,   BLACK_TERRACOTTA         -> new java.awt.Color( 25,  25,  25);

            // Glass
            case GLASS                                                    -> new java.awt.Color(255, 255, 255);
            case TINTED_GLASS                                             -> new java.awt.Color( 50,  40,  60);

            // Misc
            case SPONGE, WET_SPONGE                                      -> new java.awt.Color(204, 204,  64);
            case GLOWSTONE                                                -> new java.awt.Color(209, 177,  96);
            case SHROOMLIGHT                                              -> new java.awt.Color(225, 133,  35);
            case SEA_LANTERN                                              -> new java.awt.Color(172, 207, 204);
            case BOOKSHELF, CHISELED_BOOKSHELF                           -> new java.awt.Color(109,  87,  55);
            case CRAFTING_TABLE                                           -> new java.awt.Color(160,  80,  40);
            case FURNACE, BLAST_FURNACE, SMOKER                          -> new java.awt.Color( 90,  90,  90);
            case TNT                                                      -> new java.awt.Color(200,  50,  50);
            case HAY_BLOCK                                                -> new java.awt.Color(216, 191,  32);
            case MOSS_BLOCK, MOSS_CARPET                                 -> new java.awt.Color( 90, 120,  50);
            case SCULK, SCULK_CATALYST, SCULK_SENSOR, SCULK_SHRIEKER    -> new java.awt.Color( 14,  38,  48);

            default                                                       -> new java.awt.Color(112, 112, 112);
        };
    }
}
