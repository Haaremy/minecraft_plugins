# hmySumo -- Sumo 1v1

## Beschreibung

hmySumo ist ein Sumo-Spielmodus fuer den Haaremy Minecraft Server. Zwei Spieler treten in einer Knockback-Arena gegeneinander an. Ziel ist es, den Gegner von der Plattform ins Void zu stossen. Das Spiel laeuft im **Best-of-3**-Modus (erster Spieler mit 2 Punkten gewinnt). Schaden wird auf 0 gesetzt -- nur der Knockback zaehlt.

**Features:**
- Automatisches Matchmaking ueber Warteschlange
- Best-of-3 Rundensystem
- ELO-Ranking mit eigener SQLite-Datenbank (K-Faktor: 32)
- Coins-Belohnung (25 Coins fuer den Gewinner)
- Live-Scoreboard waehrend des Spiels
- Countdown mit Title-Anzeige ueber hmyCore
- Automatische Lobby-Weiterleitung nach Spielende

## Commands

### Spieler-Commands

| Command | Beschreibung | Permission |
|---------|-------------|------------|
| `/sumo join` | Warteschlange beitreten | - |
| `/sumo leave` | Warteschlange oder Spiel verlassen | - |
| `/sumo stats [Spieler]` | Eigene oder fremde Statistiken anzeigen (ELO, Siege, Niederlagen, Winrate) | - |

### Admin-Commands

| Command | Beschreibung | Permission |
|---------|-------------|------------|
| `/sumo setup <arenaName> spawn1` | Setzt Spawn-Punkt 1 fuer eine Arena an deiner Position | `hmy.sumo.admin` |
| `/sumo setup <arenaName> spawn2` | Setzt Spawn-Punkt 2 fuer eine Arena an deiner Position | `hmy.sumo.admin` |

## Berechtigungen

| Permission | Beschreibung | Default |
|-----------|-------------|---------|
| `hmy.sumo.admin` | Erlaubt Arena-Setup ueber `/sumo setup` | op |

## Admin-Anleitung

### Arena einrichten

1. Baue eine Sumo-Plattform ueber dem Void (oder mit genug Fallhoehe unter Y=0).
2. Stelle dich auf die Position fuer Spieler 1 und tippe: `/sumo setup MeineArena spawn1`
3. Stelle dich auf die Position fuer Spieler 2 und tippe: `/sumo setup MeineArena spawn2`
4. Die Arena ist sofort einsatzbereit. Es koennen mehrere Arenen erstellt werden.

### Wichtige Hinweise

- Spieler fallen ins Void (unter Y=0) um eliminiert zu werden.
- Beide Spawns muessen gesetzt sein, damit die Arena funktioniert.
- Bei mehreren Arenen werden freie Arenen automatisch zugeteilt.
- Die Arena-Daten werden in `plugins/hmySumo/arenas.yml` gespeichert.

## Spieler-Anleitung

### So spielst du Sumo

1. Tippe `/sumo join` um der Warteschlange beizutreten.
2. Sobald ein zweiter Spieler beitritt und eine Arena frei ist, startet automatisch ein Match.
3. Nach einem 3-Sekunden-Countdown beginnt der Kampf.
4. **Stosse deinen Gegner von der Plattform!** Schaden gibt es nicht -- nur Knockback.
5. Wer ins Void faellt (unter Y=0), verliert die Runde.
6. **Best-of-3:** Wer zuerst 2 Runden gewinnt, gewinnt das Spiel.
7. Der Gewinner erhaelt **25 Coins** und **ELO-Punkte**.
8. Nach dem Spiel wirst du automatisch zur Lobby zurueckgeschickt.

### Waehrend des Spiels

- Bewegung ist waehrend des Countdowns eingefroren.
- Hunger wird deaktiviert.
- Item-Drops sind gesperrt.
- Ein Scoreboard zeigt Rundenstand, Score und Status an.

## Konfiguration

### arenas.yml

```yaml
arenas:
  MeineArena:
    spawn1:
      world: "sumo_world"
      x: 100.5
      y: 65.0
      z: 200.5
      yaw: 90.0
      pitch: 0.0
    spawn2:
      world: "sumo_world"
      x: 110.5
      y: 65.0
      z: 200.5
      yaw: -90.0
      pitch: 0.0
```

### Datenbank

- `plugins/hmySumo/sumo_elo.db` (SQLite)
- Tabelle `sumo_elo`: `uuid TEXT PRIMARY KEY, elo INTEGER DEFAULT 1000, wins INTEGER DEFAULT 0, losses INTEGER DEFAULT 0`
- Standard-ELO: 1000, K-Faktor: 32
