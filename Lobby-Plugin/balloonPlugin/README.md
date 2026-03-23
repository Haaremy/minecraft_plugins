# 🎈 HotAirBallon Auto-Travel Plugin

Ein cooles Minecraft Paper-Plugin für automatische Heißluftballon-Routen mit mehreren Ballons, die konstant eine feste Route fliegen! **Mit flexiblem Boarding/Dropoff System!**

## Features ✨

- 🚀 **Minecart-basiert** - Die Ballons sind fliegende Minecarts
- 📍 **Auto-Travel Routen** - Mehrere Ballons fliegen **KONSTANT und WIEDERHOLT** die gleiche Route
- 🛫 **Flexible Zonen** - Manuelle Auswahl: Welche Waypoints sind Boarding/Dropoff?
- 👥 **Automatisches Boarding** - Spieler werden automatisch aufgegriffen wenn sie in einer Zone sind
- 🎫 **Automatisches Dropoff** - Spieler werden automatisch abgesetzt
- 🟢 **Visuelle Zonen** - Grüne Partikel (Boarding) & Blaue Partikel (Dropoff)
- 🎵 **Sound & Partikel** - Hebt-ab Sound, Flug-Sounds, Ankunfts-Feuerwerk
- ✨ **Synchrone Bewegung** - Alle Ballons fliegen gleichzeitig
- ⏸️ **Automatische Landungen** - Ballons halten 2 Sekunden an jedem Waypoint an

## Installation 📦

1. **Kompilieren:**
   ```bash
   mvn clean install
   ```

2. **JAR in den Server kopieren:**
   ```bash
   cp target/hotairballon-1.0.jar /dein/server/plugins/
   ```

3. **Server starten**

## Befehle 🎮

### Route erstellen
```
/ballon route erstellen <RouteName>
```
Erstellt eine neue leere Route.

**Beispiel:**
```
/ballon route erstellen lobby-tour
```

---

### Waypoint zur Route hinzufügen
```
/ballon route waypoint <RouteName>
```
Speichert deine aktuelle Position als Waypoint. Es werden KEINE automatischen Zonen erstellt!

Wiederhole den Befehl mehrfach, um mehrere Waypoints hinzuzufügen:

**Beispiel:**
```
/ballon route waypoint lobby-tour  # Waypoint #0
teleport zu anderem Ort
/ballon route waypoint lobby-tour  # Waypoint #1
teleport zu nächstem Ort
/ballon route waypoint lobby-tour  # Waypoint #2
```

---

### 🟢 Boarding-Zone aktivieren
```
/ballon route boarding <RouteName> <WaypointNummer>
```
Markiert einen Waypoint als **Boarding-Zone** (grüne Partikel).

Spieler in dieser Zone werden automatisch auf den nächsten Ballon aufgegriffen!

**Beispiel - Waypoint #0 und #1 als Boarding aktivieren:**
```
/ballon route boarding lobby-tour 0
/ballon route boarding lobby-tour 1
```

---

### 🔵 Dropoff-Zone aktivieren
```
/ballon route dropoff <RouteName> <WaypointNummer>
```
Markiert einen Waypoint als **Dropoff-Zone** (blaue Partikel).

Spieler in dieser Zone werden automatisch abgesetzt (wenn Ballon hält)!

**Beispiel - Waypoint #1 und #2 als Dropoff aktivieren:**
```
/ballon route dropoff lobby-tour 1
/ballon route dropoff lobby-tour 2
```

---

### Route Waypoints anzeigen
```
/ballon route list <RouteName>
```
Zeigt alle Waypoints, Boarding und Dropoff Zonen einer Route.

**Beispiel:**
```
/ballon route list lobby-tour
```

**Output:**
```
=== Route 'lobby-tour' ===
Status: ✗ Inaktiv
Waypoints:
  #0 → X: 100 Y: 64 Z: 200
  #1 → X: 150 Y: 80 Z: 250
  #2 → X: 200 Y: 70 Z: 300
```

---

