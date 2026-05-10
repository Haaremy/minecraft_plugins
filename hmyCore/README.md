# hmyCore -- Infrastruktur-Plugin

## Beschreibung

hmyCore ist das zentrale Infrastruktur-Plugin des Haaremy Minecraft Servers. Es stellt grundlegende Systeme bereit, die von allen Spielmodi-Plugins genutzt werden:

- **Economy-System** -- Coins-Verwaltung mit SQLite-Datenbank
- **Statistik-System** -- Spieler-Statistiken (Siege, Niederlagen, Kills, Tode, Spielzeit) pro Spielmodus
- **Arena-System** -- Erstellung, Verwaltung und Welt-Reset von Arenen mit Template-Welten
- **Scoreboard-System** -- Dynamische Sidebar-Scoreboards mit MiniMessage-Unterstuetzung
- **Countdown-System** -- Konfigurierbare Countdowns mit Title-Anzeige und Sound-Effekten
- **Team-System** -- Farbige Teams mit automatischer Balancierung
- **GUI-System** -- Builder-Pattern fuer klickbare Inventar-Menues
- **Lobby-Connector** -- Spieler-Weiterleitung via BungeeCord/Velocity Plugin-Messaging

## Commands

### Spieler-Commands

| Command | Beschreibung | Permission |
|---------|-------------|------------|
| `/coins` | Zeigt deine aktuelle Coin-Anzahl an | - |
| `/stats [spielmodus]` | Zeigt deine Statistiken an (optional fuer einen bestimmten Spielmodus) | - |
| `/lobby` | Sendet dich zurueck zur Lobby (via Velocity/BungeeCord) | - |

### Admin-Commands

| Command | Beschreibung | Permission |
|---------|-------------|------------|
| `/arena create <name> <gameType>` | Erstellt eine neue Arena an deiner Position | `hmy.arena.admin` |
| `/arena delete <name>` | Loescht eine Arena | `hmy.arena.admin` |
| `/arena list` | Zeigt alle Arenen mit Status und Spielerzahl | - |
| `/arena join <name>` | Einer Arena beitreten | - |
| `/arena leave` | Die aktuelle Arena verlassen | - |
| `/arena start <name>` | Eine Arena manuell starten | `hmy.arena.admin` |

## Berechtigungen

| Permission | Beschreibung | Default |
|-----------|-------------|---------|
| `hmy.arena.admin` | Erlaubt Arena-Verwaltung (create, delete, start) | op |

## Admin-Anleitung

### Plugin einrichten

1. `hmyCore-1.jar` in den `plugins/`-Ordner des Servers kopieren.
2. Server starten -- die `config.yml` und der `data.db`-Ordner werden automatisch erstellt.
3. Optional: LuckPerms installieren (Softdepend fuer erweiterte Rechte).

### Arena erstellen (hmyCore-eigenes System)

1. Gehe zur gewuenschten Position in der Arena-Welt.
2. `/arena create <name> <gameType>` -- erstellt die Arena und setzt den ersten Spawnpunkt an deiner Position.
3. Die Arena wird als YAML-Datei unter `plugins/hmyCore/arenas/<name>.yml` gespeichert.

### Template-Welten fuer Arena-Reset

Arenen koennen eine Template-Welt besitzen. Nach Spielende wird die Arena-Welt aus dem Template wiederhergestellt:
1. Kopiere die fertige Arena-Welt als Template in den Server-Ordner.
2. Setze `templateWorld` in der Arena-YAML-Datei.
3. Der ArenaManager entlaedt die Welt, loescht den Ordner, kopiert das Template und laedt die Welt neu.

## Spieler-Anleitung

### Coins

- Coins werden durch das Spielen von Spielmodi verdient (z.B. Sumo-Sieg = 25 Coins).
- Zeige deine Coins mit `/coins` an.

### Statistiken

- Tippe `/stats` fuer eine Uebersicht deiner Standard-Statistiken.
- Tippe `/stats sumo` oder `/stats tntrun` fuer spielmodus-spezifische Statistiken.
- Angezeigt werden: Siege, Niederlagen, Winrate, Kills, Tode, K/D und Spielzeit.

### Zurueck zur Lobby

- Tippe `/lobby` um jederzeit zurueck zur Lobby zu gelangen.

## Konfiguration

### config.yml

```yaml
# Economy settings
economy:
  starting-coins: 0        # Startguthaben fuer neue Spieler

# Arena settings
arena:
  countdown-seconds: 5     # Standard-Countdown bei Arena-Start

# Messages prefix (MiniMessage-Format)
prefix: "<gradient:#ff6b35:#a8d8ea>hmyCubed</gradient> <dark_gray>|</dark_gray> "
```

### arenas/<name>.yml (pro Arena)

```yaml
name: "MeineArena"
world: "world"
gameType: "sumo"
minPlayers: 2
maxPlayers: 16
templateWorld: "arena_template"   # Optional: Template-Welt fuer Reset
spawnPoints:
  0:
    x: 100.5
    y: 65.0
    z: 200.5
    yaw: 90.0
    pitch: 0.0
```

### Datenbank

- `plugins/hmyCore/data.db` (SQLite) -- Enthaelt die Tabellen `economy` und `stats`.
- Economy: `uuid TEXT PRIMARY KEY, coins INTEGER`
- Stats: `uuid TEXT, game_type TEXT, wins INTEGER, losses INTEGER, kills INTEGER, deaths INTEGER, playtime_seconds INTEGER`
