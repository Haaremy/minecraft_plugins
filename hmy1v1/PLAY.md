# hmy1v1 — So spielst du

> Spielmodus: 1v1-Duelle (Ranked & Unranked) auf dem **kitsune**-Server.
> Diese Seite richtet sich an Spieler. Admin-Setup → [README.md](README.md).

## Quick Start

1. **Hinkommen:** In der Lobby den Kompass (Navigator) öffnen → **Duelle** wählen → du landest auf `kitsune`.
2. **Modus wählen:** `/duel join unranked` (locker) oder `/duel join ranked` (mit ELO).
3. **Kit wählen:** `/duel kit` öffnet das Kit-Menü (Iron, Diamond, Archer, Soup, Classic).
4. **Match:** Sobald ein Gegner gefunden ist, geht es los. Wer die Lebenspunkte des Gegners auf 0 bringt, gewinnt.
5. **Belohnung:** 30 Coins für den Sieg, 5 Coins für die Niederlage. Im Ranked-Modus zusätzlich ELO-Änderung.

## Spieler-Befehle

| Befehl | Was er tut |
|---|---|
| `/duel <Spieler>` | Direkte Herausforderung (30 s Timeout) |
| `/duel accept` / `/duel deny` | Herausforderung annehmen / ablehnen |
| `/duel join [ranked\|unranked]` | Warteschlange beitreten (Default: unranked) |
| `/duel leave` | Warteschlange, Match oder Spectator verlassen |
| `/duel kit` | Kit-Auswahl-GUI öffnen |
| `/duel stats [Spieler]` | ELO, Siege, Niederlagen, Winrate anzeigen |
| `/duel top` | Top-10 ELO-Rangliste |
| `/duel spectate <Spieler>` | Einem laufenden Match zuschauen |

## Permissions (Spieler-Defaults)

Alle oben gelisteten Spieler-Commands sind **ohne Permission-Gate** für jeden Spieler im Netzwerk nutzbar. Admin-Setup-Permissions stehen in der [README](README.md#berechtigungen).

## Tipps

- **Iron-Kit** ist Anfänger-freundlich (Vollrüstung, Schwert, Heiltränke).
- **Soup-Kit** ist klassischer PVP-Style — Suppe per Rechtsklick = Sofortheal.
- **Archer-Kit** belohnt Ziel-Genauigkeit; halte Distanz.
- **Ranked erst, wenn dein Kit sitzt** — Niederlagen senken deine ELO sichtbar.
- Im Spectator-Modus lernst du Strategie der Top-Spieler — `/duel spectate <name>`.

## How to play (English)

1. From the lobby compass open **Duels** → connects you to `kitsune`.
2. `/duel join unranked` for casual, `/duel join ranked` for ELO.
3. `/duel kit` to pick from Iron / Diamond / Archer / Soup / Classic.
4. Reduce your opponent's HP to 0 to win.
5. Rewards: **30 coins** for a win, **5 coins** for a loss. Ranked also adjusts ELO.

All commands work for every player by default — no permission required.
