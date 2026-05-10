package de.haaremy.hmyparkour;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum ParkourDifficulty {

    EASY("Einfach", NamedTextColor.GREEN, 10),
    MEDIUM("Mittel", NamedTextColor.YELLOW, 25),
    HARD("Schwer", NamedTextColor.RED, 50),
    EXPERT("Experte", NamedTextColor.DARK_RED, 100);

    private final String displayName;
    private final TextColor color;
    private final int coinReward;

    ParkourDifficulty(String displayName, TextColor color, int coinReward) {
        this.displayName = displayName;
        this.color = color;
        this.coinReward = coinReward;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TextColor getColor() {
        return color;
    }

    public int getCoinReward() {
        return coinReward;
    }

    public String getMiniMessageColor() {
        return switch (this) {
            case EASY -> "<green>";
            case MEDIUM -> "<yellow>";
            case HARD -> "<red>";
            case EXPERT -> "<dark_red>";
        };
    }
}
