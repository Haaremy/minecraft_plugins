# KitsuneSegen -- Kitsune Segen

## Beschreibung

KitsuneSegen (hmyKitsuneSegen) ist ein Fortnite-aehnliches Battle-Royale-Kampfspiel auf mythologischer Grundlage fuer den Haaremy Minecraft Server. Spieler spawnen auf einer Karte, looten Truhen mit zufaelligen Items (Crossbows mit verschiedenen Raritaeten, Pfeile, Traenke) und kaempfen bis nur noch ein Spieler uebrig ist.

**Features:**
- Battle-Royale-Spielmodus fuer 2-20 Spieler
- Zwei Spawn-Modi: "random" (auf Obsidian-Bloecken) oder "flight" (Elytra-Start)
- Truhen-System mit zufaelligem Loot (normale und spezielle Truhen)
- Crossbow-Waffen in 5 Raritaetsstufen (Gewoehnlich bis Legendaer)
- 4 Crossbow-Kategorien: Multishot, Speedshot, Distanceshot, Precisionshot
- Heiltranke, Schadenstranke, Schilder als seltene Items
- Automatischer Welt-Reset nach jeder Runde (aus Backup)
- AGB-System (Spieler muessen die AGB akzeptieren bevor sie spielen koennen)
- Hub-Welt als Wartebereich mit automatischem Countdown
- Platzierungssystem mit Ergebnis-Anzeige
- Spectator-Modus fuer ausgeschiedene Spieler
- Scoreboard-Anzeige waehrend des Spiels

## Commands

### Admin-Commands

| Command | Beschreibung | Permission |
|---------|-------------|------------|
| `/game start` | Spiel sofort starten (ueberspringt Countdown) | `hmy.kitsunesegen.admin` |
| `/game countdown` | Countdown manuell starten (nur aus WAITING-Zustand) | `hmy.kitsunesegen.admin` |
| `/game stop` | Laufendes Spiel beenden oder Countdown abbrechen | `hmy.kitsunesegen.admin` |
| `/game info` | Aktuellen Spielstatus anzeigen (Zustand, lebende Spieler, Kills) | `hmy.kitsunesegen.admin` |
| `/game kick <Spieler>` | Spieler aus dem laufenden Spiel entfernen | `hmy.kitsunesegen.admin` |
| `/game reset` | Spielstatus zuruecksetzen (ohne Welt-Reset) | `hmy.kitsunesegen.admin` |

### Spieler-Commands

| Command | Beschreibung | Permission |
|---------|-------------|------------|
| `/agb accept` | Die AGB akzeptieren (einmalig pro Spieler) | - |

## Berechtigungen

| Permission | Beschreibung | Default |
|-----------|-------------|---------|
| `hmy.kitsunesegen.admin` | Vollzugriff auf alle `/game` Befehle | op |
| `hmy.kitsunesegen.play` | Erlaubt die Teilnahme am Spiel | true |
| `hmy.kitsunesegen.inventory.edit` | Erlaubt das Bearbeiten des Inventars im Spiel (Debug) | false |
| `hmy.kitsune.build` | Erlaubt Block-Platzieren/Abbauen in Hub- und Spielwelt | op |

## Admin-Anleitung

### Server einrichten

1. `hmyKitsuneSegen.jar` in den `plugins/`-Ordner des Kitsune-Servers kopieren.
2. LuckPerms muss installiert sein (Hard-Dependency).
3. Server starten -- `config.yml` wird erstellt.
4. Zwei Welten vorbereiten:
   - **Hub-Welt** (Standard: `hub`) -- Wartebereich fuer Spieler
   - **Spiel-Welt** (Standard: `game`) -- Die Battle-Royale-Karte

### Karte vorbereiten

1. **Spawnpunkte**: Platziere **Obsidian-Bloecke** in der Spielwelt. Diese werden automatisch als Spieler-Spawnpunkte erkannt.
2. **Truhen-Spots**: Platziere **Eichenbloecke** (OAK_PLANKS) an Positionen, wo Truhen spawnen sollen. Beim Spielstart werden 60% davon zu Truhen (20% davon zu speziellen Ender-Truhen).
3. **Welt-Backup**: Kopiere die fertige Spielwelt nach `world_backups/game` (oder den in der Config definierten Pfad). Nach jeder Runde wird die Welt daraus wiederhergestellt.