### 🚀 Auto-Travel starten
```
/ballon route start <RouteName> <AnzahlBallons>
```
**Startet die Auto-Travel Route mit mehreren Ballons!**

Die markierten Zonen werden mit Partikeln angezeigt:
- 🟢 **Grüne Kreise** = Boarding-Zonen
- 🔵 **Blaue Kreise** = Dropoff-Zonen

**Beispiel - 3 Ballons starten:**
```
/ballon route start lobby-tour 3
```

---

### Auto-Travel stoppen
```
/ballon route stop <RouteName>
```
Beendet das Auto-Travel und entfernt alle Ballons der Route.

**Beispiel:**
```
/ballon route stop lobby-tour
```

---

### Aktive Routen anzeigen
```
/ballon route info
```
Zeigt alle Routen mit Status, Waypoints und Zonenzahl.

**Output:**
```
=== Aktive Routen ===
  lobby-tour [✓ Aktiv] - 3 Waypoints, 2 Boarding, 2 Dropoff
  spawn-tour [✗ Inaktiv] - 5 Waypoints, 3 Boarding, 3 Dropoff
```

---

## 🎯 Praktisches Setup-Beispiel

Hier ein komplettes Beispiel für eine 3-Station-Tour:

```bash
# === SETUP PHASE ===

# 1. Route erstellen
/ballon route erstellen city-tour

# 2. Waypoints definieren (nur Zwischenpunkte, KEINE Zonen!)
# Station 1: Spawn
/ballon route waypoint city-tour

# Station 2: Shop
/ballon route waypoint city-tour

# Station 3: Arena
/ballon route waypoint city-tour

# 3. Nur BESTIMMTE Waypoints als Zonen markieren!

# Station 1 (Spawn): Boarding aktivieren ← Spieler steigen hier ein
/ballon route boarding city-tour 0

# Station 2 (Shop): BOTH aktivieren ← Spieler steigen aus/ein
/ballon route dropoff city-tour 1
/ballon route boarding city-tour 1

# Station 3 (Arena): Dropoff aktivieren ← Spieler steigen nur aus
/ballon route dropoff city-tour 2

# === LAUFZEIT PHASE ===

# 4. Überprüfen
/ballon route list city-tour

# 5. Auto-Travel mit 5 Ballons starten!
/ballon route start city-tour 5

# Jetzt:
# - Grüne Partikel bei Waypoint #0 und #1 (Boarding)
# - Blaue Partikel bei Waypoint #1 und #2 (Dropoff)
# - Spieler können natürlich rauf/runter fahren!
```

---

## 🔄 Ablauf für Spieler

```
Spieler-Perspektive:

1. STATION 1 (Spawn - Boarding Zone)
   ├─ Sehe grüne Partikel ✨
   ├─ Gehe rein (≤5 Blöcke)
   └─ Automatisch auf Ballon! 🎈

2. FLUG ZU STATION 2
   ├─ ☁️ Cloud-Partikel während Flug
   ├─ 🔊 Flug-Sounds
   └─ ➡️ Ballon fliegt los

3. STATION 2 (Shop - Boarding & Dropoff Zone)
   ├─ ✨ Feuerwerk + Sound (Ankunft)
   ├─ ⏸️ Ballon hält 2 Sekunden
   ├─ Sehe blaue Partikel (Dropoff) ✨
   ├─ Optional: Gehe rein und steige aus
   ├─ Optional: Andere Spieler können einsteigen (Boarding)
   └─ Oder: Bleibe drin für nächsten Flug

4. FLUG ZU STATION 3
   ├─ ☁️ Cloud-Partikel
   ├─ 🔊 Flug-Sounds
   └─ ➡️ Ballon fliegt los

5. STATION 3 (Arena - Dropoff Zone)
   ├─ ✨ Feuerwerk + Sound
   ├─ ⏸️ Ballon hält 2 Sekunden
   ├─ Sehe nur blaue Partikel (kein Boarding!)
   ├─ Gehe rein → Automatisch abgesetzt
   └─ Fertig! 🎉
```

---

## Zone-System erklärt 🎫

