# GAMES.md — Spieler-Guide für hmyCubed

> **(hmy)³ — the Haaremy Minecraft Server**
> Server-Adresse: `mc.haaremy.de` · Minecraft-Version: 1.21.x · Velocity-Netzwerk
>
> Diese Seite zeigt dir, **welche Spielmodi es gibt und wie du sie spielst**. Detail-Anleitungen je Spielmodus findest du in der jeweils verlinkten `PLAY.md`. Admin-/Setup-Doku liegt in der `README.md` neben dem Plugin.

---

## So kommst du in ein Spiel

1. Verbinde dich zu **`mc.haaremy.de`** — du landest automatisch in der **Lobby**.
2. In der Hotbar liegt ein **Kompass** (Slot 4). **Rechtsklick** öffnet das Spielmodi-Menü.
3. Klicke das gewünschte Icon — du wirst auf den passenden Server (`kitsune`, `survival`, …) verbunden.
4. Im Spielmodus joinst du das Match per `/duel join`, `/sumo join`, `/spleef join` etc. (siehe Tabelle unten).

Schneller Wechsel ohne Lobby-Umweg: **`/play <server>`**. Zurück in die Lobby: **`/lobby`**.

Mehr zum Lobby-Hotbar (Friends, Cosmetics, Settings): [Lobby-Plugin/PLAY.md](Lobby-Plugin/PLAY.md). Mehr zum Kompass: [hmyNavigator/PLAY.md](hmyNavigator/PLAY.md).

---

## Spielmodi-Übersicht

| Spielmodus | Worum geht's? | Join-Befehl | Belohnung | Anleitung |
|---|---|---|---|---|
| **1v1 Duelle** | Direkter PVP-Kampf in 5 Kits, Ranked oder Unranked | `/duel join [ranked\|unranked]` | 30 Coins (Win) / 5 Coins (Loss) + ELO im Ranked | [hmy1v1/PLAY.md](hmy1v1/PLAY.md) |
| **Sumo** | Best-of-3, schubse den Gegner mit der Schaufel in den Void | `/sumo join` | 25 Coins (Win) | [hmySumo/PLAY.md](hmySumo/PLAY.md) |
| **TNT Run** | 3 Ebenen, der Boden verschwindet — letzter Überlebender gewinnt | `/tntrun join` | 50 Coins (Win) | [hmyTNTRun/PLAY.md](hmyTNTRun/PLAY.md) |
| **Spleef** | Schlag dem Gegner den Schneeboden weg | `/spleef join` | 40 Coins (Win) + 5 Coins Teilnahme | [hmySpleef/PLAY.md](hmySpleef/PLAY.md) |
| **Parkour** | Kurse mit Checkpoints, Bestzeiten, Leaderboard | `/pk list` → `/pk join <Kurs>` | Coins pro Lauf + Bonus bei neuer Bestzeit | [hmyParkour/PLAY.md](hmyParkour/PLAY.md) |
| **Kitsune Segen** | Battle-Royale mit Crossbow-Loot in 5 Raritäten | (automatisch im Hub) | Letzter Überlebender gewinnt | [KitsuneSegen/PLAY.md](KitsuneSegen/PLAY.md) |

---

## Lobby- und Account-Features

| Feature | Wie du es nutzt | Anleitung |
|---|---|---|
| **Daily Rewards** | `/daily` — Streak halten lohnt sich | [hmyDailyRewards/PLAY.md](hmyDailyRewards/PLAY.md) |
| **Freunde** | Hotbar-Slot 1 (Spielerkopf) → Liste, Add, Follow | siehe README hmyVelocity |
| **DMs** | `/dm <Spieler> <Nachricht>` cross-server | siehe README hmyVelocity |
| **Cosmetics** | Hotbar-Slot 7 (Truhe) → Hüte, Effekte | [Lobby-Plugin/PLAY.md](Lobby-Plugin/PLAY.md) |
| **Sprache** | `/hmy language de` oder `/hmy language en` | global im Netzwerk |

---

## Coins & Progression

- **hmyCoins** sind die zentrale Netzwerk-Währung. Du verdienst sie durch:
  - Spiel-Siege (siehe Tabelle oben)
  - `/daily` (täglich, Streak-Bonus)
  - Teilnahme (Spleef gibt z. B. 5 Coins auch bei Niederlage)
- Coins sind **cross-server** — was du auf `kitsune` verdienst, kannst du überall ausgeben (geplanter Shop, Events).
- **ELO** existiert separat pro Spielmodus (1v1 Ranked, Sumo). Stats-Sidebar in der Lobby (Settings-Menü → an) zeigt sie live.

---

## Wichtige Server-Befehle

| Befehl | Was er tut | Wo |
|---|---|---|
| `/lobby` | Zurück in die Lobby | jeder Spielmodus-Server |
| `/play <server>` | Direkt zu einem Spielmodus | Lobby |
| `/spawn` | Zum Lobby-Spawn | Lobby |
| `/hmy coins` | Aktuelles Coin-Wallet anzeigen | überall |
| `/friend list` | Freunde online/offline | überall |
| `/dm <Spieler> <Text>` | Privatnachricht cross-server | überall |
| `/r <Text>` | Antwort auf letzte DM | überall |

---

## Englisch / English

This page is the **player-facing guide**. Each game mode has its own `PLAY.md` next to the source. Admin/setup is documented in the `README.md` of each plugin.

**Connect:** `mc.haaremy.de` (1.21.x) → spawns you in the lobby. Right-click the **compass** in hotbar slot 4 to open the game-mode menu. Inside any mode, the join command is in the table above. Use `/lobby` to return.

Currency is **hmyCoins** — earned by wins, daily login (`/daily`) and participation. Coins are cross-server. ELO is per game mode and shown in the lobby's stats sidebar (toggle via settings menu, slot 8).

To switch language any time: `/hmy language de` / `/hmy language en`.
