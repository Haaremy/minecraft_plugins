package de.haaremy.hmylobby.minigames;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class TicTacToeGame {

    public enum State { WAITING, CHOOSING, PLAYING, FINISHED }

    private static final Material P1_WOOL = Material.RED_WOOL;
    private static final Material P2_WOOL = Material.BLUE_WOOL;
    private static final Material EMPTY   = Material.WHITE_STAINED_GLASS;

    private final String fieldId;
    private final String name;
    private final Location[] cells         = new Location[9];
    private final Material[] originalBlocks = new Material[9];
    private final Material[] board          = new Material[9];

    private Player  player1;
    private Player  player2;
    private boolean singlePlayer = false;
    private boolean player1Turn  = true;
    private State   state        = State.WAITING;

    private final JavaPlugin plugin;

    public TicTacToeGame(String fieldId, String name, Location corner1, Location corner2, JavaPlugin plugin) {
        this.fieldId = fieldId;
        this.name    = name;
        this.plugin  = plugin;
        computeCells(corner1, corner2);
    }

    private void computeCells(Location c1, Location c2) {
        int minX = Math.min(c1.getBlockX(), c2.getBlockX());
        int minZ = Math.min(c1.getBlockZ(), c2.getBlockZ());
        int y    = c1.getBlockY();
        World world = c1.getWorld();

        int totalX = Math.abs(c2.getBlockX() - c1.getBlockX());
        int totalZ = Math.abs(c2.getBlockZ() - c1.getBlockZ());
        int stepX  = totalX > 0 ? totalX / 2 : 1;
        int stepZ  = totalZ > 0 ? totalZ / 2 : 1;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int idx = row * 3 + col;
                cells[idx]         = new Location(world, minX + col * stepX, y, minZ + row * stepZ);
                originalBlocks[idx] = cells[idx].getBlock().getType();
            }
        }
    }

    // ── Joining ───────────────────────────────────────────────────────────────

    public boolean join(Player player) {
        if (state == State.FINISHED) return false;

        if (player1 == null) {
            player1 = player;
            state   = State.CHOOSING;
            openModeMenu(player);
            return true;
        }
        if (!singlePlayer && player2 == null && !player.equals(player1)) {
            player2 = player;
            state   = State.PLAYING;
            startBoard();
            broadcastGameStart();
            return true;
        }
        return false;
    }

    private void openModeMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§6TicTacToe §8– §7Modus wählen"));
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        inv.setItem(11, createItem(Material.PLAYER_HEAD,    "§aSingleplayer", "§7Gegen den Computer"));
        inv.setItem(15, createItem(Material.PLAYER_HEAD,    "§eMultiplayer",  "§7Warte auf einen Mitspieler"));

        player.openInventory(inv);
    }

    public void chooseSinglePlayer() {
        singlePlayer = true;
        state        = State.PLAYING;
        startBoard();
        player1.sendMessage(Component.text("§6[TicTacToe] §7Singleplayer gestartet! §aDu bist §cROT§a."));
        player1.sendMessage(Component.text("§7Rechtsklicke auf ein Feld um zu spielen."));
    }

    public void chooseMultiplayer() {
        player1.sendMessage(Component.text("§6[TicTacToe] §7Warte auf einen Mitspieler..."));
        state = State.WAITING; // still waiting for player2
    }

    private void startBoard() {
        for (int i = 0; i < 9; i++) {
            board[i] = null;
            cells[i].getBlock().setType(EMPTY);
        }
    }

    private void broadcastGameStart() {
        player1.sendMessage(Component.text("§6[TicTacToe] §7Spiel gestartet! §aDu bist §c" + player1.getName() + " §8(§cROT§8)§a."));
        player2.sendMessage(Component.text("§6[TicTacToe] §7Spiel gestartet! §aDu bist §9" + player2.getName() + " §8(§9BLAU§8)§a."));
        notifyTurn();
    }

    // ── Gameplay ──────────────────────────────────────────────────────────────

    public boolean makeMove(Player player, int cellIdx) {
        if (state != State.PLAYING) return false;
        if (board[cellIdx] != null) {
            player.sendMessage(Component.text("§cDieses Feld ist bereits belegt!"));
            return false;
        }

        boolean isP1 = player.equals(player1);
        if (player1Turn != isP1) {
            player.sendMessage(Component.text("§cDu bist nicht dran!"));
            return false;
        }

        board[cellIdx] = player1Turn ? P1_WOOL : P2_WOOL;
        cells[cellIdx].getBlock().setType(board[cellIdx]);
        player1Turn = !player1Turn;

        Player winner = checkWinner();
        if (winner != null) {
            endGame(winner);
            return true;
        }
        if (isDraw()) {
            endGameDraw();
            return true;
        }

        notifyTurn();

        // AI move in single player
        if (singlePlayer && state == State.PLAYING) {
            Bukkit.getScheduler().runTaskLater(plugin, this::doAiMove, 20L);
        }
        return true;
    }

    private void doAiMove() {
        if (state != State.PLAYING) return;
        int idx = getBestAiMove();
        if (idx == -1) return;

        board[idx] = P2_WOOL;
        cells[idx].getBlock().setType(P2_WOOL);
        player1Turn = true;

        Player winner = checkWinner();
        if (winner != null) { endGame(winner); return; }
        if (isDraw())       { endGameDraw();   return; }
        notifyTurn();
    }

    // Simple AI: win > block > center > random
    private int getBestAiMove() {
        int[][] lines = {{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};

        // Try to win
        for (int[] line : lines) {
            int empty = -1; int aiCount = 0;
            for (int i : line) {
                if (board[i] == P2_WOOL)  aiCount++;
                else if (board[i] == null) empty = i;
            }
            if (aiCount == 2 && empty != -1) return empty;
        }
        // Try to block player
        for (int[] line : lines) {
            int empty = -1; int p1Count = 0;
            for (int i : line) {
                if (board[i] == P1_WOOL)  p1Count++;
                else if (board[i] == null) empty = i;
            }
            if (p1Count == 2 && empty != -1) return empty;
        }
        // Center
        if (board[4] == null) return 4;
        // Random free cell
        int[] free = java.util.stream.IntStream.range(0, 9).filter(i -> board[i] == null).toArray();
        if (free.length == 0) return -1;
        return free[new Random().nextInt(free.length)];
    }

    private void notifyTurn() {
        Player current = player1Turn ? player1 : (singlePlayer ? null : player2);
        if (current == null) return;
        current.sendMessage(Component.text("§6[TicTacToe] §aDu bist dran! Rechtsklicke auf ein Feld."));
        if (!singlePlayer) {
            Player other = player1Turn ? player2 : player1;
            if (other != null) other.sendMessage(Component.text("§6[TicTacToe] §7Warte auf " + current.getName() + "..."));
        }
    }

    // ── Win Detection ─────────────────────────────────────────────────────────

    public Player checkWinner() {
        int[][] lines = {{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};
        for (int[] line : lines) {
            Material m = board[line[0]];
            if (m != null && m == board[line[1]] && m == board[line[2]]) {
                return (m == P1_WOOL) ? player1 : (singlePlayer ? null : player2);
            }
        }
        return null;
    }

    public boolean isDraw() {
        for (Material m : board) if (m == null) return false;
        return true;
    }

    private void endGame(Player winner) {
        state = State.FINISHED;
        String winnerName = (winner != null) ? winner.getName() : "Computer";

        if (player1 != null) player1.sendMessage(Component.text(
                winner != null && winner.equals(player1) ? "§6[TicTacToe] §aGlückwunsch! Du hast gewonnen! 🎉"
                                                         : "§6[TicTacToe] §c" + winnerName + " hat gewonnen!"));
        if (!singlePlayer && player2 != null) player2.sendMessage(Component.text(
                winner != null && winner.equals(player2) ? "§6[TicTacToe] §aGlückwunsch! Du hast gewonnen! 🎉"
                                                         : "§6[TicTacToe] §c" + winnerName + " hat gewonnen!"));

        if (winner != null) spawnFireworks(winner.getLocation());

        scheduleReset();
    }

    private void endGameDraw() {
        state = State.FINISHED;
        if (player1 != null) player1.sendMessage(Component.text("§6[TicTacToe] §eUnentschieden!"));
        if (!singlePlayer && player2 != null) player2.sendMessage(Component.text("§6[TicTacToe] §eUnentschieden!"));
        scheduleReset();
    }

    private void spawnFireworks(Location loc) {
        for (int i = 0; i < 3; i++) {
            final int delay = i * 15;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Firework fw = loc.getWorld().spawn(loc, Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BURST)
                        .withColor(Color.YELLOW, Color.ORANGE)
                        .withFade(Color.WHITE)
                        .withFlicker().build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
            }, delay);
        }
    }

    private void scheduleReset() {
        Bukkit.getScheduler().runTaskLater(plugin, this::reset, 100L); // 5 Sekunden
    }

    public void reset() {
        for (int i = 0; i < 9; i++) {
            cells[i].getBlock().setType(originalBlocks[i]);
            board[i] = null;
        }
        player1     = null;
        player2     = null;
        singlePlayer = false;
        player1Turn  = true;
        state        = State.WAITING;
    }

    // ── Cell lookup ───────────────────────────────────────────────────────────

    public int getCellIndex(Location loc) {
        for (int i = 0; i < cells.length; i++) {
            Location c = cells[i];
            if (c.getBlockX() == loc.getBlockX()
                    && c.getBlockY() == loc.getBlockY()
                    && c.getBlockZ() == loc.getBlockZ()) return i;
        }
        return -1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ItemStack createItem(Material m, String name, String... lore) {
        ItemStack item = new ItemStack(m);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name));
            if (lore.length > 0)
                meta.lore(java.util.Arrays.stream(lore).map(Component::text).toList());
        });
        return item;
    }

    public State  getState()   { return state; }
    public String getName()    { return name; }
    public String getFieldId() { return fieldId; }
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }

    public String getStatusText() {
        return switch (state) {
            case WAITING  -> "§aFrei";
            case CHOOSING -> "§eWarten auf Modus-Wahl";
            case PLAYING  -> player2 == null ? "§eWarten auf Gegner" : "§cBesetzt";
            case FINISHED -> "§7Reset...";
        };
    }
}
