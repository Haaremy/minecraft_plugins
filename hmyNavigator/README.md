# hmyNavigator -- Lobby-Navigator

## Beschreibung

hmyNavigator ist das Lobby-Navigations-Plugin fuer den Haaremy Minecraft Server. Spieler erhalten beim Joinen automatisch einen Kompass, der per Rechtsklick ein Spielmodi-Menue oeffnet. Von dort koennen sie sich zu verschiedenen Servern im Velocity-Netzwerk verbinden.

**Features:**
- Automatischer Kompass beim Joinen und Respawnen (Slot konfigurierbar)
- GUI-Menue mit allen Spielmodi (3x9 Inventar mit Haaremy-Gradient-Design)
- Server-Weiterleitung ueber BungeeCord/Velocity Plugin-Messaging
- Konfigurierbare Server-Mappings (Spielmodus -> Backend-Server)
- Kompass ist nicht dropbar und durch PersistentDataContainer eindeutig identifiziert

## Commands

### Spieler-Commands

| Command | Beschreibung | Permission |
|---------|-------------|------------|
| `/navigator` | Oeffnet das Spielmodi-Menue | - |
| `/play <servername>` | Wechselt direkt zum angegebenen Spielmodus-Server | - |

## Berechtigungen

Dieses Plugin definiert keine eigenen Permissions. Alle Commands sind fuer alle Spieler verfuegbar.

## Admin-Anleitung

### Plugin einrichten

1. `hmyNavigator-1.jar` in den `plugins/`-Ordner des **Lobby-Servers** kopieren.
2. Server starten -- die `config.yml` wird automatisch erstellt.
3. Server-Mappings in der `config.yml` anpassen (siehe unten).
4. Sicherstellen, dass der `BungeeCord`-Plugin-Messaging-Channel in der Velocity/BungeeCord-Konfiguration aktiviert ist.

### Spielmodi-Menue anpassen

Das GUI-Layout ist im Code definiert (NavigatorGui.java):

| Slot | Spielmodus | Material | Ziel-Server (via Config) |
|------|-----------|----------|-------------------------|
| 10 | Sumo | Golden Boots | kitsune |
| 11 | 1v1 Duell | Diamond Sword | kitsune |
| 12 | Parkour | Leather Boots | kitsune |
| 13 | TNT Run | TNT | kitsune |
| 14 | Spleef | Diamond Shovel | kitsune |
| 16 | Survival | Grass Block | survival |

## Spieler-Anleitung

### So nutzt du den Navigator

1. Beim Betreten der Lobby erhaeltst du automatisch einen **Kompass** im konfigurierten Hotbar-Slot.
2. **Rechtsklick** mit dem Kompass oeffnet das Spielmodi-Menue.
3. Klicke auf einen Spielmodus um zum entsprechenden Server weitergeleitet zu werden.
4. Alternativ: Tippe `/navigator` um das Menue zu oeffnen oder `/play <name>` um direkt zu verbinden.

## Konfiguration

### config.yml

```yaml
# Server-Mappings: Spielmodus -> Backend-Server-Name im Velocity-Netzwerk
servers:
  sumo: kitsune
  1v1: kitsune
  parkour: kitsune
  tntrun: kitsune
  spleef: kitsune
  survival: survival

# Inventar-Slot fuer den Kompass (0-8)
compass-slot: 4
```

### Konfiguration aendern

- `servers`: Mappt den Spielmodus-Namen auf den Velocity-Backend-Server-Namen.
- `compass-slot`: Bestimmt, in welchem Hotbar-Slot der Kompass platziert wird (0 = ganz links, 8 = ganz rechts, 4 = Mitte).