### Spielablauf (automatisch)

1. Spieler treten dem Server bei und landen in der Hub-Welt.
2. Bei genuegend Spielern (min-players) startet automatisch ein Countdown.
3. Nach dem Countdown werden Spieler in die Spielwelt teleportiert.
4. Truhen werden zufaellig auf der Karte gespawnt.
5. Wenn nur noch ein Spieler lebt, gewinnt dieser.
6. Platzierungen werden angezeigt, alle Spieler werden zurueck zum Hub teleportiert.
7. Die Spielwelt wird aus dem Backup wiederhergestellt.

## Spieler-Anleitung

### So spielst du Kitsune Segen

1. Betrete den Kitsune-Server ueber den Lobby-Navigator.
2. Beim ersten Besuch: Akzeptiere die AGB mit `/agb accept`.
3. Warte in der Hub-Welt bis genug Spieler da sind (min. 2).
4. Der Countdown startet automatisch.
5. Du wirst in die Spielwelt teleportiert und erhaeltst eine **Holzhacke** als Startwaffe.
6. **Suche Truhen!** Sie enthalten Crossbows, Pfeile, Traenke und mehr.
7. Besiege alle Gegner -- der letzte ueberlebende Spieler gewinnt!

### Items und Raritaeten

**Crossbow-Kategorien:**

| Kategorie | Effekt |
|----------|--------|
| Multishot | Mehrfachschuss-Verzauberung |
| Speedshot | Schnellladen-Verzauberung |
| Distanceshot | Rueckstoss-Verzauberung |
| Precisionshot | Staerke-Verzauberung |

**Raritaetsstufen:**

| Stufe | Farbe | Verzauberungslevel |
|-------|-------|-------------------|
| Gewoehnlich | Grau | 1 |
| Ungewoehnlich | Gruen | 2 |
| Selten | Blau | 3 |
| Episch | Lila | 4 |
| Legendaer | Gold | 5 |

**Truhen-Typen:**
- **Normale Truhe** (80%): Diamanten, Crossbow (niedrige Raritaet), Bauholz, Pfeile, selten Heiltrank
- **Spezielle Ender-Truhe** (20%): Diamanten, Crossbow (hoehere Raritaet), Bauholz, viele Pfeile, Heiltrank, selten Schadenstrankoder Schild

### Nach dem Tod

- Du wechselst in den **Spectator-Modus** und kannst dem Spiel weiter zuschauen.
- Slot 0: "Verlassen" (rote Farbe) -- klicke um zur Lobby zurueckzukehren.
- Slot 8: "Report" (Buch) -- zum Melden von Spielern.

## Konfiguration

### config.yml

```yaml
# Welten
hub-world: "hub"           # Name der Hub-/Wartewelt
game-world: "game"         # Name der Spielwelt
lobby-server: "lobby"      # Velocity-Server-Name der Lobby

# Welt-Backup fuer Reset nach jeder Runde
world-backup-path: "world_backups/game"

# Spieler-Einstellungen
min-players: 2             # Mindestanzahl fuer Countdown
max-players: 20            # Maximum pro Runde
max-health: 20.0           # Maximale Lebenspunkte

# Timing
countdown-seconds: 30      # Sekunden Countdown vor Spielstart
game-start-delay: 5        # Sekunden Einfrierzeit nach Teleport

# Spawn-Modus
spawn-mode: "random"       # "random" (Obsidian-Spawns) oder "flight" (Elytra-Start)
elytra-height: 100         # Flugstart-Hoehe (nur bei spawn-mode: flight)

# Block-Markierungen
spawn-block: "OBSIDIAN"         # Block-Typ fuer Spieler-Spawnpunkte
chest-spawn-block: "OAK_PLANKS" # Block-Typ fuer Truhen-Spawnorte

# Abbaubare Bloecke (Whitelist)
breakable-blocks:
  - "OAK_PLANKS"
  - "OAK_LOG"
  - "BIRCH_LOG"
  - "COBBLESTONE"
  - "STONE"
  - "DIRT"
  - "GRAVEL"
  - "SAND"
  - "GLASS"
  - "SNOW_BLOCK"
```
