# hmyLobby — So nutzt du die Lobby

> Hub-Plugin: Hotbar, Cosmetics, Mini-Games, Ballons, Jukeboxen und Stats. Admin-Setup → [README.md](README.md).

## Quick Start

Sobald du die Lobby betrittst, hast du eine vorgefertigte **Hotbar** mit folgenden Slots:

| Slot | Item | Was es tut |
|---|---|---|
| 1 | **Spieler-Kopf (Friends)** | Rechtsklick öffnet die Freundesliste |
| 4 | **Kompass (Navigator)** | Rechtsklick öffnet das Spielmodi-Menü → siehe [hmyNavigator/PLAY.md](../hmyNavigator/PLAY.md) |
| 7 | **Truhe (Cosmetics)** | Rechtsklick öffnet das Kosmetik-Menü (Hüte, Ballons, Effekte) |
| 8 | **Kompass-Rose (Settings)** | Rechtsklick öffnet das Einstellungs-Menü |

## Was du in der Lobby tun kannst

### Cosmetics (Kosmetik)

- **Hüte und Effekte:** Über die Truhe in Slot 7 wählst du dein aktuelles Cosmetic. Dauerhaft sichtbar für andere Spieler.
- **Ballons (Balloons):** Manche Lobby-Bereiche haben automatische Ballon-Routen — stell dich auf eine Druckplatte oder fahr mit dem Aufzug nach oben.
- **Jukeboxen:** Verteilte Boxen in der Lobby spielen Streams via OpenAudioMc — Rechtsklick startet den Stream auf deinem Kopfhörer.

### Stats-Sidebar

- Per Settings-Menü (Slot 8) kannst du das **Stats-Scoreboard** auf der rechten Seite einblenden — zeigt deine Coins, ELO, Streak etc. live.
- Default ist **aus**, weil viele Spieler eine clean View bevorzugen.

### Mini-Games in der Lobby

- **TicTacToe-Felder:** Manche Bereiche haben TicTacToe-Boards — stell dich drauf, ein Gegner gegenüber, los gehts.

### Sprache

- Nutze `/hmy language de` oder `/hmy language en` (Velocity-Befehl) um deine Sprache umzuschalten — alle Lobby-Texte folgen automatisch.

## Spieler-Befehle (lobby-spezifisch)

Die meiste Interaktion läuft über **Rechtsklicks auf Hotbar-Items**, nicht über Befehle. Falls doch:

| Befehl | Was er tut |
|---|---|
| `/lobby` | Auf einem Spielmodus-Server: zurück in die Lobby (kommt vom Velocity-Plugin) |
| `/spawn` | Zum Lobby-Spawn teleportieren |

## Permissions (Spieler-Defaults)

| Permission | Default | Bedeutung |
|---|---|---|
| `hmy.lobby.selector` | `true` | Navigator-Kompass nutzen |
| `hmy.lobby.tp.*` | `true` | Lobby-Teleportpunkte (Kompass-Menü) |
| `hmy.lobby.balloon.use` | `true` | Auf Ballons / Aufzügen mitfahren |
| `hmy.lobby.friends` | `true` | Freunde-Hotbar-Item nutzen |
| `hmy.lobby.stats.on` | `false` | Stats-Sidebar (über Settings togglen) |
| `hmy.lobby.speed` | `true` | Laufgeschwindigkeit ändern (Settings-Menü) |

Admin-Permissions (`hmy.lobby.balloon.admin`, `hmy.lobby.jukebox.admin`) stehen in der [README](README.md).

## Tipps

- **Doppelter Rechtsklick auf Cosmetic = abnehmen.**
- **Friends-Hotbar zeigt online/offline farbig** — klick auf einen Online-Freund, um ihm direkt zu folgen.
- **Settings-Menü öfter checken** — neue Einstellungen kommen mit jedem Lobby-Update dazu.
- **Lobby-Spawn vergessen?** `/spawn` bringt dich immer zum Hauptbereich zurück.

## How to use (English)

The lobby uses a hotbar-driven UI:
- Slot 1 — friends list (right-click)
- Slot 4 — game-mode compass (right-click) → see [hmyNavigator/PLAY.md](../hmyNavigator/PLAY.md)
- Slot 7 — cosmetics chest (right-click)
- Slot 8 — settings menu (right-click)

Use `/hmy language de|en` to switch language. Use `/lobby` from any sub-server to return to the lobby.

All player permissions are enabled by default; opt-in features (stats sidebar) toggle via the settings menu.
