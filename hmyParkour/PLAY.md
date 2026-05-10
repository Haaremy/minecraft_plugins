# hmyParkour — So spielst du

> Spielmodus: Parkour-Kurse mit Checkpoints, Bestzeiten und Leaderboard. Auf dem **kitsune**-Server. Admin-Setup → [README.md](README.md).

## Quick Start

1. **Hinkommen:** Lobby-Kompass → **Parkour** → du landest auf `kitsune` im Parkour-Hub.
2. **Kurs aussuchen:** `/pk list` zeigt alle Kurse mit Schwierigkeit, Bestzeit und Coin-Belohnung.
3. **Starten:** `/pk join <Kurs>` teleportiert dich an den Startpunkt — die Zeit läuft, sobald du dich bewegst.
4. **Checkpoints:** Lauf über Druckplatten — sie speichern automatisch deinen Fortschritt. Nach einem Sturz: `/pk cp` zurück zum letzten Checkpoint.
5. **Ziel:** Erreiche das Endfeld. Du bekommst **Coins** plus **Bonus bei neuer Bestzeit**.

## Spieler-Befehle

| Befehl | Was er tut |
|---|---|
| `/pk list` | Alle verfügbaren Kurse (Schwierigkeit, Bestzeit, Coins) |
| `/pk join <Kurs>` | Kurs starten |
| `/pk quit` | Aktuellen Kurs abbrechen |
| `/pk checkpoint` oder `/pk cp` | Zum letzten Checkpoint teleportieren |
| `/pk top <Kurs>` | Top-10 Bestzeiten für einen Kurs |

## Permissions (Spieler-Defaults)

Alle Spieler-Commands sind **ohne Permission-Gate**. Während eines Kurses bist du **schadensgeschützt** und kannst keine Blöcke beschädigen — Parkour ohne Crash-Risiko.

## Tipps

- **Schwierigkeit beachten** — Easy-Kurse für den Einstieg, Insane-Kurse erst, wenn dir die Sprünge in Fleisch und Blut übergehen.
- **Sprint-Jumps** sind länger als Standsprünge — du brauchst zwei Block Anlauf für maximale Distanz.
- **Sneak (Shift) am Rand** — verhindert das Runterrutschen von 1-Block-Plattformen.
- **Void = Sofort-Checkpoint:** Wenn du runterfällst, wirst du automatisch zurückgesetzt — kein Disconnect nötig.
- **Bestzeit jagen:** Coins gibt's bei jedem Run, aber **Bonus nur bei einer neuen persönlichen Bestzeit**. Optimiere die Linie.
- `/pk top <Kurs>` zeigt, wer der Schnellste ist — schau dir deren Strategie ab.

## How to play (English)

1. From the lobby compass open **Parkour** → connects you to `kitsune`.
2. `/pk list` shows all courses (difficulty, best time, coin reward).
3. `/pk join <course>` teleports you to the start. Timer starts when you move.
4. Pressure plates auto-save checkpoints. After a fall: `/pk cp` to return.
5. Reach the finish to earn **coins** + **bonus on a new personal best**.

All commands work for every player by default — damage and block protection are active during a run.
