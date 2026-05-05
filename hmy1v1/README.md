# hmy1v1 -- Duell-System

## Beschreibung

hmy1v1 ist ein umfangreiches 1v1-Duell-System fuer den Haaremy Minecraft Server. Spieler koennen sich direkt herausfordern oder ueber eine Warteschlange gematcht werden. Es gibt 5 verschiedene Kits, einen Ranked- und Unranked-Modus, ein ELO-System, eine Top-10-Rangliste und einen Zuschauer-Modus.

**Features:**
- 5 Kits: Iron, Diamond, Archer, Soup, Classic
- Ranked- und Unranked-Queue
- Direkte Herausforderungen mit 30-Sekunden-Timeout
- ELO-Ranking (nur im Ranked-Modus)
- Top-10-Rangliste
- Zuschauer-Modus (Spectate)
- Coins: 30 fuer Gewinner, 5 fuer Verlierer
- Kit-Auswahl ueber GUI (Inventar-Menue)

## Commands

### Spieler-Commands

| Command | Beschreibung | Permission |
|---------|-------------|------------|
| `/duel <Spieler>` | Einen Spieler zum Duell herausfordern | - |
| `/duel accept` | Herausforderung annehmen | - |
| `/duel deny` | Herausforderung ablehnen | - |
| `/duel join [ranked\|unranked]` | Warteschlange beitreten (Standard: unranked) | - |
| `/duel leave` | Warteschlange, Spiel oder Zuschauer-Modus verlassen | - |
| `/duel kit` | Kit-Auswahl-GUI oeffnen | - |
| `/duel stats [Spieler]` | Statistiken anzeigen (ELO, Siege, Niederlagen, Winrate) | - |
| `/duel top` | Top-10 ELO-Rangliste anzeigen | - |
| `/duel spectate <Spieler>` | Einem laufenden Duell zuschauen | - |

### Admin-Commands

| Command | Beschreibung | Permission |
|---------|-------------|------------|
| `/duel setup <arenaName> spawn1` | Setzt Spawn-Punkt 1 an deiner Position | `hmy.1v1.admin` |
| `/duel setup <arenaName> spawn2` | Setzt Spawn-Punkt 2 an deiner Position | `hmy.1v1.admin` |

## Berechtigungen

| Permission | Beschreibung | Default |
|-----------|-------------|---------|
| `hmy.1v1.admin` | Erlaubt Arena-Setup ueber `/duel setup` | op |

## Admin-Anleitung

### Arena einrichten

1. Baue eine Duell-Arena (geschlossener Raum empfohlen).
2. Stelle dich auf Position 1: `/duel setup MeineArena spawn1`
3. Stelle dich auf Position 2: `/duel setup MeineArena spawn2`
4. Die Arena ist sofort bereit. Mehrere Arenen werden unterstuetzt.
5. Arena-Daten werden in `plugins/hmy1v1/arenas.yml` gespeichert.

## Spieler-Anleitung

### So spielst du 1v1

**Ueber die Queue:**
1. Waehle optional ein Kit: `/duel kit` (Standard: Iron)
2. Tritt der Warteschlange bei: `/duel join` (unranked) oder `/duel join ranked`
3. Sobald ein Gegner gefunden wird, startet das Duell automatisch.

**Direkte Herausforderung:**
1. Waehle ein Kit: `/duel kit`
2. Fordere einen Spieler heraus: `/duel SpielerName`
3. Der Gegner hat 30 Sekunden um mit `/duel accept` anzunehmen oder mit `/duel deny` abzulehnen.
4. Herausforderungen nutzen das Kit des Herausforderers und sind immer unranked.

### Kits

| Kit | Ruestung | Waffen & Items |
|-----|---------|----------------|
| **Iron** | Eisen-Ruestung | Eisenschwert, 16 goldene Aepfel |
| **Diamond** | Diamant-Ruestung | Diamantschwert, 8 goldene Aepfel |
| **Archer** | Leder-Ruestung | Steinschwert, Bogen (Staerke 1, Unendlichkeit), 1 Pfeil |
| **Soup** | Eisen-Ruestung | Diamantschwert, 32 Pilzsuppen (Rechtsklick heilt 6 Herzen) |
| **Classic** | Diamant-Ruestung (Schutz 1) | Diamantschwert (Schaerfe 1), Bogen, 16 Pfeile, 5 goldene Aepfel |

### Zuschauer-Modus

- Tippe `/duel spectate <Spieler>` um einem laufenden Duell zuzuschauen.
- Du wirst in den Spectator-Modus versetzt und zur Arena-Mitte teleportiert.
- Verlasse den Zuschauer-Modus mit `/duel leave`.

### Belohnungen

- **Gewinner:** 30 Coins + ELO-Gewinn (nur Ranked)
- **Verlierer:** 5 Coins + ELO-Verlust (nur Ranked)
- Siege und Niederlagen werden immer getrackt.

## Konfiguration

### arenas.yml

```yaml
arenas:
  MeineArena:
    spawn1:
      world: "duel_world"
      x: 100.5
      y: 65.0
      z: 200.5
      yaw: 90.0
      pitch: 0.0
    spawn2:
      world: "duel_world"
      x: 110.5
      y: 65.0
      z: 200.5
      yaw: -90.0
      pitch: 0.0
```

### Datenbank

- `plugins/hmy1v1/duel_elo.db` (SQLite)
- Tabelle `duel_elo`: `uuid TEXT PRIMARY KEY, elo INTEGER DEFAULT 1000, wins INTEGER DEFAULT 0, losses INTEGER DEFAULT 0`
- Standard-ELO: 1000, K-Faktor: 32
