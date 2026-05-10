package de.haaremy.hmycore.countdown;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.lang.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Collection;
import java.util.function.Consumer;

public class HmyCountdown {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final int totalSeconds;
    private int remainingSeconds;
    private Consumer<Integer> onTick;
    private Runnable onFinish;
    private BukkitTask task;
    private boolean running;
    private Collection<? extends Player> players;

    public HmyCountdown(int seconds) {
        this.totalSeconds = seconds;
        this.remainingSeconds = seconds;
        this.running = false;
    }

    public HmyCountdown onTick(Consumer<Integer> onTick) {
        this.onTick = onTick;
        return this;
    }

    public HmyCountdown onFinish(Runnable onFinish) {
        this.onFinish = onFinish;
        return this;
    }

    public HmyCountdown forPlayers(Collection<? extends Player> players) {
        this.players = players;
        return this;
    }

    public void start() {
        if (running) return;
        running = true;
        remainingSeconds = totalSeconds;

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (remainingSeconds <= 0) {
                    running = false;
                    cancel();

                    // GO! Nachricht
                    if (players != null) {
                        Component goTitle = MINI.deserialize("<green><bold>GO!");
                        Title title = Title.title(goTitle, Component.empty(),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(200)));
                        for (Player player : players) {
                            if (player.isOnline()) {
                                player.showTitle(title);
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                            }
                        }
                    }

                    if (onFinish != null) {
                        onFinish.run();
                    }
                    return;
                }

                if (onTick != null) {
                    onTick.accept(remainingSeconds);
                }

                // Title/Sound fuer 3, 2, 1
                if (remainingSeconds <= 3 && players != null) {
                    String color = switch (remainingSeconds) {
                        case 3 -> "<yellow>";
                        case 2 -> "<gold>";
                        case 1 -> "<red>";
                        default -> "<white>";
                    };
                    Component countTitle = MINI.deserialize(color + "<bold>" + remainingSeconds);
                    Title title = Title.title(countTitle, Component.empty(),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(800), Duration.ofMillis(200)));

                    for (Player player : players) {
                        if (player.isOnline()) {
                            player.showTitle(title);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                }

                // Actionbar fuer alle Sekunden (per-Spieler-Locale)
                if (players != null) {
                    String secondsStr = String.valueOf(remainingSeconds);
                    for (Player player : players) {
                        if (player.isOnline()) {
                            player.sendActionBar(Lang.component(player, "core.countdown.actionbar",
                                    "seconds", secondsStr));
                        }
                    }
                }

                remainingSeconds--;
            }
        }.runTaskTimer(HmyCore.getInstance(), 0L, 20L);
    }

    public void cancel() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }
}
