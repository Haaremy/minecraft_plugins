package de.haaremy.hmypaper.utils;

import java.util.List;
import java.util.Objects;

public class WorldSettings {
    private final List<String> disabledDamageTypes;
    private final List<String> allowedPlace;
    private final List<String> allowedBreak;

    public WorldSettings(List<String> disabledDamageTypes, List<String> allowedPlace, List<String> allowedBreak) {
        this.disabledDamageTypes = Objects.requireNonNull(disabledDamageTypes, "disabledDamageTypes darf nicht null sein");
        this.allowedPlace = Objects.requireNonNull(allowedPlace, "allowedPlace darf nicht null sein");
        this.allowedBreak = Objects.requireNonNull(allowedBreak, "allowedBreak darf nicht null sein");
    }

    public List<String> getDisabledDamageTypes() {
        return disabledDamageTypes;
    }

    public List<String> getAllowedPlace() {
        return allowedPlace;
    }

    public List<String> getAllowedBreak() {
        return allowedBreak;
    }

    @Override
    public String toString() {
        return "WorldSettings = {" +
                "disabledDamageTypes=" + disabledDamageTypes +
                ", allowedPlace=" + allowedPlace +
                ", allowedBreak=" + allowedBreak +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldSettings that = (WorldSettings) o;
        return disabledDamageTypes.equals(that.disabledDamageTypes) &&
                allowedPlace.equals(that.allowedPlace) &&
                allowedBreak.equals(that.allowedBreak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(disabledDamageTypes, allowedPlace, allowedBreak);
    }
}
