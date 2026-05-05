# hmyTNTRun -- TNT Run

## Beschreibung

hmyTNTRun ist ein TNT-Run-Spielmodus fuer den Haaremy Minecraft Server. Spieler stehen auf einer mehrstufigen Arena -- der Boden unter ihren Fuessen verschwindet nach kurzer Verzoegerung (500ms). Wer als letzter noch steht, gewinnt. Unterstuetzt werden 2-12 Spieler pro Runde.

**Features:**
- Mehrstufige Arenen mit konfigurierbaren Ebenen (Y-Positionen)
- Bloecke verschwinden 500ms nachdem ein Spieler darueber gelaufen ist
- Automatisches Matchmaking mit Warteschlangen-Countdown (30 Sekunden)
- Statistiken ueber hmyCore (Siege, Niederlagen, Winrate)
- 50 Coins Belohnung fuer den Gewinner
- Ausgeschiedene Spieler werden zu Zuschauern (Spectator-Modus)
- Automatischer Arena-Reset nach Spielende (Bloecke werden wiederhergestellt)
- Live-Scoreboard

## Commands

### Spieler-Commands

| Command | Beschreibung | Permission |
|---------|-------------|------------|
| `/tntrun join` | Warteschlange beitreten | - |
| `/tntrun leave` | Warteschlange oder Spiel verlassen | - |
| `/tntrun stats [Spieler]` | Statistiken anzeigen (Siege, Niederlagen, Winrate) | - |

### Admin-Commands

| Command | Beschreibung | Permission |
|---------|-------------|------------|
| `/tntrun setup <name> spawn` | Spawn-Punkt fuer eine Arena setzen | `hmy.tntrun.admin` |
| `/tntrun setup <name> lobby` | Lobby-Punkt fuer eine Arena setzen | `hmy.tntrun.admin` |
| `/tntrun setup <name> layers <y1> <y2> <y3> <voidY>` | Die drei Ebenen und die Void-Hoehe setzen | `hmy.tntrun.admin` |

## Berechtigungen

| Permission | Beschreibung | Default |
|-----------|-------------|---------|
| `hmy.tntrun.admin` | Erlaubt Arena-Setup ueber `/tntrun setup` | op |

## Admin-Anleitung

### Arena einrichten

1. Baue eine TNT-Run-Arena mit **drei Ebenen** aus beliebigen Bloecken (z.B. Sand, Sandstein).
2. Setze den Spawn-Punkt (wo Spieler starten): `/tntrun setup MeineArena spawn`
3. Setze den Lobby-Punkt (wo Spieler vor dem Spiel warten): `/tntrun setup MeineArena lobby`
4. Definiere die drei Ebenen-Hoehen und die Void-Hoehe:
   `/tntrun setup MeineArena layers 100 90 80 70`
   - `100` = Y-Position der obersten Ebene
   - `90` = Y-Position der mittleren Ebene
   - `80` = Y-Position der untersten Ebene
   - `70` = Unter dieser Y-Position wird ein Spieler eliminiert

### Wichtige Hinweise

- Bloecke werden nur auf den konfigurierten Layer-Y-Positionen entfernt.
- Der Arena-Reset stellt alle entfernten Bloecke im Umkreis von 40 Bloecken um den Spawn wieder her.
- Standard-Ebenen bei neuer Arena (ohne `layers`-Befehl): Y=100, Y=90, Y=80, Void=70.
- Min. 2 Spieler, max. 12 Spieler pro Runde.

## Spieler-Anleitung

### So spielst du TNT Run

1. Tippe `/tntrun join` um der Warteschlange beizutreten.
2. Sobald mindestens 2 Spieler in der Queue sind, startet ein 30-Sekunden-Countdown.
3. Bei 12 Spielern (Maximum) wird der Countdown auf 5 Sekunden verkuerzt.
4. Nach einem 5-Sekunden-Countdown in der Arena geht es los!
5. **Laufe ueber die Bloecke** -- sie verschwinden 0,5 Sekunden nachdem du darueber gelaufen bist.
6. Falle nicht ins Void! Wer unter die Void-Hoehe faellt, ist ausgeschieden.
7. Ausgeschiedene Spieler werden automatisch zu Zuschauern.
8. **Der letzte ueberlebende Spieler gewinnt 50 Coins!**
9. Nach dem Spiel wirst du automatisch zur Lobby zurueckgeschickt.

### Waehrend des Spiels

- Kein Schaden (kein PvP, kein Fallschaden).
- Hunger ist deaktiviert.
- Block-Abbauen und -Platzieren ist gesperrt.
- Ein Scoreboard zeigt Status, Spieleranzahl und Ebenen an.

## Konfiguration

### arenas.yml

```yaml
arenas:
  MeineArena:
    spawn:
      world: "tntrun_world"
      x: 0.5
      y: 101.0
      z: 0.5
      yaw: 0.0
      pitch: 0.0
    lobby:
      world: "tntrun_world"
      x: 50.5
      y: 65.0
      z: 50.5
      yaw: 0.0
      pitch: 0.0
    layers:        # Y-Positionen der drei Ebenen
      - 100
      - 90
      - 80
    voidY: 70      # Unter dieser Y-Position = Elimination
```

### Statistiken

TNT Run nutzt das hmyCore-Statistiksystem mit dem Spieltyp `tntrun`. Statistiken koennen mit `/tntrun stats` oder `/stats tntrun` angezeigt werden.
