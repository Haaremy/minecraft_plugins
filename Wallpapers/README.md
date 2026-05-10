# hmyWallpaper

hmyWallpaper creates Minecraft map wallpapers from solid colors, block map colors, SVG templates, or downloaded images.

## Requirements

| Requirement | Notes |
|-------------|-------|
| Paper | API version 1.21+ |
| Network access | Only needed for `/wallpaper create image` |

## Installation

1. Build the plugin with `mvn -q package`.
2. Copy the generated JAR to the target Paper server's `plugins/` directory.
3. Start the server once so `plugins/hmyWallpaper/svgs/` is created.
4. Add custom SVG templates to `plugins/hmyWallpaper/svgs/` if needed.
5. Grant or revoke `hmy.wallpaper.use` via LuckPerms.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/wallpaper create full <#RRGGBB>` | Create one solid-color map | `hmy.wallpaper.use` |
| `/wallpaper create block <Material>` | Create one map using the block's map color | `hmy.wallpaper.use` |
| `/wallpaper create svg <Name> <#c1> <#c2> <#c3>` | Render an SVG template with replacement colors | `hmy.wallpaper.use` |
| `/wallpaper create image <URL> <parts>` | Download an image and split it into 1-16 map tiles | `hmy.wallpaper.use` |

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `hmy.wallpaper.use` | true | Allows `/wallpaper create` |
| `hmy.wallpaper.admin` | op | Reserved for future admin commands |

## SVG Templates

SVG files live in `plugins/hmyWallpaper/svgs/`. The command accepts the file name with or without `.svg`.

The renderer maps the first distinct `fill` colors in the SVG to the three colors passed by the player. Supported visual elements are `rect`, `circle`, `ellipse`, `polygon`, and nested `g` groups.

```bash
/wallpaper create svg checkers #1a1a2e #e94560 #ffffff
```

## Image Mode Limits

| Limit | Value |
|-------|-------|
| Protocols | `http://`, `https://` |
| Max download size | 10 MB |
| Connect timeout | 5 seconds |
| Read timeout | 15 seconds |
| Tile count | 1-16 maps |

Downloaded images are letterboxed to the selected map grid and sliced into 128x128 map tiles.
