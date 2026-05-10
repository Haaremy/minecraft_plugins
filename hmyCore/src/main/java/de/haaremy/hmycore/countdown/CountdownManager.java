package de.haaremy.hmycore.countdown;

import de.haaremy.hmycore.HmyCore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CountdownManager {

    private final HmyCore plugin;
    private final Map<String, HmyCountdown> countdowns = new ConcurrentHashMap<>();

    public CountdownManager(HmyCore plugin) {
        this.plugin = plugin;
    }

    public HmyCountdown createCountdown(String id, int seconds) {
        cancelCountdown(id);
        HmyCountdown countdown = new HmyCountdown(seconds);
        countdowns.put(id, countdown);
        return countdown;
    }

    public HmyCountdown getCountdown(String id) {
        return countdowns.get(id);
    }

    public void cancelCountdown(String id) {
        HmyCountdown countdown = countdowns.remove(id);
        if (countdown != null) {
            countdown.cancel();
        }
    }

    public boolean isRunning(String id) {
        HmyCountdown countdown = countdowns.get(id);
        return countdown != null && countdown.isRunning();
    }

    public void cancelAll() {
        for (HmyCountdown countdown : countdowns.values()) {
            countdown.cancel();
        }
        countdowns.clear();
    }
}
