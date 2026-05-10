# KitsuneSegen — So spielst du

> Spielmodus: Battle-Royale mit Crossbow-Loot in 5 Raritäten. Auf dem **kitsune**-Server. Admin-Setup → [README.md](README.md).

## Quick Start

1. **Hinkommen:** Lobby-Kompass → **Kitsune Segen** → du landest in der **Hub-Welt** (Wartebereich).
2. **AGB akzeptieren (einmalig):** `/agb accept` — ohne Zustimmung kannst du nicht spielen.
3. **Warten:** Sobald genügend Spieler im Hub sind, startet der Countdown automatisch (60 s Standard).
4. **Spawn:** Du wirst per Zufall (`spawn-mode: random`) oder per Elytra-Sprung (`flight`) auf die Spielwelt verteilt.
5. **Looten + Kämpfen:** Öffne Truhen für Crossbows, Heiltränke und Schilde. Letzter Überlebender gewinnt — du wirst auf einem Platzierungs-Screen geehrt.
6. **Welt-Reset:** Nach jeder Runde wird die Spielwelt komplett aus Backup wiederhergestellt — keine bleibenden Schäden.

## Spieler-Befehle

| Befehl | Was er tut |
|---|---|
| `/agb accept` | AGB akzeptieren (einmalig pro Account, Pflicht zum Spielen) |

> Es gibt für Spieler **keine Join-/Leave-Befehle** — du nimmst automatisch teil, sobald du im Hub bist und das Spiel startet.

## Permissions (Spieler-Defaults)

| Permission | Default | Bedeutung für dich |
|---|---|---|
| `hmy.kitsunesegen.play` | `true` | Du darfst mitspielen |
| `hmy.kitsune.build` | `op` | Im Spiel **nicht** bauen/abbauen — Welt ist read-only |

Admin-Permissions stehen in der [README](README.md#berechtigungen).

## Crossbow-Raritäten

Es gibt 4 Crossbow-Kategorien × 5 Raritätsstufen (Gewöhnlich → Legendär):

- **Multishot** — feuert mehrere Pfeile gleichzeitig (gut gegen Gruppen)
- **Speedshot** — sehr hohe Cadence (gut gegen einzelne, beweglich)
- **Distanceshot** — Reichweiten-Sniper (positioniere dich erhöht)
- **Precisionshot** — extrem präzise (Headshots belohnt)

Höhere Raritäten = stärkere Stats, aber seltener im Loot.

## Tipps

- **Truhen zuerst** — ohne Crossbow bist du wehrlos. Lauf direkt zur nächsten Truhe.
- **Heiltränke aufheben** — sind die seltensten und entscheidendsten Items im Endgame.
- **Höhe = Vorteil** — Distanceshot von oben beherrscht weite Bereiche.
- **Schilde gegen Multishot** — der Schaden geht stark runter, wenn du sneakst und blockst.
- **Spectator-Mode nach Tod** — schau anderen über die Schulter, lerne deren Loot-Routen.
- **Welt darf nicht beschädigt werden** — du kannst keine Blöcke abbauen oder platzieren, das ist normal.

## How to play (English)

1. From the lobby compass open **Kitsune Segen** → connects you to `kitsune`, hub world first.
2. **First time:** `/agb accept` to accept the terms — required to play.
3. Wait in the hub. When enough players are present, a countdown starts (default 60 s).
4. You spawn into the game world (random teleport or elytra flight depending on config).
5. Loot chests for crossbows (4 categories × 5 rarities), healing/damage potions, and shields.
6. **Last player standing wins.** The world is reset from backup after each round — nothing you do persists.

You cannot break or place blocks during a match — the world is read-only.
