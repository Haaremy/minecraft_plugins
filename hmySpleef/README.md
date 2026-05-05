# hmySpleef -- Spleef

## Beschreibung

hmySpleef ist ein Spleef-Spielmodus fuer den Haaremy Minecraft Server. Spieler erhalten eine Diamantschaufel mit Effizienz V und muessen den Schneeboden unter ihren Gegnern zerstoeren. Wer ins Void faellt, ist ausgeschieden. Der letzte ueberlebende Spieler gewinnt.

**Features:**
- Arenen mit Snow-Block-Boden und mehreren Spawn-Punkten
- Diamantschaufel (Effizienz V, Unzerstoerbar)
- Nur Snow Blocks koennen abgebaut werden (kein Block-Drop)
- Automatisches Matchmaking mit Warteschlangen-Countdown (20 Sekunden)
- 2-8 Spieler pro Runde
- Statistiken ueber hmyCore (Siege, Niederlagen, Winrate)
- 40 Coins fuer Gewinner, 5 Coins Teilnahme-Bonus fuer jeden
- Ausgeschiedene Spieler werden zu Zuschauern
- Automatischer Arena-Reset (Snow Blocks werden wiederhergestellt)

## Commands

### Spieler-Commands

| Command | Beschreibung | Permission |
|---------|-------------|------------|
| `/spleef join` | Warteschlange beitreten | - |
| `/spleef leave` | Warteschlange oder Spiel verlassen | - |
| `/spleef stats [Spieler]` | Statistiken anzeigen (Siege, Niederlagen, Winrate) | - |

### Admin-Commands

| Command | Beschreibung | Permission |
|---------|-------------|------------|
| `/spleef setup <name> addspawn` | Spawn-Punkt an deiner Position hinzufuegen | `hmy.spleef.admin` |
| `/spleef setup <name> floor <floorY> <voidY>` | Boden-Ebene (Y) und Void-Hoehe setzen | `hmy.spleef.admin` |

## Berechtigungen

| Permission | Beschreibung | Default |
|-----------|-------------|---------|
| `hmy.spleef.admin` | Erlaubt Arena-Setup ueber `/spleef setup` | op |

## Admin-Anleitung

### Arena einrichten

1. Baue eine Spleef-Arena mit einem Boden aus **Snow Blocks** (Schneeblock).
2. Darunter sollte Void oder eine tiefe Grube sein (Spieler muessen unter `voidY` fallen).
3. Fuer jeden Spieler-Spawn stelle dich an die Position und tippe:
   `/spleef setup MeineArena addspawn`
   - Wiederhole dies fuer alle gewuenschten Spawn-Positionen (min. 2 empfohlen).
4. Setze die Boden-Ebene und Void-Hoehe:
   `/spleef setup MeineArena floor 100 80`
   - `100` = Y-Position der Snow-Block-Ebene
   - `80` = Unter dieser Y-Position wird ein Spieler eliminiert

### Wichtige Hinweise

- Nur **Snow Blocks** koennen von Spielern abgebaut werden.
- Der Arena-Reset stellt nur Snow Blocks im Umkreis von 40 Bloecken wieder her.
- Spawn-Punkte werden zyklisch zugewiesen (Spieler 1 bekommt Spawn 1, Spieler 2 bekommt Spawn 2, etc.).
- Standard-Werte bei neuer Arena: `floorY` = Y des Spawns - 1, `voidY` = Y des Spawns - 20.

## Spieler-Anleitung

### So spielst du Spleef

1. Tippe `/spleef join` um der Warteschlange beizutreten.
2. Sobald mindestens 2 Spieler in der Queue sind, startet ein 20-Sekunden-Countdown.
3. Bei 8 Spielern (Maximum) wird der Countdown auf 5 Sekunden verkuerzt.
4. Nach einem 5-Sekunden-Countdown in der Arena geht es los!
5. Du erhaeltst eine **Diamantschaufel** (Effizienz V, Unzerstoerbar).
6. **Grabe den Schnee unter deinen Gegnern weg!** Nur Snow Blocks koennen abgebaut werden.
7. Wer unter die Void-Hoehe faellt, ist ausgeschieden.
8. Ausgeschiedene Spieler werden automatisch zu Zuschauern.
9. **Der letzte ueberlebende Spieler gewinnt 40 Coins!**
10. Jeder Teilnehmer erhaelt 5 Coins Teilnahme-Bonus.
11. Nach dem Spiel wirst du automatisch zur Lobby zurueckgeschickt.

### Waehrend des Spiels

- Schaden ist komplett deaktiviert (kein PvP, kein Fallschaden).
- Hunger ist deaktiviert.
- Block-Platzieren ist gesperrt.
- Item-Drops sind gesperrt.
- Waehrend des Countdowns kann nicht abgebaut werden.

## Konfiguration

### arenas.yml

```yaml
arenas:
  MeineArena:
    spawns:
      spawn0:
        world: "spleef_world"
        x: 100.5
        y: 101.0
        z: 200.5
        yaw: 0.0
        pitch: 0.0
      spawn1:
        world: "spleef_world"
        x: 110.5
        y: 101.0
        z: 200.5
        yaw: 180.0
        pitch: 0.0
    floorY: 100       # Y-Position der Snow-Block-Ebene
    voidY: 80          # Unter dieser Y-Position = Elimination
```

### Statistiken

Spleef nutzt das hmyCore-Statistiksystem mit dem Spieltyp `spleef`. Statistiken koennen mit `/spleef stats` oder `/stats spleef` angezeigt werden.
