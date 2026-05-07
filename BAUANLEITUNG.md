# Bauanleitung – Lobby & Spielplattformen

Praktische Schritt-für-Schritt-Anleitung zum **physischen Aufbau** der Lobby und der Spiel-Arenen für das hmyCubed-Netzwerk (`mc.haaremy.de`). Diese Anleitung ergänzt die plugin-spezifischen READMEs (Commands, Permissions, Configs) und konzentriert sich ausschließlich auf den **Welt-Aufbau in Minecraft**.

> Für Plugin-Setup, Commands und Berechtigungen siehe das [Top-Level README](README.md) und die jeweilige Plugin-README.

---

## Inhalt

1. [Voraussetzungen & Welt-Layout](#1-voraussetzungen--welt-layout)
2. [Lobby aufbauen](#2-lobby-aufbauen)
3. [Sumo-Plattform (`hmySumo`)](#3-sumo-plattform-hmysumo)
4. [1v1-Arena (`hmy1v1`)](#4-1v1-arena-hmy1v1)
5. [TNT-Run-Arena (`hmyTNTRun`)](#5-tnt-run-arena-hmytntrun)
6. [Spleef-Arena (`hmySpleef`)](#6-spleef-arena-hmyspleef)
7. [Parkour-Kurs (`hmyParkour`)](#7-parkour-kurs-hmyparkour)
8. [KitsuneSegen-Karte (`KitsuneSegen`)](#8-kitsunesegen-karte-kitsunesegen)
9. [Welt-Backup & Rollback](#9-welt-backup--rollback)

---

## 1. Voraussetzungen & Welt-Layout

### Backend-Server

| Backend | Welten | Spielmodi |
|---------|--------|-----------|
| `lobby` | `world` (Lobby-Hub) | Hub, Lobby-Games, Navigator |
| `kitsune` | je eine Welt pro Spiel (`sumo_world`, `tntrun_world`, `spleef_world`, `duel_world`, `parkour_world`, …) | 1v1, Sumo, TNT Run, Spleef, Parkour, KitsuneSegen |
| `survival` | `world` | Survival/Vanilla |

### Empfohlene Welt-Generierung

Für die Spiel-Welten **flat empty world** mit Void-Generator nutzen:

```yaml
# bukkit.yml
worlds:
  sumo_world:
    generator: VoidWorldGenerator
  tntrun_world:
    generator: VoidWorldGenerator
  spleef_world:
    generator: VoidWorldGenerator
  duel_world:
    generator: VoidWorldGenerator
  parkour_world:
    generator: VoidWorldGenerator
```

Alternativ ein `superflat`-Preset mit nur einer `air`-Schicht. Wichtig: **kein Mob-Spawning, kein Wetter, fester Tag**.

```bash
# pro Spiel-Welt einmalig im Minecraft-Server:
/gamerule doDaylightCycle false
/gamerule doWeatherCycle false
/gamerule doMobSpawning false
/gamerule mobGriefing false
/gamerule keepInventory true
/time set day
```

### Allgemeine Regeln für jede Arena

- **Eine Arena = eindeutiger Welt-Name oder eindeutiger Bereich** (Y-Koordinate trennt Arenen mit gleichem Welt-Namen sauber).
- **Mindestabstand 100 Blöcke** zwischen Arenen, sonst überschreiben sich die Reset-Bereiche der Plugins (z. B. TNT Run resettet 40 Blöcke um den Spawn).
- **Spawn-Punkte exakt 1 Block über dem Boden setzen** (`y: BodenY + 1`), sonst spawnen Spieler im Block.

---

## 2. Lobby aufbauen

Das hmyLobby-Plugin braucht **keine** vorgegebene Form, aber feste Punkte für den Hub-Spawn, Hotbar-Items, Jukeboxes und (optional) Balloons.

### 2.1 Lobby-Welt-Layout

```
┌────────────────────────────────────┐
│           Skybox / Decke           │
│                                    │
│   [Spawn-Plattform / Hub]          │
│        ▲ (Spieler landen hier)     │
│                                    │
│   [Navigator-Statue / Portal]      │
│   [Jukebox-Bühne]                  │
│   [Shop / Daily Reward NPC]        │
│   [TicTacToe-Felder, Lottery-Crate]│
│                                    │
│   [Heißluftballon-Wege (optional)] │
└────────────────────────────────────┘
```

### 2.2 Spawn-Punkt setzen

Stell dich auf die exakte Lande-Position (1 Block über dem Boden, mit gewünschtem Yaw):

```bash
/setworldspawn
```

Anschließend in `hmySettings/lobby.yml` den Lobby-Punkt eintragen oder per Command setzen.

### 2.3 Hotbar / Navigator

Der Navigator-Kompass landet automatisch in **Slot 4** (mittig). Konfigurierbar in `plugins/hmyNavigator/config.yml`. Es muss kein physisches Item gebaut werden – das Plugin gibt das Item beim Join.

Empfehlung: **gut sichtbarer Glas-Pavillon** über dem Spawn mit Schildern „Rechtsklick mit Kompass = Spielmodi". Anfänger sehen sonst den Kompass nicht.

### 2.4 Jukebox-Bühne

Pro Lobby-Jukebox ein **physischer Jukebox-Block** platzieren:

1. Jukebox-Block setzen.
2. Daneben (Sichtweite!) eine `Note Block`-Plattform für Show-Effekt.
3. Ingame:
   ```bash
   /jukebox create lobby1
   # Mit goldenem Schwert (gibt das Plugin) auf den Jukebox-Block rechtsklicken
   /jukebox lobby1 play endless
   ```
4. Für Stream-Jukeboxes (OpenAudioMc Pflicht):
   ```bash
   /jukebox create radio1
   # rechtsklick auf jukebox
   /jukebox radio1 set stream https://example.com/stream.mp3
   /jukebox radio1 play endless
   ```

### 2.5 Heißluftballon-Routen (optional)

Mehrere Wegpunkte ergeben einen Rundflug. Stell dich an die Punkte und führe nacheinander aus:

```bash
/hmy ballon route erstellen city-tour
/hmy ballon route waypoint city-tour       # wiederholen für jeden Punkt
/hmy ballon route boarding city-tour 0     # Einstiegs-Wegpunkt
/hmy ballon route dropoff city-tour 0      # Ausstiegs-Wegpunkt
/hmy ballon route start city-tour 3        # 3 Sekunden pro Segment
```

Aufzüge funktionieren analog mit `/hmy ballon elevator …` zwischen festen Etagen.

### 2.6 Lobby-Games (TicTacToe, Crate)

- **TicTacToe**: 3×3-Feld aus zwei Block-Arten (z. B. weiß/schwarze Wolle) bauen, dann pro Feld:
  ```bash
  /lobbygame create tiktaktoe board1
  ```
  Auf jeden der 9 Slots einmal klicken (Plugin merkt sich die Position).
- **Lottery-Crate**: eine Truhe platzieren, hineinschauen und:
  ```bash
  /lobbygame create crate
  ```

### 2.7 Anti-Build

Lobby-Welt komplett **anti-build** halten. In `hmySettings/general.yml` die Welt eintragen, sodass nur OPs / Builder mit `hmy.lobby.build` bauen können.

---

## 3. Sumo-Plattform (`hmySumo`)

### Geometrie

```
   spawn1 ●─────● spawn2
          │     │
          ◯─────◯   ← Plattform (z. B. 7×7, eine Schicht)
              │
              │   30+ Blöcke Fall
              ▼
            Void
```

- **Größe:** 7×7 bis 9×9 Blöcke. Kleiner = aggressiver, größer = defensiver.
- **Material:** rutschfest, harte Optik – z. B. Andesit-Polished, Dark-Oak-Planks, Quartz-Block. Keine Slime-/Honey-Blöcke (würden Knockback brechen).
- **Rand:** **kein Zaun, keine Wand.** Spieler müssen runtergeschubst werden können.
- **Untergrund:** mindestens **30 Blöcke leerer Raum oder Void**, damit Spieler tatsächlich „verlieren".
- **Spawn-Höhe:** 1 Block über der Plattform.

### Schritte

1. Plattform 7×7 bei z. B. Y=80 bauen.
2. Auf Spawn-Position 1 stellen (eine Ecke, Blickrichtung Mitte):
   ```bash
   /sumo setup arena1 spawn1
   ```
3. Auf Spawn-Position 2 stellen (gegenüberliegende Ecke, Blickrichtung Mitte):
   ```bash
   /sumo setup arena1 spawn2
   ```
4. Mehrere Arenen? Plattform daneben mit ≥100 Block Abstand bauen, Name `arena2`, etc.

---

## 4. 1v1-Arena (`hmy1v1`)

### Geometrie

```
   ┌─────────────────────────┐
   │  ●spawn1                │
   │                         │
   │       Kampfzone         │
   │                         │
   │                spawn2●  │
   └─────────────────────────┘
```

- **Größe:** 15×15 bis 25×25, **geschlossener Raum mit Wand und Decke** (Pfeile dürfen nicht abhauen).
- **Boden:** rutschfest, neutral – Cobblestone, Stone-Bricks, Andesit. Kein Sand/Gravel (Fall-Damage-Probleme).
- **Wände:** 4–5 Blöcke hoch, danach Glas-Decke. Andernfalls schießen Archer-Kits Pfeile ins Nirvana.
- **Spawns:** an gegenüberliegenden Enden, **mit Sichtkontakt** (kein Wand zwischen Spawns).
- **Untergrund:** dicht – Spieler dürfen *nicht* in Void fallen können. Das Plugin ende-Logik basiert auf Tod, nicht auf Falling.

### Schritte

```bash
/duel setup arena1 spawn1   # Position 1, Blickrichtung Gegner
/duel setup arena1 spawn2   # Position 2, Blickrichtung Gegner
```

> **Tipp:** Mehrere Arenen in unterschiedlichen Themes (Wüste, Eishöhle, Cyber) erhöhen Replay-Wert. Min. 3 Arenen empfehlenswert.

---

## 5. TNT-Run-Arena (`hmyTNTRun`)

### Geometrie

```
       ┌─────────────────┐  ← Layer 1 (Y=100)
       │ Sand/Sandstone  │
       └─────────────────┘
              ⋮ 10 Blöcke Luft
       ┌─────────────────┐  ← Layer 2 (Y=90)
       │                 │
       └─────────────────┘
              ⋮
       ┌─────────────────┐  ← Layer 3 (Y=80)
       │                 │
       └─────────────────┘
              ⋮
              ▼ Y=70 = voidY → Elimination

       [Lobby-Wartebereich an Y=65, getrennt]
```

- **Layer-Form:** quadratisch, 20×20 bis 30×30 Blöcke. Gleiche Größe pro Layer.
- **Layer-Material:** `Sand`, `Red Sand`, `Sandstone`, `Snow Block` – ein **einziger Block-Typ** pro Layer für sauberen Reset.
- **Layer-Abstand:** **8–12 Blöcke vertikal**, damit Spieler nach dem Durchfallen nicht auf Sand-Save-Hopser landen.
- **Layer ausrichten:** Layer 2 und 3 **unter Layer 1** liegen lassen, gleiche XZ-Ausdehnung. Sonst wirkt es zufällig.
- **Reset-Radius:** 40 Blöcke um den Spawn. Andere Arenen → **mindestens 100 Blöcke entfernt**, sonst überschreiben sich Resets.
- **Wartebereich (Lobby):** kleine Plattform unterhalb von Layer 3 (z. B. Y=65), mit Sichtkontakt nach oben (Glas-Decke).

### Schritte

1. Drei gleich große Layer aus Sand bei Y=100, 90, 80 bauen.
2. Lobby-Plattform bei Y=65.
3. Stell dich auf Layer 1, mittig, dann:
   ```bash
   /tntrun setup arena1 spawn
   ```
4. Stell dich auf die Lobby-Plattform:
   ```bash
   /tntrun setup arena1 lobby
   ```
5. Layer + Void definieren:
   ```bash
   /tntrun setup arena1 layers 100 90 80 70
   ```

> **Wichtig:** Das Plugin entfernt nur Blöcke auf den genannten Y-Werten. Wenn dein Sand-Layer bei Y=100,5 statt Y=100 sitzt → kein Effekt.

---

## 6. Spleef-Arena (`hmySpleef`)

### Geometrie

```
   ┌───────────────────────┐
   │ ●spawn0    ●spawn1    │
   │                       │
   │                       │  ← Snow-Block-Boden (Y=100)
   │ ●spawn3    ●spawn2    │
   └───────────────────────┘
              │
              ▼  20 Blöcke Fall
            Void (voidY=80)
```

- **Boden:** **ausschließlich `Snow Block`** (Schneeblock, *nicht* Snow-Layer). Andere Blöcke werden vom Plugin nicht akzeptiert und nicht zurückgesetzt.
- **Größe:** 16×16 bis 24×24 Blöcke. Bei 8 Spielern: lieber 24×24.
- **Wände:** keine. Stattdessen **Glas-Käfig 5 Blöcke über dem Boden** (verhindert Pillaring), aber Boden offen lassen.
- **Spawn-Anzahl:** mindestens so viele Spawns wie Maximum-Spieler (8). Spawns gleichmäßig am Rand verteilen, Blickrichtung zur Mitte.
- **Untergrund:** mindestens **20 Blöcke Fallhöhe** unter dem Boden, dann Void.

### Schritte

1. 20×20 Snow-Block-Plattform bei Y=100 bauen.
2. Pro Spawn-Position einmal:
   ```bash
   /spleef setup arena1 addspawn
   ```
   (mindestens 2, empfohlen 8)
3. Boden + Void setzen:
   ```bash
   /spleef setup arena1 floor 100 80
   ```

> **Reset-Radius:** 40 Blöcke um den Spawn — keine zweite Spleef-Arena näher als 100 Blöcke.

---

## 7. Parkour-Kurs (`hmyParkour`)

### Aufbau-Prinzip

Der Kurs wird **per Druckplatten erkannt**, nicht über Block-Markierungen:

| Druckplatte | Funktion |
|------------|----------|
| **Heavy Weighted Pressure Plate** (Eisen) | Start |
| **Light Weighted Pressure Plate** (Gold) | Checkpoint |
| **Stone Pressure Plate** | Ziel |

### Geometrie & Schwierigkeitsgrade

| Schwierigkeit | Sprungtypen | Länge |
|---------------|-------------|-------|
| EASY | Nur 2-Block-Sprünge, viele Checkpoints | 30–60 Sekunden |
| MEDIUM | 3-Block-Sprünge, Treppe, Slime, Eis | 60–120 Sekunden |
| HARD | 4-Block-Sprünge, Head-Hitter, Ladder-Jumps | 2–4 Minuten |
| EXPERT | 4-Block-Sprünge mit Drehung, Quad-Jumps, Neo-Jumps | 4+ Minuten |

### Bau-Tipps

- **Startbereich:** Plattform 5×5, in der Mitte die Iron-Pressure-Plate. Schild „Hier starten – /pk join".
- **Checkpoints:** Gold-Pressure-Plates an gut sichtbaren Stellen (z. B. auf einer flachen Erweiterung des Sprungweges). Reihenfolge **muss eingehalten werden**, also linear bauen.
- **Ziel:** Stone-Pressure-Plate auf einer kleinen Belohnungs-Plattform.
- **Schutz:** Spieler erhalten Anti-Damage und Anti-Build während des Laufs. Trotzdem den Kurs **mit Wänden links/rechts** versehen, sonst springen Spieler aus Versehen weg.
- **Void:** Bei Fall in den Void wird auf den letzten Checkpoint teleportiert. **Kein Boden unter dem Kurs nötig** – ein offener Void-Sturz ist Teil des Designs.

### Schritte

```bash
# Auf der Iron-Pressure-Plate stehend:
/pk setup create speedrun MEDIUM

# An jedem Checkpoint nacheinander (in der Reihenfolge, in der Spieler sie erreichen):
/pk setup addcheckpoint speedrun

# Auf der Stone-Pressure-Plate stehend:
/pk setup end speedrun
```

> **Pro Tipp:** Erst den Kurs ohne Druckplatten bauen, durchspielen, dann erst Druckplatten platzieren. So verhinderst du blockierte Routen.

---

## 8. KitsuneSegen-Karte (`KitsuneSegen`)

Battle Royale für 2–20 Spieler. Zwei Welten nötig: **Hub** (Wartebereich) und **Game** (Kampf-Karte).

### 8.1 Hub-Welt (`hub`)

- Kleiner geschützter Bereich (10×10 reicht), Spieler warten hier auf den Countdown.
- **Anti-Build, Anti-Damage** (Plugin erzwingt das).
- Schilder mit Hinweis auf `/agb accept` für Erstbesucher.

### 8.2 Game-Welt (`game`) – Kampf-Karte

#### Grundlayout

```
   ┌─────────────────────────────────────┐
   │   🌳   ⛰   🏠   ⛰   🌳            │
   │      [O]            [P]             │  O = OBSIDIAN (Spieler-Spawn)
   │   ⛰    [P]   [O]    [P]   ⛰        │  P = OAK_PLANKS (Truhen-Spot)
   │      [P]   [O]   [P]                │
   │   🌳   [O]   ⛰    [O]   🌳         │
   └─────────────────────────────────────┘
```

#### Zwei Spawn-Modi

| Modus | Setup |
|-------|-------|
| `random` (Standard) | **Obsidian-Blöcke** an gewünschte Spawn-Positionen platzieren (1 pro möglichem Spieler-Spawn, also ≥ `max-players`). Spieler werden zufällig auf einen Obsidian teleportiert. |
| `flight` | Spieler starten in der Luft auf `elytra-height` (Standard 100) und landen mit Elytra. Keine Obsidian-Markierungen nötig. |

#### Truhen-Spots

- **Oak Planks** (oder konfigurierter `chest-spawn-block`) an den gewünschten Stellen platzieren.
- 60 % davon werden beim Spielstart automatisch zu Truhen, 20 % davon zu Ender-Truhen (selten = besseres Loot).
- **Mengen-Empfehlung:** ca. 4–6 Truhen-Spots pro erwartetem Spieler. Bei 20 Spielern → 80–120 Spots.
- Verteilung: nicht alle in der Mitte – sonst Blutbad zu Spielbeginn. Ränder, Häuser, versteckte Spots einbauen.

#### Bau-Tipps

- **Karten-Größe:** 200×200 bis 400×400 Blöcke. Kleiner = schneller, größer = taktischer.
- **Verschiedene Biome / Themes:** Wald, Ruine, Berg, Dorf – sonst wirkt es zu eintönig.
- **Höhenunterschiede:** Wege, Türme, Höhlen – Crossbow-Kämpfe brauchen Linie of Sight UND Cover.
- **Zerstörbare Blöcke:** Liste in `config.yml` (`breakable-blocks`). Standard erlaubt OAK_PLANKS, OAK_LOG, COBBLESTONE, STONE, DIRT, GRAVEL, SAND, GLASS, SNOW_BLOCK. Nur diese in der Karte verwenden, wenn Spieler sich durchkämpfen können sollen.

### 8.3 Welt-Backup (Pflicht!)

Nach jedem Spiel resettet das Plugin die Game-Welt aus einem Backup. Vor dem ersten Spielstart:

```bash
# Auf dem Server:
cd /root/minecraft/kitsune
cp -r game world_backups/game
```

Pfad in `config.yml`:
```yaml
world-backup-path: "world_backups/game"
```

Ändert sich die Karte → Backup neu erstellen, sonst werden Änderungen nach jedem Spiel zurückgerollt.

### 8.4 Server-Konfiguration

Mindestens setzen in `plugins/hmyKitsuneSegen/config.yml`:

```yaml
hub-world: "hub"
game-world: "game"
lobby-server: "lobby"
world-backup-path: "world_backups/game"
min-players: 2
max-players: 20
spawn-mode: "random"   # oder "flight"
```

---

## 9. Welt-Backup & Rollback

Vor jedem größeren Bau-Eingriff (besonders Production VM 111):

```bash
# auf dem betreffenden Server:
cd /root/minecraft-backups
mkdir -p prod-pre-build-$(date +%Y%m%d-%H%M%S)
cd prod-pre-build-*
tar czf kitsune-worlds.tar.gz /root/minecraft/kitsune/{hub,game,sumo_world,tntrun_world,spleef_world,duel_world,parkour_world}
```

Bei Fehler einfach das tar zurückrollen. **Niemals** ohne Backup an Spieler-besuchten Welten arbeiten.

### Reihenfolge bei neuen Arenen

1. **Erst auf VM 107 (Dev)** bauen, alle Setup-Commands ausführen, mit Test-OP-Account 1 Runde durchspielen.
2. Konfigurations-Files (`arenas.yml`, `courses.yml`) per `rsync` nach VM 111 kopieren.
3. Welt per `rsync` nach VM 111 spiegeln.
4. Auf VM 111 reload/restart, kurzes Smoke-Test.
5. Erst dann öffentlich ankündigen.

---

## Weiterführende Links

- [Top-Level README](README.md) – Plugin-Übersicht, Build-Anweisungen
- [hmyLobby README](Lobby-Plugin/README.md)
- [hmyNavigator README](hmyNavigator/README.md)
- [hmy1v1 README](hmy1v1/README.md)
- [hmySumo README](hmySumo/README.md)
- [hmyTNTRun README](hmyTNTRun/README.md)
- [hmySpleef README](hmySpleef/README.md)
- [hmyParkour README](hmyParkour/README.md)
- [KitsuneSegen README](KitsuneSegen/README.md)