### Boarding-Zone (🟢 Grün)
- **Was:** Spieler werden hier automatisch aufgegriffen
- **Aktiv:** Immer (Ballon fliegt)
- **Aktion:** Gehe rein → auf Ballon
- **Radius:** 5 Blöcke

### Dropoff-Zone (🔵 Blau)
- **Was:** Spieler werden hier automatisch abgesetzt
- **Aktiv:** Nur wenn Ballon hält (2 Sekunden Pause)
- **Aktion:** Gehe rein → von Ballon
- **Radius:** 5 Blöcke

### Kombination
- **Beide aktiv:** Spieler können ein- und aussteigen
- **Nur Boarding:** Endstation (nur aussteigen mit `/ballon exit`)
- **Nur Dropoff:** Nur zum Aussteigen
- **Keine:** Ballon fliegt vorbei (Zwischenpunkt)

---

## Praktische Beispiele 🎯

### Beispiel 1: Einfache 3er-Route (Alle sind Zonen)
```
Waypoint #0 (Spawn)
├─ Boarding: JA ✓
└─ Dropoff: NEIN

Waypoint #1 (Mitte)
├─ Boarding: JA ✓
└─ Dropoff: JA ✓

Waypoint #2 (Ende)
├─ Boarding: NEIN
└─ Dropoff: JA ✓
```

### Beispiel 2: Tour mit Zwischenpunkten (Nur Start & Ende)
```
Waypoint #0 (Start)
├─ Boarding: JA ✓
└─ Dropoff: NEIN

Waypoint #1 (Schöne Aussicht - kein Halt!)
├─ Boarding: NEIN
└─ Dropoff: NEIN
└─ → Ballon fliegt einfach vorbei!

Waypoint #2 (End)
├─ Boarding: NEIN
└─ Dropoff: JA ✓
```

### Beispiel 3: Kreisförmige Route (Alle haben beide)
```
Waypoint #0 ├─ Boarding: JA ✓
           └─ Dropoff: JA ✓

Waypoint #1 ├─ Boarding: JA ✓
           └─ Dropoff: JA ✓

Waypoint #2 ├─ Boarding: JA ✓
           └─ Dropoff: JA ✓
```

---

## Technische Details 🔧

- **Java-Version:** 17+
- **Server:** Paper 1.20+
- **Zone-Radius:** 5 Blöcke
- **Zone-Check:** Jeden Tick (20 Updates/s)
- **Ballongeschwindigkeit:** 0.3 Blöcke pro Tick
- **Landing-Dauer:** 40 Ticks = 2 Sekunden
- **Update-Frequenz Ballons:** Alle 2 Ticks (40 Updates/s)

---

## Permissions 🔐

- `hotairballon.use` - Erlaubt, Ballons zu nutzen (default: true)
- `hotairballon.admin` - Admin-Rechte (default: op)

---

## Troubleshooting 🐛

**"Ich sehe keine Partikel"**
- Überprüfe mit `/ballon route list <name>`
- Starte die Route neu mit `/ballon route start`

**"Ich werde nicht aufgegriffen"**
- Gehe näher an die grüne Zone (innerhalb 5 Blöcke)
- Stelle sicher, dass Waypoint als Boarding aktiviert ist

**"Ich kann nicht aussteigen"**
- Der Ballon muss still halten (Landung/Pause)
- Waypoint muss als Dropoff aktiviert sein
- Gehe in die blaue Zone

**"Performance-Probleme"**
- Reduziere Ballonanzahl
- Vergrößere Abstände zwischen Waypoints

---

## Ideen für weitere Features 💡

- [ ] Config-Datei für Zone-Größe und Geschwindigkeit
- [ ] Persistente Routes (in JSON/YAML speichern)
- [ ] Waypoint-Namen statt nur Nummern
- [ ] Admin-Befehle zum Bearbeiten/Löschen
- [ ] Verschiedene Ballon-Designs
- [ ] Musik während Fahrt
- [ ] Größere oder kleinere Zonen pro Waypoint

---

**Dein flexibles Ballon-System ist ready!** 🎈✨
