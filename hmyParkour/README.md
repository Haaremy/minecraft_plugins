# hmyParkour -- Parkour

## Beschreibung

hmyParkour ist ein Parkour-System fuer den Haaremy Minecraft Server mit Checkpoints, Bestzeiten und Leaderboards. Kurse haben verschiedene Schwierigkeitsgrade und belohnen Spieler mit Coins. Druckplatten werden als Start-, Checkpoint- und Zielpunkte genutzt.

Der registrierte Hauptbefehl ist `/pk`; `plugin.yml` enthaelt ausserdem den Alias `/parcour`.

**Features:**
- Mehrere Parkour-Kurse mit Schwierigkeitsgraden (Einfach, Mittel, Schwer, Experte)
- Checkpoint-System mit Teleport zurueck zum letzten Checkpoint
- Bestzeiten-Tracking mit SQLite-Datenbank
- Top-10-Leaderboard pro Kurs
- Coin-Belohnungen (Bonus bei neuer Bestzeit)
- Live-Scoreboard mit Laufzeit, Checkpoint-Fortschritt und Bestzeit
- Automatische Kurs-Erkennung ueber Druckplatten
- Schadensschutz und Block-Schutz waehrend des Parkours
- Void-Detection mit automatischem Checkpoint-Teleport

## Commands

### Spieler-Commands

| Command | Beschreibung | Permission |
|---------|-------------|------------|
| `/pk list` | Alle verfuegbaren Kurse anzeigen (mit Schwierigkeit, Bestzeit, Coins) | - |
| `/pk join <Kurs>` | Einen Parkour-Kurs starten | - |
| `/pk quit` | Den aktuellen Kurs verlassen | - |
| `/pk checkpoint` oder `/pk cp` | Zum letzten Checkpoint teleportieren | - |
| `/pk top <Kurs>` | Top-10 Bestzeiten fuer einen Kurs anzeigen | - |

### Admin-Commands

| Command | Beschreibung | Permission |
|---------|-------------|------------|
| `/pk setup create <Name> <Schwierigkeit>` | Neuen Kurs erstellen (Startpunkt = deine Position) | `hmyparkour.admin` |
| `/pk setup end <Name>` | Zielpunkt fuer einen Kurs setzen | `hmyparkour.admin` |
| `/pk setup addcheckpoint <Name>` | Checkpoint an deiner Position hinzufuegen | `hmyparkour.admin` |
| `/pk setup delete <Name>` | Kurs loeschen | `hmyparkour.admin` |

## Berechtigungen

| Permission | Beschreibung | Default |
|-----------|-------------|---------|
| `hmyparkour.admin` | Erlaubt Parkour-Kurs-Verwaltung ueber `/pk setup` | op |

## Admin-Anleitung

### Kurs einrichten

1. Baue den Parkour-Kurs.
2. Stelle dich an den Startpunkt: `/pk setup create MeinKurs MEDIUM`
   - Schwierigkeiten: `EASY`, `MEDIUM`, `HARD`, `EXPERT`
3. Stelle dich an jeden Checkpoint (in Reihenfolge): `/pk setup addcheckpoint MeinKurs`
4. Stelle dich an das Ziel: `/pk setup end MeinKurs`
5. Der Kurs ist bereit, sobald Start und Ziel gesetzt sind.

### Druckplatten-System

Das Plugin erkennt Kurse automatisch ueber Druckplatten, wenn ein Spieler darueber laeuft:

| Druckplatte | Funktion |
|------------|----------|
| **Eiserne Druckplatte** (Heavy Weighted Pressure Plate) | Startpunkt -- startet den Kurs automatisch |
| **Goldene Druckplatte** (Light Weighted Pressure Plate) | Checkpoint -- registriert den Fortschritt |
| **Stein-Druckplatte** (Stone Pressure Plate) | Zielpunkt -- schliesst den Kurs ab |

### Schwierigkeitsgrade und Belohnungen

| Schwierigkeit | Farbe | Coins (normal) | Coins (Bestzeit-Bonus) |
|--------------|-------|----------------|----------------------|
| EASY (Einfach) | Gruen | 10 | 15 |
| MEDIUM (Mittel) | Gelb | 25 | 37 |
| HARD (Schwer) | Rot | 50 | 75 |
| EXPERT (Experte) | Dunkelrot | 100 | 150 |

## Spieler-Anleitung

### So spielst du Parkour

1. Zeige verfuegbare Kurse: `/pk list`
2. Starte einen Kurs: `/pk join <Kursname>` oder laufe ueber eine eiserne Druckplatte.
3. Springe durch den Parkour -- goldene Druckplatten sind Checkpoints.
4. Wenn du faellst: Tippe `/pk checkpoint` um zum letzten Checkpoint zurueckzukehren.
5. Faellst du ins Void (unter Y=0), wirst du automatisch zum Checkpoint teleportiert.
6. Laufe ueber die Stein-Druckplatte am Ziel um den Kurs abzuschliessen.
7. **Alle Checkpoints muessen in Reihenfolge erreicht werden!**
8. Coins und Bestzeit werden automatisch gespeichert.

### Scoreboard waehrend des Parkours

- Kursname und Schwierigkeit
- Aktuelle Laufzeit (mm:ss.ms)
- Checkpoint-Fortschritt (z.B. 3/5)
- Persoenliche Bestzeit

## Konfiguration

### courses.yml

```yaml
courses:
  meinkurs:
    difficulty: MEDIUM
    start:
      world: "parkour_world"
      x: 100.5
      y: 65.0
      z: 200.5
      yaw: 0.0
      pitch: 0.0
    end:
      world: "parkour_world"
      x: 200.5
      y: 80.0
      z: 300.5
      yaw: 0.0
      pitch: 0.0
    checkpoint-count: 3
    checkpoints:
      0:
        world: "parkour_world"
        x: 130.5
        y: 70.0
        z: 230.5
        yaw: 0.0
        pitch: 0.0
```

### Datenbank

- `plugins/hmyParkour/parkour_times.db` (SQLite)
- Tabelle `parkour_times`: `course_name TEXT, uuid TEXT, best_time_ms BIGINT, completed_at TIMESTAMP`
- Primary Key: `(course_name, uuid)`
